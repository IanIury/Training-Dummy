package net.ian.trainingdummy.client.layer

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.ian.trainingdummy.client.model.DummyModel
import net.ian.trainingdummy.entity.DummyEntity
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FastColor

class DummyOutlineLayer(
    renderer: RenderLayerParent<DummyEntity, DummyModel>
) : RenderLayer<DummyEntity, DummyModel>(renderer) {

    private val WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("suomodid", "textures/entity/white.png")

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

        //if (!entity.shouldHaveOutline) return

        val model = this.parentModel


        model.copyPropertiesTo(model)
        model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick)
        model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch)

        poseStack.pushPose()

        val outlineThickness = 1.06f
        poseStack.scale(outlineThickness, outlineThickness, outlineThickness)

        val outlineColor = FastColor.ARGB32.color(255, 255, 200, 0)

        val vertexConsumer: VertexConsumer = buffer.getBuffer(RenderType.eyes(WHITE_TEXTURE))

        model.renderToBuffer(
            poseStack,
            vertexConsumer,
            15728880, // Brilho total (Fullbright) para a borda brilhar no escuro
            LivingEntityRenderer.getOverlayCoords(entity, 0.0f),
            outlineColor
        )

        poseStack.popPose()
    }
}