package net.ian.trainingdummy.item

import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.item.custom.TrainingDummySpawnItem
import net.minecraft.world.item.Item
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import thedarkcolour.kotlinforforge.neoforge.forge.getValue

object ModItems {

    val REGISTRY = DeferredRegister.createItems(TrainingDummy.ID)

    val DUMMY_ITEM_SPAWN by REGISTRY.register("dummy_item_spawn") { ->
        TrainingDummySpawnItem(Item.Properties().stacksTo(3))
    }


}