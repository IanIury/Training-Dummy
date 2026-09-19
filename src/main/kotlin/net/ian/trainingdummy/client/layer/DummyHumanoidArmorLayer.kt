package net.ian.trainingdummy.client.layer

import com.mojang.blaze3d.vertex.PoseStack
import net.ian.trainingdummy.client.model.DummyModel
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer
import net.minecraft.client.resources.model.ModelManager
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity


class DummyHumanoidArmorLayer<T : LivingEntity, M : HumanoidModel<T>, A : HumanoidModel<T>>(
    rendererLayerParent: RenderLayerParent<T, M>,
    innerModel: A,
    outerModel: A,
    modelManager: ModelManager
) : HumanoidArmorLayer<T, M, A>(rendererLayerParent, innerModel, outerModel, modelManager) {

    override fun render(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        entity: T,
        limbSwing: Float,
        limbSwingAmount: Float,
        partialTick: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
        poseStack.pushPose()

        poseStack.translate(0.0, 1.5, 0.0)

        val dummyModel = this.parentModel as? DummyModel
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

            //poseStack.translate(0.0, -1.5, 0.0)
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

    override fun setPartVisibility(model: A, slot: EquipmentSlot) {
        model.setAllVisible(false)

        model.rightArm.xScale = 1.0f
        model.rightArm.yScale = 1.0f
        model.rightArm.zScale = 1.0f

        model.leftArm.xScale = 1.0f
        model.leftArm.yScale = 1.0f
        model.leftArm.zScale = 1.0f

        when (slot) {
            EquipmentSlot.HEAD -> {
                model.head.visible = true
                model.hat.visible = true
            }
            EquipmentSlot.CHEST -> {
                model.body.visible = true
                model.rightArm.visible = true
                model.leftArm.visible = true

                // Aplica a micro-expansão nas ombreiras/mangas para sair de cima do peito
                model.rightArm.xScale = 1.01f
                model.rightArm.yScale = 1.01f
                model.rightArm.zScale = 1.01f

                model.leftArm.xScale = 1.01f
                model.leftArm.yScale = 1.01f
                model.leftArm.zScale = 1.01f

            }
            EquipmentSlot.LEGS, EquipmentSlot.FEET -> {
                model.rightLeg.visible = true
                model.leftLeg.visible = false

            }
            else -> {}
        }
    }
}
