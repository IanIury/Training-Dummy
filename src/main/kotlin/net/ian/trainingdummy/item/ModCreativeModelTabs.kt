package net.ian.trainingdummy.item

import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.block.ModBlocks
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import thedarkcolour.kotlinforforge.neoforge.forge.getValue

object ModCreativeModelTabs {

    val REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,TrainingDummy.ID)

    val TRAINING_DUMMY_TAB by REGISTRY.register("training_dummy_tab") { ->
        CreativeModeTab.builder().icon {ItemStack(ModItems.DUMMY_ITEM_SPAWN) }
            .title(Component.translatable("creativetab.trainingdummy.training_dummy_tab"))
            .displayItems { parameters, output ->
                output.accept { ModItems.DUMMY_ITEM_SPAWN }
                output.accept(ModBlocks.DISPLAY_DUMMY_BLOCK)
            }.build()
    }

    fun register(eventBus : IEventBus){
        REGISTRY.register(eventBus)
    }
}