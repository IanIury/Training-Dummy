package net.ian.trainingdummy.entity


import net.ian.trainingdummy.entity.animation.DummyHitAnimationState
import net.ian.trainingdummy.entity.animation.DummyMaceAnimationState
import net.ian.trainingdummy.entity.part.DummyPartEntity
import net.ian.trainingdummy.event.utils.DamageData
import net.ian.trainingdummy.event.utils.ModDataSerializers
import net.ian.trainingdummy.item.ModItems
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

open class DummyEntity(
    type: EntityType<out Mob>,
    level: Level
) : Mob(type, level) {



    // ========================================================================
    // PROPRIEDADES DE PARTES E SUB-ENTIDADES
    // ========================================================================

    val rightArmPart = DummyPartEntity(this, DummyPartEntity.Companion.DummyPartType.RightArm, 0.15f, 0.75f)
    val leftArmPart = DummyPartEntity(this, DummyPartEntity.Companion.DummyPartType.LeftArm, 0.15f, 0.75f)
    val basePart = DummyPartEntity(this, DummyPartEntity.Companion.DummyPartType.Base, 1.0f, 0.085f)

    val subEntities = arrayOf(this.rightArmPart, this.leftArmPart, this.basePart)

    // ========================================================================
    // ESTADOS E ANIMAÇÕES
    // ========================================================================

    val hitAnimation = DummyHitAnimationState(this)
    val maceAnimation = DummyMaceAnimationState(this)
    val spawnAnimation = AnimationState()



    var damageDataOLD = DamageData()
    var damageData = DamageData()


    var displayTicks: Int
        get() = entityData.get(DISPLAY_TICKS)
        set(value) = entityData.set(DISPLAY_TICKS, value)

    // ========================================================================
    // INICIALIZAÇÃO E REGISTRO DE DADOS
    // ========================================================================

    override fun setId(id: Int) {
        super.setId(id)

        // Atribui IDs de rede únicos para as partes baseadas no ID do pai
        this.rightArmPart.id = id + 1
        this.leftArmPart.id = id + 2
        this.basePart.id = id + 3
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(DISPLAY_TICKS, 0)
        builder.define(DAMAGE_DATA, DamageData())

        builder.define(MACE_DAMAGE, 0.0f)
        builder.define(MACE_HIT_TRIGGER, 0)

        builder.define(HIT_PITCH, 0.0f)
        builder.define(HIT_ROLL, 0.0f)
        builder.define(HIT_TRIGGER, 0)
    }

    // ========================================================================
    // TICK & CICLO DE VIDA
    // ========================================================================

    override fun tick() {
        super.tick()

        updateArmPartPositions()

        // Trava a rotação do corpo com a da cabeça para não torcer o modelo
        this.yBodyRot = this.yRot
        this.yHeadRot = this.yRot

        if (!level().isClientSide && displayTicks > 0) {
            displayTicks--
        }
    }

    override fun onAddedToLevel() {
        super.onAddedToLevel()
        if (level().isClientSide) {
            spawnAnimation.start(tickCount)
        }
    }

    // ========================================================================
    // COMPORTAMENTO, MOVIMENTO E FÍSICA
    // ========================================================================

    override fun registerGoals() {
        // Deixa vazio para que o Dummy não tenha IA (não anda nem ataca)
    }

    override fun removeWhenFarAway(distanceToClosestPlayer: Double): Boolean = false

    override fun requiresCustomPersistence(): Boolean = true

    override fun isPushable(): Boolean = false
    override fun push(x: Double, y: Double, z: Double) {}
    override fun doPush(entity: Entity) {}
    override fun pushEntities() {}
    override fun knockback(strength: Double, x: Double, z: Double) {} // Ignora repulsão

    // ========================================================================
    // SISTEMA DE DANO E ANIMAÇÃO DE IMPACTO
    // ========================================================================

    private fun setupAnimation(){}

    override fun hurt(source: DamageSource, amount: Float): Boolean {

        if (isInvulnerableTo(source) || level().isClientSide) {
            return false
        }
        val wasHurt = super.hurt(source, amount)
        if (wasHurt) {
            this.displayTicks = 60 // Tempo que a informação ficará visível
            this.health = this.maxHealth
        }

        return wasHurt
    }



    override fun onSyncedDataUpdated(key: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(key)

        if (level().isClientSide) {

            when(key) {

                HIT_TRIGGER -> {
                    hitAnimation.isNewHitRequested=true
                    hitAnimation.start(tickCount)
                }
                MACE_HIT_TRIGGER -> {
                    val mace_amount = maceAnimation.maceDamage
                    if (mace_amount > 0.0f) {
                        val escalaCalculada = Mth.clamp(1.0f - (mace_amount / 100f), 0.1f, 1.0f)

                        if (escalaCalculada < maceAnimation.maceScaleSize) {
                            maceAnimation.maceScaleSize = escalaCalculada
                            maceAnimation.maceScaleSizeSmooth = false
                        } else {
                            maceAnimation.maceScaleSizeSmooth = true
                        }
                        maceAnimation.start(tickCount)

                    }else{
                        maceAnimation.maceScaleSizeSmooth = true
                        maceAnimation.start(tickCount)

                    }
                }

            }
        }
    }

    /*
    override fun handleEntityEvent(id: Byte) {
        if (id.toInt() == 42) {
            this.hitTick = this.tickCount
            val attacker = this.lastDamageSource?.entity

            if (attacker != null) {

                if(attacker.weaponItem?.item is MaceItem){
                   // maceAnimation=DummyAnimationMace(true,this.getDamageDataS().newDamage)
                }

                val dx = attacker.x - this.x
                val dz = attacker.z - this.z
                val attackAngle = Mth.atan2(dz, dx) * (180.0f / Math.PI.toFloat()) - 90.0f
                val relativeAngle = Mth.wrapDegrees(attackAngle - this.yRot)
                val rad = Math.toRadians(relativeAngle.toDouble())

                this.hitPitchAngle = -Mth.cos(rad.toFloat())
                this.hitRollAngle = Mth.sin(rad.toFloat())
            } else {
                this.hitPitchAngle = -1.0f
                this.hitRollAngle = 0.0f
            }
            this.hitStrength = 1.2f
        } else {
            super.handleEntityEvent(id)
        }
    }
    */

    override fun getHurtSound(damageSource: DamageSource): SoundEvent = SoundEvents.ARMOR_STAND_HIT
    override fun getDeathSound(): SoundEvent = SoundEvents.ARMOR_STAND_BREAK

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

        // Atualização bruta das coordenadas anteriores para evitar interpolação errada
        this.rightArmPart.xo = rightX
        this.rightArmPart.yo = armBaseY
        this.rightArmPart.zo = rightZ
        this.rightArmPart.xOld = rightX
        this.rightArmPart.yOld = armBaseY
        this.rightArmPart.zOld = rightZ

        this.leftArmPart.xo = leftX
        this.leftArmPart.yo = armBaseY
        this.leftArmPart.zo = leftZ
        this.leftArmPart.xOld = leftX
        this.leftArmPart.yOld = armBaseY
        this.leftArmPart.zOld = leftZ

        this.basePart.xo = this.x
        this.basePart.yo = this.y
        this.basePart.zo = this.z
        this.basePart.xOld = this.x
        this.basePart.yOld = this.y
        this.basePart.zOld = this.z
    }

    private fun tickPart(part: DummyPartEntity, offsetX: Double, offsetY: Double, offsetZ: Double) {
        part.setPos(this.x + offsetX, this.y + offsetY, this.z + offsetZ)
    }

    // ========================================================================
    // INTERAÇÃO E EQUIPAMENTOS
    // ========================================================================

    override fun getPickResult(): ItemStack? = ItemStack(ModItems.DUMMY_ITEM_SPAWN)

    override fun interactAt(player: Player, vec: Vec3, hand: InteractionHand): InteractionResult {
        val heldItem = player.getItemInHand(hand)

        if (heldItem.`is`(Items.NAME_TAG)) return InteractionResult.PASS
        if (player.isSpectator) return InteractionResult.SUCCESS
        if (level().isClientSide) return InteractionResult.CONSUME

        // 1. Determina qual slot do Dummy o jogador está mirado com base na altura Y
        val relativeY = vec.y / this.bbHeight.toDouble()
        val slotFromY = when {
            relativeY >= 0.80 -> EquipmentSlot.HEAD
            relativeY >= 0.40 -> EquipmentSlot.CHEST
            relativeY >= 0.20 -> EquipmentSlot.LEGS
            else -> EquipmentSlot.FEET
        }

        // 2. Determina o slot de destino
        val itemEquipSlot = getEquipmentSlotForItem(heldItem)
        val targetSlot = if (itemEquipSlot != null && itemEquipSlot.isArmor) {
            itemEquipSlot
        } else {
            slotFromY
        }

        // 3. Tenta realizar a troca de item
        if (swapItem(player, targetSlot, heldItem, hand)) {
            return InteractionResult.SUCCESS
        }

        return InteractionResult.PASS
    }

    fun interactWithHandSlot(player: Player, hand: InteractionHand, slot: EquipmentSlot): InteractionResult {
        val heldItem = player.getItemInHand(hand)

        if (player.isSpectator) return InteractionResult.SUCCESS
        if (level().isClientSide) return InteractionResult.CONSUME

        if (swapItem(player, slot, heldItem, hand)) {
            return InteractionResult.SUCCESS
        }

        return InteractionResult.PASS
    }

    fun interactBaseDummy(player: Player, hand: InteractionHand): InteractionResult {
        return InteractionResult.PASS
    }

    private fun swapItem(player: Player, slot: EquipmentSlot, playerStack: ItemStack, hand: InteractionHand): Boolean {
        val dummyStack = getItemBySlot(slot)

        if (playerStack.isEmpty && dummyStack.isEmpty) {
            return false
        }

        if (slot.isArmor && !playerStack.isEmpty) {
            val itemSlot = getEquipmentSlotForItem(playerStack)
            if (itemSlot != slot) return false
        }

        // Modo Criativo: copia o item sem gastar do jogador
        if (player.hasInfiniteMaterials() && dummyStack.isEmpty && !playerStack.isEmpty) {
            setItemSlot(slot, playerStack.copyWithCount(1))
            return true
        }

        // Caso o jogador esteja segurando um pack (> 1)
        if (!playerStack.isEmpty && playerStack.count > 1) {
            if (!dummyStack.isEmpty) return false
            val singleItem = playerStack.split(1)
            setItemSlot(slot, singleItem)
            return true
        }

        // Troca padrão
        setItemSlot(slot, playerStack)
        player.setItemInHand(hand, dummyStack)
        return true
    }

    // ========================================================================
    // GETTERS & DATA ACCESSORS
    // ========================================================================

    fun getDamageDataS(): DamageData = entityData.get(DAMAGE_DATA)

    // ========================================================================
    // COMPANION OBJECT & ATTRIBUTES
    // ========================================================================

    companion object {



        val DISPLAY_TICKS: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.INT)

        val DAMAGE_DATA: EntityDataAccessor<DamageData> =
            SynchedEntityData.defineId(DummyEntity::class.java, ModDataSerializers.DAMAGE_DATA)

        val HIT_PITCH: EntityDataAccessor<Float> = SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.FLOAT)
        val HIT_ROLL: EntityDataAccessor<Float> = SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.FLOAT)
        val HIT_TRIGGER: EntityDataAccessor<Int> = SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.INT)

        val MACE_DAMAGE: EntityDataAccessor<Float> = SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.FLOAT)
        val MACE_HIT_TRIGGER: EntityDataAccessor<Int> = SynchedEntityData.defineId(DummyEntity::class.java, EntityDataSerializers.INT)

        fun createAttributes(): AttributeSupplier.Builder {
            return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 0.0)
        }

    }
}