package net.ian.trainingdummy

import net.ian.trainingdummy.block.ModBlockEntities
import net.ian.trainingdummy.block.ModBlocks
import net.ian.trainingdummy.client.ClientSetup
import net.ian.trainingdummy.entity.ModEntities
import net.ian.trainingdummy.init.components.ModDataComponents
import net.ian.trainingdummy.init.serializers.ModDataSerializersRegistry
import net.ian.trainingdummy.item.ModCreativeModelTabs
import net.ian.trainingdummy.item.ModItems
import net.ian.trainingdummy.network.ServerboundChangeModePayload
import net.ian.trainingdummy.network.ServerboundDummyManagementActionPayload
import net.ian.trainingdummy.network.ClientboundRanksPayload
import net.ian.trainingdummy.network.ServerboundRequestRanksPayload
import net.ian.trainingdummy.network.ServerboundSetDetailedDamageWindowPayload
import net.ian.trainingdummy.network.ServerboundSetWindowShapePayload
import net.ian.trainingdummy.screen.ModMenuTypes
import net.ian.trainingdummy.server.ServerSetup
import net.minecraft.client.Minecraft
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist

/**
 * Main mod class.
 *
 * An example for blocks is in the `blocks` package of this mod.
 */
@Mod(TrainingDummy.ID)
@EventBusSubscriber
object TrainingDummy {
    const val ID = "trainingdummy"

    // the logger for our mod
    val LOGGER: Logger = LogManager.getLogger(ID)

    init {
        LOGGER.log(Level.INFO, "Hello world!")

        ModDataSerializersRegistry.register(MOD_BUS)

        // Register the KDeferredRegister to the mod-specific event bus
        ModBlocks.register(MOD_BUS)
        ModBlockEntities.register((MOD_BUS))
        ModItems.register(MOD_BUS)
        ModEntities.register(MOD_BUS)
        ModDataComponents.register(MOD_BUS)
        ModCreativeModelTabs.register(MOD_BUS)
        ModMenuTypes.register(MOD_BUS)

        MOD_BUS.addListener(ModEntities::registerAttributes)



        val obj = runForDist(clientTarget = {
            //MOD_BUS.addListener(::onClientSetup)

            MOD_BUS.addListener(ClientSetup::onClientSetup)

            MOD_BUS.addListener(ClientSetup::registerClientExtensions)


            Minecraft.getInstance()
        }, serverTarget = {
            //MOD_BUS.addListener(::onServerSetup)
            MOD_BUS.addListener(ServerSetup::onServerSetup)

            "test"
        })

        println(obj)
    }

    @SubscribeEvent
    fun onCommonSetup(event: FMLCommonSetupEvent) {
        LOGGER.log(Level.INFO, "Hello! This is working!")
    }

    @SubscribeEvent
    fun register(event: RegisterPayloadHandlersEvent) {
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
