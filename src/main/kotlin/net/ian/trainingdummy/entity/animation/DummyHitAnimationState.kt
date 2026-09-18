package net.ian.trainingdummy.entity.animation

import net.ian.trainingdummy.entity.DummyEntity
import net.minecraft.world.entity.AnimationState
import net.minecraft.network.syncher.EntityDataAccessor


class DummyHitAnimationState(private val entity: DummyEntity) : AnimationState() {

    var offsetPositionX: Float = 0.0f
    var offsetPositionZ: Float = 0.0f
    var isNewHitRequested: Boolean = false


    // --- Propriedades limpas com getters e setters encapsulados ---

    fun trigger(){
        entity.entityData.set(
            DummyEntity.HIT_TRIGGER,
            entity.entityData.get(DummyEntity.HIT_TRIGGER) + 1
        )
    }

    var hitPitch: Float
        get() = entity.entityData.get(DummyEntity.HIT_PITCH)
        set(value) = entity.entityData.set(DummyEntity.HIT_PITCH, value)

    var hitRoll: Float
        get() = entity.entityData.get(DummyEntity.HIT_ROLL)
        set(value) = entity.entityData.set(DummyEntity.HIT_ROLL, value)

}