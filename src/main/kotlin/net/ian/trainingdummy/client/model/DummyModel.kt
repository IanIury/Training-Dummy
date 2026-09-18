package net.ian.trainingdummy.client.model

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.entity.DummyEntity
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.MaceItem
import kotlin.math.exp


class DummyModel(val root : ModelPart) : HumanoidModel<DummyEntity>(root.getChild("wobble_node")) {

    val wobble_node: ModelPart

    val body_feathers: ModelPart
    val jacket: ModelPart
    val left_sleeve: ModelPart
    val right_sleeve: ModelPart
    val left_pants: ModelPart
    val base_plate: ModelPart

    init {
        this.wobble_node = root.getChild("wobble_node")
        this.body_feathers = this.body.getChild("body_feathers")
        this.jacket = this.wobble_node.getChild("jacket")
        this.left_sleeve = this.wobble_node.getChild("left_sleeve")
        this.right_sleeve = this.wobble_node.getChild("right_sleeve")
        this.left_pants = this.wobble_node.getChild("left_pants")
        this.base_plate = root.getChild("base_plate")
    }


    override fun setupAnim(
        entity: DummyEntity,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
        root.allParts.forEach { it.resetPose() }
        val headIsEmpty = entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty
        hat.allParts.forEach { it.visible = headIsEmpty }

        //if(!entity.spawnAnimation.isStarted){entity.spawnAnimation.start(entity.tickCount)}

        entity.spawnAnimation.ifStarted { animation ->
            animation.updateTime(ageInTicks, 1.0f)

            val time = animation.accumulatedTime.toFloat()

            if (time < 800.0f) {
                val progress = Mth.clamp(time / 800.0f, 0.0f, 1.0f)
                val smooth = 1.0f - (1.0f - progress) * (1.0f - progress)

                /*wobble_node.y = Mth.lerp(
                    smooth,
                    25.0f,
                    0.0f
                )*/
                wobble_node.yScale = Mth.lerp(smooth, 0.15f, 1.0f)
                val wobble = Mth.sin(progress * Mth.PI * 2.0f) * 0.08f
                wobble_node.zRot = wobble

            } else {
                //wobble_node.y = 0.0f
                wobble_node.yScale = 1.0f
                wobble_node.zRot = 0.0f

                animation.stop()
            }
        }

        entity.hitAnimation.ifStarted { animation ->
            val state = entity.hitAnimation

            animation.updateTime(ageInTicks, 1.0f)

            val timeMs = animation.accumulatedTime.toFloat()
            val durationMs = 1000.0f

            if (timeMs < durationMs) {
                val timeInTicks = timeMs / 50.0f

                val frequency = 0.6f
                val decay = 0.2f
                val wave = (Mth.sin(timeInTicks * frequency) * exp((-timeInTicks * decay).toDouble())).toFloat()

                val maxAngleRad = Math.toRadians(30.0).toFloat() * 1.5f//entity.hitStrength

                // Angulo alvo gerado pelo NOVO golpe
                val targetXRot = state.hitPitch * wave * maxAngleRad
                val targetZRot = state.hitRoll * wave * maxAngleRad

                // Amortecimento da posição capturada (vai de 1.0 até 0.0 suavemente)
                //  Usamos um tempo de transição curto (~150ms / 3 ticks) para absorver o recuo antigo
                val blendFactor = Mth.clamp(timeMs / 150.0f, 0.0f, 1.0f)
                val smoothBlend = blendFactor * blendFactor * (3.0f - 2.0f * blendFactor) // Smoothstep

                // Decaimento exponencial para zerar a posição residual do offset
                val offsetDecay = exp((-timeInTicks * 0.5f).toDouble()).toFloat()
                val residualX = state.offsetPositionX * offsetDecay
                val residualZ = state.offsetPositionZ * offsetDecay

                // Interpola da posição antiga + residual para o novo vetor de movimento
                // Isso impede a inversão brusca de sinal no ponto oposto do zero
                wobble_node.xRot = Mth.lerp(smoothBlend, residualX, targetXRot)
                wobble_node.zRot = Mth.lerp(smoothBlend, residualZ, targetZRot)

            } else {
                wobble_node.xRot = 0.0f
                wobble_node.zRot = 0.0f
                state.offsetPositionX = 0.0f
                state.offsetPositionZ = 0.0f
                state.isNewHitRequested = false
                animation.stop()
            }

            // quando o novo hit é solicitado, captura a rotação ATUAL do nó
            //mas o xRot e resetado pelo it.resetPose() refazer essa formula ---///
            if (state.isNewHitRequested) {
                state.offsetPositionX = wobble_node.xRot
                state.offsetPositionZ = wobble_node.zRot
                state.isNewHitRequested = false
            }

        }

        entity.maceAnimation.ifStarted { animation ->
            animation.updateTime(ageInTicks, 1.0f)

            val state = entity.maceAnimation
            val time = animation.accumulatedTime.toFloat()

            // Caso o tempo atinja a janela natural de retorno (2000ms a 2500ms) OU a flag antecipada esteja ativa:
            if ((time in 2000.0f..2500.0f || state.maceScaleSizeSmooth) && state.maceScaleSize < 1.0f) {

                // Mantém a interpolação suave até voltar ao tamanho normal 1.0f
                val progress = Mth.clamp((time - 2000.0f) / 500.0f, 0.01f, 1.0f)
                val smooth = 1.0f - (1.0f - progress) * (1.0f - progress)

                state.maceScaleSize = Mth.lerp(smooth, state.maceScaleSize, 1.0f)

            } else if (time > 2500.0f) {
                state.maceScaleSizeNormalizer = true
            }

            // Normaliza e encerra a animação quando atingir o tamanho cheio ou estourar o tempo... algo assim...
            if (state.maceScaleSizeNormalizer || state.maceScaleSize >= 1.0f) {
                state.maceScaleSize = 1.0f
                state.maceScaleSizeNormalizer = false
                state.maceScaleSizeSmooth = false
                wobble_node.yScale = 1.0f
                //state.maceDamage = 0.0f
                animation.stop()
            } else {
                wobble_node.yScale = state.maceScaleSize
            }

            //verrifica c etsa sicronizada com outros player / servidor / multplayer
        }

        //super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch)
    }

    override fun renderToBuffer(
        poseStack: PoseStack,
        vertexConsumer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int,
        color: Int
    ) {
        //super.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, color)
        wobble_node.render(poseStack, vertexConsumer, packedLight, packedOverlay, color)
        base_plate.render(poseStack, vertexConsumer, packedLight, packedOverlay, color)
    }

    companion object {

        val LAYER_LOCATION = ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(TrainingDummy.ID, "training_dummy"),
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

            val hat = wobble_node.addOrReplaceChild(
                "hat",
                CubeListBuilder.create().texOffs(0, 17)
                    .addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, CubeDeformation(0.1f))
                    .texOffs(28, 15).addBox(0.0f, -12.0f, -3.0f, 0.0f, 4.0f, 6.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, -25.75f, 0.0f)
            )

            val taproot = hat.addOrReplaceChild(
                "taproot",
                CubeListBuilder.create().texOffs(28, 15)
                    .addBox(0.0f, -12.0f, -3.0f, 0.0f, 4.0f, 6.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 0.0f, 0.0f)
            )

            val taproot_2_r1 = taproot.addOrReplaceChild(
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
                    .addBox(-4.0f, -11.5f, -2.0f, 8.0f, 1.0f, 0.0f, CubeDeformation(0.0f))
                    .texOffs(4, 49).addBox(-4.0f, -11.5f, 2.0f, 8.0f, 1.0f, 0.0f, CubeDeformation(0.0f))
                    .texOffs(10, 49).addBox(-4.0f, 0.5f, -2.0f, 8.0f, 1.0f, 0.0f, CubeDeformation(0.0f))
                    .texOffs(10, 49).addBox(-4.0f, 0.5f, 2.0f, 8.0f, 1.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 11.0f, 0.0f)
            )

            val body_feather_r1 = body_feathers.addOrReplaceChild(
                "body_feather_r1",
                CubeListBuilder.create().texOffs(4, 49)
                    .addBox(-4.0f, -11.5f, -2.0f, 8.0f, 1.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, -3.7f, 6.4f, 0.6981f, 0.0f, 0.0f)
            )

            val body_feather_r2 = body_feathers.addOrReplaceChild(
                "body_feather_r2",
                CubeListBuilder.create().texOffs(9, 49)
                    .addBox(-4.0f, -11.5f, -2.0f, 8.0f, 1.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, 7.9f, 10.8f, 0.6981f, 0.0f, 0.0f)
            )

            val body_feather_r3 = body_feathers.addOrReplaceChild(
                "body_feather_r3",
                CubeListBuilder.create().texOffs(4, 49)
                    .addBox(-4.0f, -11.5f, -2.0f, 8.0f, 1.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, -0.7f, -2.9f, -0.6283f, 0.0f, 0.0f)
            )

            val body_feather_r4 = body_feathers.addOrReplaceChild(
                "body_feather_r4",
                CubeListBuilder.create().texOffs(6, 49)
                    .addBox(-4.0f, -11.5f, -2.0f, 8.0f, 1.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(0.0f, 11.0f, -7.0f, -0.6283f, 0.0f, 0.0f)
            )

            val body_feather_r5 = body_feathers.addOrReplaceChild(
                "body_feather_r5",
                CubeListBuilder.create().texOffs(18, 49)
                    .addBox(0.0f, -11.5f, -2.0f, 4.0f, 1.0f, 0.0f, CubeDeformation(0.0f))
                    .texOffs(10, 49).addBox(0.0f, -23.5f, -2.0f, 4.0f, 1.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-6.0f, 12.0f, -2.0f, 0.0f, -1.5708f, 0.0f)
            )

            val body_feather_r6 = body_feathers.addOrReplaceChild(
                "body_feather_r6",
                CubeListBuilder.create().texOffs(13, 49)
                    .addBox(0.0f, -11.5f, -2.0f, 4.0f, 1.0f, 0.0f, CubeDeformation(0.0f))
                    .texOffs(8, 49).addBox(0.0f, -23.5f, -2.0f, 4.0f, 1.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(2.0f, 12.0f, -2.0f, 0.0f, -1.5708f, 0.0f)
            )

            val body_feather_r7 = body_feathers.addOrReplaceChild(
                "body_feather_r7",
                CubeListBuilder.create().texOffs(14, 49)
                    .addBox(0.0f, -11.5f, -2.0f, 4.0f, 1.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(9.9f, 10.5f, -2.0f, 0.0f, -1.5708f, -0.7069f)
            )

            val body_feather_r8 = body_feathers.addOrReplaceChild(
                "body_feather_r8",
                CubeListBuilder.create().texOffs(10, 49)
                    .addBox(0.0f, -11.5f, -2.0f, 4.0f, 1.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(1.6f, -1.2f, -2.0f, 0.0f, -1.5708f, -0.7069f)
            )

            val body_feather_r9 = body_feathers.addOrReplaceChild(
                "body_feather_r9",
                CubeListBuilder.create().texOffs(13, 49)
                    .addBox(0.0f, -11.5f, -2.0f, 4.0f, 1.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-12.9f, 7.8f, -2.0f, 0.0f, -1.5708f, 0.7069f)
            )

            val body_feather_r10 = body_feathers.addOrReplaceChild(
                "body_feather_r10",
                CubeListBuilder.create().texOffs(10, 49)
                    .addBox(0.0f, -11.5f, -2.0f, 4.0f, 1.0f, 0.0f, CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-4.4f, -3.9f, -2.0f, 0.0f, -1.5708f, 0.7069f)
            )

            val jacket = wobble_node.addOrReplaceChild(
                "jacket",
                CubeListBuilder.create().texOffs(0, 17)
                    .addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, CubeDeformation(0.25f)),
                PartPose.offset(0.0f, -24.75f, 0.0f)
            )

            val left_arm = wobble_node.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create().texOffs(32, 33)
                    .addBox(0.0f, -1.0f, -1.0f, 2.0f, 12.0f, 2.0f, CubeDeformation(0.0f))
                    .texOffs(40, 47).addBox(-1.0f, -1.0f, -1.0f, 1.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(5.0f, -23.75f, 0.0f)
            )

            val left_sleeve = wobble_node.addOrReplaceChild(
                "left_sleeve",
                CubeListBuilder.create().texOffs(55, 50)
                    .addBox(0.0f, -1.0f, -1.0f, 2.0f, 12.0f, 2.0f, CubeDeformation(0.25f))
                    .texOffs(40, 47).addBox(-1.0f, -1.0f, -1.0f, 1.0f, 2.0f, 2.0f, CubeDeformation(0.25f)),
                PartPose.offset(5.0f, -23.75f, 0.0f)
            )

            val right_arm = wobble_node.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create().texOffs(40, 33)
                    .addBox(-2.0f, -1.0f, -1.0f, 2.0f, 12.0f, 2.0f, CubeDeformation(0.0f))
                    .texOffs(46, 47).addBox(0.0f, -1.0f, -1.0f, 1.0f, 2.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-5.0f, -23.75f, 0.0f)
            )

            val right_sleeve = wobble_node.addOrReplaceChild(
                "right_sleeve",
                CubeListBuilder.create().texOffs(56, 50)
                    .addBox(-2.0f, -1.0f, -1.0f, 2.0f, 12.0f, 2.0f, CubeDeformation(0.25f))
                    .texOffs(46, 47).addBox(0.0f, -1.0f, -1.0f, 1.0f, 2.0f, 2.0f, CubeDeformation(0.25f)),
                PartPose.offset(-5.0f, -23.75f, 0.0f)
            )

            val left_leg = wobble_node.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create().texOffs(24, 33)
                    .addBox(-0.9f, -1.0f, -1.0f, 2.0f, 13.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-0.1f, -12.75f, 0.0f)
            )

            val right_leg = wobble_node.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create().texOffs(24, 33)
                    .addBox(-0.9f, -1.0f, -1.0f, 2.0f, 13.0f, 2.0f, CubeDeformation(0.0f)),
                PartPose.offset(-0.1f, -12.75f, 0.0f)
            )

            val left_pants = wobble_node.addOrReplaceChild(
                "left_pants",
                CubeListBuilder.create().texOffs(24, 48)
                    .addBox(-0.9f, -12.0f, -1.0f, 2.0f, 13.0f, 2.0f, CubeDeformation(0.25f)),
                PartPose.offset(-0.1f, -1.75f, 0.0f)
            )

            val base_plate = partdefinition.addOrReplaceChild(
                "base_plate",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-8.0f, -1.0f, -8.0f, 16.0f, 1.0f, 16.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 24.0f, 0.0f)
            )

            return LayerDefinition.create(meshdefinition, 64, 64)
        }
    }
}