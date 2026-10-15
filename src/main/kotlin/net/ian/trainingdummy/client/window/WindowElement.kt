package net.ian.trainingdummy.client.window

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f

sealed interface WindowElement {
    fun getWidth(): Float
    fun getHeight(): Float

    fun render(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        x: Float,
        y: Float,
        availableWidth: Float,
        packedLight: Int
    )

    class Text(
        private val text: Component,
        private val color: WindowColor = WindowColor.Solid(0xFFFFFFFFL),
        private val shadow: Boolean = false,
        private val textScale: Float = 1f,
        private val alignment: TextAlignment = TextAlignment.LEFT
    ) : WindowElement {
        private val font: Font
            get() = Minecraft.getInstance().font

        private val safeScale = textScale.coerceAtLeast(0.1f)

        override fun getWidth(): Float = font.width(text) * safeScale
        override fun getHeight(): Float = font.lineHeight * safeScale

        override fun render(
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            x: Float,
            y: Float,
            availableWidth: Float,
            packedLight: Int
        ) {
            val textWidth = getWidth()
            val drawX = x + when (alignment) {
                TextAlignment.LEFT -> 0f
                TextAlignment.CENTER -> (availableWidth - textWidth) / 2f
                TextAlignment.RIGHT -> availableWidth - textWidth
            }.coerceAtLeast(0f)

            poseStack.pushPose()
            poseStack.translate(drawX.toDouble(), y.toDouble(), 0.0)
            poseStack.scale(safeScale, safeScale, 1f)
            val matrix = poseStack.last().pose()

            if (color is WindowColor.Solid) {
                font.drawInBatch(
                    text,
                    0f,
                    0f,
                    color.resolve().toArgb(),
                    shadow,
                    matrix,
                    buffer,
                    Font.DisplayMode.NORMAL,
                    0,
                    packedLight
                )
            } else {
                drawAnimatedText(matrix, buffer, packedLight)
            }
            poseStack.popPose()
        }

        private fun drawAnimatedText(matrix: Matrix4f, buffer: MultiBufferSource, packedLight: Int) {
            var currentX = 0f
            val rawText = text.string
            val fullWidth = font.width(text).toFloat()
            for (character in rawText) {
                val glyph = character.toString()
                val glyphWidth = font.width(glyph).toFloat()
                val u = if (fullWidth > 0f) currentX / fullWidth else 0f
                font.drawInBatch(
                    glyph,
                    currentX,
                    0f,
                    color.resolve(u, 0f).toArgb(),
                    shadow,
                    matrix,
                    buffer,
                    Font.DisplayMode.NORMAL,
                    0,
                    packedLight
                )
                currentX += glyphWidth
            }
        }

        enum class TextAlignment { LEFT, CENTER, RIGHT }
    }

    class ItemIcon(
        private val stack: ItemStack,
        private val size: Float = 16f
    ) : WindowElement {
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
            if (stack.isEmpty) return

            val minecraft = Minecraft.getInstance()
            poseStack.pushPose()
            poseStack.translate((x + iconSize / 2f).toDouble(), (y + iconSize / 2f).toDouble(), 0.2)
            poseStack.scale(iconSize, -iconSize, 0.01f)
            minecraft.itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.GUI,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                minecraft.level,
                0
            )
            poseStack.popPose()
        }
    }

    class ImageIcon(
        private val texture: ResourceLocation,
        private val size: Float = 9f,
        private val tint: WindowColor = WindowColor.Solid(0xFFFFFFFFL)
    ) : WindowElement {
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
            val builder = buffer.getBuffer(RenderType.entityCutoutNoCull(texture))
            val matrix = poseStack.last().pose()
            val color = tint.resolve()
            val left = x
            val right = x + iconSize
            val top = y
            val bottom = y + iconSize
            val z = 0.2f

            addTexturedVertex(builder, matrix, left, top, z, 0f, 0f, color, packedLight)
            addTexturedVertex(builder, matrix, left, bottom, z, 0f, 1f, color, packedLight)
            addTexturedVertex(builder, matrix, right, bottom, z, 1f, 1f, color, packedLight)
            addTexturedVertex(builder, matrix, right, top, z, 1f, 0f, color, packedLight)
        }

        private fun addTexturedVertex(
            builder: VertexConsumer,
            matrix: Matrix4f,
            x: Float,
            y: Float,
            z: Float,
            u: Float,
            v: Float,
            color: WindowColor.RGBA,
            packedLight: Int
        ) {
            builder.addVertex(matrix, x, y, z)
                .setColor(color.r, color.g, color.b, color.a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0f, 0f, 1f)
        }
    }

    class Spacer(private val height: Float) : WindowElement {
        private val safeHeight = height.coerceAtLeast(0f)

        override fun getWidth(): Float = 0f
        override fun getHeight(): Float = safeHeight

        override fun render(
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            x: Float,
            y: Float,
            availableWidth: Float,
            packedLight: Int
        ) = Unit
    }

    class Separator(
        private val color: WindowColor,
        private val thickness: Float,
        private val margin: Float
    ) : WindowElement {
        private val safeThickness = thickness.coerceAtLeast(0.1f)
        private val safeMargin = margin.coerceAtLeast(0f)

        override fun getWidth(): Float = 0f
        override fun getHeight(): Float = safeThickness + safeMargin * 2f

        override fun render(
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            x: Float,
            y: Float,
            availableWidth: Float,
            packedLight: Int
        ) {
            if (availableWidth <= 0f) return

            val top = y + safeMargin
            val bottom = top + safeThickness
            val builder = buffer.getBuffer(RenderType.debugQuads())
            val matrix = poseStack.last().pose()
            val topLeft = color.resolve(0f, 0f)
            val bottomLeft = color.resolve(0f, 1f)
            val bottomRight = color.resolve(1f, 1f)
            val topRight = color.resolve(1f, 0f)
            builder.addVertex(matrix, x, top, 0.1f).setColor(topLeft.r, topLeft.g, topLeft.b, topLeft.a)
            builder.addVertex(matrix, x, bottom, 0.1f).setColor(bottomLeft.r, bottomLeft.g, bottomLeft.b, bottomLeft.a)
            builder.addVertex(matrix, x + availableWidth, bottom, 0.1f)
                .setColor(bottomRight.r, bottomRight.g, bottomRight.b, bottomRight.a)
            builder.addVertex(matrix, x + availableWidth, top, 0.1f)
                .setColor(topRight.r, topRight.g, topRight.b, topRight.a)
        }
    }

    class Row(
        private val elements: List<WindowElement>,
        private val spacing: Float = 2f,
        private val alignment: Text.TextAlignment = Text.TextAlignment.LEFT
    ) : WindowElement {
        private val safeSpacing = spacing.coerceAtLeast(0f)

        override fun getWidth(): Float =
            elements.sumOf { it.getWidth().toDouble() }.toFloat() + safeSpacing * (elements.size - 1).coerceAtLeast(0)

        override fun getHeight(): Float = elements.maxOfOrNull { it.getHeight() } ?: 0f

        override fun render(
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            x: Float,
            y: Float,
            availableWidth: Float,
            packedLight: Int
        ) {
            val startX = x + when (alignment) {
                Text.TextAlignment.LEFT -> 0f
                Text.TextAlignment.CENTER -> (availableWidth - getWidth()) / 2f
                Text.TextAlignment.RIGHT -> availableWidth - getWidth()
            }.coerceAtLeast(0f)
            var currentX = startX
            for (element in elements) {
                val elementHeight = element.getHeight()
                element.render(
                    poseStack,
                    buffer,
                    currentX,
                    y + (getHeight() - elementHeight) / 2f,
                    element.getWidth(),
                    packedLight
                )
                currentX += element.getWidth() + safeSpacing
            }
        }
    }

    companion object {
        internal fun addVertex(
            builder: VertexConsumer,
            matrix: Matrix4f,
            x: Float,
            y: Float,
            z: Float,
            color: WindowColor.RGBA
        ) {
            builder.addVertex(matrix, x, y, z).setColor(color.r, color.g, color.b, color.a)
        }
    }
}
