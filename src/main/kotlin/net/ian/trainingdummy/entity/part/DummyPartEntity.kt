package net.ian.trainingdummy.entity.part

import net.ian.trainingdummy.entity.DummyEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.entity.PartEntity

class DummyPartEntity(
    val parentEntity: DummyEntity,
    val dummyPartType: DummyPartType,
    width: Float,
    height: Float
) : PartEntity<DummyEntity>(parentEntity) {

    private val partDimensions = EntityDimensions.scalable(width, height)

    init {
        this.refreshDimensions()
    }

    override fun getDimensions(pose: Pose): EntityDimensions {
        return partDimensions
    }

    override fun isPickable(): Boolean = true
    override fun isNoGravity(): Boolean = true

    override fun skipAttackInteraction(attacker: Entity): Boolean = false

    override fun `is`(entity: Entity): Boolean {
        return this === entity || this.parentEntity === entity
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {

        val targetSlot = when (dummyPartType) {
            DummyPartType.RightArm -> EquipmentSlot.MAINHAND
            DummyPartType.LeftArm -> EquipmentSlot.OFFHAND
            DummyPartType.Base -> return parentEntity.interactBaseDummy(player,hand)
        }

        return parentEntity.interactWithHandSlot(player, hand, targetSlot)
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        return parentEntity.hurt(source, amount)
    }

    override fun defineSynchedData(builder: net.minecraft.network.syncher.SynchedEntityData.Builder) {}
    override fun readAdditionalSaveData(compound: CompoundTag) {}
    override fun addAdditionalSaveData(compound: CompoundTag) {}

    companion object {
        enum class DummyPartType {
            RightArm,LeftArm,Base
        }
    }
}


