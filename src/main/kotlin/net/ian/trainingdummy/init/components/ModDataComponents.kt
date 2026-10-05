package net.ian.trainingdummy.init.components

import net.ian.trainingdummy.TrainingDummy
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

object ModDataComponents {

    val COMPONENTS: DeferredRegister<DataComponentType<*>> = 
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, TrainingDummy.ID)

    val TRAINING_MODULE_CONFIG : DeferredHolder<DataComponentType<*>, DataComponentType<TrainingModuleConfig>> =
        COMPONENTS.register("training_config") { ->
            DataComponentType.builder<TrainingModuleConfig>()
                .persistent(TrainingModuleConfig.CODEC)       // Permite salvar no disco/Item
                .networkSynchronized(TrainingModuleConfig.STREAM_CODEC) // Permite enviar pela rede
                .build()
        }

    /*
    val DUMMY_DATA: DeferredHolder<DataComponentType<*>, DataComponentType<DummyData>> = 
        COMPONENTS.register("dummy_data") { ->
            DataComponentType.builder<DummyData>()
                .persistent(DummyData.CODEC)
                .build()
        }

     */

    fun register(bus: IEventBus) {
        COMPONENTS.register(bus)
    }
}