package net.ian.trainingdummy.block

import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.block.custom.DisplayDummyBlock
import net.ian.trainingdummy.block.custom.DummyManagementBlock
import net.ian.trainingdummy.item.ModItems
import net.ian.trainingdummy.item.custom.DisplayDummyBlockItem
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredRegister
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

// THIS LINE IS REQUIRED FOR USING PROPERTY DELEGATES
import thedarkcolour.kotlinforforge.neoforge.forge.getValue
import java.util.function.Supplier

object ModBlocks {

    val REGISTRY = DeferredRegister.createBlocks(TrainingDummy.ID)

    val EXAMPLE_BLOCK by REGISTRY.register("example_block") { ->
        Block(BlockBehaviour.Properties.of().lightLevel { 15 }.strength(3.0f))
    }

    val DISPLAY_DUMMY_BLOCK by display_dummyRegister("display_dummy") {
        DisplayDummyBlock(BlockBehaviour.Properties.of()
            .strength(0.3f)
            .lightLevel{1}
            .noOcclusion()

        )
    }

    val DUMMY_MANAGEMENT_BLOCK by display_dummyRegister("dummy_management_block") {
        DummyManagementBlock(BlockBehaviour.Properties.of()
            .strength(1f)
        )
    }

    fun register(bus: IEventBus) {
        REGISTRY.register(MOD_BUS)
    }

    private fun <T : Block> registerBlock(name : String, registerItem : Boolean = false ,block : Supplier<T>) : DeferredBlock<T> {
        val toReturn : DeferredBlock<T> = REGISTRY.register(name,block)
        if(registerItem){registerBlockItem(name,toReturn)}
        return toReturn
    }

    private fun <T : Block> registerBlockItem(name: String, block : DeferredBlock<T>) {
        ModItems.REGISTRY.registerItem(name){ BlockItem(block.get(), Item.Properties()) }
    }

    private fun <T : Block> display_dummyRegister(name: String, block: Supplier<T>): DeferredBlock<T> {
        val toReturn: DeferredBlock<T> = REGISTRY.register(name, block)

        ModItems.REGISTRY.registerItem(name) {
            DisplayDummyBlockItem(toReturn.get(), Item.Properties())
        }

        return toReturn
    }


}
