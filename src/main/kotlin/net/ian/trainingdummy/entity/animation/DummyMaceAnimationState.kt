package net.ian.trainingdummy.entity.animation

import net.ian.trainingdummy.entity.DummyEntity
import net.minecraft.world.entity.AnimationState

class DummyMaceAnimationState(private val entity : DummyEntity) : AnimationState() {

    var maceScaleSize: Float = 1.0f
    var maceScaleSizeSmooth: Boolean = false
    var maceScaleSizeNormalizer: Boolean = false

    // --- Propriedades limpas com getters e setters encapsulados ---

    fun trigger(){
        entity.entityData.set(
            DummyEntity.MACE_HIT_TRIGGER,
            entity.entityData.get(DummyEntity.MACE_HIT_TRIGGER) + 1
        )
    }

    var maceDamage: Float
        get() = entity.entityData.get(DummyEntity.MACE_DAMAGE)
        set(value) = entity.entityData.set(DummyEntity.MACE_DAMAGE, value)


}