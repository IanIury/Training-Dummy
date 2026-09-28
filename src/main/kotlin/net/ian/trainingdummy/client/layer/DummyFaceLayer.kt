package net.ian.trainingdummy.client.layer

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.ian.trainingdummy.client.model.DummyModel
import net.ian.trainingdummy.entity.DummyEntity
import net.ian.trainingdummy.init.utils.ClientSkinUtils
import net.minecraft.client.model.geom.EntityModelSet
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.resources.ResourceLocation
import org.joml.Matrix4f

class DummyFaceLayer(
    renderer: RenderLayerParent<DummyEntity, DummyModel>,
    modelSet: EntityModelSet
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
        val dummyModel = this.parentModel ?: return
        if(entity.customName == null || entity.customName?.string.isNullOrBlank()) return
        val profile = entity.profilePlayer ?: return

        val playerSkin = ClientSkinUtils.getPlayerSkin(profile.gameProfile) ?: return

        poseStack.pushPose()

        dummyModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch)

        dummyModel.wobble_node.translateAndRotate(poseStack)
        poseStack.translate(0.0, -1.85, -0.26)

        val baseScale = 0.45f
        poseStack.scale(
            baseScale * dummyModel.wobble_node.xScale,
            baseScale * dummyModel.wobble_node.yScale,
            baseScale * dummyModel.wobble_node.zScale
        )


        val vertexBuilder = buffer.getBuffer(RenderType.entityCutoutNoCull(playerSkin.texture))
        val matrix = poseStack.last().pose()

        drawFaceQuad(
            matrix,
            vertexBuilder,
            packedLight,
            uMin = 0.125f,
            uMax = 0.25f,
            vMin = 0.125f,
            vMax = 0.25f
        )

        poseStack.popPose()
    }

    private fun drawFaceQuad(
        matrix: Matrix4f,
        builder: VertexConsumer,
        packedLight: Int,
        uMin: Float, uMax: Float,
        vMin: Float, vMax: Float
    ) {

        val overlay = OverlayTexture.NO_OVERLAY

        builder.addVertex(matrix, -0.5f, -0.5f, 0.0f)
            .setColor(255, 255, 255, 255)
            .setUv(uMin, vMin)
            .setOverlay(overlay)
            .setLight(packedLight)
            .setNormal(0f, 0f, 1f)

        builder.addVertex(matrix, 0.5f, -0.5f, 0.0f)
            .setColor(255, 255, 255, 255)
            .setUv(uMax, vMin)
            .setOverlay(overlay)
            .setLight(packedLight)
            .setNormal(0f, 0f, 1f)

        builder.addVertex(matrix, 0.5f, 0.5f, 0.0f)
            .setColor(255, 255, 255, 255)
            .setUv(uMax, vMax)
            .setOverlay(overlay)
            .setLight(packedLight)
            .setNormal(0f, 0f, 1f)

        builder.addVertex(matrix, -0.5f, 0.5f, 0.0f)
            .setColor(255, 255, 255, 255)
            .setUv(uMin, vMax)
            .setOverlay(overlay)
            .setLight(packedLight)
            .setNormal(0f, 0f, 1f)
    }

    override fun getTextureLocation(entity: DummyEntity): ResourceLocation {
        val skin = entity.profilePlayer?.gameProfile?.let { ClientSkinUtils.getPlayerSkin(it) }
        // Retorna a textura baixada da skin, ou a skin padrão (Steve) caso o perfil seja nulo
        return skin?.texture ?: DefaultPlayerSkin.getDefaultTexture()
    }
}