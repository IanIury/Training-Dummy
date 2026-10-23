package net.ian.trainingdummy.client

import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.client.renderer.DummySpecialRenderer
import net.minecraft.resources.Identifier
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent
import org.apache.logging.log4j.Level

object ClientSetup {

    fun onClientSetup(event: FMLClientSetupEvent) {
        TrainingDummy.LOGGER.log(Level.INFO, "Initializing client...")
    }

    fun registerClientExtensions(event: RegisterClientExtensionsEvent) {
    }

    fun registerSpecialModelRenderers(event: RegisterSpecialModelRendererEvent) {
        event.register(
            Identifier.fromNamespaceAndPath(TrainingDummy.ID, "dummy"),
            DummySpecialRenderer.Unbaked.MAP_CODEC
        )
    }
}
