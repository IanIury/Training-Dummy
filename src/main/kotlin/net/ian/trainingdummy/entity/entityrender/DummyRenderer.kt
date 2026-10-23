package net.ian.trainingdummy.entity.entityrender

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import dev.ian.worldpanel.FloatingWindow
import dev.ian.worldpanel.WindowAlignment
import dev.ian.worldpanel.WindowColor
import dev.ian.worldpanel.WindowElement
import dev.ian.worldpanel.shape.Volume3DConfig
import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.client.gui.WindowShapes
import net.ian.trainingdummy.client.model.DummyModel
import net.ian.trainingdummy.entity.DummyEntity
import net.ian.trainingdummy.init.enums.TrainingModos
import net.ian.trainingdummy.init.enums.WindowShapeType
import net.ian.trainingdummy.init.utils.ClientSkinUtils
import net.ian.trainingdummy.screen.custom.DummyScreen
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.ArmorModelSet
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.HumanoidMobRenderer
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.entity.layers.WingsLayer
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import java.util.Locale

private const val COLOR_CYCLE_TICKS = 150f
private const val COLOR_SATURATION = 0.9f

data class DummyWindowProjection(
    val x: Double,
    val y: Double,
    val z: Double,
    val rightX: Double,
    val rightZ: Double
)

class DummyRenderer(context: EntityRendererProvider.Context) : HumanoidMobRenderer<DummyEntity, DummyRenderState, DummyModel>(
    context,
    DummyModel(context.bakeLayer(DummyModel.LAYER_LOCATION)),
    0.5f
) {

    init {
        this.layers.removeIf { it is WingsLayer<*, *> }

        val armorLayers = ModelLayers.PLAYER_ARMOR
        val armorModelSet = ArmorModelSet(
            DummyArmorModel(context.bakeLayer(armorLayers.head()), EquipmentSlot.HEAD),
            DummyArmorModel(context.bakeLayer(armorLayers.chest()), EquipmentSlot.CHEST),
            DummyArmorModel(context.bakeLayer(armorLayers.legs()), EquipmentSlot.LEGS),
            DummyArmorModel(context.bakeLayer(armorLayers.feet()), EquipmentSlot.FEET)
        )

        this.addLayer(BasePlateLayer())
        this.addLayer(DummyHumanoidArmorLayer(this, armorModelSet, context.equipmentRenderer))
        this.addLayer(DummyWingsLayer(this, context.modelSet, context.equipmentRenderer))
        this.addLayer(DummyFaceLayer())
        this.addLayer(JebColorLayer())
        this.addLayer(NamedRenderEffectLayer())
        this.addLayer(HandHologramLayer())
    }

    private class DummyArmorModel(
        root: ModelPart,
        private val slot: EquipmentSlot
    ) : HumanoidModel<DummyRenderState>(root) {

        override fun setupAnim(state: DummyRenderState) {
            head.visible = false
            hat.visible = false
            body.visible = false
            rightArm.visible = false
            leftArm.visible = false
            rightLeg.visible = false
            leftLeg.visible = false

            head.setPos(0.0f, -25.75f, 0.0f)
            head.setRotation(0.0f, 0.0f, 0.0f)

            hat.setPos(0.0f, 0.0f, 0.0f)
            hat.setRotation(0.0f, 0.0f, 0.0f)

            body.setPos(0.0f, -24.75f, 0.0f)
            body.setRotation(0.0f, 0.0f, 0.0f)

            rightArm.setPos(-5.0f, -23.75f, 0.0f)
            rightArm.setRotation(0.0f, 0.0f, 0.0f)

            leftArm.setPos(5.0f, -23.75f, 0.0f)
            leftArm.setRotation(0.0f, 0.0f, 0.0f)

            rightLeg.setPos(-0.1f, -12.75f, 0.0f)
            rightLeg.setRotation(0.0f, 0.0f, 0.0f)

            leftLeg.setPos(-0.1f, -12.75f, 0.0f)
            leftLeg.setRotation(0.0f, 0.0f, 0.0f)

            when (slot) {
                EquipmentSlot.HEAD -> {
                    head.visible = true
                    hat.visible = true
                }
                EquipmentSlot.CHEST -> {
                    body.visible = true
                    rightArm.visible = true
                    leftArm.visible = true
                    rightArm.xScale = 1.01f
                    rightArm.yScale = 1.01f
                    rightArm.zScale = 1.01f
                    leftArm.xScale = 1.01f
                    leftArm.yScale = 1.01f
                    leftArm.zScale = 1.01f
                }
                EquipmentSlot.LEGS, EquipmentSlot.FEET -> {
                    rightLeg.visible = true
                    leftLeg.visible = false
                }
                else -> {}
            }
        }
    }

    private inner class DummyHumanoidArmorLayer(
        renderer: RenderLayerParent<DummyRenderState, DummyModel>,
        armorModelSet: ArmorModelSet<DummyArmorModel>,
        equipmentRenderer: EquipmentLayerRenderer
    ) : HumanoidArmorLayer<DummyRenderState, DummyModel, DummyArmorModel>(renderer, armorModelSet, equipmentRenderer) {

        override fun submit(
            poseStack: PoseStack,
            collector: SubmitNodeCollector,
            packedLight: Int,
            state: DummyRenderState,
            yRot: Float,
            xRot: Float
        ) {
            parentModel.setupAnim(state)
            poseStack.pushPose()
            parentModel.wobble_node.translateAndRotate(poseStack)
            super.submit(poseStack, collector, packedLight, state, yRot, xRot)
            poseStack.popPose()
        }
    }

    private inner class DummyWingsLayer(
        renderer: RenderLayerParent<DummyRenderState, DummyModel>,
        modelSet: net.minecraft.client.model.geom.EntityModelSet,
        equipmentRenderer: EquipmentLayerRenderer
    ) : WingsLayer<DummyRenderState, DummyModel>(renderer, modelSet, equipmentRenderer) {

        override fun submit(
            poseStack: PoseStack,
            collector: SubmitNodeCollector,
            packedLight: Int,
            state: DummyRenderState,
            yRot: Float,
            xRot: Float
        ) {
            parentModel.setupAnim(state)
            poseStack.pushPose()
            parentModel.wobble_node.translateAndRotate(poseStack)
            parentModel.body.translateAndRotate(poseStack)
            super.submit(poseStack, collector, packedLight, state, yRot, xRot)
            poseStack.popPose()
        }
    }

    private inner class BasePlateLayer : RenderLayer<DummyRenderState, DummyModel>(this@DummyRenderer) {
        override fun submit(
            poseStack: PoseStack,
            collector: SubmitNodeCollector,
            packedLight: Int,
            state: DummyRenderState,
            yRot: Float,
            xRot: Float
        ) {
            val baseRenderType = this@DummyRenderer.model.renderType(getTextureLocation(state))
            collector.submitModelPart(
                this@DummyRenderer.model.base_plate,
                poseStack,
                baseRenderType,
                packedLight,
                LivingEntityRenderer.getOverlayCoords(state, 0f),
                null
            )
        }
    }

    private inner class DummyFaceLayer : RenderLayer<DummyRenderState, DummyModel>(this@DummyRenderer) {
        override fun submit(
            poseStack: PoseStack,
            collector: SubmitNodeCollector,
            packedLight: Int,
            state: DummyRenderState,
            yRot: Float,
            xRot: Float
        ) {
            val skinLocation = state.skinTexture ?: return
            if (!state.headItem.isEmpty) return

            val overlay = LivingEntityRenderer.getOverlayCoords(state, 0f)
            val renderType = RenderTypes.entityTranslucent(skinLocation)

            parentModel.setupAnim(state)
            poseStack.pushPose()
            parentModel.wobble_node.translateAndRotate(poseStack)
            parentModel.head.translateAndRotate(poseStack)

            collector.submitCustomGeometry(poseStack, renderType) { pose, consumer ->
                val minX = -4.0f / 16.0f
                val maxX = 4.0f / 16.0f
                val minY = -8.0f / 16.0f
                val maxY = 0.0f / 16.0f

                // Rosto base subdividido em grade 8x8 texels com bordas suavizadas e tonalização quente de estopa
                // para integrar organicamente à cabeça do dummy em vez de parecer uma foto colada.
                val faceZ = -4.005f / 16.0f
                val baseU0 = 8.0f / 64.0f
                val baseV0 = 8.0f / 64.0f
                val texStep = 1.0f / 64.0f
                val posStepX = (maxX - minX) / 8.0f
                val posStepY = (maxY - minY) / 8.0f

                for (ty in 0 until 8) {
                    for (tx in 0 until 8) {
                        val isCorner = (tx == 0 || tx == 7) && (ty == 0 || ty == 7)
                        val isBorder = tx == 0 || tx == 7 || ty == 0 || ty == 7

                        val alpha = when {
                            isCorner -> 145
                            isBorder -> 215
                            else -> 248
                        }
                        val shade = if (isBorder) 0.90f else 0.97f
                        val r = (255 * shade).toInt().coerceIn(0, 255)
                        val g = (245 * shade).toInt().coerceIn(0, 255)
                        val b = (230 * shade).toInt().coerceIn(0, 255)

                        val x0 = minX + tx * posStepX
                        val x1 = x0 + posStepX
                        val y0 = minY + ty * posStepY
                        val y1 = y0 + posStepY

                        // Na face frontal (olhando para -Z), X local negativo está à direita do observador,
                        // portanto espelhamos horizontalmente o índice de texel para alinhar ao rosto do jogador.
                        val u0 = baseU0 + (7 - tx) * texStep
                        val u1 = u0 + texStep
                        val v0 = baseV0 + ty * texStep
                        val v1 = v0 + texStep

                        emitQuad(pose, consumer, x0, y0, x1, y1, faceZ, u1, v0, u0, v1, r, g, b, alpha, packedLight, overlay)
                    }
                }

                // Segunda camada (hat/hair layer da skin: [40..48, 8..16]) com leve relevo 3D
                if (state.headEquipment.isEmpty) {
                    val hatZ = -4.14f / 16.0f
                    val hatMinX = -4.12f / 16.0f
                    val hatMaxX = 4.12f / 16.0f
                    val hatMinY = -8.12f / 16.0f
                    val hatMaxY = 0.12f / 16.0f
                    val hatU0 = 40.0f / 64.0f
                    val hatU1 = 48.0f / 64.0f
                    val hatV0 = 8.0f / 64.0f
                    val hatV1 = 16.0f / 64.0f

                    emitQuad(
                        pose,
                        consumer,
                        hatMinX,
                        hatMinY,
                        hatMaxX,
                        hatMaxY,
                        hatZ,
                        hatU1,
                        hatV0,
                        hatU0,
                        hatV1,
                        250,
                        242,
                        230,
                        245,
                        packedLight,
                        overlay
                    )
                }
            }

            poseStack.popPose()
        }

        private fun emitQuad(
            pose: PoseStack.Pose,
            consumer: VertexConsumer,
            x0: Float,
            y0: Float,
            x1: Float,
            y1: Float,
            z: Float,
            u0: Float,
            v0: Float,
            u1: Float,
            v1: Float,
            r: Int,
            g: Int,
            b: Int,
            a: Int,
            packedLight: Int,
            overlay: Int
        ) {
            consumer.addVertex(pose, x0, y1, z)
                .setColor(r, g, b, a)
                .setUv(u0, v1)
                .setOverlay(overlay)
                .setLight(packedLight)
                .setNormal(pose, 0.0f, 0.0f, -1.0f)

            consumer.addVertex(pose, x1, y1, z)
                .setColor(r, g, b, a)
                .setUv(u1, v1)
                .setOverlay(overlay)
                .setLight(packedLight)
                .setNormal(pose, 0.0f, 0.0f, -1.0f)

            consumer.addVertex(pose, x1, y0, z)
                .setColor(r, g, b, a)
                .setUv(u1, v0)
                .setOverlay(overlay)
                .setLight(packedLight)
                .setNormal(pose, 0.0f, 0.0f, -1.0f)

            consumer.addVertex(pose, x0, y0, z)
                .setColor(r, g, b, a)
                .setUv(u0, v0)
                .setOverlay(overlay)
                .setLight(packedLight)
                .setNormal(pose, 0.0f, 0.0f, -1.0f)
        }
    }

    private inner class JebColorLayer : RenderLayer<DummyRenderState, DummyModel>(this@DummyRenderer) {
        override fun submit(
            poseStack: PoseStack,
            collector: SubmitNodeCollector,
            packedLight: Int,
            state: DummyRenderState,
            yRot: Float,
            xRot: Float
        ) {
            if (!state.isJeb) return

            val cycleProgress = state.ageInTicks / COLOR_CYCLE_TICKS
            val hue = cycleProgress - kotlin.math.floor(cycleProgress)
            val rgb = Mth.hsvToRgb(hue, COLOR_SATURATION, 1f)
            val color = (0xFF shl 24) or (rgb and 0x00FFFFFF)
            val texture = getTextureLocation(state)
            val renderType = RenderTypes.entityTranslucentEmissive(texture)
            val overlay = LivingEntityRenderer.getOverlayCoords(state, 0f)

            collector.submitModel(
                parentModel,
                state,
                poseStack,
                renderType,
                packedLight,
                overlay,
                color,
                null,
                state.outlineColor,
                null
            )
            collector.submitModelPart(
                parentModel.base_plate,
                poseStack,
                renderType,
                packedLight,
                overlay,
                null,
                color,
                null
            )

            collector.order(1).submitModel(
                parentModel,
                state,
                poseStack,
                RenderTypes.entityGlint(),
                packedLight,
                overlay,
                -1,
                null,
                0,
                null
            )
            collector.order(1).submitModelPart(
                parentModel.base_plate,
                poseStack,
                RenderTypes.entityGlint(),
                packedLight,
                overlay,
                null,
                -1,
                null
            )
        }
    }

    private inner class NamedRenderEffectLayer : RenderLayer<DummyRenderState, DummyModel>(this@DummyRenderer) {
        override fun submit(
            poseStack: PoseStack,
            collector: SubmitNodeCollector,
            packedLight: Int,
            state: DummyRenderState,
            yRot: Float,
            xRot: Float
        ) {
            val effect = state.customNameString ?: return
            val renderType = when {
                effect.equals("-endGateway-", ignoreCase = true) -> RenderTypes.endGateway()
                effect.equals("-entityGlint-", ignoreCase = true) -> RenderTypes.entityGlint()
                effect.equals("-translucent-", ignoreCase = true) -> RenderTypes.entityTranslucent(getTextureLocation(state))
                effect.equals("-lightning-", ignoreCase = true) -> RenderTypes.lightning()
                else -> return
            }

            val overlay = LivingEntityRenderer.getOverlayCoords(state, 0f)
            collector.order(1).submitModel(
                parentModel,
                state,
                poseStack,
                renderType,
                packedLight,
                overlay,
                -1,
                null,
                state.outlineColor,
                null
            )
            collector.order(1).submitModelPart(
                parentModel.base_plate,
                poseStack,
                renderType,
                packedLight,
                overlay,
                null,
                -1,
                null
            )
        }
    }

    private inner class HandHologramLayer : RenderLayer<DummyRenderState, DummyModel>(this@DummyRenderer) {
        override fun submit(
            poseStack: PoseStack,
            collector: SubmitNodeCollector,
            packedLight: Int,
            state: DummyRenderState,
            yRot: Float,
            xRot: Float
        ) {
            if (!isRenderingHandHologram) return

            val texture = getTextureLocation(state)
            val renderType = RenderTypes.entityTranslucentEmissive(texture)
            val overlay = LivingEntityRenderer.getOverlayCoords(state, 0f)

            collector.order(1).submitCustomGeometry(poseStack, renderType) { pose, baseConsumer ->
                val snapshotStack = PoseStack()
                snapshotStack.last().pose().set(pose.pose())
                snapshotStack.last().normal().set(pose.normal())

                val gradientConsumer = HologramGradientVertexConsumer(baseConsumer)
                parentModel.setupAnim(state)
                parentModel.renderToBuffer(
                    snapshotStack,
                    gradientConsumer,
                    packedLight,
                    overlay,
                    -1
                )
                parentModel.base_plate.render(
                    snapshotStack,
                    gradientConsumer,
                    packedLight,
                    overlay,
                    -1
                )
            }
        }
    }

    private class HologramGradientVertexConsumer(
        private val delegate: VertexConsumer
    ) : VertexConsumer {
        private var lastY: Float = 0f

        override fun addVertex(x: Float, y: Float, z: Float): VertexConsumer {
            lastY = y
            return delegate.addVertex(x, y, z)
        }

        override fun setColor(red: Int, green: Int, blue: Int, alpha: Int): VertexConsumer {
            val t = ((lastY + 0.5f) / 2.0f).coerceIn(0f, 1f)
            val r = (0f + t * 50f).toInt().coerceIn(0, 255)
            val g = (255f - t * 95f).toInt().coerceIn(0, 255)
            val b = 255
            val a = (200f - t * 140f).toInt().coerceIn(0, 255)
            return delegate.setColor(r, g, b, a)
        }

        override fun setColor(color: Int): VertexConsumer {
            val t = ((lastY + 0.5f) / 2.0f).coerceIn(0f, 1f)
            val r = (0f + t * 50f).toInt().coerceIn(0, 255)
            val g = (255f - t * 95f).toInt().coerceIn(0, 255)
            val b = 255
            val a = (200f - t * 140f).toInt().coerceIn(0, 255)
            return delegate.setColor(r, g, b, a)
        }

        override fun setUv(u: Float, v: Float): VertexConsumer = delegate.setUv(u, v)
        override fun setUv1(u: Int, v: Int): VertexConsumer = delegate.setUv1(u, v)
        override fun setUv2(u: Int, v: Int): VertexConsumer = delegate.setUv2(u, v)
        override fun setNormal(x: Float, y: Float, z: Float): VertexConsumer = delegate.setNormal(x, y, z)
        override fun setLineWidth(width: Float): VertexConsumer = delegate.setLineWidth(width)
    }

    override fun createRenderState(): DummyRenderState = DummyRenderState()

    override fun extractRenderState(entity: DummyEntity, state: DummyRenderState, partialTick: Float) {
        super.extractRenderState(entity, state, partialTick)
        state.partialTick = partialTick
        state.dummyEntity = entity
        state.dummyId = entity.id
        state.dummyUuid = entity.uuid
        state.isGui = DummyScreen.isRenderingEntityInGui
        state.isJeb = entity.customName?.string?.equals("jeb_", ignoreCase = true) == true
        state.customNameString = entity.customName?.string
        state.skinTexture = if (entity.customName != null && entity.profilePlayer != null) {
            ClientSkinUtils.getPlayerSkin(entity.profilePlayer)?.body()?.texturePath()
        } else {
            null
        }
        state.trainingMode = entity.trainingModuleConfig.trainingModo
        state.windowShape = entity.windowShape
        state.detailedDamage = entity.detailedDamageWindow
        state.dps = entity.dpsTotalDamage
        state.maxDamage = entity.dpsHighestHit
        state.totalDamage = entity.dpsTotalDamage
        state.hitCount = entity.dpsHitCount
        state.dummyTexture = getTextureLocation(state)
    }

    override fun getTextureLocation(state: DummyRenderState): Identifier {
        return Identifier.fromNamespaceAndPath(TrainingDummy.ID, "textures/entity/training_dummy.png")
    }

    override fun submit(
        state: DummyRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        cameraState: CameraRenderState
    ) {
        super.submit(state, poseStack, collector, cameraState)

        val entity = state.dummyEntity
        if (entity != null && !state.isGui && !isRenderingHandHologram) {
            renderFloatingWindowModo(entity, poseStack, collector, state.lightCoords, partialTick = state.partialTick)
        }
    }

    override fun shouldRender(
        entity: DummyEntity,
        camera: net.minecraft.client.renderer.culling.Frustum,
        camX: Double,
        camY: Double,
        camZ: Double
    ): Boolean {
        val expandedBox = entity.boundingBox.inflate(1.5, 3.5, 1.5)
        return camera.isVisible(expandedBox)
    }

    companion object {
        var isRenderingHandHologram: Boolean = false

        fun windowDefault3(window: FloatingWindow.Builder, entity: DummyEntity) {
            window
                .setOffset(0.0, 2.8, 0.0)
                .setScale(0.03f)
                .attachToEntityRotation(entity)
                .setAlignment(WindowAlignment.CENTER)
                .setBackground(WindowColor.Gradient(0xFFFFFFFFL, 0x3C3C3CAAL))
                .setVolume3D(Volume3DConfig.DEFAULT)
                .addText(
                    text = "★ MESTRE DAS ARMAS ★",
                    color = WindowColor.Rainbow(speed = 0.8f),
                    shadow = true,
                    textScale = 1.3f
                )
                .addText(
                    component = Component.literal("Nível 100"),
                    color = WindowColor.Solid(0xFFFBBF24L),
                    shadow = true,
                    textScale = 0.9f
                )
                .build()
        }

        fun renderFloatingWindowModo(
            entity: DummyEntity,
            poseStack: PoseStack,
            collector: SubmitNodeCollector,
            packedLight: Int,
            projection: DummyWindowProjection? = null,
            partialTick: Float = 0f
        ) {
            val modoAtual = entity.trainingModuleConfig.trainingModo
            if (projection == null && modoAtual != TrainingModos.DPS && entity.displayTicks <= 0) return

            val damageData = entity.damageData
            val damageDataOld = entity.damageDataOld

            val window = FloatingWindow.Builder()

            when (modoAtual) {
                TrainingModos.DEFAULT -> {
                    windowConfigDefault(
                        window,
                        entity,
                        detailed = entity.detailedDamageWindow,
                        projection = projection
                    )
                }

                TrainingModos.DPS -> {
                    if (projection == null) {
                        window.attachToEntityRotation(entity).setOffset(0.9, 2.0, 0.0)
                    } else {
                        window.setFaceCamera(true)
                            .setOffset(projection.x, projection.y, projection.z)
                    }
                    applyWindowShape(window, entity)
                    window
                        .setAlignment(WindowAlignment.CENTER)
                        .setPadding(2f)
                        .setSize(38f, 30f, autoScaleContent = true, autoFitWidth = true, autoFitHeight = true)
                        .setBorder(
                            when (entity.dpsTestState) {
                                DummyEntity.DPS_STATE_READY -> WindowColor.AnimatedGradient(
                                    argbStart = 0xFF65E8A0L,
                                    argbEnd = 0xFF21B6A8L,
                                    speed = 0.8f,
                                    scale = 0.8f
                                )
                                DummyEntity.DPS_STATE_COOLDOWN -> WindowColor.AnimatedGradient(
                                    argbStart = 0xFFFFB454L,
                                    argbEnd = 0xFFFF5A5AL,
                                    speed = 0.8f,
                                    scale = 0.8f
                                )
                                else -> WindowColor.AnimatedGradient(
                                    argbStart = 0xFFC5A5FBL,
                                    argbEnd = 0xFF5700ACL,
                                    speed = 1.2f,
                                    scale = 0.8f
                                )
                            },
                            width = 0.75f
                        )
                        .setBackground(
                            WindowColor.Gradient(
                                argbStart = 0xF0181C2AL,
                                argbEnd = 0xF02B233DL
                            )
                        )

                    val formatDamage: (Float) -> String = { value ->
                        String.format(Locale.ROOT, "%.1f", value)
                    }

                    when (entity.dpsTestState) {
                        DummyEntity.DPS_STATE_MEASURING -> {
                            val remainingSeconds =
                                (entity.dpsTicksRemaining - partialTick.coerceIn(0f, 1f)).coerceAtLeast(0f) / 20f
                            val elapsedTicks = DummyEntity.DPS_TEST_DURATION_TICKS - entity.dpsTicksRemaining
                            val currentDps = if (elapsedTicks >= 20) {
                                entity.dpsTotalDamage / (elapsedTicks / 20f)
                            } else {
                                null
                            }

                            window.addText(
                                Component.translatable(
                                    "message.trainingdummy.dps.countdown",
                                    formatDamage(remainingSeconds),
                                    currentDps?.let(formatDamage) ?: "--"
                                ),
                                color = 0xFF9BE7FF.toInt(),
                                shadow = true,
                                textScale = 0.9f,
                                alignment = WindowElement.Text.TextAlignment.CENTER
                            )
                                .addText(
                                    Component.translatable(
                                        "message.trainingdummy.dps.summary",
                                        formatDamage(entity.dpsTotalDamage),
                                        entity.dpsHitCount
                                    ),
                                    color = 0xFFFFFFFF.toInt(),
                                    textScale = 0.9f,
                                    alignment = WindowElement.Text.TextAlignment.CENTER
                                )
                        }

                        DummyEntity.DPS_STATE_COOLDOWN -> {
                            val averageHit = if (entity.dpsHitCount > 0) {
                                entity.dpsTotalDamage / entity.dpsHitCount
                            } else {
                                0f
                            }

                            window.addText(
                                Component.translatable(
                                    "message.trainingdummy.dps.final",
                                    formatDamage(entity.dpsTotalDamage / 5f)
                                ),
                                color = 0xFFE5D4FF.toInt(),
                                shadow = true,
                                textScale = 0.95f,
                                alignment = WindowElement.Text.TextAlignment.CENTER
                            )
                                .addText(
                                    Component.translatable(
                                        "message.trainingdummy.dps.summary",
                                        formatDamage(entity.dpsTotalDamage),
                                        entity.dpsHitCount
                                    ),
                                    color = 0xFFFFFFFF.toInt(),
                                    textScale = 0.9f,
                                    alignment = WindowElement.Text.TextAlignment.CENTER
                                )
                                .addText(
                                    Component.translatable(
                                        "message.trainingdummy.dps.hitStats",
                                        formatDamage(averageHit),
                                        formatDamage(entity.dpsHighestHit)
                                    ),
                                    color = 0xFFD7C8FF.toInt(),
                                    textScale = 0.85f,
                                    alignment = WindowElement.Text.TextAlignment.CENTER
                                )
                        }

                        else -> {
                            window.addText(
                                Component.translatable("message.trainingdummy.dps.ready"),
                                color = 0xFFB7F7C5.toInt(),
                                shadow = true,
                                textScale = 0.9f,
                                alignment = WindowElement.Text.TextAlignment.CENTER
                            )
                        }
                    }
                }

                TrainingModos.ACCUMULATED -> {
                    configureModeWindow(
                        window,
                        entity,
                        Component.translatable("message.trainingdummy.accumulated_title"),
                        projection = projection
                    )
                    window.addSeparator(color = modeWindowSeparator(), thickness = 0.5f, margin = 2f)
                        .addText(
                            Component.translatable(
                                "message.trainingdummy.accumulated_value",
                                String.format(Locale.ROOT, "%.1f", damageData.newDamage)
                            ),
                            color = WindowColor.Pulse(0xFF9BE7FFL),
                            shadow = true,
                            textScale = 2.5f,
                            alignment = WindowElement.Text.TextAlignment.CENTER
                        )
                }

                TrainingModos.COMPARISON -> {
                    buildComparisonPanel(
                        window,
                        entity,
                        Component.translatable("message.trainingdummy.previous"),
                        entity.damageStackOld,
                        damageDataOld.newDamage,
                        offsetX = 0.8,
                        projection = projection,
                        projectionOffsetX = 0.35
                    )

                    val window2 = FloatingWindow.Builder()
                    buildComparisonPanel(
                        window2,
                        entity,
                        Component.translatable("message.trainingdummy.current"),
                        entity.damageStack,
                        damageData.newDamage,
                        offsetX = -0.8,
                        projection = projection,
                        projectionOffsetX = -0.35
                    )
                    window2.build().render(poseStack, collector, packedLight)

                    val difference = damageData.newDamage - damageDataOld.newDamage
                    val winnerStack = when {
                        difference > 0.001f -> entity.damageStack
                        difference < -0.001f -> entity.damageStackOld
                        else -> ItemStack.EMPTY
                    }
                    val differenceColor = when {
                        difference > 0.001f -> 0xFF75E6A5.toInt()
                        difference < -0.001f -> 0xFFFF9A9A.toInt()
                        else -> 0xFFFFD479.toInt()
                    }
                    val differenceWindow = FloatingWindow.Builder()
                    configureModeWindow(
                        differenceWindow,
                        entity,
                        Component.translatable("message.trainingdummy.comparison.title"),
                        offsetY = 2.5,
                        offsetX = 0.0,
                        width = 28f,
                        height = 22f,
                        projection = projection,
                        projectionOffsetY = 0.60
                    )
                    differenceWindow.addSeparator(color = modeWindowSeparator(), thickness = 0.5f, margin = 2f)
                        .addText(
                            Component.translatable(
                                "message.trainingdummy.comparison.difference",
                                "%+.1f".format(Locale.ROOT, difference)
                            ),
                            color = differenceColor,
                            shadow = true,
                            textScale = 1.5f,
                            alignment = WindowElement.Text.TextAlignment.CENTER
                        )
                    if (winnerStack.isEmpty) {
                        differenceWindow.addText(
                            Component.translatable("message.trainingdummy.comparison.tie"),
                            color = differenceColor,
                            alignment = WindowElement.Text.TextAlignment.CENTER
                        )
                    } else {
                        differenceWindow.addItemText(
                            winnerStack,
                            Component.translatable("message.trainingdummy.comparison.best_damage"),
                            color = differenceColor,
                            itemSize = 18f,
                            textScale = 0.8f,
                            alignment = WindowElement.Text.TextAlignment.CENTER
                        )
                    }
                    differenceWindow.build().render(poseStack, collector, packedLight)
                }
            }

            window.build().render(poseStack, collector, packedLight)
        }

        private val armorHudIcon = Identifier.fromNamespaceAndPath(
            "minecraft",
            "textures/gui/sprites/hud/armor_full.png"
        )

        private val absorptionHudIcon = Identifier.fromNamespaceAndPath(
            "minecraft",
            "textures/gui/sprites/hud/heart/absorbing_full.png"
        )

        private val enchantItem by lazy { Items.ENCHANTED_BOOK.defaultInstance }
        private val potionItem by lazy {
            Items.POTION.defaultInstance.also {
                it.set(
                    DataComponents.POTION_CONTENTS,
                    PotionContents(Potions.HEALING)
                )
            }
        }
        private val innateResistanceItem by lazy { Items.STRUCTURE_VOID.defaultInstance }
        private val invulnerabilityItem by lazy { Items.BARRIER.defaultInstance }

        private fun windowConfigDefault(
            window: FloatingWindow.Builder,
            entity: DummyEntity,
            detailed: Boolean = false,
            projection: DummyWindowProjection? = null
        ) {
            val damageData = entity.damageData
            if (projection == null) {
                window.attachToEntityRotation(entity).setOffset(0.9, 2.0, 0.0)
            } else {
                window.setFaceCamera(true)
                    .setOffset(projection.x, projection.y, projection.z)
            }

            applyWindowShape(window, entity)

            window.setSize(30f, 30f, autoScaleContent = true, autoFitWidth = true, autoFitHeight = true)
                .setAlignment(WindowAlignment.CENTER)
                .setPadding(3f)
                .setBorder(
                    WindowColor.AnimatedGradient(
                        argbStart = 0xFFC5A5FBL,
                        argbEnd = 0xFF5700ACL,
                        speed = 1.2f,
                        scale = 0.8f
                    ),
                    width = 0.75f
                )
                .setBackground(
                    WindowColor.Gradient(
                        argbStart = 0xF0181C2AL,
                        argbEnd = 0xF02B233DL
                    )
                )

            if (detailed) {
                window.addText(
                    Component.translatable("message.trainingdummy.original_damage").append(":"),
                    textScale = 0.8f,
                    shadow = true,
                    color = 0xDCDCDCFF.toInt()
                )
                    .addText(
                        "${damageData.originalDamage}",
                        color = 0xFF9BE7FF.toInt(),
                        shadow = true,
                        alignment = WindowElement.Text.TextAlignment.CENTER,
                        textScale = 2.5f
                    )
                    .addSeparator(
                        color = WindowColor.AnimatedGradient(
                            argbStart = 0xFF67E8F9L,
                            argbEnd = 0xFF8B5CF6L,
                            speed = 1.2f,
                            scale = 0.8f
                        ),
                        thickness = 0.5f,
                        margin = 2f
                    )
                    .addText(
                        Component.translatable("message.trainingdummy.final_damage"),
                        textScale = 0.8f,
                        shadow = true,
                        color = 0xDCDCDCFF.toInt()
                    )
            }

            window.addText(
                "${
                    if (detailed) {
                        damageData.newDamage
                    } else {
                        "%.2f".format(damageData.newDamage)
                    }
                }",
                color = if (damageData.isCriticalHit) {
                    WindowColor.AnimatedGradient(
                        argbStart = 0xFF0000L,
                        argbEnd = 0xff8c00L,
                        speed = 5.0f,
                        scale = 0.8f
                    )
                } else {
                    WindowColor.Pulse(0xFF9BE7FFL)
                },
                shadow = true,
                alignment = WindowElement.Text.TextAlignment.CENTER,
                textScale = 2.5f
            )

            if (damageData.isCriticalHit) {
                window.addText(
                    Component.translatable(
                        "message.trainingdummy.critical_multiplier",
                        String.format(Locale.ROOT, "%.1f", damageData.damageMultiplier)
                    ),
                    shadow = true,
                    alignment = WindowElement.Text.TextAlignment.CENTER,
                    textScale = 0.8f,
                    color = WindowColor.Pulse(color = 0xff1a1aL, speed = 1.5f)
                )
            }

            if (damageData.isReductions()) {
                window.addSeparator(
                    color = WindowColor.AnimatedGradient(
                        argbStart = 0xFF67E8F9L,
                        argbEnd = 0xFF8B5CF6L,
                        speed = 1.2f,
                        scale = 0.8f
                    ),
                    thickness = 0.5f,
                    margin = 2f
                )

                if (damageData.armor > 0.0f) {
                    window.addIconText(
                        armorHudIcon,
                        modeReductionText(
                            detailed,
                            "message.trainingdummy.reduction.armor",
                            damageData.armor
                        ),
                        alignment = WindowElement.Text.TextAlignment.CENTER
                    )
                }
                if (damageData.enchantments > 0.0f) {
                    window.addItemText(
                        enchantItem,
                        modeReductionText(
                            detailed,
                            "message.trainingdummy.reduction.enchantments",
                            damageData.enchantments
                        ),
                        alignment = WindowElement.Text.TextAlignment.CENTER
                    )
                }
                if (damageData.mobEffects > 0.0f) {
                    window.addItemText(
                        potionItem,
                        modeReductionText(
                            detailed,
                            "message.trainingdummy.reduction.effects",
                            damageData.mobEffects
                        ),
                        alignment = WindowElement.Text.TextAlignment.CENTER
                    )
                }
                if (damageData.absorption > 0.0f) {
                    window.addIconText(
                        absorptionHudIcon,
                        modeReductionText(
                            detailed,
                            "message.trainingdummy.reduction.absorption",
                            damageData.absorption
                        ),
                        alignment = WindowElement.Text.TextAlignment.CENTER
                    )
                }
                if (damageData.innateResistance > 0.0f) {
                    window.addItemText(
                        innateResistanceItem,
                        modeReductionText(
                            detailed,
                            "message.trainingdummy.reduction.innate_resistance",
                            damageData.innateResistance
                        ),
                        alignment = WindowElement.Text.TextAlignment.CENTER
                    )
                }
                if (damageData.invulnerability > 0.0f) {
                    window.addItemText(
                        invulnerabilityItem,
                        modeReductionText(
                            detailed,
                            "message.trainingdummy.reduction.invulnerability",
                            damageData.invulnerability
                        ),
                        alignment = WindowElement.Text.TextAlignment.CENTER
                    )
                }

                window.addSeparator(
                    color = WindowColor.AnimatedGradient(
                        argbStart = 0xFF67E8F9L,
                        argbEnd = 0xFF8B5CF6L,
                        speed = 1.2f,
                        scale = 0.8f
                    ),
                    thickness = 0.5f,
                    margin = 2f
                )
            }
        }

        private fun applyWindowShape(window: FloatingWindow.Builder, entity: DummyEntity) {
            when (val type = entity.windowShape) {
                WindowShapeType.ROUNDED -> {
                    window.setCornerRadius(3f)
                    window.setShape(WindowShapes.toPanelShape(type))
                }
                WindowShapeType.ROUNDED_RECTANGLE -> {
                    window.setCornerRadius(8f)
                    window.setShape(WindowShapes.toPanelShape(type))
                }
                else -> {
                    window.setShape(WindowShapes.toPanelShape(type))
                }
            }
        }

        private fun configureModeWindow(
            window: FloatingWindow.Builder,
            entity: DummyEntity,
            title: Component,
            offsetX: Double = 0.8,
            offsetY: Double = 2.0,
            width: Float = 30f,
            height: Float = 30f,
            projection: DummyWindowProjection? = null,
            projectionOffsetX: Double = 0.0,
            projectionOffsetY: Double = 0.0
        ) {
            if (projection == null) {
                window.attachToEntityRotation(entity).setOffset(offsetX, offsetY, 0.0)
            } else {
                window.setFaceCamera(true)
                    .setOffset(
                        projection.x + projectionOffsetX * projection.rightX,
                        projection.y + projectionOffsetY,
                        projection.z + projectionOffsetX * projection.rightZ
                    )
            }
            applyWindowShape(window, entity)
            window
                .setAlignment(WindowAlignment.CENTER)
                .setPadding(2f)
                .setSize(width, height, autoScaleContent = true, autoFitWidth = true, autoFitHeight = true)
                .setBorder(
                    WindowColor.AnimatedGradient(
                        argbStart = 0xFFC5A5FBL,
                        argbEnd = 0xFF5700ACL,
                        speed = 1.2f,
                        scale = 0.8f
                    ),
                    width = 0.75f
                )
                .setBackground(
                    WindowColor.Gradient(
                        argbStart = 0xF0181C2AL,
                        argbEnd = 0xF02B233DL
                    )
                )
                .addText(
                    title,
                    color = 0xFFFFFFFF.toInt(),
                    textScale = 0.85f,
                    alignment = WindowElement.Text.TextAlignment.CENTER
                )
        }

        private fun modeWindowSeparator() = WindowColor.AnimatedGradient(
            argbStart = 0xFF67E8F9L,
            argbEnd = 0xFF8B5CF6L,
            speed = 1.2f,
            scale = 0.8f
        )

        private fun buildComparisonPanel(
            window: FloatingWindow.Builder,
            entity: DummyEntity,
            title: Component,
            stack: ItemStack,
            damageValue: Float,
            offsetX: Double,
            projection: DummyWindowProjection? = null,
            projectionOffsetX: Double = 0.0
        ) {
            configureModeWindow(
                window,
                entity,
                title,
                offsetX = offsetX,
                projection = projection,
                projectionOffsetX = projectionOffsetX
            )
            window.addSeparator(color = modeWindowSeparator(), thickness = 0.5f, margin = 2f)
            if (stack.isEmpty) {
                window.addText(
                    Component.translatable("message.trainingdummy.bare_hands"),
                    color = 0xFF9BE7FF.toInt(),
                    textScale = 0.8f,
                    alignment = WindowElement.Text.TextAlignment.CENTER
                )
            } else {
                window.addItem(stack, size = 18f)
            }
            window.addText(
                String.format(Locale.ROOT, "%.1f", damageValue),
                color = 0xFF9BE7FF.toInt(),
                shadow = true,
                textScale = 2.0f,
                alignment = WindowElement.Text.TextAlignment.CENTER
            )
        }

        private fun modeReductionText(
            detailed: Boolean,
            translationKey: String,
            value: Float
        ): Component {
            val formattedValue = if (detailed) {
                value.toString()
            } else {
                String.format(Locale.ROOT, "%.1f", value)
            }

            return if (detailed) {
                Component.translatable(translationKey, formattedValue)
            } else {
                Component.literal("-$formattedValue")
            }
        }
    }
}
