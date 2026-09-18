package net.ian.trainingdummy.client.model

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.entity.DummyEntity
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import kotlin.math.exp

/*
class DummyModelOLD<T : DummyEntity>(val root: ModelPart) : EntityModel<T>() {

    val pila2: ModelPart = root.getChild("pila2")
    val body: ModelPart = pila2.getChild("body")
    val head: ModelPart = body.getChild("head")
    val headwear: ModelPart = head.getChild("headwear")
    val bodyFeathers: ModelPart = body.getChild("body feathers")
    val leftArm: ModelPart = body.getChild("left_arm")
    val rightArm: ModelPart = body.getChild("right_arm")
    val base2: ModelPart = root.getChild("base2")

    override fun setupAnim(
        entity: T,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
        root.allParts.forEach { it.resetPose() }

        // Animação de impacto (Hit)
        val timeSinceHit = entity.tickCount.toFloat() - entity.hitTick.toFloat()
        if (timeSinceHit in 0.0f..20.0f) {
            val frequency = 0.6f
            val decay = 0.2f
            val wave = (Mth.sin(timeSinceHit * frequency) * exp((-timeSinceHit * decay).toDouble())).toFloat()

            val maxAngleRad = Math.toRadians(30.0).toFloat() * entity.hitStrength
            val pitchRotation = entity.hitPitchAngle * wave * maxAngleRad
            val rollRotation = entity.hitRollAngle * wave * maxAngleRad

            this.pila2.xRot += pitchRotation
            this.pila2.zRot += rollRotation
        }
    }

    override fun renderToBuffer(
        poseStack: PoseStack,
        buffer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int,
        color: Int
    ) {
        pila2.render(poseStack, buffer, packedLight, packedOverlay, color)
        base2.render(poseStack, buffer, packedLight, packedOverlay, color)
    }



    companion object {
        val LAYER_LOCATION = ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(TrainingDummy.ID, "training_dummy"), "main"
        )


        fun createBodyLayer(): LayerDefinition {
            val meshdefinition = MeshDefinition()
            val partdefinition = meshdefinition.root


            val pila2 = partdefinition.addOrReplaceChild(
                "pila2",
                CubeListBuilder.create().texOffs(24, 33).addBox(-0.9f, -12.0f, -1.0f, 2.0f, 13.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-0.1f, 23.0f, 0.0f)
            )

            val body = pila2.addOrReplaceChild(
                "body",
                CubeListBuilder.create().texOffs(0, 33).addBox(-4.0f, -12.0f, -2.0f, 8.0f, 12.0f, 4.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.1f, -11.0f, 0.0f)
            )

            val head = body.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(32, 47).addBox(-1.0f, -1.0f, -1.0f, 2.0f, 2.0f, 2.0f, CubeDeformation(0.0f))
                    .texOffs(0, 17).addBox(-4.0f, -9.0f, -4.0f, 8.0f, 8.0f, 8.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -12.0f, 0.0f)
            )

            val headwear = head.addOrReplaceChild(
                "headwear",
                // Adicionado CubeDeformation(0.25f) para evitar Z-Fighting com o capacete e a cabeça
                CubeListBuilder.create().texOffs(32, 17).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, CubeDeformation(0.25f))
                    .texOffs(28, 15).addBox(0.0f, -12.0f, -3.0f, 0.0f, 4.0f, 6.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -1.0f, 0.0f)
            )

            headwear.addOrReplaceChild(
                "taproot_r1",
                CubeListBuilder.create().texOffs(28, 11).addBox(1.0f, -4.0f, -5.0f, 0.0f, 4.0f, 6.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(2.0f, -8.0f, 1.0f, 0.0f, 1.5708f, 0.0f)
            )

            // APLICADO CUBEDEFORMATION DE 0.01f NAS PENAS PARA AFASTAR DA SUPERFÍCIE DO CORPO
            val bodyFeathers = body.addOrReplaceChild(
                "body feathers",
                CubeListBuilder.create().texOffs(2, 49).addBox(-4.0f, -11.5f, -2.01f, 8.0f, 1.0f, 0.0f, CubeDeformation(0.01f))
                    .texOffs(4, 49).addBox(-4.0f, -11.5f, 2.01f, 8.0f, 1.0f, 0.0f, CubeDeformation(0.01f))
                    .texOffs(10, 49).addBox(-4.0f, 0.5f, -2.01f, 8.0f, 1.0f, 0.0f, CubeDeformation(0.01f))
                    .texOffs(10, 49).addBox(-4.0f, 0.5f, 2.01f, 8.0f, 1.0f, 0.0f, CubeDeformation(0.01f)),
                PartPose.offset(0.0f, -1.0f, 0.0f)
            )

            bodyFeathers.addOrReplaceChild(
                "body_feather_r1",
                CubeListBuilder.create().texOffs(4, 49).addBox(-4.0f, -11.5f, -2.0f, 8.0f, 1.0f, 0.0f, CubeDeformation(0.01f)),
                PartPose.offsetAndRotation(0.0f, -3.7f, 6.4f, 0.6981f, 0.0f, 0.0f)
            )

            bodyFeathers.addOrReplaceChild(
                "body_feather_r2",
                CubeListBuilder.create().texOffs(9, 49).addBox(-4.0f, -11.5f, -2.0f, 8.0f, 1.0f, 0.0f, CubeDeformation(0.01f)),
                PartPose.offsetAndRotation(0.0f, 7.9f, 10.8f, 0.6981f, 0.0f, 0.0f)
            )

            bodyFeathers.addOrReplaceChild(
                "body_feather_r3",
                CubeListBuilder.create().texOffs(4, 49).addBox(-4.0f, -11.5f, -2.0f, 8.0f, 1.0f, 0.0f, CubeDeformation(0.01f)),
                PartPose.offsetAndRotation(0.0f, -0.7f, -2.9f, -0.6283f, 0.0f, 0.0f)
            )

            bodyFeathers.addOrReplaceChild(
                "body_feather_r4",
                CubeListBuilder.create().texOffs(6, 49).addBox(-4.0f, -11.5f, -2.0f, 8.0f, 1.0f, 0.0f, CubeDeformation(0.01f)),
                PartPose.offsetAndRotation(0.0f, 11.0f, -7.0f, -0.6283f, 0.0f, 0.0f)
            )

            bodyFeathers.addOrReplaceChild(
                "body_feather_r5",
                CubeListBuilder.create().texOffs(18, 49).addBox(0.0f, -11.5f, -2.0f, 4.0f, 1.0f, 0.0f, CubeDeformation(0.01f))
                    .texOffs(10, 49).addBox(0.0f, -23.5f, -2.0f, 4.0f, 1.0f, 0.0f, CubeDeformation(0.01f)),
                PartPose.offsetAndRotation(-6.0f, 12.0f, -2.0f, 0.0f, -1.5708f, 0.0f)
            )

            bodyFeathers.addOrReplaceChild(
                "body_feather_r6",
                CubeListBuilder.create().texOffs(13, 49).addBox(0.0f, -11.5f, -2.0f, 4.0f, 1.0f, 0.0f, CubeDeformation(0.01f))
                    .texOffs(8, 49).addBox(0.0f, -23.5f, -2.0f, 4.0f, 1.0f, 0.0f, CubeDeformation(0.01f)),
                PartPose.offsetAndRotation(2.0f, 12.0f, -2.0f, 0.0f, -1.5708f, 0.0f)
            )

            bodyFeathers.addOrReplaceChild(
                "body_feather_r7",
                CubeListBuilder.create().texOffs(14, 49).addBox(0.0f, -11.5f, -2.0f, 4.0f, 1.0f, 0.0f, CubeDeformation(0.01f)),
                PartPose.offsetAndRotation(9.9f, 10.5f, -2.0f, 0.0f, -1.5708f, -0.7069f)
            )

            bodyFeathers.addOrReplaceChild(
                "body_feather_r8",
                CubeListBuilder.create().texOffs(10, 49).addBox(0.0f, -11.5f, -2.0f, 4.0f, 1.0f, 0.0f, CubeDeformation(0.01f)),
                PartPose.offsetAndRotation(1.6f, -1.2f, -2.0f, 0.0f, -1.5708f, -0.7069f)
            )

            bodyFeathers.addOrReplaceChild(
                "body_feather_r9",
                CubeListBuilder.create().texOffs(13, 49).addBox(0.0f, -11.5f, -2.0f, 4.0f, 1.0f, 0.0f, CubeDeformation(0.01f)),
                PartPose.offsetAndRotation(-12.9f, 7.8f, -2.0f, 0.0f, -1.5708f, 0.7069f)
            )

            bodyFeathers.addOrReplaceChild(
                "body_feather_r10",
                CubeListBuilder.create().texOffs(10, 49).addBox(0.0f, -11.5f, -2.0f, 4.0f, 1.0f, 0.0f, CubeDeformation(0.01f)),
                PartPose.offsetAndRotation(-4.4f, -3.9f, -2.0f, 0.0f, -1.5708f, 0.7069f)
            )

            // AJUSTADO OS BRAÇOS PARA FICAREM LIGEIRAMENTE MENORES/AFASTADOS DA ARMADURA
            body.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create().texOffs(32, 33).addBox(1.0f, -1.0f, -1.0f, 2.0f, 12.0f, 2.0f, CubeDeformation(-0.01f))
                    .texOffs(40, 47).addBox(0.0f, -1.0f, -1.0f, 1.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(4.0f, -11.0f, 0.0f)
            )

            body.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create().texOffs(40, 33).addBox(-3.0f, -1.0f, -1.0f, 2.0f, 12.0f, 2.0f, CubeDeformation(-0.01f))
                    .texOffs(46, 47).addBox(-1.0f, -1.0f, -1.0f, 1.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-4.0f, -11.0f, 0.0f)
            )

            partdefinition.addOrReplaceChild(
                "base2",
                CubeListBuilder.create().texOffs(0, 0).addBox(-8.0f, -1.0f, -8.0f, 16.0f, 1.0f, 16.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 24.0f, 0.0f)
            )

            return LayerDefinition.create(meshdefinition, 64, 64)
        }
    }
}*/