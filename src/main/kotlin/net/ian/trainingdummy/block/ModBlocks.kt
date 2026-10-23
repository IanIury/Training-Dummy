package net.ian.trainingdummy.block

import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.block.custom.DisplayDummyBlock
import net.ian.trainingdummy.block.custom.DummyManagementBlock
import net.ian.trainingdummy.util.getValue
import net.minecraft.world.level.block.Block
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister

object ModBlocks {

    val REGISTRY: DeferredRegister.Blocks = DeferredRegister.createBlocks(TrainingDummy.ID)

    val EXAMPLE_BLOCK: Block by REGISTRY.registerBlock("example_block") { props ->
        Block(props.lightLevel { 15 }.strength(3.0f))
    }

    val DISPLAY_DUMMY_BLOCK: DisplayDummyBlock by REGISTRY.registerBlock("display_dummy") { props ->
        DisplayDummyBlock(
            props
                .strength(0.3f)
                .lightLevel { 1 }
                .noOcclusion()
        )
    }

    val DUMMY_MANAGEMENT_BLOCK: DummyManagementBlock by REGISTRY.registerBlock("dummy_management_block") { props ->
        DummyManagementBlock(
            props.strength(1f)
        )
    }

    fun register(bus: IEventBus) {
        REGISTRY.register(bus)
    }
}
