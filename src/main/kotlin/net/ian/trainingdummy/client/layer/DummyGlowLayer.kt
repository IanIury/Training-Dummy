package net.ian.trainingdummy.client.layer

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.ian.trainingdummy.client.model.DummyModel
import net.ian.trainingdummy.entity.DummyEntity
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.FastColor

class DummyGlowLayer(
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
        // if (!entity.isGlowActive()) return

        val dummyModel = this.parentModel ?: return
        val texture = this.getTextureLocation(entity)

        poseStack.pushPose()
        //dummyModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch)
        //poseStack.translate(0.0, -1.5, 0.0)
        //dummyModel.wobble_node.translateAndRotate(poseStack)



        val vertexConsumer: VertexConsumer = buffer.getBuffer(RenderType.eyes(texture))
        val color = 0x0577ef28

        /*coloredCutoutModelCopyLayerRender(this.parentModel,dummyModel,texture,poseStack,buffer,
            packedLight,entity,
            1f,1f,1f,1f,1f,1f,packedLight)

         */
        /*
        this.parentModel.renderToBuffer(
            poseStack,
            vertexConsumer,
            15728880, // Brilho total no escuro (Fullbright Lightmap)
            OverlayTexture.NO_OVERLAY,
            color
        )*/


        poseStack.popPose()
    }

}