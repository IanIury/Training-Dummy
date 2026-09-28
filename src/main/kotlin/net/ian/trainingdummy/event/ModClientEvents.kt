package net.ian.trainingdummy.event

import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.screen.ModMenuTypes
import net.ian.trainingdummy.screen.custom.DummyScreen
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent

@EventBusSubscriber(modid = TrainingDummy.ID)
object ModClientEvents {

    @SubscribeEvent
    fun onRegisterMenuScreens(event: RegisterMenuScreensEvent) {

        event.register(ModMenuTypes.DUMMY_MENU.get(), ::DummyScreen)
    }

}