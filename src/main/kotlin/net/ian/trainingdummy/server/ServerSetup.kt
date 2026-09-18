package net.ian.trainingdummy.server

import net.ian.trainingdummy.TrainingDummy.LOGGER
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import org.apache.logging.log4j.Level

object ServerSetup {

    /**
     * Fired on the global Forge bus.
     */
    fun onServerSetup(event: FMLDedicatedServerSetupEvent) {
        LOGGER.log(Level.INFO, "Server starting...")
    }

}