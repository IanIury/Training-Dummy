package net.ian.trainingdummy.block

import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.block.custom.DisplayDummyBlock
import net.ian.trainingdummy.block.entityblock.DisplayDummyBlockEntity
import net.ian.trainingdummy.block.entityblock.DummyManagementBlockEntity
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.getValue

object ModBlockEntities {

    val REGISTRY = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, TrainingDummy.ID)

    val DISPLAY_DUMMY_BE by REGISTRY.register("display_dummy_be"){ ->
        BlockEntityType.Builder.of(
            ::DisplayDummyBlockEntity, ModBlocks.DISPLAY_DUMMY_BLOCK
        )
            .build(null)
    }

    val DUMMY_MANAGEMENT_BE by REGISTRY.register("dummy_management_be"){ ->
        BlockEntityType.Builder.of(
            ::DummyManagementBlockEntity, ModBlocks.DUMMY_MANAGEMENT_BLOCK
        )

            .build(null)
    }

    fun register(bus: IEventBus) {
        REGISTRY.register(MOD_BUS)
    }

}