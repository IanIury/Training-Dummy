package net.ian.trainingdummy.entity

import net.ian.trainingdummy.entity.animation.DummyHitAnimationState
import net.ian.trainingdummy.entity.animation.DummyMaceAnimationState
import net.ian.trainingdummy.entity.part.DummyPartEntity
import net.ian.trainingdummy.event.utils.DamageData
import net.ian.trainingdummy.init.components.ModDataComponents
import net.ian.trainingdummy.init.components.TrainingModuleConfig
import net.ian.trainingdummy.init.enums.TrainingModos
import net.ian.trainingdummy.init.enums.WindowShapeType
import net.ian.trainingdummy.rank.DummyRankData
import net.ian.trainingdummy.init.serializers.ModDataSerializers
import net.ian.trainingdummy.item.ModItems
import net.ian.trainingdummy.item.custom.DisplayDummyBlockItem
import net.ian.trainingdummy.item.custom.TrainingModuleItem
import net.ian.trainingdummy.screen.custom.DummyMenu
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.AnimationState
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemContainerContents
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.SkullBlockEntity
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.items.ItemStackHandler
import java.util.Optional
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

open class DummyEntity(
    type: EntityType<out Mob>,
    level: Level
) : Mob(type, level) {

    // ========================================================================
    // PARTES E SUB-ENTIDADES
    // ========================================================================


    val rightArmPart = DummyPartEntity(this, DummyPartEntity.Companion.DummyPartType.RightArm, 0.15f, 0.75f)
    val leftArmPart = DummyPartEntity(this, DummyPartEntity.Companion.DummyPartType.LeftArm, 0.15f, 0.75f)
    val basePart = DummyPartEntity(this, DummyPartEntity.Companion.DummyPartType.Base, 1.0f, 0.085f)

    val subEntities = arrayOf(this.rightArmPart, this.leftArmPart, this.basePart)

    // ========================================================================
    // ESTADOS DE ANIMAÇÃO E DADOS LOCAIS
    // ========================================================================

    val hitAnimation = DummyHitAnimationState(this)
    val maceAnimation = DummyMaceAnimationState(this)
    val spawnAnimation = AnimationState()

    var damageDataOld : DamageData
        get() = entityData.get(DAMAGE_DATA_OLD)
        set(value) = entityData.set(DAMAGE_DATA_OLD, value)

    var damageData: DamageData
        get() = entityData.get(DAMAGE_DATA)
        set(value) = entityData.set(DAMAGE_DATA, value)

    var detailedDamageWindow: Boolean
        get() = entityData.get(DETAILED_DAMAGE_WINDOW)
        set(value) = entityData.set(DETAILED_DAMAGE_WINDOW, value)

    /** Formato geométrico da janela flutuante (sincronizado com o cliente e salvo no NBT). */
    var windowShape: WindowShapeType
        get() = WindowShapeType.byId(entityData.get(WINDOW_SHAPE))
        set(value) = entityData.set(WINDOW_SHAPE, value.ordinal)

    var preDamageData: DamageData = DamageData()

    // Último jogador que atacou (preenchido no servidor antes de onCustomDamageReceived) e dono do teste de DPS atual
    var lastAttackerUuid: UUID? = null
    var lastAttackerName: String? = null
    private var dpsOwnerUuid: UUID? = null
    private var dpsOwnerName: String? = null

    var damageStack : ItemStack
        get() = entityData.get(DAMAGE_ITEMSTACK)
        set(value) = entityData.set(DAMAGE_ITEMSTACK,value)

    var damageStackOld : ItemStack
        get() = entityData.get(DAMAGE_ITEMSTACK_OLD)
        set(value) = entityData.set(DAMAGE_ITEMSTACK_OLD,value)

    var displayTicks: Int
        get() = entityData.get(DISPLAY_TICKS)
        set(value) = entityData.set(DISPLAY_TICKS, value)

    var dpsTestState: Int
        get() = entityData.get(DPS_TEST_STATE)
        private set(value) = entityData.set(DPS_TEST_STATE, value)

    var dpsTicksRemaining: Int
        get() = entityData.get(DPS_TICKS_REMAINING)
        private set(value) = entityData.set(DPS_TICKS_REMAINING, value)

    var dpsTotalDamage: Float
        get() = entityData.get(DPS_TOTAL_DAMAGE)
        private set(value) = entityData.set(DPS_TOTAL_DAMAGE, value)

    var dpsHitCount: Int
        get() = entityData.get(DPS_HIT_COUNT)
        private set(value) = entityData.set(DPS_HIT_COUNT, value)

    var dpsHighestHit: Float
        get() = entityData.get(DPS_HIGHEST_HIT)
        private set(value) = entityData.set(DPS_HIGHEST_HIT, value)

    // ========================================================================
    // INICIALIZAÇÃO E REGISTRO DE DADOS
    // ========================================================================

    override fun setId(id: Int) {
        super.setId(id)
        this.rightArmPart.id = id + 1
        this.leftArmPart.id = id + 2
        this.basePart.id = id + 3
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(DISPLAY_TICKS, 0)
        builder.define(DAMAGE_DATA, DamageData())
        builder.define(DAMAGE_DATA_OLD, DamageData())
        builder.define(MACE_DAMAGE, 0.0f)
        builder.define(MACE_HIT_TRIGGER, 0)
        builder.define(HIT_PITCH, 0.0f)
        builder.define(HIT_ROLL, 0.0f)
        builder.define(HIT_TRIGGER, 0)

        builder.define(OPTIONAL_PROFILE, Optional.empty())
        builder.define(TRAINING_CONFIG_DATA, TrainingModuleConfig())

        builder.define(DAMAGE_ITEMSTACK, ItemStack.EMPTY)
        builder.define(DAMAGE_ITEMSTACK_OLD, ItemStack.EMPTY)
        builder.define(DPS_TEST_STATE, DPS_STATE_READY)
        builder.define(DPS_TICKS_REMAINING, 0)
        builder.define(DPS_TOTAL_DAMAGE, 0f)
        builder.define(DPS_HIT_COUNT, 0)
        builder.define(DPS_HIGHEST_HIT, 0f)
        builder.define(DETAILED_DAMAGE_WINDOW, false)
        builder.define(WINDOW_SHAPE, WindowShapeType.DEFAULT.ordinal)

    }



    // ========================================================================
    // COMPORTAMENTO, MOVIMENTO E FÍSICA
    // ========================================================================

    override fun registerGoals() {}
    override fun removeWhenFarAway(distanceToClosestPlayer: Double): Boolean = false
    override fun requiresCustomPersistence(): Boolean = true
    override fun isPushable(): Boolean = false
    override fun push(x: Double, y: Double, z: Double) {}
    override fun doPush(entity: Entity) {}
    override fun pushEntities() {}
    override fun knockback(strength: Double, x: Double, z: Double) {}

    // ========================================================================
    // DANO E EVENTOS
    // ========================================================================

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        if (isInvulnerableTo(source) || level().isClientSide) {
            return false
        }
        val wasHurt = super.hurt(source, amount)
        if (wasHurt) {

            if (source.`is`(DamageTypes.FELL_OUT_OF_WORLD) || source.`is`(DamageTypes.GENERIC_KILL)) {
                return true
            }

            this.displayTicks = 160
            this.health = this.maxHealth
        }
        return wasHurt
    }

    override fun onSyncedDataUpdated(key: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(key)

        if (level().isClientSide) {
            when (key) {
                HIT_TRIGGER -> {
                    hitAnimation.isNewHitRequested = true
                    hitAnimation.start(tickCount)
                }
                MACE_HIT_TRIGGER -> {
                    val maceAmount = maceAnimation.maceDamage
                    if (maceAmount > 0.0f) {
                        val escalaCalculada = Mth.clamp(1.0f - (maceAmount / 100f), 0.1f, 1.0f)
                        maceAnimation.maceScaleSizeSmooth = escalaCalculada >= maceAnimation.maceScaleSize
                        if (!maceAnimation.maceScaleSizeSmooth) {
                            maceAnimation.maceScaleSize = escalaCalculada
                        }
                    } else {
                        maceAnimation.maceScaleSizeSmooth = true
                    }
                    maceAnimation.start(tickCount)
                }
            }
        }

    }

    override fun handleEntityEvent(id: Byte) {
        if (id.toInt() == 42) {
            spawnAnimation.start(tickCount)
        } else {
            super.handleEntityEvent(id)
        }
    }

    override fun getHurtSound(damageSource: DamageSource): SoundEvent = SoundEvents.ARMOR_STAND_HIT
    override fun getDeathSound(): SoundEvent = SoundEvents.ARMOR_STAND_BREAK

    override fun remove(reason: Entity.RemovalReason) {
        // Se a entidade está sendo descartada por morte, dropa qualquer item residual que tenha sobrado nos slots
        if (reason == Entity.RemovalReason.KILLED && !level().isClientSide) {
            for (slot in EquipmentSlot.entries) {
                val stack = getItemBySlot(slot)
                if (!stack.isEmpty) {
                    spawnAtLocation(stack)
                    setItemSlot(slot, ItemStack.EMPTY)
                }
            }
        }
        super.remove(reason)
    }

    // ========================================================================
    // HITBOXES MULTIPART E POSICIONAMENTO
    // ========================================================================

    override fun isMultipartEntity(): Boolean = true

    override fun getParts(): Array<DummyPartEntity> {
        return arrayOf(rightArmPart, leftArmPart, basePart)
    }

    private fun updateArmPartPositions() {
        val rad = Math.toRadians((-this.yRot - 90.0f).toDouble())
        val cos = cos(rad)
        val sin = sin(rad)

        val offsetSide = 0.39
        val armBaseY = this.y + 0.75

        val rightX = this.x + (offsetSide * sin)
        val rightZ = this.z + (offsetSide * cos)
        val leftX = this.x - (offsetSide * sin)
        val leftZ = this.z - (offsetSide * cos)

        this.rightArmPart.setPos(rightX, armBaseY, rightZ)
        this.leftArmPart.setPos(leftX, armBaseY, leftZ)
        this.basePart.setPos(this.x, this.y, this.z)

        this.rightArmPart.xo = rightX; this.rightArmPart.yo = armBaseY; this.rightArmPart.zo = rightZ
        this.rightArmPart.xOld = rightX; this.rightArmPart.yOld = armBaseY; this.rightArmPart.zOld = rightZ

        this.leftArmPart.xo = leftX; this.leftArmPart.yo = armBaseY; this.leftArmPart.zo = leftZ
        this.leftArmPart.xOld = leftX; this.leftArmPart.yOld = armBaseY; this.leftArmPart.zOld = leftZ

        this.basePart.xo = this.x; this.basePart.yo = this.y; this.basePart.zo = this.z
        this.basePart.xOld = this.x; this.basePart.yOld = this.y; this.basePart.zOld = this.z
    }

    // ========================================================================
    // CRIAÇÃO DE ITEM & PERSISTÊNCIA EM ITEMSTACK
    // ========================================================================


    val dummyInventory = object : ItemStackHandler(7) {
        override fun onContentsChanged(slot: Int) {
            super.onContentsChanged(slot)

            val stack = getStackInSlot(slot)

            // Sincroniza o inventário da GUI com os slots nativos de equipamento da Entidade
            equipmentSlotForInventorySlot(slot)?.let { setItemSlot(it, stack) }

            if (slot == TRAINING_MODULE_SLOT && !level().isClientSide) {
                syncTrainingModuleConfig(stack)
            }
        }

        override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
            return when (slot) {
                HEAD_INVENTORY_SLOT -> stack.canEquip(EquipmentSlot.HEAD, this@DummyEntity)
                CHEST_INVENTORY_SLOT -> stack.canEquip(EquipmentSlot.CHEST, this@DummyEntity)
                LEGS_INVENTORY_SLOT -> stack.canEquip(EquipmentSlot.LEGS, this@DummyEntity)
                FEET_INVENTORY_SLOT -> stack.canEquip(EquipmentSlot.FEET, this@DummyEntity)
                TRAINING_MODULE_SLOT -> stack.`is`(ModItems.TRAINING_MODULE_ITEM)
                MAIN_HAND_INVENTORY_SLOT, OFF_HAND_INVENTORY_SLOT -> true
                else -> false
            }
        }

        override fun getSlotLimit(slot: Int): Int =
            if (slot == MAIN_HAND_INVENTORY_SLOT || slot == OFF_HAND_INVENTORY_SLOT) 64 else 1


    }

    override fun getPickResult(): ItemStack? = createItemFromDummy()

    private fun setEquipmentAndSync(slot: EquipmentSlot, stack: ItemStack) {
        val invSlot = when (slot) {
            EquipmentSlot.HEAD -> HEAD_INVENTORY_SLOT
            EquipmentSlot.CHEST -> CHEST_INVENTORY_SLOT
            EquipmentSlot.LEGS -> LEGS_INVENTORY_SLOT
            EquipmentSlot.FEET -> FEET_INVENTORY_SLOT
            EquipmentSlot.MAINHAND -> MAIN_HAND_INVENTORY_SLOT
            EquipmentSlot.OFFHAND -> OFF_HAND_INVENTORY_SLOT
            else -> -1
        }

        if (invSlot != -1) {
            // Isso engatilha o onContentsChanged automaticamente, atualizando o visual
            dummyInventory.setStackInSlot(invSlot, stack)
        } else {
            setItemSlot(slot, stack)
        }
    }

    /** Converte o Dummy atual em um item com inventário, perfil e nome. */
    fun createItemFromDummy(): ItemStack {
        val itemStack = ItemStack(ModItems.DUMMY_ITEM_SPAWN)

        val itemsList = NonNullList.withSize(dummyInventory.slots, ItemStack.EMPTY)
        for (i in 0 until dummyInventory.slots) {
            itemsList[i] = dummyInventory.getStackInSlot(i)
        }

        itemStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(itemsList))
        itemStack.set(DataComponents.PROFILE,profilePlayer)
        itemStack.set(DataComponents.CUSTOM_NAME, this.customName)

        return itemStack
    }

    fun applyCompoundTagsFromItem(stack: ItemStack) {
        stack.get(DataComponents.PROFILE)?.let { this.profilePlayer = it }
        stack.get(DataComponents.CUSTOM_NAME)?.let { super.customName = it }
    }

    fun applyCustomName(name: Component?) {
        super.customName = name
    }

    // ========================================================================
    // INTERAÇÕES E EQUIPAMENTOS
    // ========================================================================

    fun updateDummyEquipmentUsingAnItem(stack: ItemStack) {
        val containerContents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
        val itemsList = NonNullList.withSize(this.dummyInventory.slots, ItemStack.EMPTY)
        containerContents.copyInto(itemsList)
        for (i in 0 until this.dummyInventory.slots) {
            this.dummyInventory.setStackInSlot(i, itemsList.getOrElse(i) { ItemStack.EMPTY })
        }
    }

    override fun dropCustomDeathLoot(serverLevel: ServerLevel, source: DamageSource, p_21387_: Boolean) {
        val dummyStack = createItemFromDummy()
        for (slot in 0 until dummyInventory.slots) {
            dummyInventory.setStackInSlot(slot, ItemStack.EMPTY)
        }
        spawnAtLocation(dummyStack)
    }

    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {

        if (!this.isAlive) {
            return InteractionResult.PASS
        }

        val heldItem = player.getItemInHand(hand)

        return InteractionResult.PASS
    }

    override fun interactAt(player: Player, vec: Vec3, hand: InteractionHand): InteractionResult {

        if (!this.isAlive) {
            return InteractionResult.PASS
        }

        val heldItem = player.getItemInHand(hand)

        if (heldItem.`is`(ModItems.DUMMY_ITEM_SPAWN) || heldItem.item is DisplayDummyBlockItem) {
            return InteractionResult.PASS
        }

        if (!heldItem.isEmpty && (heldItem.getUseDuration(player) > 0 || heldItem.`is`(Items.SPLASH_POTION) || heldItem.`is`(Items.LINGERING_POTION))) {
            return InteractionResult.PASS
        }

        if (heldItem.`is`(Items.NAME_TAG)) return InteractionResult.PASS
        if (player.isSpectator) return InteractionResult.SUCCESS
        if (level().isClientSide) return InteractionResult.CONSUME

        val relativeY = vec.y / this.bbHeight.toDouble()
        val slotFromY = when {
            relativeY >= 0.80 -> EquipmentSlot.HEAD
            relativeY >= 0.40 -> EquipmentSlot.CHEST
            relativeY >= 0.20 -> EquipmentSlot.LEGS
            else -> EquipmentSlot.FEET
        }

        val itemEquipSlot = getEquipmentSlotForItem(heldItem)
        val targetSlot = if (itemEquipSlot != null && itemEquipSlot.isArmor) itemEquipSlot else slotFromY

        if (swapItem(player, targetSlot, heldItem, hand)) {
            return InteractionResult.SUCCESS
        }

        return InteractionResult.PASS
    }

    override fun setCustomName(name: Component?) {
        super.setCustomName(name)
        name?.also {
            val nameText = it.string
            if (
                nameText.equals("jeb_", ignoreCase = true) ||
                nameText.equals("-endGateway-", ignoreCase = true) ||
                nameText.equals("-entityGlint-", ignoreCase = true) ||
                nameText.equals("-translucent-", ignoreCase = true)
            ) {
                profilePlayer = null
            } else {
                setSkinByUsername(nameText)
            }
        }
            ?: run { profilePlayer = null }
    }

    fun interactWithHandSlot(player: Player, hand: InteractionHand, slot: EquipmentSlot): InteractionResult {
        if (player.isSpectator) return InteractionResult.SUCCESS
        if (level().isClientSide) return InteractionResult.CONSUME

        if (swapItem(player, slot, player.getItemInHand(hand), hand)) {
            return InteractionResult.SUCCESS
        }

        return InteractionResult.PASS
    }

    fun interactBaseDummy(player: Player, hand: InteractionHand) : InteractionResult {

        if (level().isClientSide) {
            return InteractionResult.sidedSuccess(true)
        }

        if (!this.isAlive) return InteractionResult.PASS

        if (player.isShiftKeyDown) {
            if (!level().isClientSide) {
                dropDummyAsItem(player)
            }
            return InteractionResult.sidedSuccess(level().isClientSide)
        }

        if (!level().isClientSide) {
            if (player is ServerPlayer) {
                player.openMenu(
                    SimpleMenuProvider(
                        { containerId, playerInventory, _ ->
                            DummyMenu(containerId, playerInventory, this)
                        },
                        Component.empty() // Passa um componente vazio
                    )
                ) { buffer ->
                    buffer.writeInt(this.id)
                }
            }
        }

        return InteractionResult.sidedSuccess(level().isClientSide)
    }

    fun dropDummyAsItem(player: Player) {
        if (!level().isClientSide) {
            val dummyStack = createItemFromDummy()
            if (!player.inventory.add(dummyStack)) {
                player.drop(dummyStack, false)
            }
            discard()
        }
    }

    private fun swapItem(player: Player, slot: EquipmentSlot, playerStack: ItemStack, hand: InteractionHand): Boolean {
        val dummyStack = getItemBySlot(slot)

        if (playerStack.isEmpty && dummyStack.isEmpty) return false

        if (slot.isArmor && !playerStack.isEmpty) {
            val itemSlot = getEquipmentSlotForItem(playerStack)
            if (itemSlot != slot) return false
        }

        if (player.hasInfiniteMaterials() && dummyStack.isEmpty && !playerStack.isEmpty) {
            setEquipmentAndSync(slot, playerStack.copyWithCount(1)) // SUBSTITUÍDO
            return true
        }

        if (!playerStack.isEmpty && playerStack.count > 1) {
            if (!dummyStack.isEmpty) return false
            val singleItem = playerStack.split(1)
            setEquipmentAndSync(slot, singleItem) // SUBSTITUÍDO
            return true
        }

        setEquipmentAndSync(slot, playerStack) // SUBSTITUÍDO
        player.setItemInHand(hand, dummyStack)
        return true
    }

    // ========================================================================
    // VARIÁVEIS DE CONTROLE DE MODO
    // ========================================================================

    var trainingModuleConfig : TrainingModuleConfig
        get() {
            if (this.level().isClientSide) {
                return this.entityData.get(TRAINING_CONFIG_DATA)
            }
            val moduleStack = dummyInventory.getStackInSlot(TRAINING_MODULE_SLOT)
            return moduleStack.get(ModDataComponents.TRAINING_MODULE_CONFIG)
                ?: TrainingModuleConfig(
                    TrainingModuleItem.getItemTrainingMode(moduleStack) ?: TrainingModos.DEFAULT
                )
        }
        set(value) {
            val stack = dummyInventory.getStackInSlot(TRAINING_MODULE_SLOT).copy()
            if (!stack.`is`(ModItems.TRAINING_MODULE_ITEM)) {
                if (!this.level().isClientSide) {
                    this.entityData.set(TRAINING_CONFIG_DATA, TrainingModuleConfig())
                }
                return
            }
            stack.set(ModDataComponents.TRAINING_MODULE_CONFIG,value)
            dummyInventory.setStackInSlot(TRAINING_MODULE_SLOT,stack)
            if (!this.level().isClientSide) {
                this.entityData.set(TRAINING_CONFIG_DATA, value)
            }
        }

    private fun syncTrainingModuleConfig(moduleStack: ItemStack) {
        val config = if (moduleStack.`is`(ModItems.TRAINING_MODULE_ITEM)) {
            moduleStack.get(ModDataComponents.TRAINING_MODULE_CONFIG)
                ?: TrainingModuleConfig(
                    TrainingModuleItem.getItemTrainingMode(moduleStack) ?: TrainingModos.DEFAULT
                )
        } else {
            TrainingModuleConfig()
        }
        entityData.set(TRAINING_CONFIG_DATA, config)
    }

    fun onCustomDamageReceived(incomingData: DamageData) {
        val modo = trainingModuleConfig.trainingModo
        when (modo) {
            TrainingModos.DEFAULT -> {
                this.damageData = incomingData
            }
            TrainingModos.DPS -> {
                when (dpsTestState) {
                    DPS_STATE_READY -> {
                        dpsOwnerUuid = lastAttackerUuid
                        dpsOwnerName = lastAttackerName
                        dpsTotalDamage = 0f
                        dpsHitCount = 0
                        dpsHighestHit = 0f
                        dpsTicksRemaining = DPS_TEST_DURATION_TICKS
                        dpsTestState = DPS_STATE_MEASURING
                    }
                    DPS_STATE_COOLDOWN -> {
                        damageData = incomingData
                        return
                    }
                }

                val hitDamage = incomingData.newDamage.coerceAtLeast(0f)
                dpsTotalDamage += hitDamage
                dpsHitCount++
                dpsHighestHit = maxOf(dpsHighestHit, hitDamage)
                damageData = incomingData
            }

            TrainingModos.ACCUMULATED -> {
                this.damageData = incomingData + damageDataOld
            }

            TrainingModos.COMPARISON -> {
                damageDataOld = damageData
                this.damageData = incomingData
            }
        }
    }

    // ========================================================================
    // TICK & CICLO DE VIDA
    // ========================================================================

    override fun tick() {
        super.tick()

        updateArmPartPositions()

        this.yBodyRot = this.yRot
        this.yHeadRot = this.yRot

        if (!level().isClientSide) {
            if (displayTicks > 0) {
                displayTicks--
            }

            when (dpsTestState) {
                DPS_STATE_MEASURING -> {
                    dpsTicksRemaining = (dpsTicksRemaining - 1).coerceAtLeast(0)
                    if (dpsTicksRemaining == 0) {
                        submitDpsRank()
                        dpsTicksRemaining = DPS_RESULT_DURATION_TICKS
                        dpsTestState = DPS_STATE_COOLDOWN
                    }
                }
                DPS_STATE_COOLDOWN -> {
                    dpsTicksRemaining = (dpsTicksRemaining - 1).coerceAtLeast(0)
                    if (dpsTicksRemaining == 0) {
                        dpsTestState = DPS_STATE_READY
                        dpsTotalDamage = 0f
                        dpsHitCount = 0
                        dpsHighestHit = 0f
                    }
                }
            }
        }
    }

    /** `true` quando há um módulo de treino instalado (somente nesse caso os golpes valem para o ranking). */
    fun hasTrainingModule(): Boolean =
        dummyInventory.getStackInSlot(TRAINING_MODULE_SLOT).`is`(ModItems.TRAINING_MODULE_ITEM)

    /** Envia o resultado do teste de DPS que acabou de terminar para o ranking persistente do mundo. */
    private fun submitDpsRank() {
        val owner = dpsOwnerUuid ?: return
        val name = dpsOwnerName ?: return
        dpsOwnerUuid = null
        dpsOwnerName = null

        val serverLevel = level() as? ServerLevel ?: return
        val dps = dpsTotalDamage / (DPS_TEST_DURATION_TICKS / 20f)
        if (dps <= 0f) return

        val server = serverLevel.server
        val data = DummyRankData.get(server)
        if (data.recordDps(owner, name, dps)) {
            server.playerList.getPlayer(owner)?.displayClientMessage(
                Component.translatable(
                    "message.trainingdummy.rank.new_best_dps",
                    String.format(java.util.Locale.ROOT, "%.1f", dps)
                ),
                true
            )
        }
    }

    fun resetModoState() {
        dpsTestState = DPS_STATE_READY
        dpsTicksRemaining = 0
        dpsTotalDamage = 0f
        dpsHitCount = 0
        dpsHighestHit = 0f
        damageData = DamageData()
        damageDataOld = DamageData()
    }

    // ========================================================================
    // GETTERS & DATA ACCESSORS (PROFILE & NBT)
    // ========================================================================

    var profilePlayer: ResolvableProfile?
        get() = entityData.get(OPTIONAL_PROFILE).orElse(null)
        set(value) = entityData.set(OPTIONAL_PROFILE, Optional.ofNullable(value))

    fun setSkinByUsername(username: String?) {

        val serverLevel = this.level() as? ServerLevel ?: return

        if (username.isNullOrBlank()) {
            profilePlayer = null
            return
        }

        if (profilePlayer?.name?.map { it.equals(username, true) }?.orElse(false) ?: false) return

        profilePlayer = null

        SkullBlockEntity.fetchGameProfile(username).thenAccept { profileOpt ->
            serverLevel.server.execute {
                if (isRemoved || level() !== serverLevel ||
                    !customName?.string.equals(username, ignoreCase = true)
                ) {
                    return@execute
                }
                profileOpt.ifPresent { profile ->
                    profilePlayer = ResolvableProfile(profile)
                }
            }
        }

    }

    // ========================================================================
    // SALVAMENTO NO MUNDO (DISCO/WORLD NBT)
    // ========================================================================

    override fun addAdditionalSaveData(compoundTag: CompoundTag) {
        super.addAdditionalSaveData(compoundTag)

        compoundTag.remove(LEGACY_DUMMY_INVENTORY_TAG)
        compoundTag.put(DUMMY_INVENTORY_TAG, dummyInventory.serializeNBT(level().registryAccess()))
        compoundTag.putBoolean(DETAILED_DAMAGE_WINDOW_TAG, detailedDamageWindow)
        compoundTag.putInt(WINDOW_SHAPE_TAG, windowShape.ordinal)

        val registryContext = registryAccess().createSerializationContext(NbtOps.INSTANCE)

        compoundTag.remove(LEGACY_PROFILE_TAG)
        compoundTag.remove(PROFILE_TAG)
        profilePlayer?.let { profile ->
            ResolvableProfile.CODEC.encodeStart(registryContext, profile)
                .result()
                .ifPresent { profileTag ->
                    compoundTag.put(PROFILE_TAG, profileTag)
                }
        }

    }

    override fun readAdditionalSaveData(compoundTag: CompoundTag) {
        super.readAdditionalSaveData(compoundTag)
        detailedDamageWindow = compoundTag.getBoolean(DETAILED_DAMAGE_WINDOW_TAG)
        windowShape = if (compoundTag.contains(WINDOW_SHAPE_TAG)) {
            WindowShapeType.byId(compoundTag.getInt(WINDOW_SHAPE_TAG))
        } else {
            WindowShapeType.DEFAULT
        }

        val inventoryTag = when {
            compoundTag.contains(DUMMY_INVENTORY_TAG) -> compoundTag.getCompound(DUMMY_INVENTORY_TAG)
            compoundTag.contains(LEGACY_DUMMY_INVENTORY_TAG) -> compoundTag.getCompound(LEGACY_DUMMY_INVENTORY_TAG)
            else -> null
        }
        if (inventoryTag != null) {
            dummyInventory.deserializeNBT(level().registryAccess(), inventoryTag)
            for (inventorySlot in 0 until dummyInventory.slots) {
                equipmentSlotForInventorySlot(inventorySlot)?.let { equipmentSlot ->
                    setItemSlot(equipmentSlot, dummyInventory.getStackInSlot(inventorySlot))
                }
            }
        }

        syncTrainingModuleConfig(dummyInventory.getStackInSlot(TRAINING_MODULE_SLOT))

        val registryContext = registryAccess().createSerializationContext(NbtOps.INSTANCE)
        profilePlayer = null
        val profileTag = when {
            compoundTag.contains(PROFILE_TAG) -> compoundTag.get(PROFILE_TAG)
            compoundTag.contains(LEGACY_PROFILE_TAG) -> compoundTag.get(LEGACY_PROFILE_TAG)
            else -> null
        }
        if (profileTag != null) {
            ResolvableProfile.CODEC.parse(registryContext, profileTag)
                .result()
                .ifPresent { profile ->
                    this.profilePlayer = profile
                }
        }

    }

    private fun equipmentSlotForInventorySlot(inventorySlot: Int): EquipmentSlot? =
        when (inventorySlot) {
            HEAD_INVENTORY_SLOT -> EquipmentSlot.HEAD
            CHEST_INVENTORY_SLOT -> EquipmentSlot.CHEST
            LEGS_INVENTORY_SLOT -> EquipmentSlot.LEGS
            FEET_INVENTORY_SLOT -> EquipmentSlot.FEET
            MAIN_HAND_INVENTORY_SLOT -> EquipmentSlot.MAINHAND
            OFF_HAND_INVENTORY_SLOT -> EquipmentSlot.OFFHAND
            else -> null
        }

    override fun isCurrentlyGlowing(): Boolean {
        if (super.isCurrentlyGlowing()) return true
        if (this.level().isClientSide) {
            return net.ian.trainingdummy.client.DisplayDummyClientUtil.isDummyTargetedByPlayer(this.uuid)
        }
        return false
    }

    override fun getTeamColor(): Int {
        if (this.level().isClientSide) {
            if (net.ian.trainingdummy.client.DisplayDummyClientUtil.isDummyTargetedByPlayer(this.uuid)) {
                return 0x00E5FF
            }
        }
        return super.getTeamColor()
    }


    // ========================================================================
    // COMPANION OBJECT & ATRIBUTOS
    // ========================================================================

    companion object {

        val DISPLAY_TICKS: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.INT)

        val DPS_TEST_STATE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.INT)
        val DPS_TICKS_REMAINING: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.INT)
        val DPS_TOTAL_DAMAGE: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.FLOAT)
        val DPS_HIT_COUNT: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.INT)
        val DPS_HIGHEST_HIT: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.FLOAT)

        const val DPS_STATE_READY = 0
        const val DPS_STATE_MEASURING = 1
        const val DPS_STATE_COOLDOWN = 2
        const val DPS_TEST_DURATION_TICKS = 100
        const val DPS_RESULT_DURATION_TICKS = 60

        val DAMAGE_DATA : EntityDataAccessor<DamageData> =
            SynchedEntityData.defineId(DummyEntity::class.java, ModDataSerializers.DAMAGE_DATA)

        val DAMAGE_DATA_OLD : EntityDataAccessor<DamageData> =
            SynchedEntityData.defineId(DummyEntity::class.java, ModDataSerializers.DAMAGE_DATA)


        val HIT_PITCH: EntityDataAccessor<Float> = SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.FLOAT)
        val HIT_ROLL: EntityDataAccessor<Float> = SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.FLOAT)
        val HIT_TRIGGER: EntityDataAccessor<Int> = SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.INT)

        val MACE_DAMAGE: EntityDataAccessor<Float> = SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.FLOAT)
        val MACE_HIT_TRIGGER: EntityDataAccessor<Int> = SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.INT)

        val OPTIONAL_PROFILE: EntityDataAccessor<Optional<ResolvableProfile>> =
            SynchedEntityData.defineId(DummyEntity::class.java, ModDataSerializers.OPTIONAL_RESOLVABLE_PROFILE)

        val TRAINING_CONFIG_DATA: EntityDataAccessor<TrainingModuleConfig> =
            SynchedEntityData.defineId(DummyEntity::class.java, TrainingModuleConfig.TRAINING_MODULE_CONFIG)

        val DAMAGE_ITEMSTACK : EntityDataAccessor<ItemStack> = SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.ITEM_STACK)
        val DAMAGE_ITEMSTACK_OLD : EntityDataAccessor<ItemStack> = SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.ITEM_STACK)
        val DETAILED_DAMAGE_WINDOW: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.BOOLEAN)

        val WINDOW_SHAPE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.INT)


        private const val HEAD_INVENTORY_SLOT = 0
        private const val CHEST_INVENTORY_SLOT = 1
        private const val LEGS_INVENTORY_SLOT = 2
        private const val FEET_INVENTORY_SLOT = 3
        private const val TRAINING_MODULE_SLOT = 4
        private const val MAIN_HAND_INVENTORY_SLOT = 5
        private const val OFF_HAND_INVENTORY_SLOT = 6
        private const val DUMMY_INVENTORY_TAG = "trainingdummy:DummyInventory"
        private const val DETAILED_DAMAGE_WINDOW_TAG = "trainingdummy:DetailedDamageWindow"
        private const val WINDOW_SHAPE_TAG = "trainingdummy:WindowShape"
        private const val PROFILE_TAG = "trainingdummy:ProfilePlayer"
        private const val LEGACY_DUMMY_INVENTORY_TAG = "DummyInventory"
        private const val LEGACY_PROFILE_TAG = "ProfilePlayer"


        fun createAttributes(): AttributeSupplier.Builder {
            return createMobAttributes()
                .add(Attributes.MAX_HEALTH, (Float.MAX_VALUE - 1).toDouble())
                .add(Attributes.KNOCKBACK_RESISTANCE, 10.0)
                .add(Attributes.STEP_HEIGHT, 0.0)
        }
    }
}