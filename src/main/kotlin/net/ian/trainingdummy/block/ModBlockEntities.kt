package net.ian.trainingdummy.block

import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.block.entityblock.DisplayDummyBlockEntity
import net.ian.trainingdummy.block.entityblock.DummyManagementBlockEntity
import net.ian.trainingdummy.util.getValue
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister

object ModBlockEntities {

    val REGISTRY: DeferredRegister<BlockEntityType<*>> = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, TrainingDummy.ID)

    val DISPLAY_DUMMY_BE: BlockEntityType<DisplayDummyBlockEntity> by REGISTRY.register("display_dummy_be") { ->
        BlockEntityType(::DisplayDummyBlockEntity, setOf(ModBlocks.DISPLAY_DUMMY_BLOCK))
    }

    val DUMMY_MANAGEMENT_BE: BlockEntityType<DummyManagementBlockEntity> by REGISTRY.register("dummy_management_be") { ->
        BlockEntityType(::DummyManagementBlockEntity, setOf(ModBlocks.DUMMY_MANAGEMENT_BLOCK))
    }

    fun register(bus: IEventBus) {
        REGISTRY.register(bus)
    }
}
