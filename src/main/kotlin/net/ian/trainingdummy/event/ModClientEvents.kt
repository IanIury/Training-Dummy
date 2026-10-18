package net.ian.trainingdummy.event

import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.screen.ModMenuTypes
import net.ian.trainingdummy.screen.custom.DummyScreen
import net.ian.trainingdummy.screen.custom.DummyManagementScreen
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent


@EventBusSubscriber(modid = TrainingDummy.ID, value = [Dist.CLIENT])
object ModClientEvents {

    @SubscribeEvent
    fun onRegisterMenuScreens(event: RegisterMenuScreensEvent) {

        event.register(ModMenuTypes.DUMMY_MENU.get(), ::DummyScreen)
        event.register(ModMenuTypes.DUMMY_MANAGEMENT_MENU.get(), ::DummyManagementScreen)
    }

}