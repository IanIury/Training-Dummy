package net.ian.trainingdummy

import net.ian.trainingdummy.item.MyItemModelProvider
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.data.event.GatherDataEvent


@EventBusSubscriber(modid = TrainingDummy.ID)
object TrainingDummyDataGen {

    @SubscribeEvent
    fun getherClientData(event: GatherDataEvent){
        val generator = event.generator
        val output = generator.packOutput
        val existingFileHelper = event.existingFileHelper

        generator.addProvider(
            event.includeClient(), MyItemModelProvider(output, existingFileHelper))
    }
}