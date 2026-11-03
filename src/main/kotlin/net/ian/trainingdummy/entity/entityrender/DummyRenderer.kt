package net.ian.trainingdummy.entity.entityrender

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.QuadInstance
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
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Hud
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.Model
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.renderer.OrderedSubmitNodeCollector
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
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.geometry.BakedQuad
import net.minecraft.client.resources.model.sprite.SpriteGetter
import net.minecraft.client.resources.model.sprite.SpriteId
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import net.minecraft.util.StringUtil
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import net.neoforged.neoforge.client.ClientHooks
import org.joml.Matrix4f
import org.joml.Vector3f
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val COLOR_CYCLE_TICKS = 150f
private const val COLOR_SATURATION = 0.9f
private const val HOLOGRAM_FULL_BRIGHT = 0xF000F0

data class DummyWindowProjection(
    val x: Double,
    val y: Double,
    val z: Double,
    val rightX: Double,
    val rightZ: Double,
    val alwaysVisible: Boolean = false,
    val isBlock: Boolean = false
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
                effect.equals("-translucent-", ignoreCase = true) -> RenderTypes.secondaryBlockOutline()
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

    /**
     * Intercepta todas as submissões do Dummy e de suas layers (corpo, base_plate, armadura,
     * asas/élitros, rosto do jogador, itens nas mãos, cabeça customizada) quando [isRenderingHandHologram]
     * está ativo, convertendo cada camada em uma projeção holográfica translúcida estilo sci-fi de cinema
     * (sem passe opaco por baixo).
     */
    private open class HologramOrderedCollector(
        protected val orderedDelegate: OrderedSubmitNodeCollector,
        protected val invRootMatrix: Matrix4f,
        protected val timeTicks: Float
    ) : OrderedSubmitNodeCollector by orderedDelegate {

        override fun <S : Any> submitModel(
            model: Model<in S>,
            state: S,
            poseStack: PoseStack,
            renderType: RenderType,
            lightCoords: Int,
            overlayCoords: Int,
            tintedColor: Int,
            sprite: TextureAtlasSprite?,
            outlineColor: Int,
            crumblingOverlay: ModelFeatureRenderer.CrumblingOverlay?
        ) {
            submitHologramModel(
                orderedDelegate,
                model,
                state,
                poseStack,
                renderType,
                tintedColor,
                sprite,
                invRootMatrix,
                timeTicks
            )
        }

        override fun <S : Any> submitModel(
            model: Model<in S>,
            state: S,
            poseStack: PoseStack,
            renderType: RenderType,
            lightCoords: Int,
            overlayCoords: Int,
            outlineColor: Int,
            crumblingOverlay: ModelFeatureRenderer.CrumblingOverlay?
        ) {
            this.submitModel(
                model,
                state,
                poseStack,
                renderType,
                lightCoords,
                overlayCoords,
                -1,
                null,
                outlineColor,
                crumblingOverlay
            )
        }

        override fun <S : Any> submitModel(
            model: Model<in S>,
            state: S,
            poseStack: PoseStack,
            texture: Identifier,
            lightCoords: Int,
            overlayCoords: Int,
            outlineColor: Int,
            crumblingOverlay: ModelFeatureRenderer.CrumblingOverlay?
        ) {
            this.submitModel(
                model,
                state,
                poseStack,
                model.renderType(texture),
                lightCoords,
                overlayCoords,
                -1,
                null,
                outlineColor,
                crumblingOverlay
            )
        }

        override fun <S : Any> submitModel(
            model: Model<S>,
            state: S,
            poseStack: PoseStack,
            lightCoords: Int,
            overlayCoords: Int,
            tintedColor: Int,
            sprite: SpriteId,
            sprites: SpriteGetter,
            outlineColor: Int,
            crumblingOverlay: ModelFeatureRenderer.CrumblingOverlay?
        ) {
            this.submitModel(
                model,
                state,
                poseStack,
                sprite.renderType(model::renderType),
                lightCoords,
                overlayCoords,
                tintedColor,
                sprites.get(sprite),
                outlineColor,
                crumblingOverlay
            )
        }

        override fun submitModelPart(
            modelPart: ModelPart,
            poseStack: PoseStack,
            renderType: RenderType,
            lightCoords: Int,
            overlayCoords: Int,
            sprite: TextureAtlasSprite?
        ) {
            this.submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, sprite, -1, null, 0)
        }

        override fun submitModelPart(
            modelPart: ModelPart,
            poseStack: PoseStack,
            renderType: RenderType,
            lightCoords: Int,
            overlayCoords: Int,
            sprite: TextureAtlasSprite?,
            tintedColor: Int,
            crumblingOverlay: ModelFeatureRenderer.CrumblingOverlay?
        ) {
            this.submitModelPart(
                modelPart,
                poseStack,
                renderType,
                lightCoords,
                overlayCoords,
                sprite,
                tintedColor,
                crumblingOverlay,
                0
            )
        }

        override fun submitModelPart(
            modelPart: ModelPart,
            poseStack: PoseStack,
            renderType: RenderType,
            lightCoords: Int,
            overlayCoords: Int,
            sprite: TextureAtlasSprite?,
            tintedColor: Int,
            crumblingOverlay: ModelFeatureRenderer.CrumblingOverlay?,
            outlineColor: Int
        ) {
            val holoRenderType = toHologramRenderType(renderType)
            orderedDelegate.submitCustomGeometry(poseStack, holoRenderType) { pose, buffer ->
                val snapshotStack = PoseStack()
                snapshotStack.last().set(pose)
                val baseConsumer = if (sprite != null) sprite.wrap(buffer) else buffer
                val holoConsumer = HologramVertexConsumer(baseConsumer, invRootMatrix, timeTicks)
                modelPart.render(
                    snapshotStack,
                    holoConsumer,
                    HOLOGRAM_FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    tintedColor
                )
            }
        }

        override fun submitCustomGeometry(
            poseStack: PoseStack,
            renderType: RenderType,
            customGeometryRenderer: SubmitNodeCollector.CustomGeometryRenderer
        ) {
            val holoRenderType = toHologramRenderType(renderType)
            orderedDelegate.submitCustomGeometry(poseStack, holoRenderType) { pose, buffer ->
                val holoConsumer = HologramVertexConsumer(buffer, invRootMatrix, timeTicks)
                customGeometryRenderer.render(pose, holoConsumer)
            }
        }

        override fun submitItem(
            poseStack: PoseStack,
            displayContext: ItemDisplayContext,
            lightCoords: Int,
            overlayCoords: Int,
            outlineColor: Int,
            tintLayers: IntArray,
            quads: List<BakedQuad>,
            foilType: ItemStackRenderState.FoilType
        ) {
            submitHologramItem(
                orderedDelegate,
                poseStack,
                tintLayers,
                quads,
                invRootMatrix,
                timeTicks
            )
        }
    }

    private class HologramSubmitNodeCollector(
        private val delegate: SubmitNodeCollector,
        invRootMatrix: Matrix4f,
        timeTicks: Float,
        private val orderOffset: Int = 1
    ) : HologramOrderedCollector(delegate.order(orderOffset), invRootMatrix, timeTicks), SubmitNodeCollector {

        override fun order(order: Int): OrderedSubmitNodeCollector {
            return HologramOrderedCollector(delegate.order(order + orderOffset), invRootMatrix, timeTicks)
        }
    }

    /**
     * VertexConsumer que aplica o efeito visual de holograma sci-fi de cinema a qualquer geometria:
     * - Transparência real luminosa (permite ver através do holograma mantendo os detalhes da textura/armadura/rosto);
     * - Linhas horizontais de varredura (scanlines) animadas subindo pelo modelo;
     * - Feixe de escaneamento periódico (laser scan wave) percorrendo da base ao topo;
     * - Brilho de borda 3D (Fresnel rim glow) calculado a partir da normal em espaço de câmera;
     * - Leve cintilação (flicker) de projetor holográfico.
     */
    private class HologramVertexConsumer(
        private val delegate: VertexConsumer,
        private val invRootMatrix: Matrix4f,
        private val timeTicks: Float
    ) : VertexConsumer {
        private var localY: Float = 0f
        private var baseR: Int = 255
        private var baseG: Int = 255
        private var baseB: Int = 255
        private var baseA: Int = 255
        private var colorPending: Boolean = false

        override fun addVertex(x: Float, y: Float, z: Float): VertexConsumer {
            localY = invRootMatrix.m01() * x + invRootMatrix.m11() * y + invRootMatrix.m21() * z + invRootMatrix.m31()
            colorPending = false
            return delegate.addVertex(x, y, z)
        }

        override fun setColor(red: Int, green: Int, blue: Int, alpha: Int): VertexConsumer {
            baseR = red.coerceIn(0, 255)
            baseG = green.coerceIn(0, 255)
            baseB = blue.coerceIn(0, 255)
            baseA = alpha.coerceIn(0, 255)
            colorPending = true
            return this
        }

        override fun setColor(color: Int): VertexConsumer {
            val a = (color ushr 24) and 0xFF
            val r = (color ushr 16) and 0xFF
            val g = (color ushr 8) and 0xFF
            val b = color and 0xFF
            return setColor(r, g, b, a)
        }

        override fun setUv(u: Float, v: Float): VertexConsumer = delegate.setUv(u, v)
        override fun setUv1(u: Int, v: Int): VertexConsumer = delegate.setUv1(u, v)
        override fun setUv2(u: Int, v: Int): VertexConsumer = delegate.setUv2(
            HOLOGRAM_FULL_BRIGHT and 0xFFFF,
            (HOLOGRAM_FULL_BRIGHT ushr 16) and 0xFFFF
        )

        override fun setLight(packedLightCoords: Int): VertexConsumer = delegate.setLight(HOLOGRAM_FULL_BRIGHT)
        override fun setOverlay(packedOverlayCoords: Int): VertexConsumer = delegate.setOverlay(OverlayTexture.NO_OVERLAY)

        override fun setNormal(x: Float, y: Float, z: Float): VertexConsumer {
            if (colorPending) {
                emitHologramColor(x, y, z)
                colorPending = false
            }
            return delegate.setNormal(x, y, z)
        }

        override fun setLineWidth(width: Float): VertexConsumer {
            if (colorPending) {
                emitHologramColor(0f, 0f, -1f)
                colorPending = false
            }
            return delegate.setLineWidth(width)
        }

        private fun emitHologramColor(nx: Float, ny: Float, nz: Float) {
            // Altura normalizada no dummy (0.0 na base_plate até 1.0 no topo da cabeça)
            val h = (localY / 1.95f).coerceIn(0f, 1f)

            // Fresnel rim glow: bordas inclinadas em relação à câmera brilham mais intensamente
            val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.001f)
            val absNz = abs(nz / nLen).coerceIn(0f, 1f)
            val rim = (1.0f - absNz) * (1.0f - absNz)

            // Scanlines finas horizontais subindo continuamente
            val scanlineWave = 0.5f + 0.5f * sin(h * 90.0f - timeTicks * 0.45f)
            val fineScan = 0.5f + 0.5f * sin(h * 210.0f + timeTicks * 0.25f)
            val scanFactor = 0.72f + 0.28f * (0.7f * scanlineWave + 0.3f * fineScan)

            // Feixe de escaneamento principal percorrendo verticalmente a cada ciclo
            val beamPos = ((timeTicks * 0.032f) % 1.5f) - 0.25f
            val beamDist = abs(h - beamPos)
            val beam = (1.0f - (beamDist / 0.13f)).coerceIn(0f, 1f).let { it * it }

            // Cintilação sutil de projetor holográfico
            val flicker = 0.94f + 0.06f * sin(timeTicks * 0.65f) * cos(timeTicks * 1.85f)

            // Preserva parte da cor/tint original da camada (ex: armadura tingida, jeb_, rosto) misturada ao ciano holográfico
            val srcR = baseR / 255f
            val srcG = baseG / 255f
            val srcB = baseB / 255f

            val holoBaseR = (0.14f + 0.22f * srcR + 0.38f * rim + 0.48f * beam).coerceIn(0f, 1f)
            val holoBaseG = (0.68f + 0.22f * srcG + 0.20f * rim + 0.25f * beam).coerceIn(0f, 1f)
            val holoBaseB = (0.92f + 0.08f * srcB + 0.08f * rim + 0.08f * beam).coerceIn(0f, 1f)

            val r = (holoBaseR * scanFactor * flicker * 255f).toInt().coerceIn(0, 255)
            val g = (holoBaseG * scanFactor * flicker * 255f).toInt().coerceIn(0, 255)
            val b = (holoBaseB * flicker * 255f).toInt().coerceIn(0, 255)

            // Transparência real de holograma: translúcido no centro (~115..145) e mais visível nas bordas/feixe (~175..210)
            val baseAlphaRatio = (baseA / 255f).coerceIn(0f, 1f)
            val holoAlpha = (98f + 36f * scanlineWave + 48f * rim + 52f * beam) * flicker * baseAlphaRatio
            val a = holoAlpha.toInt().coerceIn(25, 215)

            delegate.setColor(r, g, b, a)
        }
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
        state.effectsWindow = entity.effectsWindow
        state.faceCamera = entity.faceCamera
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
        if (isRenderingHandHologram) {
            val invRootMatrix = Matrix4f(poseStack.last().pose()).invert()
            val holoCollector = HologramSubmitNodeCollector(
                collector,
                invRootMatrix = invRootMatrix,
                timeTicks = state.ageInTicks
            )
            super.submit(state, poseStack, holoCollector, cameraState)
            renderHologramProjectorEffect(poseStack, collector, state.ageInTicks)
            return
        }

        super.submit(state, poseStack, collector, cameraState)

        val entity = state.dummyEntity
        if (entity != null && !state.isGui && !isRenderingHandHologram) {
            val projection = if (state.faceCamera || entity.faceCamera) {
                val cameraRight = Vector3f(1f, 0f, 0f).rotate(cameraState.orientation)
                DummyWindowProjection(
                    0.0,
                    2.0,
                    0.0,
                    cameraRight.x.toDouble(),
                    cameraRight.z.toDouble(),
                    alwaysVisible = false,
                    isBlock = false
                )
            } else {
                null
            }

            renderFloatingWindowModo(entity, poseStack, collector, state.lightCoords, projection = projection, partialTick = state.partialTick)
            if (state.effectsWindow || entity.effectsWindow) {
                renderEffectsWindow(entity, poseStack, collector, state.lightCoords, projection = projection, partialTick = state.partialTick)
            }
        }
    }

    /**
     * Renderiza o cone luminoso de projeção emergindo do pivot do item DisplayDummy
     * e anéis de varredura holográfica subindo ao redor do dummy.
     */
    private fun renderHologramProjectorEffect(
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        timeTicks: Float
    ) {
        collector.order(2).submitCustomGeometry(poseStack, RenderTypes.lightning()) { pose, consumer ->
            val segments = 16
            val bottomY = -0.10f
            val topY = 0.02f
            val bottomR = 0.10f
            val topR = 0.52f

            // Cone de luz do projetor (do centro do item até a base_plate do dummy)
            for (i in 0 until segments) {
                val a0 = (i.toFloat() / segments) * (Math.PI.toFloat() * 2f)
                val a1 = ((i + 1).toFloat() / segments) * (Math.PI.toFloat() * 2f)
                val c0 = cos(a0)
                val s0 = sin(a0)
                val c1 = cos(a1)
                val s1 = sin(a1)

                consumer.addVertex(pose, c0 * bottomR, bottomY, s0 * bottomR).setColor(80, 235, 255, 115)
                consumer.addVertex(pose, c1 * bottomR, bottomY, s1 * bottomR).setColor(80, 235, 255, 115)
                consumer.addVertex(pose, c1 * topR, topY, s1 * topR).setColor(0, 195, 255, 18)
                consumer.addVertex(pose, c0 * topR, topY, s0 * topR).setColor(0, 195, 255, 18)
            }

            // Anel de escaneamento holográfico subindo periodicamente pelo dummy
            val beamProgress = (timeTicks * 0.032f) % 1.0f
            val ringY = beamProgress * 1.85f
            val ringHalfH = 0.035f
            val ringRadius = 0.48f
            val ringAlpha = ((1.0f - abs(beamProgress - 0.5f) * 1.6f).coerceIn(0.15f, 1.0f) * 85f).toInt()

            for (i in 0 until segments) {
                val a0 = (i.toFloat() / segments) * (Math.PI.toFloat() * 2f)
                val a1 = ((i + 1).toFloat() / segments) * (Math.PI.toFloat() * 2f)
                val x0 = cos(a0) * ringRadius
                val z0 = sin(a0) * ringRadius
                val x1 = cos(a1) * ringRadius
                val z1 = sin(a1) * ringRadius

                consumer.addVertex(pose, x0, ringY - ringHalfH, z0).setColor(40, 225, 255, 0)
                consumer.addVertex(pose, x1, ringY - ringHalfH, z1).setColor(40, 225, 255, 0)
                consumer.addVertex(pose, x1, ringY + ringHalfH, z1).setColor(120, 245, 255, ringAlpha)
                consumer.addVertex(pose, x0, ringY + ringHalfH, z0).setColor(120, 245, 255, ringAlpha)
            }
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

    override fun shouldShowName(entity: DummyEntity, distanceSq: Double): Boolean = false

    companion object {
        var isRenderingHandHologram: Boolean = false

        private val sampler0Regex = Regex("Sampler0=TextureBinding\\[location=([^,\\]]+)")
        private val hologramRenderTypeCache = ConcurrentHashMap<RenderType, RenderType>()

        private fun toHologramRenderType(renderType: RenderType): RenderType {
            return hologramRenderTypeCache.computeIfAbsent(renderType) { rt ->
                val desc = rt.toString()
                val match = sampler0Regex.find(desc)
                val textureId = match?.groupValues?.getOrNull(1)?.let { Identifier.tryParse(it) }
                when {
                    textureId == null -> rt
                    desc.contains("armor") -> RenderTypes.armorTranslucent(textureId)
                    else -> RenderTypes.entityTranslucent(textureId, false)
                }
            }
        }

        private fun <S : Any> submitHologramModel(
            collector: OrderedSubmitNodeCollector,
            model: Model<in S>,
            state: S,
            poseStack: PoseStack,
            renderType: RenderType,
            tintedColor: Int,
            sprite: TextureAtlasSprite?,
            invRootMatrix: Matrix4f,
            timeTicks: Float
        ) {
            val holoRenderType = toHologramRenderType(renderType)
            collector.submitCustomGeometry(poseStack, holoRenderType) { pose, buffer ->
                val snapshotStack = PoseStack()
                snapshotStack.last().set(pose)
                val baseConsumer = if (sprite != null) sprite.wrap(buffer) else buffer
                val holoConsumer = HologramVertexConsumer(baseConsumer, invRootMatrix, timeTicks)
                model.setupAnim(state)
                val previousVisibility = ClientHooks.updateModelVisibility(model, state)
                model.renderToBuffer(
                    snapshotStack,
                    holoConsumer,
                    HOLOGRAM_FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    tintedColor
                )
                ClientHooks.resetVisibility(previousVisibility)
            }
        }

        private fun submitHologramItem(
            collector: OrderedSubmitNodeCollector,
            poseStack: PoseStack,
            tintLayers: IntArray,
            quads: List<BakedQuad>,
            invRootMatrix: Matrix4f,
            timeTicks: Float
        ) {
            if (quads.isEmpty()) return
            val quadsByRenderType = quads.groupBy { toHologramRenderType(it.materialInfo().itemRenderType()) }
            for ((holoRenderType, quadGroup) in quadsByRenderType) {
                collector.submitCustomGeometry(poseStack, holoRenderType) { pose, buffer ->
                    val holoConsumer = HologramVertexConsumer(buffer, invRootMatrix, timeTicks)
                    val quadInstance = QuadInstance()
                    quadInstance.setLightCoords(HOLOGRAM_FULL_BRIGHT)
                    quadInstance.setOverlayCoords(OverlayTexture.NO_OVERLAY)
                    for (quad in quadGroup) {
                        val material = quad.materialInfo()
                        val tintIndex = material.tintIndex()
                        val color = if (material.isTinted() && tintIndex >= 0 && tintIndex < tintLayers.size) {
                            tintLayers[tintIndex]
                        } else {
                            -1
                        }
                        quadInstance.setColor(color)
                        holoConsumer.putBakedQuad(pose, quad, quadInstance)
                    }
                }
            }
        }

        private class MobEffectIcon(
            private val sprite: TextureAtlasSprite,
            size: Float = 9f
        ) : WindowElement {
            override val isIcon: Boolean get() = true
            private val iconSize = size.coerceAtLeast(1f)

            override fun getWidth(): Float = iconSize
            override fun getHeight(): Float = iconSize

            override fun render(
                poseStack: PoseStack,
                collector: OrderedSubmitNodeCollector,
                x: Float,
                y: Float,
                availableWidth: Float,
                packedLight: Int
            ) {
                val renderType = RenderTypes.entityCutout(sprite.atlasLocation())
                collector.submitCustomGeometry(poseStack, renderType) { pose, builder ->
                    val matrix = pose.pose()
                    val right = x + iconSize
                    val bottom = y + iconSize
                    val u0 = sprite.u0
                    val u1 = sprite.u1
                    val v0 = sprite.v0
                    val v1 = sprite.v1

                    builder.addVertex(matrix, x, y, 0.05f).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0f, 0f, 1f)
                    builder.addVertex(matrix, x, bottom, 0.05f).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0f, 0f, 1f)
                    builder.addVertex(matrix, right, bottom, 0.05f).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0f, 0f, 1f)
                    builder.addVertex(matrix, right, y, 0.05f).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0f, 0f, 1f)
                }
            }
        }

        /**
         * Elemento de item que instancia seu próprio [ItemStackRenderState] a cada renderização.
         * Corrige o bug do WorldPanelItemRenderer em que o compartilhamento de um único
         * ItemStackRenderState sobrescrevia a lista de BakedQuads antes do flush do SubmitNodeCollector,
         * fazendo todas as janelas exibirem o mesmo item no modo Comparação.
         */
        private class SafeItemIcon(
            stack: ItemStack,
            size: Float = 16f
        ) : WindowElement {
            override val isIcon: Boolean get() = true
            private val itemStack: ItemStack = stack.copy()
            private val iconSize = size.coerceAtLeast(1f)

            override fun getWidth(): Float = iconSize
            override fun getHeight(): Float = iconSize

            override fun render(
                poseStack: PoseStack,
                collector: OrderedSubmitNodeCollector,
                x: Float,
                y: Float,
                availableWidth: Float,
                packedLight: Int
            ) {
                if (itemStack.isEmpty) return

                val minecraft = Minecraft.getInstance()
                val renderState = ItemStackRenderState()
                minecraft.itemModelResolver.updateForTopItem(
                    renderState,
                    itemStack,
                    ItemDisplayContext.GUI,
                    minecraft.level,
                    null,
                    0
                )
                poseStack.pushPose()
                poseStack.translate((x + iconSize / 2f).toDouble(), (y + iconSize / 2f).toDouble(), 0.05)
                poseStack.scale(iconSize, -iconSize, 0.01f)
                val submitCollector = if (collector is SubmitNodeCollector) {
                    collector
                } else {
                    object : SubmitNodeCollector, OrderedSubmitNodeCollector by collector {
                        override fun order(order: Int): OrderedSubmitNodeCollector = collector
                    }
                }
                renderState.submit(poseStack, submitCollector, packedLight, OverlayTexture.NO_OVERLAY, 0)
                poseStack.popPose()
            }
        }

        private fun FloatingWindow.Builder.addSafeItem(
            stack: ItemStack,
            size: Float = 16f,
            alignment: WindowElement.Text.TextAlignment = WindowElement.Text.TextAlignment.CENTER
        ): FloatingWindow.Builder = addRow(
            SafeItemIcon(stack, size),
            spacing = 0f,
            alignment = alignment
        )

        private fun FloatingWindow.Builder.addSafeItemText(
            stack: ItemStack,
            component: Component,
            color: WindowColor = WindowColor.Solid(0xFFFFFFFFL),
            itemSize: Float = 9f,
            textScale: Float = 1f,
            shadow: Boolean = false,
            spacing: Float = 1f,
            alignment: WindowElement.Text.TextAlignment = WindowElement.Text.TextAlignment.LEFT
        ): FloatingWindow.Builder = addRow(
            SafeItemIcon(stack, itemSize),
            WindowElement.Text(
                text = component,
                color = color,
                shadow = shadow,
                textScale = textScale
            ),
            spacing = spacing,
            alignment = alignment
        )

        private fun FloatingWindow.Builder.addSafeItemText(
            stack: ItemStack,
            component: Component,
            color: Int,
            itemSize: Float = 9f,
            textScale: Float = 1f,
            shadow: Boolean = false,
            spacing: Float = 1f,
            alignment: WindowElement.Text.TextAlignment = WindowElement.Text.TextAlignment.LEFT
        ): FloatingWindow.Builder = addSafeItemText(
            stack,
            component,
            WindowColor.Solid(color.toLong() and 0xFFFFFFFFL),
            itemSize,
            textScale,
            shadow,
            spacing,
            alignment
        )

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
            val alwaysVisible = projection?.alwaysVisible == true
            if (!alwaysVisible && modoAtual != TrainingModos.DPS && entity.displayTicks <= 0) return

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
                        val lateralOffset = if (projection.isBlock) 0.0 else 0.9
                        window.setFaceCamera(true)
                            .setOffset(
                                projection.x + lateralOffset * projection.rightX,
                                projection.y,
                                projection.z + lateralOffset * projection.rightZ
                            )
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
                    val lateralOffset = if (projection?.isBlock == true) 0.0 else 0.8
                    configureModeWindow(
                        window,
                        entity,
                        Component.translatable("message.trainingdummy.accumulated_title"),
                        offsetX = 0.8,
                        projection = projection,
                        projectionOffsetX = lateralOffset
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
                    val isBlock = projection?.isBlock == true
                    val sideOffset = if (isBlock) 0.48 else 0.8
                    window.setRenderOrder(0)
                    buildComparisonPanel(
                        window,
                        entity,
                        Component.translatable("message.trainingdummy.previous"),
                        entity.damageStackOld,
                        damageDataOld.newDamage,
                        damageDataOld.isCriticalHit,
                        offsetX = 0.8,
                        projection = projection,
                        projectionOffsetX = sideOffset
                    )

                    val window2 = FloatingWindow.Builder().setRenderOrder(2)
                    buildComparisonPanel(
                        window2,
                        entity,
                        Component.translatable("message.trainingdummy.current"),
                        entity.damageStack,
                        damageData.newDamage,
                        damageData.isCriticalHit,
                        offsetX = -0.8,
                        projection = projection,
                        projectionOffsetX = -sideOffset
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
                    val differenceWindow = FloatingWindow.Builder().setRenderOrder(4)
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
                            Component.translatable(
                                if (kotlin.math.abs(difference) <= 0.001f) {
                                    "message.trainingdummy.comparison.tie"
                                } else {
                                    "message.trainingdummy.bare_hands"
                                }
                            ),
                            color = differenceColor,
                            alignment = WindowElement.Text.TextAlignment.CENTER
                        )
                    } else {
                        differenceWindow.addSafeItemText(
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

        private fun calculateEffectsWindowBaseY(
            entity: DummyEntity,
            projection: DummyWindowProjection? = null
        ): Double {
            val isProjection = projection != null
            val modoAtual = entity.trainingModuleConfig.trainingModo
            val hasActiveSideWindow = (projection?.alwaysVisible == true || modoAtual == TrainingModos.DPS || entity.displayTicks > 0)
            val baseHeadY = if (isProjection) 0.40 else 2.40

            if (!hasActiveSideWindow) {
                return baseHeadY
            }

            return when (modoAtual) {
                TrainingModos.DEFAULT -> {
                    val damageData = entity.damageData
                    var estimatedHeightPx = 24f
                    if (entity.detailedDamageWindow) {
                        estimatedHeightPx += 26f
                    }
                    if (damageData.isCriticalHit) {
                        estimatedHeightPx += 12f
                    }
                    if (damageData.isReductions()) {
                        estimatedHeightPx += 6f
                        if (damageData.armor > 0f) estimatedHeightPx += 11f
                        if (damageData.enchantments > 0f) estimatedHeightPx += 11f
                        if (damageData.mobEffects > 0f) estimatedHeightPx += 11f
                        if (damageData.absorption > 0f) estimatedHeightPx += 11f
                        if (damageData.innateResistance > 0f) estimatedHeightPx += 11f
                        if (damageData.invulnerability > 0f) estimatedHeightPx += 11f
                    }
                    val halfHeightMeters = (estimatedHeightPx * 0.025f) / 2.0
                    val topMeters = 2.0 + halfHeightMeters
                    val targetY = topMeters + 0.12
                    if (isProjection) (targetY - 2.0).coerceAtLeast(0.40) else targetY.coerceAtLeast(2.40)
                }

                TrainingModos.COMPARISON -> {
                    val targetY = 2.95
                    if (isProjection) 0.95 else targetY
                }

                TrainingModos.DPS -> {
                    val estimatedHeightPx = when (entity.dpsTestState) {
                        DummyEntity.DPS_STATE_COOLDOWN -> 42f
                        DummyEntity.DPS_STATE_MEASURING -> 32f
                        else -> 18f
                    }
                    val halfHeightMeters = (estimatedHeightPx * 0.025f) / 2.0
                    val topMeters = 2.0 + halfHeightMeters
                    val targetY = topMeters + 0.10
                    if (isProjection) (targetY - 2.0).coerceAtLeast(0.40) else targetY.coerceAtLeast(2.40)
                }

                TrainingModos.ACCUMULATED -> {
                    val halfHeightMeters = (30f * 0.025f) / 2.0
                    val topMeters = 2.0 + halfHeightMeters
                    val targetY = topMeters + 0.10
                    if (isProjection) (targetY - 2.0).coerceAtLeast(0.40) else targetY.coerceAtLeast(2.40)
                }
            }
        }

        fun renderEffectsWindow(
            entity: DummyEntity,
            poseStack: PoseStack,
            collector: SubmitNodeCollector,
            packedLight: Int,
            projection: DummyWindowProjection? = null,
            partialTick: Float = 0f
        ) {
            val activeEffects = entity.activeEffects
            if (activeEffects.isEmpty()) return

            val detailed = entity.detailedDamageWindow

            val window = FloatingWindow.Builder().setRenderOrder(6)
            if (projection == null) {
                val offsetY = calculateEffectsWindowBaseY(entity, projection = null)
                window.attachToEntityRotation(entity).setOffset(0.0, offsetY, 0.0)
            } else {
                val projectionOffsetY = calculateEffectsWindowBaseY(entity, projection = projection)
                window.setFaceCamera(true)
                    .setOffset(
                        projection.x,
                        projection.y + projectionOffsetY,
                        projection.z
                    )
            }

            applyWindowShape(window, entity)
            window
                .setAlignment(WindowAlignment.BOTTOM_CENTER)
                .setScale(0.013f)
                .setPadding(3f)
                .setContentFit(FloatingWindow.ContentFit.EXPAND_TO_CONTENT)
                .setBorder(
                    WindowColor.AnimatedGradient(
                        argbStart = 0xFF67E8F9L,
                        argbEnd = 0xFF8B5CF6L,
                        speed = 1.0f,
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
                    Component.translatable("message.trainingdummy.effects_title"),
                    color = 0xFFFFFFFF.toInt(),
                    textScale = if (detailed) 1.0f else 0.9f,
                    alignment = WindowElement.Text.TextAlignment.CENTER
                )
                .addSeparator(color = modeWindowSeparator(), thickness = 0.5f, margin = 2f)

            val guiAtlas = Minecraft.getInstance().atlasManager.getAtlasOrThrow(Identifier.withDefaultNamespace("gui"))

            if (!detailed) {
                // Modo compacto: APENAS os ícones organizados em fileiras horizontais
                val iconSize = 14f
                val maxPerRow = 5
                for (chunk in activeEffects.chunked(maxPerRow)) {
                    val rowElements = chunk.map { effectInstance ->
                        val sprite = guiAtlas.getSprite(Hud.getMobEffectSprite(effectInstance.effect))
                        MobEffectIcon(sprite, size = iconSize)
                    }
                    window.addRow(
                        *rowElements.toTypedArray(),
                        spacing = 3f,
                        alignment = WindowElement.Text.TextAlignment.CENTER
                    )
                }
            } else {
                // Modo detalhado: Ícone + Nome + Tempo restante com tamanho ampliado
                for (effectInstance in activeEffects) {
                    val effectHolder = effectInstance.effect
                    val effect = effectHolder.value()
                    val sprite = guiAtlas.getSprite(Hud.getMobEffectSprite(effectHolder))
                    val durationStr = if (effectInstance.isInfiniteDuration) "∞" else StringUtil.formatTickDuration(effectInstance.duration, 20f)
                    val effectName = Component.translatable(effect.descriptionId)
                    val label = if (effectInstance.amplifier > 0) {
                        Component.translatable(
                            "potion.withAmplifier",
                            effectName,
                            Component.translatable("enchantment.level.${effectInstance.amplifier + 1}")
                        )
                    } else {
                        effectName
                    }.copy().append(Component.literal(" ($durationStr)"))

                    val effectColor = effect.color
                    val textColor = if (effectColor != 0) (0xFF shl 24) or (effectColor and 0x00FFFFFF) else 0xFFFFFFFF.toInt()

                    window.addRow(
                        MobEffectIcon(sprite, size = 12f),
                        WindowElement.Text(
                            text = label,
                            color = WindowColor.Solid(textColor.toLong() and 0xFFFFFFFFL),
                            shadow = true,
                            textScale = 0.95f,
                            alignment = WindowElement.Text.TextAlignment.LEFT
                        ),
                        spacing = 3f,
                        alignment = WindowElement.Text.TextAlignment.LEFT
                    )
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
                window.attachToEntityRotation(entity).setOffset(0.8, 2.0, 0.0)
            } else {
                val lateralOffset = if (projection.isBlock) 0.0 else 0.8
                window.setFaceCamera(true)
                    .setOffset(
                        projection.x + lateralOffset * projection.rightX,
                        projection.y,
                        projection.z + lateralOffset * projection.rightZ
                    )
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
                    window.addSafeItemText(
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
                    window.addSafeItemText(
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
                    window.addSafeItemText(
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
                    window.addSafeItemText(
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
            isCriticalHit: Boolean = false,
            offsetX: Double,
            projection: DummyWindowProjection? = null,
            projectionOffsetX: Double = offsetX
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
                window.addSafeItem(stack, size = 18f)
            }
            window.addText(
                String.format(Locale.ROOT, "%.1f", damageValue),
                color = if (isCriticalHit) {
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
