package net.ian.trainingdummy.client

import net.ian.trainingdummy.TrainingDummy.LOGGER
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import org.apache.logging.log4j.Level

object ClientSetup {

    /**
     * This is used for initializing client specific
     * things such as renderers and keymaps
     * Fired on the mod specific event bus.
     */
    fun onClientSetup(event: FMLClientSetupEvent) {
        LOGGER.log(Level.INFO, "Initializing client...")
    }

}