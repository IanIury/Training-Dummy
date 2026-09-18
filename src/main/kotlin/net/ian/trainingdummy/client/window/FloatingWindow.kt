package net.ian.trainingdummy.client.window

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import org.joml.Matrix4f

class FloatingWindow private constructor(
    val elements: List<WindowElement>,
    val padding: Float,
    val background: WindowColor,
    val border: WindowColor?,
    val borderWidth: Float,
    val scale: Float,
    val fixedWidth: Float?,
    val fixedHeight: Float?,
    val autoScaleContent: Boolean,
    val alignment: WindowAlignment,
    val faceCamera: Boolean,
    val entityYaw: Float?,
    val offsetWorldX: Double,
    val offsetWorldY: Double,
    val offsetWorldZ: Double
) {

    fun render(poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int) {
        poseStack.pushPose()

        // 1. Aplica a rotação da entidade PRIMEIRO para alinhar o sistema de coordenadas local
        if (faceCamera) {
            // Se estiver olhando para a câmera, aplica o translate globalmente primeiro
            poseStack.translate(offsetWorldX, offsetWorldY, offsetWorldZ)

            val camera = Minecraft.getInstance().gameRenderer.mainCamera
            poseStack.mulPose(camera.rotation())
        } else {
            // Rotaciona para alinhar com o olhar da entidade
            val yaw = (entityYaw ?: 0f) + 180.0f
            poseStack.rotateAround(Axis.YP.rotationDegrees(-yaw), 0.0f, 0.0f, 0.0f)

            // Agora o translate se move RELATIVO ao corpo do Dummy
            // X = Direita/Esquerda da entidade | Y = Altura | Z = Frente/Trás da entidade
            poseStack.translate(offsetWorldX, offsetWorldY, offsetWorldZ)
        }

        // 2. Inverte os eixos Y e Z para o padrão GUI do Minecraft
        poseStack.rotateAround(Axis.ZP.rotationDegrees(180.0f), 0.0f, 0.0f, 0.0f)
        if (faceCamera) {
            poseStack.rotateAround(Axis.YP.rotationDegrees(180.0f), 0.0f, 0.0f, 0.0f)
        }

        // 3. Escala
        poseStack.scale(scale, scale, scale)

        // 4. Cálculo do Conteúdo (Medição exata sem espaço sobrando)
        val rawContentWidth = elements.maxOfOrNull { it.getWidth() } ?: 0f
        val rawContentHeight = elements.sumOf { it.getHeight().toDouble() }.toFloat()

        // Determina tamanho da janela
        val totalWidth = fixedWidth ?: (rawContentWidth + (padding * 2))
        val totalHeight = fixedHeight ?: (rawContentHeight + (padding * 2))

        // Canto X e Y dinâmicos baseados no ALINHAMENTO/PIVOT
        val x = -totalWidth * alignment.xRatio
        val y = -totalHeight * alignment.yRatio
        val matrix = poseStack.last().pose()

        // --- RENDERIZAR FUNDO (Z = 0.0f) ---
        drawColoredQuad(matrix, buffer, x, y, x + totalWidth, y + totalHeight, background, 0.0f)

        // --- RENDERIZAR BORDA (Z = -0.05f) ---
        if (border != null && borderWidth > 0f) {
            val t = borderWidth
            val z = -0.05f
            drawColoredQuad(matrix, buffer, x, y, x + totalWidth, y + t, border, z) // Topo
            drawColoredQuad(matrix, buffer, x, y + totalHeight - t, x + totalWidth, y + totalHeight, border, z) // Baixo
            drawColoredQuad(matrix, buffer, x, y, x + t, y + totalHeight, border, z) // Esquerda
            drawColoredQuad(matrix, buffer, x + totalWidth - t, y, x + totalWidth, y + totalHeight, border, z) // Direita
        }

        // --- RENDERIZAR CONTEÚDO (Z = -0.1f) ---
        poseStack.pushPose()
        poseStack.translate(0.0, 0.0, -0.1)

        val availWidth = totalWidth - (padding * 2)
        val availHeight = totalHeight - (padding * 2)

        // Escala o conteúdo se a janela for de tamanho fixo (.setSize) e não couber
        val contentScaleFactor = if (autoScaleContent && fixedWidth != null && fixedHeight != null &&
            (rawContentWidth > availWidth || rawContentHeight > availHeight)) {
            val scaleX = availWidth / rawContentWidth
            val scaleY = availHeight / rawContentHeight
            minOf(scaleX, scaleY)
        } else {
            1.0f
        }

        poseStack.translate((x + padding).toDouble(), (y + padding).toDouble(), 0.0)

        if (contentScaleFactor != 1.0f) {
            poseStack.scale(contentScaleFactor, contentScaleFactor, 1.0f)
        }

        var currentY = 0f
        for (element in elements) {
            element.render(poseStack, buffer, 0f, currentY, packedLight)
            currentY += element.getHeight()
        }

        poseStack.popPose()
        poseStack.popPose()
    }

    private fun drawColoredQuad(
        matrix: Matrix4f, buffer: MultiBufferSource,
        minX: Float, minY: Float, maxX: Float, maxY: Float,
        color: WindowColor, zOffset: Float
    ) {
        val builder: VertexConsumer = buffer.getBuffer(RenderType.gui())

        val cTL = color.resolve(0f, 0f)
        val cBL = color.resolve(0f, 1f)
        val cBR = color.resolve(1f, 1f)
        val cTR = color.resolve(1f, 0f)

        builder.addVertex(matrix, minX, minY, zOffset).setColor(cTL.r, cTL.g, cTL.b, cTL.a)
        builder.addVertex(matrix, minX, maxY, zOffset).setColor(cBL.r, cBL.g, cBL.b, cBL.a)
        builder.addVertex(matrix, maxX, maxY, zOffset).setColor(cBR.r, cBR.g, cBR.b, cBR.a)
        builder.addVertex(matrix, maxX, minY, zOffset).setColor(cTR.r, cTR.g, cTR.b, cTR.a)
    }

    // Builder Fluído da API
    class Builder {
        private val elements = mutableListOf<WindowElement>()
        private var padding = 4f
        private var background: WindowColor = WindowColor.Solid(0xDD000000)
        private var border: WindowColor? = null
        private var borderWidth = 0.5f
        private var scale = 0.025f
        private var fixedWidth: Float? = null
        private var fixedHeight: Float? = null
        private var autoScaleContent = false
        private var alignment = WindowAlignment.TOP_LEFT
        private var faceCamera = true
        private var entityYaw: Float? = null
        private var offsetX = 0.0
        private var offsetY = 1.3
        private var offsetZ = 0.0

        fun addText(text: String, color: Int = 0xFFFFFFFF.toInt(), shadow: Boolean = false) = apply {
            elements.add(WindowElement.Text(Component.literal(text), color, shadow))
        }

        fun addText(component: Component, color: Int = 0xFFFFFFFF.toInt(), shadow: Boolean = false) = apply {
            elements.add(WindowElement.Text(component, color, shadow))
        }

        fun addSpacer(height: Float) = apply {
            elements.add(WindowElement.Spacer(height))
        }

        fun setSize(width: Float, height: Float, autoScaleContent: Boolean = true) = apply {
            this.fixedWidth = width
            this.fixedHeight = height
            this.autoScaleContent = autoScaleContent
        }

        /**
         * Define o alinhamento da janela (onde ela fica presa e para onde cresce).
         * Ex: WindowAlignment.TOP_LEFT fará a janela crescer apenas para a DIREITA e para BAIXO.
         */
        fun setAlignment(alignment: WindowAlignment) = apply {
            this.alignment = alignment
        }

        /**
         * Se true: vira sempre para a câmera do jogador (Billboard).
         * Se false: fica virada para a direção da Entidade.
         */
        fun setFaceCamera(faceCamera: Boolean) = apply {
            this.faceCamera = faceCamera
        }

        /**
         * Passa a entidade para alinhar com a rotação dela caso faceCamera seja false.
         */
        fun attachToEntityRotation(entity: Entity) = apply {
            this.entityYaw = entity.yRot
            this.faceCamera = false
        }

        fun setPadding(padding: Float) = apply { this.padding = padding }
        fun setBackground(color: WindowColor) = apply { this.background = color }
        fun setBorder(color: WindowColor, width: Float = 0.5f) = apply {
            this.border = color
            this.borderWidth = width
        }
        fun setScale(scale: Float) = apply { this.scale = scale }
        fun setOffset(x: Double, y: Double, z: Double) = apply {
            this.offsetX = x
            this.offsetY = y
            this.offsetZ = z
        }

        fun build(): FloatingWindow {
            return FloatingWindow(
                elements, padding, background, border, borderWidth, scale,
                fixedWidth, fixedHeight, autoScaleContent, alignment,
                faceCamera, entityYaw, offsetX, offsetY, offsetZ
            )
        }
    }
}