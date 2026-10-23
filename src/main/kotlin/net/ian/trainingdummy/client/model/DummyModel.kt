package net.ian.trainingdummy.client.model

import com.mojang.blaze3d.vertex.PoseStack
import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.entity.entityrender.DummyRenderState
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.renderer.entity.state.HumanoidRenderState
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import net.minecraft.world.entity.HumanoidArm
import kotlin.math.exp

class DummyModel(val modelRoot: ModelPart) : HumanoidModel<DummyRenderState>(modelRoot.getChild("wobble_node")) {

    val wobble_node: ModelPart
    val body_feathers: ModelPart
    val jacket: ModelPart
    val left_sleeve: ModelPart
    val right_sleeve: ModelPart
    val left_pants: ModelPart
    val base_plate: ModelPart

    init {
        this.wobble_node = modelRoot.getChild("wobble_node")
        this.body_feathers = this.body.getChild("body_feathers")
        this.jacket = this.wobble_node.getChild("jacket")
        this.left_sleeve = this.wobble_node.getChild("left_sleeve")
        this.right_sleeve = this.wobble_node.getChild("right_sleeve")
        this.left_pants = this.wobble_node.getChild("left_pants")
        this.base_plate = modelRoot.getChild("base_plate")
    }

    override fun translateToHand(state: HumanoidRenderState, side: HumanoidArm, poseStack: PoseStack) {
        this.wobble_node.translateAndRotate(poseStack)
        val arm = this.getArm(side)
        arm.translateAndRotate(poseStack)
        val offsetX = if (side == HumanoidArm.RIGHT) -1.0f / 16.0f else 1.0f / 16.0f
        poseStack.translate(offsetX, 0.0f, 0.0f)
    }

    override fun setupAnim(state: DummyRenderState) {
        modelRoot.allParts.forEach { it.resetPose() }
        val headIsEmpty = state.headEquipment.isEmpty
        val ageInTicks = state.ageInTicks
        val entity = state.dummyEntity
        hat.allParts.forEach { it.visible = headIsEmpty }

        val spawnAnim = entity?.spawnAnimation
        if (spawnAnim != null && spawnAnim.isStarted) {
            val time = spawnAnim.getTimeInMillis(ageInTicks).toFloat()
            if (time < 800.0f) {
                val progress = Mth.clamp(time / 800.0f, 0.0f, 1.0f)
                val smooth = 1.0f - (1.0f - progress) * (1.0f - progress)
                wobble_node.yScale = Mth.lerp(smooth, 0.15f, 1.0f)
                val wobble = Mth.sin((progress * Mth.PI * 2.0f).toDouble()) * 0.08f
                wobble_node.zRot = wobble
            } else {
                wobble_node.yScale = 1.0f
                wobble_node.zRot = 0.0f
                spawnAnim.stop()
            }
        }

        val hitAnim = entity?.hitAnimation
        if (hitAnim != null && hitAnim.isStarted) {
            val timeMs = hitAnim.getTimeInMillis(ageInTicks).toFloat()
            val durationMs = 1000.0f

            if (timeMs < durationMs) {
                val timeInTicks = timeMs / 50.0f
                val frequency = 0.6f
                val decay = 0.2f
                val wave = (Mth.sin((timeInTicks * frequency).toDouble()) * exp((-timeInTicks * decay).toDouble())).toFloat()
                val maxAngleRad = Math.toRadians(30.0).toFloat() * 1.5f

                val targetXRot = hitAnim.hitPitch * wave * maxAngleRad
                val targetZRot = hitAnim.hitRoll * wave * maxAngleRad

                val blendFactor = Mth.clamp(timeMs / 150.0f, 0.0f, 1.0f)
                val smoothBlend = blendFactor * blendFactor * (3.0f - 2.0f * blendFactor)

                val offsetDecay = exp((-timeInTicks * 0.5f).toDouble()).toFloat()
                val residualX = hitAnim.offsetPositionX * offsetDecay
                val residualZ = hitAnim.offsetPositionZ * offsetDecay

                wobble_node.xRot = Mth.lerp(smoothBlend, residualX, targetXRot)
                wobble_node.zRot = Mth.lerp(smoothBlend, residualZ, targetZRot)
            } else {
                wobble_node.xRot = 0.0f
                wobble_node.zRot = 0.0f
                hitAnim.offsetPositionX = 0.0f
                hitAnim.offsetPositionZ = 0.0f
                hitAnim.isNewHitRequested = false
                hitAnim.stop()
            }

            if (hitAnim.isNewHitRequested) {
                hitAnim.offsetPositionX = wobble_node.xRot
                hitAnim.offsetPositionZ = wobble_node.zRot
                hitAnim.isNewHitRequested = false
            }
        }

        val maceAnim = entity?.maceAnimation
        if (maceAnim != null && maceAnim.isStarted) {
            val time = maceAnim.getTimeInMillis(ageInTicks).toFloat()
            if ((time in 2000.0f..2500.0f || maceAnim.maceScaleSizeSmooth) && maceAnim.maceScaleSize < 1.0f) {
                val progress = Mth.clamp((time - 2000.0f) / 500.0f, 0.01f, 1.0f)
                val smooth = 1.0f - (1.0f - progress) * (1.0f - progress)
                maceAnim.maceScaleSize = Mth.lerp(smooth, maceAnim.maceScaleSize, 1.0f)
            } else if (time > 2500.0f) {
                maceAnim.maceScaleSizeNormalizer = true
            }

            if (maceAnim.maceScaleSizeNormalizer || maceAnim.maceScaleSize >= 1.0f) {
                maceAnim.maceScaleSize = 1.0f
                maceAnim.maceScaleSizeNormalizer = false
                maceAnim.maceScaleSizeSmooth = false
                wobble_node.yScale = 1.0f
                maceAnim.stop()
            } else {
                wobble_node.yScale = maceAnim.maceScaleSize
            }
        }
    }

    companion object {
        val LAYER_LOCATION = ModelLayerLocation(
            Identifier.fromNamespaceAndPath(TrainingDummy.ID, "training_dummy"),
            "main"
        )

        fun createBodyLayer(): LayerDefinition {
            val meshdefinition = MeshDefinition()
            val partdefinition = meshdefinition.root

            val wobble_node = partdefinition.addOrReplaceChild(
                "wobble_node",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, 24.0f, 0.0f)
            )

            val head = wobble_node.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(32, 47)
                    .addBox(-1.0f, 0.0f, -1.0f, 2.0f, 2.0f, 2.0f, CubeDeformation(0.0f))
                    .texOffs(32, 17).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -25.75f, 0.0f)
            )

            val hat = head.addOrReplaceChild(
                "hat",
                CubeListBuilder.create().texOffs(0, 17)
                    .addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, CubeDeformation(0.1f))
                    .texOffs(28, 15).addBox(0.0f, -12.0f, -3.0f, 0.0f, 4.0f, 6.0f, CubeDeformation(0.0f)),
                PartPose.ZERO
            )

            val taproot = hat.addOrReplaceChild(
                "taproot",
                CubeListBuilder.create().texOffs(28, 15)
                    .addBox(0.0f, -12.0f, -3.0f, 0.0f, 4.0f, 6.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 0.0f, 0.0f)
            )

            taproot.addOrReplaceChild(
                "taproot_2_r1",
                CubeListBuilder.create().texOffs(28, 11)
                    .addBox(1.0f, -4.0f, -5.0f, 0.0f, 4.0f, 6.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(2.0f, -8.0f, 1.0f, 0.0f, 1.5708f, 0.0f)
            )

            val body = wobble_node.addOrReplaceChild(
                "body",
                CubeListBuilder.create().texOffs(0, 33)
                    .addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -24.75f, 0.0f)
            )

            val body_feathers = body.addOrReplaceChild(
                "body_feathers",
                CubeListBuilder.create().texOffs(2, 49)
                    .addBox(-4.0f, -11.5f, -2.01f, 8.0f, 1.0f, 0.01f, CubeDeformation(0.02f))
                    .texOffs(4, 49).addBox(-4.0f, -11.5f, 2.01f, 8.0f, 1.0f, 0.01f, CubeDeformation(0.02f))
                    .texOffs(10, 49).addBox(-4.0f, 0.5f, -2.01f, 8.0f, 1.0f, 0.01f, CubeDeformation(0.02f))
                    .texOffs(10, 49).addBox(-4.0f, 0.5f, 2.01f, 8.0f, 1.0f, 0.01f, CubeDeformation(0.02f)),
                PartPose.offset(0.0f, 11.0f, 0.0f)
            )

            body_feathers.addOrReplaceChild(
                "body_feather_r1",
                CubeListBuilder.create().texOffs(4, 49)
                    .addBox(-4.0f, -11.5f, -2.0f, 8.0f, 1.0f, 0.01f, CubeDeformation(0.02f)),
                PartPose.offsetAndRotation(0.0f, -3.7f, 6.41f, 0.6981f, 0.0f, 0.0f)
            )

            body_feathers.addOrReplaceChild(
                "body_feather_r2",
                CubeListBuilder.create().texOffs(9, 49)
                    .addBox(-4.0f, -11.5f, -2.0f, 8.0f, 1.0f, 0.01f, CubeDeformation(0.02f)),
                PartPose.offsetAndRotation(0.0f, 7.9f, 10.81f, 0.6981f, 0.0f, 0.0f)
            )

            body_feathers.addOrReplaceChild(
                "body_feather_r3",
                CubeListBuilder.create().texOffs(4, 49)
                    .addBox(-4.0f, -11.5f, -2.0f, 8.0f, 1.0f, 0.01f, CubeDeformation(0.02f)),
                PartPose.offsetAndRotation(0.0f, -0.7f, -2.91f, -0.6283f, 0.0f, 0.0f)
            )

            body_feathers.addOrReplaceChild(
                "body_feather_r4",
                CubeListBuilder.create().texOffs(6, 49)
                    .addBox(-4.0f, -11.5f, -2.0f, 8.0f, 1.0f, 0.01f, CubeDeformation(0.02f)),
                PartPose.offsetAndRotation(0.0f, 11.0f, -7.01f, -0.6283f, 0.0f, 0.0f)
            )

            body_feathers.addOrReplaceChild(
                "body_feather_r5",
                CubeListBuilder.create().texOffs(18, 49)
                    .addBox(0.0f, -11.5f, -2.0f, 4.0f, 1.0f, 0.01f, CubeDeformation(0.02f))
                    .texOffs(10, 49).addBox(0.0f, -23.5f, -2.0f, 4.0f, 1.0f, 0.01f, CubeDeformation(0.02f)),
                PartPose.offsetAndRotation(-6.01f, 12.0f, -2.0f, 0.0f, -1.5708f, 0.0f)
            )

            body_feathers.addOrReplaceChild(
                "body_feather_r6",
                CubeListBuilder.create().texOffs(13, 49)
                    .addBox(0.0f, -11.5f, -2.0f, 4.0f, 1.0f, 0.01f, CubeDeformation(0.02f))
                    .texOffs(8, 49).addBox(0.0f, -23.5f, -2.0f, 4.0f, 1.0f, 0.01f, CubeDeformation(0.02f)),
                PartPose.offsetAndRotation(2.01f, 12.0f, -2.0f, 0.0f, -1.5708f, 0.0f)
            )

            body_feathers.addOrReplaceChild(
                "body_feather_r7",
                CubeListBuilder.create().texOffs(14, 49)
                    .addBox(0.0f, -11.5f, -2.0f, 4.0f, 1.0f, 0.01f, CubeDeformation(0.02f)),
                PartPose.offsetAndRotation(9.91f, 10.5f, -2.0f, 0.0f, -1.5708f, -0.7069f)
            )

            body_feathers.addOrReplaceChild(
                "body_feather_r8",
                CubeListBuilder.create().texOffs(10, 49)
                    .addBox(0.0f, -11.5f, -2.0f, 4.0f, 1.0f, 0.01f, CubeDeformation(0.02f)),
                PartPose.offsetAndRotation(1.61f, -1.2f, -2.0f, 0.0f, -1.5708f, -0.7069f)
            )

            body_feathers.addOrReplaceChild(
                "body_feather_r9",
                CubeListBuilder.create().texOffs(13, 49)
                    .addBox(0.0f, -11.5f, -2.0f, 4.0f, 1.0f, 0.01f, CubeDeformation(0.02f)),
                PartPose.offsetAndRotation(-12.91f, 7.8f, -2.0f, 0.0f, -1.5708f, 0.7069f)
            )

            body_feathers.addOrReplaceChild(
                "body_feather_r10",
                CubeListBuilder.create().texOffs(10, 49)
                    .addBox(0.0f, -11.5f, -2.0f, 4.0f, 1.0f, 0.01f, CubeDeformation(0.02f)),
                PartPose.offsetAndRotation(-4.41f, -3.9f, -2.0f, 0.0f, -1.5708f, 0.7069f)
            )

            wobble_node.addOrReplaceChild(
                "jacket",
                CubeListBuilder.create().texOffs(0, 17)
                    .addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, CubeDeformation(0.25f)),
                PartPose.offset(0.0f, -24.75f, 0.0f)
            )

            wobble_node.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create().texOffs(32, 33)
                    .addBox(0.0f, -1.0f, -1.0f, 2.0f, 12.0f, 2.0f, CubeDeformation(0.0f))
                    .texOffs(40, 47).addBox(-1.0f, -1.0f, -1.0f, 1.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(5.0f, -23.75f, 0.0f)
            )

            wobble_node.addOrReplaceChild(
                "left_sleeve",
                CubeListBuilder.create().texOffs(55, 50)
                    .addBox(0.0f, -1.0f, -1.0f, 2.0f, 12.0f, 2.0f, CubeDeformation(0.25f))
                    .texOffs(40, 47).addBox(-1.0f, -1.0f, -1.0f, 1.0f, 2.0f, 2.0f, CubeDeformation(0.25f)),
                PartPose.offset(5.0f, -23.75f, 0.0f)
            )

            wobble_node.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create().texOffs(40, 33)
                    .addBox(-2.0f, -1.0f, -1.0f, 2.0f, 12.0f, 2.0f, CubeDeformation(0.0f))
                    .texOffs(46, 47).addBox(0.0f, -1.0f, -1.0f, 1.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-5.0f, -23.75f, 0.0f)
            )

            wobble_node.addOrReplaceChild(
                "right_sleeve",
                CubeListBuilder.create().texOffs(56, 50)
                    .addBox(-2.0f, -1.0f, -1.0f, 2.0f, 12.0f, 2.0f, CubeDeformation(0.25f))
                    .texOffs(46, 47).addBox(0.0f, -1.0f, -1.0f, 1.0f, 2.0f, 2.0f, CubeDeformation(0.25f)),
                PartPose.offset(-5.0f, -23.75f, 0.0f)
            )

            wobble_node.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create().texOffs(24, 33)
                    .addBox(-0.9f, -1.0f, -1.0f, 2.0f, 13.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-0.1f, -12.75f, 0.0f)
            )

            wobble_node.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create().texOffs(24, 33)
                    .addBox(-0.9f, -1.0f, -1.0f, 2.0f, 13.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-0.1f, -12.75f, 0.0f)
            )

            wobble_node.addOrReplaceChild(
                "left_pants",
                CubeListBuilder.create().texOffs(24, 48)
                    .addBox(-0.9f, -12.0f, -1.0f, 2.0f, 13.0f, 2.0f, CubeDeformation(0.25f)),
                PartPose.offset(-0.1f, -1.75f, 0.0f)
            )

            partdefinition.addOrReplaceChild(
                "base_plate",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-8.0f, -1.0f, -8.0f, 16.0f, 1.0f, 16.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 24.0f, 0.0f)
            )

            return LayerDefinition.create(meshdefinition, 64, 64)
        }
    }
}
