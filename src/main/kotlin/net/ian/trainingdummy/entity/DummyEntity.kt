package net.ian.trainingdummy.entity

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.PropertyMap
import net.ian.trainingdummy.entity.animation.DummyHitAnimationState
import net.ian.trainingdummy.entity.animation.DummyMaceAnimationState
import net.ian.trainingdummy.entity.part.DummyPartEntity
import net.ian.trainingdummy.event.utils.DamageData
import net.ian.trainingdummy.event.utils.ModDataSerializers
import net.ian.trainingdummy.init.ModDataComponents
import net.ian.trainingdummy.item.ModItems
import net.ian.trainingdummy.item.custom.DisplayDummyBlockItem
import net.ian.trainingdummy.item.custom.TrainingModuleItem
import net.ian.trainingdummy.screen.custom.DummyMenu
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.ExtraCodecs
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemContainerContents
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.SkullBlockEntity
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.items.ItemStackHandler
import java.util.Optional
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

    var displayTicks: Int
        get() = entityData.get(DISPLAY_TICKS)
        set(value) = entityData.set(DISPLAY_TICKS, value)

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

        builder.define(DUMMY_DATA, CompoundTag())
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
            when (slot) {
                0 -> setItemSlot(EquipmentSlot.HEAD, stack)
                1 -> setItemSlot(EquipmentSlot.CHEST, stack)
                2 -> setItemSlot(EquipmentSlot.LEGS, stack)
                3 -> setItemSlot(EquipmentSlot.FEET, stack)
                5 -> setItemSlot(EquipmentSlot.MAINHAND, stack)
                6 -> setItemSlot(EquipmentSlot.OFFHAND, stack)
            }
        }

        override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
            return when (slot) {
                0 -> stack.canEquip(EquipmentSlot.HEAD, this@DummyEntity)
                1 -> stack.canEquip(EquipmentSlot.CHEST, this@DummyEntity)
                2 -> stack.canEquip(EquipmentSlot.LEGS, this@DummyEntity)
                3 -> stack.canEquip(EquipmentSlot.FEET, this@DummyEntity)
                4 -> stack.`is`(ModItems.TRAINING_MODULE_ITEM)
                5, 6 -> true
                else -> false
            }
        }

        override fun getSlotLimit(slot: Int): Int = if (slot == 5 || slot == 6) 64 else 1


    }

    override fun getPickResult(): ItemStack? = createItemFromDummy()

    private fun setEquipmentAndSync(slot: EquipmentSlot, stack: ItemStack) {
        val invSlot = when (slot) {
            EquipmentSlot.HEAD -> 0
            EquipmentSlot.CHEST -> 1
            EquipmentSlot.LEGS -> 2
            EquipmentSlot.FEET -> 3
            EquipmentSlot.MAINHAND -> 5
            EquipmentSlot.OFFHAND -> 6
            else -> -1
        }

        if (invSlot != -1) {
            // Isso engatilha o onContentsChanged automaticamente, atualizando o visual
            dummyInventory.setStackInSlot(invSlot, stack)
        } else {
            setItemSlot(slot, stack)
        }
    }

    /**
     * Converte o Dummy atual em um ItemStack, gravando Equipamentos e o NBT (DUMMY_DATA).
     */

    fun createItemFromDummy(): ItemStack {
        val itemStack = ItemStack(ModItems.DUMMY_ITEM_SPAWN)

        // Converte os itens do dummyInventory em uma lista para o componente CONTAINER nativo
        val itemsList = NonNullList.withSize(dummyInventory.slots, ItemStack.EMPTY)
        for (i in 0 until dummyInventory.slots) {
            itemsList[i] = dummyInventory.getStackInSlot(i)
        }

        // Salva o inventário inteiro direto no ItemStack
        itemStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(itemsList))
        itemStack.set(DataComponents.PROFILE,profilePlayer)
        itemStack.set(DataComponents.CUSTOM_NAME, this.customName)

        val dummyTag = entityData.get(DUMMY_DATA)

        return itemStack
    }

    /*
    fun createItemFromDummyOLD(): ItemStack {
        val itemStack = ItemStack(ModItems.DUMMY_ITEM_SPAWN)

        // 1. Salva os equipamentos no componente do Mod
        val armor = NonNullList.withSize(4, ItemStack.EMPTY).apply {
            this[0] = getItemBySlot(EquipmentSlot.FEET)
            this[1] = getItemBySlot(EquipmentSlot.LEGS)
            this[2] = getItemBySlot(EquipmentSlot.CHEST)
            this[3] = getItemBySlot(EquipmentSlot.HEAD)
        }
        val hands = NonNullList.withSize(2, ItemStack.EMPTY).apply {
            this[0] = getItemBySlot(EquipmentSlot.MAINHAND)
            this[1] = getItemBySlot(EquipmentSlot.OFFHAND)
        }

        itemStack.set(ModDataComponents.DUMMY_DATA.get(), DummyData(armor = armor, hands = hands))

        itemStack.set(DataComponents.CUSTOM_NAME, this.customName)

        //Salva o CompoundTag do Dummy (Contendo SkullProfile e NBTs customizadas) no CUSTOM_DATA do Item
        val dummyTag = entityData.get(DUMMY_DATA)
        if (!dummyTag.isEmpty) {
            CustomData.update(DataComponents.CUSTOM_DATA, itemStack) { tag ->
                tag.merge(dummyTag)
                tag.putString("custom_name",this.customName?.string ?: "" )
            }
        }
        return itemStack
    }
    */

    fun applyCompoundTagsFromItem(stack: ItemStack) {

        stack.get(DataComponents.PROFILE)?.let { this.profilePlayer = it }

        stack.get(DataComponents.CUSTOM_NAME)?.let {  super.customName = it }

    }

    fun applyCustomName(name : Component?){ super.customName = name }

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
       //super.dropCustomDeathLoot(serverLevel, source, p_21387_)

        EquipmentSlot.entries.forEach {
            val stack = this.getItemBySlot(it)
            if(!stack.isEmpty){
                this.setItemSlot(it, ItemStack.EMPTY)
                this.spawnAtLocation(stack)
            }
        }

        this.spawnAtLocation(createItemFromDummy())
    }

    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {

        if (!this.isAlive) {
            return InteractionResult.PASS
        }

        val heldItem = player.getItemInHand(hand)

        /*
        if (heldItem.`is`(ModItems.DUMMY_ITEM_SPAWN)) {
            if (!level().isClientSide) {
                val itemToReturn = createItemFromDummy()
                val containerContents = heldItem.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                val newItems = NonNullList.withSize(dummyInventory.slots, ItemStack.EMPTY)
                containerContents.copyInto(newItems)
                for (i in 0 until dummyInventory.slots) {
                    dummyInventory.setStackInSlot(i, newItems.getOrElse(i) { ItemStack.EMPTY })
                }
                player.setItemInHand(hand, itemToReturn)
            }
            return InteractionResult.sidedSuccess(level().isClientSide)
        }
        */

        /*
        if (heldItem.`is`(ModItems.DUMMY_ITEM_SPAWN)) {
            if (!level().isClientSide) {
                val data = heldItem.get(ModDataComponents.DUMMY_DATA.get()) ?: DummyData()

                // 1. Armazena equipamentos atuais da entidade para gravar no item da mão
                val currentArmor = NonNullList.withSize(4, ItemStack.EMPTY).apply {
                    this[0] = getItemBySlot(EquipmentSlot.FEET).copy()
                    this[1] = getItemBySlot(EquipmentSlot.LEGS).copy()
                    this[2] = getItemBySlot(EquipmentSlot.CHEST).copy()
                    this[3] = getItemBySlot(EquipmentSlot.HEAD).copy()
                }
                val currentHands = NonNullList.withSize(2, ItemStack.EMPTY).apply {
                    this[0] = getItemBySlot(EquipmentSlot.MAINHAND).copy()
                    this[1] = getItemBySlot(EquipmentSlot.OFFHAND).copy()
                }

                // 2. Aplica a nova armadura (vinda do item) na Entidade e sincroniza com a GUI
                setEquipmentAndSync(EquipmentSlot.FEET, data.armor.getOrElse(0) { ItemStack.EMPTY }.copy())
                setEquipmentAndSync(EquipmentSlot.LEGS, data.armor.getOrElse(1) { ItemStack.EMPTY }.copy())
                setEquipmentAndSync(EquipmentSlot.CHEST, data.armor.getOrElse(2) { ItemStack.EMPTY }.copy())
                setEquipmentAndSync(EquipmentSlot.HEAD, data.armor.getOrElse(3) { ItemStack.EMPTY }.copy())

                setEquipmentAndSync(EquipmentSlot.MAINHAND, data.hands.getOrElse(0) { ItemStack.EMPTY }.copy())
                setEquipmentAndSync(EquipmentSlot.OFFHAND, data.hands.getOrElse(1) { ItemStack.EMPTY }.copy())

                // 3. Atualiza o item segurado pelo jogador com o inventário antigo do Dummy
                val newData = DummyData(armor = currentArmor, hands = currentHands)
                heldItem.set(ModDataComponents.DUMMY_DATA.get(), newData)
            }
            return InteractionResult.sidedSuccess(level().isClientSide)
        }*/

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
        name?.also { setSkinByUsername(it.string) } ?: run {profilePlayer = null}
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
    private var timerModo: Int = 0          // Tempo do teste (ex: 100 ticks = 5s)
    private var timerModoEspera: Int = 0    // Cooldown/Espera pós-teste (ex: 60 ticks = 3s)
    private var aticveModo: Boolean = false
    private var emEspera: Boolean = false   // Trava o inicio de um novo combo durante os 3s
    private var accumulatedDamageInWindow: Float = 0f

    //private var lastHitDamage: Float = 0f

    fun onCustomDamageReceived(incomingData: DamageData) {
        val modo = trainingModo
        when (modo) {
            TrainingModuleItem.TrainingModos.DEFAULT -> {
                this.damageData = incomingData
            }
            TrainingModuleItem.TrainingModos.DPS -> {
                if (emEspera) return

                if (!aticveModo) {
                    timerModo = 100
                    aticveModo = true
                    accumulatedDamageInWindow = incomingData.newDamage
                } else {
                    accumulatedDamageInWindow += incomingData.newDamage
                }

                val timePassedSeconds = ((100 - timerModo).coerceAtLeast(1)) / 20f
                val currentDps = accumulatedDamageInWindow / timePassedSeconds

                this.damageData = DamageData(originalDamage = currentDps, newDamage = currentDps)
                //fazer o sistema para monstra a quatidade de hit detro desse tempo
                //usar algum valor do damageData q nao esteja usado no dps, o evento dispara no ModEvent
            }

            TrainingModuleItem.TrainingModos.ACCUMULATED -> {
                this.damageData = incomingData + damageDataOld
            }

            TrainingModuleItem.TrainingModos.COMPARISON -> {
                //val diff = incomingData.newDamage - lastHitDamage
                //lastHitDamage = incomingData.newDamage

                this.damageData = incomingData//.copy(originalDamage = diff)
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

            if (aticveModo) {
                timerModo--
                if (timerModo <= 0) {
                    val finalDps = accumulatedDamageInWindow / 5.0f
                    this.damageData = this.damageData.copy(newDamage = finalDps)

                    aticveModo = false
                    emEspera = true
                    timerModoEspera = 60
                }
            }

            else if (emEspera) {
                timerModoEspera--
                if (timerModoEspera <= 0) {
                    emEspera = false
                    accumulatedDamageInWindow = 0f
                }
            }
        }
    }

    fun resetModoState() {
        aticveModo = false
        emEspera = false
        timerModo = 0
        timerModoEspera = 0
        accumulatedDamageInWindow = 0f
        //lastHitDamage = 0f
        damageData = DamageData()
    }

    // ========================================================================
    // GETTERS & DATA ACCESSORS (PROFILE & NBT)
    // ========================================================================

    lateinit var trainingModo : TrainingModuleItem.TrainingModos


    var profilePlayer: ResolvableProfile?
        get() {
            val dummyTag = entityData.get(DUMMY_DATA)
            if (!dummyTag.contains("ResolvableProfile")) return null
            val profileTag = dummyTag.get("ResolvableProfile") ?: return null
            return ResolvableProfile.CODEC
                .parse(registryAccess().createSerializationContext(NbtOps.INSTANCE), profileTag)
                .result()
                .orElse(null)
        }
        set(value) {
            val dummyTag = entityData.get(DUMMY_DATA).copy()
            if (value != null) {
                ResolvableProfile.CODEC
                    .encodeStart(registryAccess().createSerializationContext(NbtOps.INSTANCE), value)
                    .result()
                    .ifPresent { tag ->
                        dummyTag.put("ResolvableProfile", tag)
                    }
            } else {
                dummyTag.remove("ResolvableProfile")
            }
            entityData.set(DUMMY_DATA, dummyTag)

            if (value != null && !value.isResolved) {
                value.resolve().thenAcceptAsync({ resolvedProfile ->
                    // Ao resolver a skin na thread de fundo, atualiza para o perfil resolvido
                    this.profilePlayer = resolvedProfile
                }, net.minecraft.client.Minecraft.getInstance())
            }
        }

    fun setSkinByUsername(username: String?) {

        if (this.level().isClientSide) {return}

        if (username.isNullOrBlank()) {profilePlayer = null; return}

        if(profilePlayer?.name?.map { it.equals(username,true) }?.orElse(false) ?: false){return}

        /*
        val unresolvedProfile = ResolvableProfile(
            Optional.of(username),
            Optional.empty(),
            PropertyMap()
        )
        unresolvedProfile.resolve().thenAcceptAsync({ resolvedProfile ->
            if (resolvedProfile != null) {
                this.profilePlayer = resolvedProfile
            }
        }, net.minecraft.Util.backgroundExecutor())
        */

        SkullBlockEntity.fetchGameProfile(username).thenAccept { profileOpt ->
            profileOpt.ifPresent { profile ->
                this.profilePlayer = ResolvableProfile(profile)
            }
        }

    }

    // ========================================================================
    // SALVAMENTO NO MUNDO (DISCO/WORLD NBT)
    // ========================================================================

    override fun addAdditionalSaveData(compoundTag: CompoundTag) {
        super.addAdditionalSaveData(compoundTag)

        compoundTag.remove("DummyInventory")
        compoundTag.put("DummyInventory", dummyInventory.serializeNBT(level().registryAccess()))

        compoundTag.merge(entityData.get(DUMMY_DATA))
    }

    override fun readAdditionalSaveData(compoundTag: CompoundTag) {
        super.readAdditionalSaveData(compoundTag)

        if (compoundTag.contains("DummyInventory")) {
            dummyInventory.deserializeNBT(level().registryAccess(), compoundTag.getCompound("DummyInventory"))
            setItemSlot(EquipmentSlot.HEAD, dummyInventory.getStackInSlot(0))
            setItemSlot(EquipmentSlot.CHEST, dummyInventory.getStackInSlot(1))
            setItemSlot(EquipmentSlot.LEGS, dummyInventory.getStackInSlot(2))
            setItemSlot(EquipmentSlot.FEET, dummyInventory.getStackInSlot(3))
            setItemSlot(EquipmentSlot.MAINHAND, dummyInventory.getStackInSlot(5))
            setItemSlot(EquipmentSlot.OFFHAND, dummyInventory.getStackInSlot(6))

            val stack =  dummyInventory.getStackInSlot(4) ?: ItemStack.EMPTY
            trainingModo = TrainingModuleItem.getItemMode(stack)

            compoundTag.remove("DummyInventory")
        }


        entityData.set(DUMMY_DATA, compoundTag.copy())
    }

    // ========================================================================
    // COMPANION OBJECT & ATRIBUTOS
    // ========================================================================

    companion object {

        val DISPLAY_TICKS: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.INT)

        val DAMAGE_DATA : EntityDataAccessor<DamageData> =
            SynchedEntityData.defineId(DummyEntity::class.java, ModDataSerializers.DAMAGE_DATA)

        val DAMAGE_DATA_OLD : EntityDataAccessor<DamageData> =
            SynchedEntityData.defineId(DummyEntity::class.java, ModDataSerializers.DAMAGE_DATA)


        val HIT_PITCH: EntityDataAccessor<Float> = SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.FLOAT)
        val HIT_ROLL: EntityDataAccessor<Float> = SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.FLOAT)
        val HIT_TRIGGER: EntityDataAccessor<Int> = SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.INT)

        val MACE_DAMAGE: EntityDataAccessor<Float> = SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.FLOAT)
        val MACE_HIT_TRIGGER: EntityDataAccessor<Int> = SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.INT)

        val DUMMY_DATA: EntityDataAccessor<CompoundTag> =
            SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.COMPOUND_TAG)

        fun createAttributes(): AttributeSupplier.Builder {
            return createMobAttributes()
                .add(Attributes.MAX_HEALTH, (Float.MAX_VALUE - 1).toDouble())
                .add(Attributes.KNOCKBACK_RESISTANCE, 10.0)
                .add(Attributes.STEP_HEIGHT, 0.0)
        }
    }
}