package net.ian.trainingdummy.item

import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.block.ModBlocks
import net.ian.trainingdummy.item.custom.DisplayDummyBlockItem
import net.ian.trainingdummy.item.custom.DummyManagementBlockItem
import net.ian.trainingdummy.item.custom.TrainingDummySpawnItem
import net.ian.trainingdummy.item.custom.TrainingModuleItem
import net.ian.trainingdummy.util.getValue
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister

object ModItems {

    val REGISTRY: DeferredRegister.Items = DeferredRegister.createItems(TrainingDummy.ID)

    val DUMMY_ITEM_SPAWN: TrainingDummySpawnItem by REGISTRY.registerItem("dummy_item_spawn") { props ->
        TrainingDummySpawnItem(props.stacksTo(1))
    }

    val TRAINING_MODULE_ITEM: TrainingModuleItem by REGISTRY.registerItem("training_module_item") { props ->
        TrainingModuleItem(props)
    }

    val DISPLAY_DUMMY_BLOCK_ITEM: DisplayDummyBlockItem by REGISTRY.registerItem("display_dummy") { props ->
        DisplayDummyBlockItem(ModBlocks.DISPLAY_DUMMY_BLOCK, props.useBlockDescriptionPrefix())
    }

    val DUMMY_MANAGEMENT_BLOCK_ITEM: DummyManagementBlockItem by REGISTRY.registerItem("dummy_management_block") { props ->
        DummyManagementBlockItem(ModBlocks.DUMMY_MANAGEMENT_BLOCK, props.useBlockDescriptionPrefix())
    }

    fun register(bus: IEventBus) {
        REGISTRY.register(bus)
    }
}
