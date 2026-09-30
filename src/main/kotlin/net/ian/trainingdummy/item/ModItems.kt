package net.ian.trainingdummy.item

import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.item.custom.TrainingDummySpawnItem
import net.ian.trainingdummy.item.custom.TrainingModuleItem
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.Item
import net.minecraft.world.item.component.CustomData
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.getValue

object ModItems {

    val REGISTRY = DeferredRegister.createItems(TrainingDummy.ID)

    val DUMMY_ITEM_SPAWN by REGISTRY.register("dummy_item_spawn") { ->
        TrainingDummySpawnItem(Item.Properties().stacksTo(1))
    }

    val TRAINING_MODULE_ITEM by REGISTRY.register("training_module_item") { ->
        TrainingModuleItem(Item.Properties())
    }

    fun register(bus: IEventBus) {
        REGISTRY.register(MOD_BUS)
    }

}