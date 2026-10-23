package net.ian.trainingdummy

import net.ian.trainingdummy.block.ModBlockEntities
import net.ian.trainingdummy.block.ModBlocks
import net.ian.trainingdummy.client.ClientSetup
import net.ian.trainingdummy.entity.ModEntities
import net.ian.trainingdummy.init.components.ModDataComponents
import net.ian.trainingdummy.init.serializers.ModDataSerializersRegistry
import net.ian.trainingdummy.item.ModCreativeModelTabs
import net.ian.trainingdummy.item.ModItems
import net.ian.trainingdummy.network.ClientboundRanksPayload
import net.ian.trainingdummy.network.ServerboundChangeModePayload
import net.ian.trainingdummy.network.ServerboundDummyManagementActionPayload
import net.ian.trainingdummy.network.ServerboundRequestRanksPayload
import net.ian.trainingdummy.network.ServerboundSetDetailedDamageWindowPayload
import net.ian.trainingdummy.network.ServerboundSetWindowShapePayload
import net.ian.trainingdummy.screen.ModMenuTypes
import net.ian.trainingdummy.server.ServerSetup
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * Main mod class.
 */
@Mod(TrainingDummy.ID)
class TrainingDummy(bus: IEventBus, dist: Dist) {
    companion object {
        const val ID = "trainingdummy"
        @JvmField
        val LOGGER: Logger = LogManager.getLogger(ID)
    }

    init {
        LOGGER.log(Level.INFO, "Initializing TrainingDummy mod...")

        ModDataSerializersRegistry.register(bus)

        // Register the DeferredRegisters to the mod-specific event bus
        ModBlocks.register(bus)
        ModBlockEntities.register(bus)
        ModItems.register(bus)
        ModEntities.register(bus)
        ModDataComponents.register(bus)
        ModCreativeModelTabs.register(bus)
        ModMenuTypes.register(bus)

        bus.addListener(ModEntities::registerAttributes)
        bus.addListener(::onCommonSetup)
        bus.addListener(::registerPayloads)

        if (dist == Dist.CLIENT) {
            bus.addListener(ClientSetup::onClientSetup)
            bus.addListener(ClientSetup::registerClientExtensions)
            bus.addListener(ClientSetup::registerSpecialModelRenderers)
        } else {
            bus.addListener(ServerSetup::onServerSetup)
        }
    }

    fun onCommonSetup(event: FMLCommonSetupEvent) {
        LOGGER.log(Level.INFO, "Training Dummy common setup complete.")
    }

    fun registerPayloads(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar("1.0.0")

        registrar.playToServer(
            ServerboundChangeModePayload.TYPE,
            ServerboundChangeModePayload.STREAM_CODEC,
            ServerboundChangeModePayload::handle
        )
        registrar.playToServer(
            ServerboundSetDetailedDamageWindowPayload.TYPE,
            ServerboundSetDetailedDamageWindowPayload.STREAM_CODEC,
            ServerboundSetDetailedDamageWindowPayload::handle
        )
        registrar.playToServer(
            ServerboundDummyManagementActionPayload.TYPE,
            ServerboundDummyManagementActionPayload.STREAM_CODEC,
            ServerboundDummyManagementActionPayload::handle
        )
        registrar.playToServer(
            ServerboundSetWindowShapePayload.TYPE,
            ServerboundSetWindowShapePayload.STREAM_CODEC,
            ServerboundSetWindowShapePayload::handle
        )
        registrar.playToServer(
            ServerboundRequestRanksPayload.TYPE,
            ServerboundRequestRanksPayload.STREAM_CODEC,
            ServerboundRequestRanksPayload::handle
        )
        registrar.playToClient(
            ClientboundRanksPayload.TYPE,
            ClientboundRanksPayload.STREAM_CODEC,
            ClientboundRanksPayload::handle
        )
    }
}
