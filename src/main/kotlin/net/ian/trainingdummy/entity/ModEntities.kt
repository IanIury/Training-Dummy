package net.ian.trainingdummy.entity

import net.minecraft.resources.ResourceKey

import net.ian.trainingdummy.TrainingDummy
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.decoration.ArmorStand
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.DeferredHolder

object ModEntities {


    val ENTITIES: DeferredRegister<EntityType<*>> =
        DeferredRegister.create(Registries.ENTITY_TYPE, TrainingDummy.ID)

    val DUMMY: DeferredHolder<EntityType<*>, EntityType<DummyEntity>> =
        ENTITIES.register("training_dummy") { key ->
            EntityType.Builder.of(::DummyEntity, MobCategory.MISC)
                .sized(0.6F, 2.1F)
                //.clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, key))
        }

    fun register(bus: IEventBus) {
        ENTITIES.register(bus)
    }

    // Registra os atributos da entidade no ciclo do jogo
    fun registerAttributes(event: EntityAttributeCreationEvent) {
        event.put(DUMMY.get(), DummyEntity.createAttributes().build())
    }
}