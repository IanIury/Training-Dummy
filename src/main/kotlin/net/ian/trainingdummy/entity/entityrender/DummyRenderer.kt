package net.ian.trainingdummy.entity.entityrender

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import org.joml.Matrix4f
import org.joml.Vector3f
import net.minecraft.client.Minecraft
import net.minecraft.core.Holder
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.network.chat.CommonComponents
import net.minecraft.util.StringUtil
import dev.ian.worldpanel.FloatingText
import dev.ian.worldpanel.FloatingWindow
import dev.ian.worldpanel.TextRenderMode
import dev.ian.worldpanel.WindowAlignment
import dev.ian.worldpanel.WindowColor
import dev.ian.worldpanel.WindowElement
import dev.ian.worldpanel.shape.PanelShape
import net.ian.trainingdummy.client.gui.WindowShapes
import net.ian.trainingdummy.init.enums.WindowShapeType
import dev.ian.worldpanel.shape.TextureFitMode
import dev.ian.worldpanel.region.RegionMap
import dev.ian.worldpanel.layout.ContentLayout
import dev.ian.worldpanel.depth.DepthStyle
import dev.ian.worldpanel.shape.RectF
import dev.ian.worldpanel.shape.Volume3DConfig

import net.ian.trainingdummy.client.DisplayDummyClientUtil
import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.client.layer.DummyCustomHeadLayer
import net.ian.trainingdummy.client.layer.DummyElytraLayer
import net.ian.trainingdummy.client.layer.DummyFaceLayer
import net.ian.trainingdummy.client.layer.DummyHumanoidArmorLayer
import net.ian.trainingdummy.client.layer.DummyItemInHandLayer

import net.ian.trainingdummy.client.model.DummyModel

import net.ian.trainingdummy.entity.DummyEntity
import net.ian.trainingdummy.event.utils.DamageData
import net.ian.trainingdummy.init.enums.TrainingModos
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.HumanoidMobRenderer
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer
import net.minecraft.client.renderer.entity.layers.ElytraLayer
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
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

data class DummyRenderState(
    var effectsWindow: Boolean = false,
    var faceCamera: Boolean = false
)

class MobEffectIconElement(
    private val effect: Holder<MobEffect>,
    private val size: Float = 14f
) : WindowElement {
    override val isIcon: Boolean get() = true
    private val iconSize = size.coerceAtLeast(1f)

    override fun getWidth(): Float = iconSize
    override fun getHeight(): Float = iconSize

    override fun render(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        x: Float,
        y: Float,
        availableWidth: Float,
        packedLight: Int
    ) {
        val sprite = Minecraft.getInstance().mobEffectTextures.get(effect) ?: return
        val builder = buffer.getBuffer(RenderType.entityCutoutNoCull(sprite.atlasLocation()))
        val matrix = poseStack.last().pose()
        val right = x + iconSize
        val bottom = y + iconSize

        val u0 = sprite.u0
        val u1 = sprite.u1
        val v0 = sprite.v0
        val v1 = sprite.v1

        builder.addVertex(matrix, x, y, 0.05f).setColor(1f, 1f, 1f, 1f).setUv(u0, v0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0f, 0f, 1f)
        builder.addVertex(matrix, x, bottom, 0.05f).setColor(1f, 1f, 1f, 1f).setUv(u0, v1).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0f, 0f, 1f)
        builder.addVertex(matrix, right, bottom, 0.05f).setColor(1f, 1f, 1f, 1f).setUv(u1, v1).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0f, 0f, 1f)
        builder.addVertex(matrix, right, y, 0.05f).setColor(1f, 1f, 1f, 1f).setUv(u1, v0).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0f, 0f, 1f)
    }
}


class DummyRenderer(context: EntityRendererProvider.Context) : HumanoidMobRenderer<DummyEntity, DummyModel>(
    context,
    DummyModel(context.bakeLayer(DummyModel.LAYER_LOCATION)),
    0.5f
) {

    init {

        this.layers.removeIf { it is CustomHeadLayer<*, *> }
        this.layers.removeIf { it is ItemInHandLayer<*, *> }
        this.layers.removeIf { it is ElytraLayer<*, *> }

        this.addLayer(JebColorLayer())
        this.addLayer(NamedRenderEffectLayer())

        val innerArmor = HumanoidModel<DummyEntity>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR))
        val outerArmor = HumanoidModel<DummyEntity>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR))

        this.addLayer(DummyHumanoidArmorLayer(this, innerArmor, outerArmor, context.modelManager))
        this.addLayer(DummyItemInHandLayer(this, context.itemInHandRenderer))
        this.addLayer(DummyCustomHeadLayer(this, context.modelSet, context.itemInHandRenderer))
        this.addLayer(DummyElytraLayer(this, context.modelSet))
        this.addLayer(DummyFaceLayer(this, context.modelSet))
        this.addLayer(HandHologramLayer())

        //this.addLayer(DummyOutlineLayer(this))
    }

    private inner class JebColorLayer : RenderLayer<DummyEntity, DummyModel>(this@DummyRenderer) {
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
            if (entity.customName?.string != "jeb_") return

            val cycleProgress = (entity.tickCount + partialTick) / COLOR_CYCLE_TICKS
            val hue = cycleProgress - kotlin.math.floor(cycleProgress)
            val rgb = Mth.hsvToRgb(hue, COLOR_SATURATION, 1f)
            val color = (0xFF shl 24) or (rgb and 0x00FFFFFF)
            val model = parentModel
            val vertexConsumer = ItemRenderer.getFoilBuffer(
                buffer,
                RenderType.entityTranslucentEmissive(getTextureLocation(entity)),
                false,
                true
            )

            model.renderToBuffer(
                poseStack,
                vertexConsumer,
                packedLight,
                LivingEntityRenderer.getOverlayCoords(entity, 0f),
                color
            )
        }
    }

    private inner class HandHologramLayer : RenderLayer<DummyEntity, DummyModel>(this@DummyRenderer) {
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
            if (!DisplayDummyClientUtil.isRenderingHandHologram) return

            val time = entity.tickCount + partialTick
            val texture = getTextureLocation(entity)
            val baseConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(texture))

            val gradientConsumer = HologramGradientVertexConsumer(
                delegate = baseConsumer,
                time = time,
                gradientStartY = 0.0f,
                gradientEndY = 0.95f
            )

            val overlay = LivingEntityRenderer.getOverlayCoords(entity, 0f)

            parentModel.renderToBuffer(poseStack, gradientConsumer, 15728880, overlay, -1)
        }
    }

    private class HologramGradientVertexConsumer(
        private val delegate: VertexConsumer,
        private val time: Float,
        private val gradientStartY: Float = 0.0f,
        private val gradientEndY: Float = 0.95f
    ) : VertexConsumer {

        private var currentWorldY: Float = 0.0f

        override fun addVertex(x: Float, y: Float, z: Float): VertexConsumer {
            currentWorldY = y
            delegate.addVertex(x, y, z)
            return this
        }

        override fun addVertex(matrix: Matrix4f, x: Float, y: Float, z: Float): VertexConsumer {
            // Calcula a altura vertical Y real do vértice aplicando a matriz de transformação do PoseStack
            currentWorldY = matrix.m01() * x + matrix.m11() * y + matrix.m21() * z + matrix.m31()
            delegate.addVertex(matrix, x, y, z)
            return this
        }

        override fun setColor(r: Int, g: Int, b: Int, a: Int): VertexConsumer {
            val y = currentWorldY
            val t = when {
                y <= gradientStartY -> 1.0f
                y >= gradientEndY -> 0.0f
                else -> 1.0f - ((y - gradientStartY) / (gradientEndY - gradientStartY))
            }

            if (t <= 0.0f) {
                // Acima da metade do torso: completamente transparente (sem efeito de holograma)
                delegate.setColor(0, 0, 0, 0)
                return this
            }

            // Suavização do gradiente (smoothstep)
            val smoothT = t * t * (3.0f - 2.0f * t)

            // Efeito sutil de linhas de varredura (scanlines) e pulso animado em tempo real
            val scanline = 0.92f + 0.08f * kotlin.math.sin(time * 0.35f - y * 14.0f)
            val pulse = 0.90f + 0.10f * kotlin.math.sin(time * 0.2f)

            // Opacidade máxima na base (~85%), decrescendo suavemente até 0% na cintura/metade do torso
            val finalAlpha = (smoothT * 0.88f * scanline * pulse).coerceIn(0.0f, 1.0f)
            val alphaInt = (finalAlpha * 255).toInt()

            // Cor holográfica ciano luminosa (gradiente suave de 0x38BDF8 na base para 0x67E8F9)
            val holoR = ((0x38 * smoothT + 0x67 * (1.0f - smoothT))).toInt().coerceIn(0, 255)
            val holoG = ((0xBD * smoothT + 0xE8 * (1.0f - smoothT))).toInt().coerceIn(0, 255)
            val holoB = 0xF8

            delegate.setColor(holoR, holoG, holoB, alphaInt)
            return this
        }

        override fun setUv(u: Float, v: Float): VertexConsumer {
            delegate.setUv(u, v)
            return this
        }

        override fun setUv1(u: Int, v: Int): VertexConsumer {
            delegate.setUv1(u, v)
            return this
        }

        override fun setUv2(u: Int, v: Int): VertexConsumer {
            // Brilho emissivo máximo para o holograma
            delegate.setUv2(240, 240)
            return this
        }

        override fun setNormal(x: Float, y: Float, z: Float): VertexConsumer {
            delegate.setNormal(x, y, z)
            return this
        }
    }

    private inner class NamedRenderEffectLayer : RenderLayer<DummyEntity, DummyModel>(this@DummyRenderer) {
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
            val effect = entity.customName?.string ?: return
            val renderType = when {
                effect.equals("-endGateway-", ignoreCase = true) -> RenderType.endGateway()
                effect.equals("-entityGlint-", ignoreCase = true) -> RenderType.entityGlint()
                effect.equals("-translucent-", ignoreCase = true) -> RenderType.translucent()
                effect.equals("-lightning-", ignoreCase = true) -> RenderType.lightning()
                effect.equals("-clouds-", ignoreCase = true) -> RenderType.clouds()
                else -> return
            }

            parentModel.renderToBuffer(
                poseStack,
                buffer.getBuffer(renderType),
                packedLight,
                LivingEntityRenderer.getOverlayCoords(entity, 0f),
                -1
            )
        }
    }

    // Textura da entidade (.png em assets/modid/textures/entity/dummy.png)
    override fun getTextureLocation(entity: DummyEntity): ResourceLocation {
        return ResourceLocation.fromNamespaceAndPath(TrainingDummy.ID, "textures/entity/training_dummy.png")
    }

    override fun render(
        entity: DummyEntity,
        entityYaw: Float,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight)

        if (!DisplayDummyClientUtil.isRenderingHandHologram) {
            val camera = Minecraft.getInstance().gameRenderer.mainCamera
            val cameraRight = Vector3f(1f, 0f, 0f).rotate(camera.rotation())
            val projection = if (entity.faceCamera) {
                DummyWindowProjection(0.0, 2.0, 0.0, cameraRight.x.toDouble(), cameraRight.z.toDouble())
            } else {
                null
            }
            renderFloatingWindowModo(entity, partialTicks, poseStack, buffer, packedLight, projection)
        }

    }

    override fun shouldRender(
        entity: DummyEntity,
        camera: net.minecraft.client.renderer.culling.Frustum,
        camX: Double,
        camY: Double,
        camZ: Double
    ): Boolean {
        // Expande a bounding box para não sofrer culling quando olhar para o letreiro/janela no alto
        val expandedBox = entity.boundingBox.inflate(1.5, 3.5, 1.5)
        return camera.isVisible(expandedBox)
    }

    override fun shouldShowName(entity: DummyEntity): Boolean = false


    companion object {

        private fun calculateEffectsWindowBaseY(
            entity: DummyEntity,
            isProjection: Boolean,
            fromBlockEntity: Boolean = false
        ): Double {
            val modoAtual = entity.trainingModuleConfig.trainingModo
            val hasActiveSideWindow = (fromBlockEntity || modoAtual == TrainingModos.DPS || entity.displayTicks > 0)
            val baseHeadY = if (isProjection) 0.40 else 2.40

            if (!hasActiveSideWindow) {
                return baseHeadY
            }

            return when (modoAtual) {
                TrainingModos.DEFAULT -> {
                    val damageData = entity.damageData
                    var estimatedHeightPx = 24f
                    if (entity.detailedDamageWindow) estimatedHeightPx += 26f
                    if (damageData.isCriticalHit) estimatedHeightPx += 12f
                    if (damageData.isReductions()) {
                        estimatedHeightPx += 6f // margem do separador
                        if (damageData.armor > 0f) estimatedHeightPx += 11f
                        if (damageData.enchantments > 0f) estimatedHeightPx += 11f
                        if (damageData.mobEffects > 0f) estimatedHeightPx += 11f
                        if (damageData.absorption > 0f) estimatedHeightPx += 11f
                        if (damageData.innateResistance > 0f) estimatedHeightPx += 11f
                        if (damageData.invulnerability > 0f) estimatedHeightPx += 11f
                    }
                    // A janela lateral é CENTER em Y = 2.0
                    val halfHeightMeters = (estimatedHeightPx * 0.025f) / 2.0
                    val topMeters = 2.0 + halfHeightMeters
                    val targetY = (topMeters + 0.12 - 0.30).coerceAtLeast(baseHeadY)
                    if (isProjection) (targetY - 2.0 - 0.15).coerceAtLeast(baseHeadY) else targetY
                }

                TrainingModos.COMPARISON -> {
                    // No modo de comparação, a janela central de resultado (diferença) fica em Y = 2.5 (sem projeção)
                    // ou projectionOffsetY = 0.65 (com projeção). A janela de efeitos fica posicionada logo acima.
                    val targetY = 3.05
                    if (isProjection) 1.15 else targetY
                }

                TrainingModos.DPS -> {
                    val estimatedHeightPx = when (entity.dpsTestState) {
                        DummyEntity.DPS_STATE_COOLDOWN -> 42f
                        DummyEntity.DPS_STATE_MEASURING -> 32f
                        else -> 18f
                    }
                    val halfHeightMeters = (estimatedHeightPx * 0.025f) / 2.0
                    val topMeters = 2.0 + halfHeightMeters
                    val targetY = (topMeters + 0.10 - 0.30).coerceAtLeast(baseHeadY)
                    if (isProjection) (targetY - 2.0 - 0.15).coerceAtLeast(baseHeadY) else targetY
                }

                TrainingModos.ACCUMULATED -> {
                    val halfHeightMeters = (30f * 0.025f) / 2.0
                    val topMeters = 2.0 + halfHeightMeters
                    val targetY = (topMeters + 0.10 - 0.30).coerceAtLeast(baseHeadY)
                    if (isProjection) (targetY - 2.0 - 0.15).coerceAtLeast(baseHeadY) else targetY
                }
            }
        }

        fun renderEffectsWindow(
            entity: DummyEntity,
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            packedLight: Int,
            projection: DummyWindowProjection? = null,
            fromBlockEntity: Boolean = false
        ) {
            val activeEffects = entity.activeEffects
            if (activeEffects.isEmpty()) return

            val window = FloatingWindow.Builder()
            if (projection == null) {
                val offsetY = calculateEffectsWindowBaseY(entity, isProjection = false, fromBlockEntity = fromBlockEntity)
                window.attachToEntityRotation(entity).setOffset(0.0, offsetY, 0.0)
            } else {
                val projectionOffsetY = calculateEffectsWindowBaseY(entity, isProjection = true, fromBlockEntity = fromBlockEntity)
                window.setFaceCamera(true)
                    .setOffset(projection.x, projection.y + projectionOffsetY, projection.z)
            }

            applyWindowShape(window, entity)
            window.setAlignment(WindowAlignment.BOTTOM_CENTER)
                .setScale(0.013f)
                .setPadding(3f)
                .setContentFit(FloatingWindow.ContentFit.EXPAND_TO_CONTENT)
                .setBorder(
                    WindowColor.AnimatedGradient(
                        argbStart = 0xFFC5A5FB,
                        argbEnd = 0xFF5700AC,
                        speed = 1.2f,
                        scale = 0.8f
                    ),
                    width = 0.75f
                )
                .setBackground(
                    WindowColor.Gradient(
                        argbStart = 0xF0181C2A,
                        argbEnd = 0xF02B233D
                    )
                )
                .addText(
                    Component.translatable("message.trainingdummy.effects_title"),
                    color = 0xFF9BE7FF.toInt(),
                    shadow = true,
                    alignment = WindowElement.Text.TextAlignment.CENTER,
                    textScale = 0.9f
                )
                .addSeparator(color = modeWindowSeparator(), thickness = 0.5f, margin = 2f)

            if (!entity.detailedDamageWindow) {
                // Modo Compacto: exibe APENAS os ícones em fileiras horizontais de até 5
                val chunks = activeEffects.chunked(5)
                for (chunk in chunks) {
                    val icons = chunk.map { MobEffectIconElement(it.effect, 14f) }
                    window.addRow(*icons.toTypedArray(), spacing = 3f, alignment = WindowElement.Text.TextAlignment.CENTER)
                }
            } else {
                // Modo Detalhado: Ícone (12px) + Nome + Nível (se amplificado) + Duração (∞ se infinito)
                for (instance in activeEffects) {
                    val effect = instance.effect
                    val nameComponent = Component.translatable(effect.value().descriptionId)
                    val textComponent = Component.empty().append(nameComponent)
                    if (instance.amplifier > 0) {
                        textComponent.append(CommonComponents.SPACE)
                            .append(Component.translatable("enchantment.level.${instance.amplifier + 1}"))
                    }
                    val durationStr = if (instance.isInfiniteDuration) "∞" else StringUtil.formatTickDuration(instance.duration, 20f)
                    textComponent.append(Component.literal(" ($durationStr)"))

                    val effectColor = (effect.value().color or 0xFF000000.toInt())
                    val textElement = WindowElement.Text(
                        text = textComponent,
                        color = WindowColor.Solid(effectColor.toLong() and 0xFFFFFFFFL),
                        shadow = true,
                        textScale = 0.95f,
                        alignment = WindowElement.Text.TextAlignment.LEFT
                    )
                    window.addRow(
                        MobEffectIconElement(effect, 12f),
                        textElement,
                        spacing = 3f,
                        alignment = WindowElement.Text.TextAlignment.LEFT
                    )
                }
            }

            window.build().render(poseStack, buffer, packedLight)
        }


        fun windowDefault3(window: FloatingWindow.Builder, entity: DummyEntity) {
            window
                .setOffset(0.0, 2.8, 0.0)
                .setScale(0.03f)
                .attachToEntityRotation(entity)
                .setAlignment(WindowAlignment.CENTER)
                .setBackground(WindowColor.Gradient(0xFFFFFFFF,0x3C3C3CAA))
                .setVolume3D(Volume3DConfig.DEFAULT) // Ativa o relevo 3D


                // Primeira linha: título animado
                .addText(
                    text = "★ MESTRE DAS ARMAS ★",
                    color = WindowColor.Rainbow(speed = 0.8f),
                    shadow = true,
                    textScale = 1.3f
                )
                // Segunda linha: subtítulo com componente translatável
                .addText(
                    component = Component.literal("Nível 100"),
                    color = WindowColor.Solid(0xFFFBBF24),
                    shadow = true,
                    textScale = 0.9f
                )
                .build()
                /*
                .textOnly() // Remove fundo, bordas, padding e sombras!
                .setFaceCamera(true)
                .setOffset(0.0, 2.0, 0.0)
                .setScale(0.025f)
                .setAlignment(WindowAlignment.CENTER)

                // Texto 3D extrudado
                .addText(
                    text = "RECOMPENSA",
                    color = WindowColor.Solid(0xFF38BDF8),
                    renderMode = TextRenderMode.EXTRUDED_3D,
                    extrusionDepth = 0.7f,
                    textScale = 1.2f,
                    shadow = true
                )
                // Linha com ícone de item e texto
                .addItemText(
                    stack = Items.DIAMOND.defaultInstance,
                    text = "x64 Diamantes",
                    color = 0xFF67E8F9.toInt(),
                    itemSize = 12f,
                    textScale = 1.0f
                )
                .build()
            */

        }

        fun renderFloatingWindowModo(
            entity: DummyEntity,
            partialTicks: Float,
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            packedLight: Int,
            projection: DummyWindowProjection? = null,
            fromBlockEntity: Boolean = false
        ) {

            if (entity.effectsWindow && entity.activeEffects.isNotEmpty()) {
                renderEffectsWindow(entity, poseStack, buffer, packedLight, projection, fromBlockEntity)
            }

            val modoAtual = entity.trainingModuleConfig.trainingModo
            if (!fromBlockEntity && modoAtual != TrainingModos.DPS && entity.displayTicks <= 0) return

            val damageData = entity.damageData
            val damageDataOld = entity.damageDataOld


            val window = FloatingWindow.Builder()

            when (modoAtual) {

                TrainingModos.DEFAULT -> {
                   // windowDefault3(window,entity)

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
                            .setOffset(
                                projection.x + 0.9 * projection.rightX,
                                projection.y,
                                projection.z + 0.9 * projection.rightZ
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
                                    argbStart = 0xFF65E8A0,
                                    argbEnd = 0xFF21B6A8,
                                    speed = 0.8f,
                                    scale = 0.8f
                                )

                                DummyEntity.DPS_STATE_COOLDOWN -> WindowColor.AnimatedGradient(
                                    argbStart = 0xFFFFB454,
                                    argbEnd = 0xFFFF5A5A,
                                    speed = 0.8f,
                                    scale = 0.8f
                                )

                                else -> WindowColor.AnimatedGradient(
                                    argbStart = 0xFFC5A5FB,
                                    argbEnd = 0xFF5700AC,
                                    speed = 1.2f,
                                    scale = 0.8f
                                )
                            },
                            width = 0.75f
                        )
                        .setBackground(
                            WindowColor.Gradient(
                                argbStart = 0xF0181C2A,
                                argbEnd = 0xF02B233D
                            )
                        )

                    val formatDamage: (Float) -> String = { value ->
                        String.format(Locale.ROOT, "%.1f", value)
                    }

                    when (entity.dpsTestState) {
                        DummyEntity.DPS_STATE_MEASURING -> {
                            val remainingSeconds =
                                (entity.dpsTicksRemaining - partialTicks.coerceIn(0f, 1f)).coerceAtLeast(0f) / 20f
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
                        offsetX = 0.8,
                        projection = projection,
                        projectionOffsetX = 0.8
                    )
                    window.addSeparator(color = modeWindowSeparator(), thickness = 0.5f, margin = 2f)
                        .addText(
                            Component.translatable(
                                "message.trainingdummy.accumulated_value",
                                String.format(Locale.ROOT, "%.1f", damageData.newDamage)
                            ),
                            color = WindowColor.Pulse(0xFF9BE7FF),
                            shadow = true,
                            textScale = 2.5f,
                            alignment = WindowElement.Text.TextAlignment.CENTER
                        )
                }

                TrainingModos.COMPARISON -> {
                    val compOffsetX = if (fromBlockEntity) 0.45 else 0.8
                    buildComparisonPanel(
                        window,
                        entity,
                        Component.translatable("message.trainingdummy.previous"),
                        entity.damageStackOld,
                        damageDataOld.newDamage,
                        offsetX = 0.8,
                        projection = projection,
                        projectionOffsetX = compOffsetX
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
                        projectionOffsetX = -compOffsetX
                    )
                    window2.build().render(poseStack, buffer, packedLight)

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
                        projectionOffsetY = 0.65
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
                    differenceWindow.build().render(poseStack, buffer, packedLight)
                }

                //null -> {return}
            }

            window.build().render(poseStack, buffer, packedLight)

        }

        private val armorHudIcon = ResourceLocation.fromNamespaceAndPath(
            "minecraft",
            "textures/gui/sprites/hud/armor_full.png"
        )

        private val absorptionHudIcon = ResourceLocation.fromNamespaceAndPath(
            "minecraft",
            "textures/gui/sprites/hud/heart/absorbing_full.png"
        )

        private val enchantItem = Items.ENCHANTED_BOOK.defaultInstance
        private val potionItem = Items.POTION.defaultInstance.also {
            it.set(
                DataComponents.POTION_CONTENTS,
                PotionContents(Potions.HEALING)
            )
        }
        private val innateResistanceItem = Items.STRUCTURE_VOID.defaultInstance
        private val invulnerabilityItem = Items.BARRIER.defaultInstance


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
                    .setOffset(
                        projection.x + 0.9 * projection.rightX,
                        projection.y,
                        projection.z + 0.9 * projection.rightZ
                    )
            }

            applyWindowShape(window, entity)


            window.setSize(30f, 30f, autoScaleContent = true, autoFitWidth = true, autoFitHeight = true)
                .setAlignment(WindowAlignment.CENTER)
                .setPadding(3f)



                .setBorder(
                    WindowColor.AnimatedGradient(
                        argbStart = 0xC5A5FB,
                        argbEnd = 0x5700ac,
                        speed = 1.2f,
                        scale = 0.8f
                    ),
                    width = 0.75f
                )
                .setBackground(
                    WindowColor.Gradient(
                        argbStart = 0xF0181C2A,
                        argbEnd = 0xF02B233D
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
                            argbStart = 0xFF67E8F9,
                            argbEnd = 0xFF8B5CF6,
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
                        argbStart = 0xFF0000,
                        argbEnd = 0xff8c00,
                        speed = 5.0f,
                        scale = 0.8f
                    )
                } else {
                    WindowColor.Pulse(0xFF9BE7FF)
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
                    color = WindowColor.Pulse(color = 0xff1a1a, speed = 1.5f)
                )

            }

            if (damageData.isReductions()) {

                window.addSeparator(
                    color = WindowColor.AnimatedGradient(
                        argbStart = 0xFF67E8F9,
                        argbEnd = 0xFF8B5CF6,
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
                        argbStart = 0xFF67E8F9,
                        argbEnd = 0xFF8B5CF6,
                        speed = 1.2f,
                        scale = 0.8f
                    ),
                    thickness = 0.5f,
                    margin = 2f
                )

            }

        }

        /** Aplica à janela o formato geométrico escolhido no menu do dummy. */
        private fun applyWindowShape(window: FloatingWindow.Builder, entity: DummyEntity) {
            when (val type = entity.windowShape) {
                WindowShapeType.ROUNDED -> {
                    window.setCornerRadius(3f)
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
            projectionOffsetX: Double = offsetX,
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
            window.setSize(width, height, autoScaleContent = true, autoFitWidth = true, autoFitHeight = true)
                .setAlignment(WindowAlignment.CENTER)
                .setPadding(3f)
                .setBorder(
                    WindowColor.AnimatedGradient(
                        argbStart = 0xFFC5A5FB,
                        argbEnd = 0xFF5700AC,
                        speed = 1.2f,
                        scale = 0.8f
                    ),
                    width = 0.75f
                )
                .setBackground(
                    WindowColor.Gradient(
                        argbStart = 0xF0181C2A,
                        argbEnd = 0xF02B233D
                    )
                )
                .addText(
                    title,
                    color = 0xFF9BE7FF.toInt(),
                    shadow = true,
                    alignment = WindowElement.Text.TextAlignment.CENTER
                )
        }

        private fun buildComparisonPanel(
            window: FloatingWindow.Builder,
            entity: DummyEntity,
            title: Component,
            stack: ItemStack,
            damage: Float,
            offsetX: Double,
            projection: DummyWindowProjection? = null,
            projectionOffsetX: Double = 0.0
        ) {
            configureModeWindow(
                window = window,
                entity = entity,
                title = title,
                offsetX = offsetX,
                width = 24f,
                height = 25f,
                projection = projection,
                projectionOffsetX = projectionOffsetX
            )
            window.addSeparator(color = modeWindowSeparator(), thickness = 0.5f, margin = 2f)
            if (stack.isEmpty) {
                window.addText(
                    Component.translatable("message.trainingdummy.comparison.no_weapon"),
                    color = 0xFFB9C9DC.toInt(),
                    textScale = 0.8f,
                    alignment = WindowElement.Text.TextAlignment.CENTER
                )
            } else {
                window.addItemText(
                    stack,
                    stack.hoverName,
                    itemSize = 18f,
                    textScale = 0.8f,
                    alignment = WindowElement.Text.TextAlignment.CENTER
                )
            }
            window.addSeparator(color = modeWindowSeparator(), thickness = 0.5f, margin = 2f)
                .addText(
                    Component.literal(String.format(Locale.ROOT, "%.1f", damage)),
                    color = WindowColor.Pulse(0xFF9BE7FF),
                    shadow = true,
                    textScale = 2f,
                    alignment = WindowElement.Text.TextAlignment.CENTER
                )
        }

        private fun modeWindowSeparator(): WindowColor.AnimatedGradient = WindowColor.AnimatedGradient(
            argbStart = 0xFF67E8F9,
            argbEnd = 0xFF8B5CF6,
            speed = 1.2f,
            scale = 0.8f
        )

        private fun windowDefault(
            window: FloatingWindow.Builder,
            damageData: DamageData,
            entity: DummyEntity,
            title: Component = Component.translatable("message.trainingdummy.damage_title")
        ) {

            window.attachToEntityRotation(entity)
                .setOffset(0.6, 2.0, 0.0)
                .setSize(24f, 24f, autoScaleContent = true, autoFitWidth = true, autoFitHeight = true)
                .setAlignment(WindowAlignment.CENTER)
                .setPadding(1f)
                .setCornerRadius(3f)
                .setBorder(
                    WindowColor.AnimatedGradient(
                        argbStart = 0xFF67E8F9,
                        argbEnd = 0xFF8B5CF6,
                        speed = 1.2f,
                        scale = 0.8f
                    ),
                    width = 0.75f
                )
                .setBackground(
                    WindowColor.Gradient(
                        argbStart = 0xF0181C2A,
                        argbEnd = 0xF02B233D
                    )
                )
                .addText(title, color = 0xFF9BE7FF.toInt(), shadow = true)
                .addSeparator(
                    color = WindowColor.AnimatedGradient(
                        argbStart = 0xFF67E8F9,
                        argbEnd = 0xFF8B5CF6,
                        speed = 1.2f,
                        scale = 0.8f
                    ),
                    thickness = 0.5f,
                    margin = 2f
                )

            var finalDamage: Component

            if (damageData.isReductions()) {
                window.addText(
                    Component.translatable(
                        "message.trainingdummy.raw_damage",
                        String.format(Locale.ROOT, "%.1f", damageData.originalDamage)
                    ).withStyle(ChatFormatting.RED)
                )
                    .addSeparator(color = WindowColor.Rainbow(speed = 1.0f), thickness = 0.5f, margin = 2f)
                    .addText(Component.translatable("message.trainingdummy.reductions"), color = 0x0087F0.toInt())
            } else {
                window.setPadding(3f);
            }

            if (damageData.armor > 0.0f) {
                window.addText(reductionText("message.trainingdummy.reduction.armor", damageData.armor))
            }
            if (damageData.enchantments > 0.0f) {
                window.addText(reductionText("message.trainingdummy.reduction.enchantments", damageData.enchantments))
            }
            if (damageData.mobEffects > 0.0f) {
                window.addText(reductionText("message.trainingdummy.reduction.effects", damageData.mobEffects))
            }
            if (damageData.absorption > 0.0f) {
                window.addText(reductionText("message.trainingdummy.reduction.absorption", damageData.absorption))
            }
            if (damageData.innateResistance > 0.0f) {
                window.addText(
                    reductionText(
                        "message.trainingdummy.reduction.innate_resistance",
                        damageData.innateResistance
                    )
                )
            }
            if (damageData.invulnerability > 0.0f) {
                window.addText(
                    reductionText(
                        "message.trainingdummy.reduction.invulnerability",
                        damageData.invulnerability
                    )
                )
            }

            if (damageData.isReductions()) {
                window.addSeparator(color = WindowColor.Rainbow(speed = 1.0f), thickness = 0.5f, margin = 2f)
                finalDamage = Component.translatable(
                    "message.trainingdummy.final_damage_value",
                    String.format(Locale.ROOT, "%.1f", damageData.newDamage)
                )
            } else {
                finalDamage = Component.literal("%.2f".format(Locale.ROOT, damageData.newDamage))
            }

            if (damageData.isCriticalHit) {
                window.addText(
                    finalDamage.append(" ").append(
                        Component.translatable(
                            "message.trainingdummy.critical_multiplier",
                            String.format(Locale.ROOT, "%.1f", damageData.damageMultiplier)
                        )
                    ),
                    color = WindowColor.AnimatedGradient(
                        argbStart = 0xFF0000, // Cor inicial (Branco)
                        argbEnd = 0xFFFFFF,   // Cor final (Vermelho)
                        speed = 5.0f,          // Velocidade do deslocamento
                        scale = 1.0f           // Densidade/Frequência das ondas no texto
                    )
                )
            } else {
                window.addText(finalDamage)
            }
        }

        private fun modeReductionText(detailed: Boolean, nameKey: String, value: Float): Component {
            val key = if (detailed) nameKey else "message.trainingdummy.reduction.icon_amount"
            val formattedValue = Component.literal(String.format(Locale.ROOT, "%.1f", value))
                .withStyle(ChatFormatting.WHITE)
            return Component.translatable(key, formattedValue)
                .withStyle(ChatFormatting.GREEN)
        }

        private fun reductionText(key: String, value: Float): Component =
            Component.translatable(key, String.format(Locale.ROOT, "%.1f", value))
                .withStyle(ChatFormatting.GREEN)
    }
}
