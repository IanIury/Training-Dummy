package net.ian.trainingdummy.client.layer

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.ian.trainingdummy.client.model.DummyModel
import net.ian.trainingdummy.client.render.DummyGlowRenderType
import net.ian.trainingdummy.entity.DummyEntity
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer

class DummyOutlineLayer(
    renderer: RenderLayerParent<DummyEntity, DummyModel>
) : RenderLayer<DummyEntity, DummyModel>(renderer) {

    override fun render(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        entity: DummyEntity,
        limbSwing: Float,
        limbSwingAmount: Float,
        partialTick: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
        val model = parentModel
        poseStack.pushPose()
        try {
            poseStack.scale(1.06f, 1.06f, 1.06f)
            val vertexConsumer: VertexConsumer =
                buffer.getBuffer(DummyGlowRenderType.outlineNoXray(getTextureLocation(entity)))
            model.renderToBuffer(
                poseStack,
                vertexConsumer,
                15728880,
                LivingEntityRenderer.getOverlayCoords(entity, 0f),
                0xFFFFC800.toInt()
            )
        } finally {
            poseStack.popPose()
        }
    }
}
