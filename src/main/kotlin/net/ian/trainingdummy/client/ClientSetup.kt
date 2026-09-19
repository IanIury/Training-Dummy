package net.ian.trainingdummy.client

import net.ian.trainingdummy.TrainingDummy.LOGGER
import net.ian.trainingdummy.client.model.DummyModel
import net.ian.trainingdummy.item.ModItems
import net.ian.trainingdummy.item.custom.DummyItemRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent
import org.apache.logging.log4j.Level


object ClientSetup {


    fun onClientSetup(event: FMLClientSetupEvent) {
        LOGGER.log(Level.INFO, "Initializing client...")
    }

    fun registerClientExtensions(event: RegisterClientExtensionsEvent) {
        // Registra o BEWLR no seu DummyItem
        event.registerItem(object : IClientItemExtensions {
            private val renderer by lazy { DummyItemRenderer() }

            override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
                return renderer
            }
        }, ModItems.DUMMY_ITEM_SPAWN)
    }


}