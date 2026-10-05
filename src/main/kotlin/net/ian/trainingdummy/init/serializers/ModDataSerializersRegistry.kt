package net.ian.trainingdummy.init.serializers

import net.ian.trainingdummy.TrainingDummy
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.world.item.component.ResolvableProfile
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.function.Supplier

object ModDataSerializersRegistry {

    val SERIALIZERS: DeferredRegister<EntityDataSerializer<*>> = DeferredRegister.create(
        NeoForgeRegistries.ENTITY_DATA_SERIALIZERS,
        TrainingDummy.ID
    )

    val DAMAGE_DATA_SERIALIZER = SERIALIZERS.register("damage_data", Supplier { ModDataSerializers.DAMAGE_DATA })

    val OPTIONAL_RESOLVABLE_PROFILE = SERIALIZERS.register("optional_resolvable_profile", Supplier { ModDataSerializers.OPTIONAL_RESOLVABLE_PROFILE})

    fun register(eventBus: IEventBus) {
        SERIALIZERS.register(eventBus)
    }

}