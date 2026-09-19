package net.ian.trainingdummy.client.layer

import com.mojang.blaze3d.vertex.PoseStack
import net.ian.trainingdummy.client.model.DummyModel
import net.ian.trainingdummy.entity.DummyEntity
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.HeadedModel
import net.minecraft.client.model.geom.EntityModelSet
import net.minecraft.client.renderer.ItemInHandRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer
import net.minecraft.world.entity.LivingEntity


class DummyCustomHeadLayer(
    renderer: RenderLayerParent<DummyEntity, DummyModel>,
    modelSet: EntityModelSet,
    itemInHandRenderer: ItemInHandRenderer
) : CustomHeadLayer<DummyEntity, DummyModel>(
    renderer,
    modelSet,
    itemInHandRenderer
) {

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
        poseStack.pushPose()
        poseStack.translate(0.0, 1.5, 0.0)
        val dummyModel = this.parentModel
        if (dummyModel != null) {
            val wobble = dummyModel.wobble_node
            val wobbleXRot = wobble.xRot
            val wobbleZRot = wobble.zRot
            poseStack.mulPose(com.mojang.math.Axis.XP.rotation(wobbleXRot))
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(wobbleZRot))
            poseStack.scale(
                wobble.xScale,
                wobble.yScale,
                wobble.zScale
            )
        }
        super.render(
            poseStack,
            buffer,
            packedLight,
            entity,
            limbSwing,
            limbSwingAmount,
            partialTick,
            ageInTicks,
            netHeadYaw,
            headPitch
        )
        poseStack.popPose()
    }
}