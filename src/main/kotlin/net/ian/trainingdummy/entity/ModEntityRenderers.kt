package net.ian.trainingdummy.entity

import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.client.model.DummyModel
import net.ian.trainingdummy.entity.entityrender.DummyRenderer
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.EntityRenderersEvent

@EventBusSubscriber(modid = TrainingDummy.ID , value = [Dist.CLIENT])
object ModEntityRenderers {

    @SubscribeEvent
    fun registerRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerEntityRenderer(ModEntities.DUMMY.get(),::DummyRenderer)
    }

    @SubscribeEvent
    fun registerLayerDefinitions(event : EntityRenderersEvent.RegisterLayerDefinitions) {
        event.registerLayerDefinition(DummyModel.LAYER_LOCATION) { DummyModel.createBodyLayer() }
    }
}