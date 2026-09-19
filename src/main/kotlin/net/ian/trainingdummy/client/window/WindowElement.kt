package net.ian.trainingdummy.client.window

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
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
        private val shadow: Boolean = false
    ) : WindowElement {
        private val font = Minecraft.getInstance().font

        override fun getWidth(): Float = font.width(text).toFloat()
        override fun getHeight(): Float = font.lineHeight.toFloat()

        override fun render(
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            x: Float,
            y: Float,
            availableWidth: Float,
            packedLight: Int
        ) {
            val matrix = poseStack.last().pose()

            // Para cores sólidas ou fakes simples (que não dependem do progresso do texto)
            if (color is WindowColor.Solid || color is WindowColor.Fade) {
                val colorInt = color.resolve(0f, 0f).toArgb()
                font.drawInBatch(
                    text, x, y, colorInt, shadow, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight
                )
                return
            }

            // Para Gradientes (Gradient, AnimatedGradient, Rainbow), renderiza caractere por caractere
            val rawText = text.string
            val totalWidth = getWidth()
            var currentX = x

            for (i in rawText.indices) {
                val charStr = rawText[i].toString()
                val charWidth = font.width(charStr).toFloat()

                // Calcula o progresso do gradiente (U de 0.0 a 1.0) na posição do caractere
                val u = if (totalWidth > 0f) (currentX - x) / totalWidth else 0f
                val charColor = color.resolve(u = u, v = 0f).toArgb()

                font.drawInBatch(
                    charStr, currentX, y, charColor, shadow, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight
                )

                currentX += charWidth
            }
        }
    }

    /*
    class Text(
        private val text: Component,
        private val color: Int = 0xFFFFFFFF.toInt(),
        private val shadow: Boolean = false
    ) : WindowElement {
        private val font = Minecraft.getInstance().font

        override fun getWidth(): Float = font.width(text).toFloat()
        override fun getHeight(): Float = font.lineHeight.toFloat()

        override fun render(
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            x: Float,
            y: Float,
            availableWidth: Float,
            packedLight: Int
        ) {
            val matrix = poseStack.last().pose()
            font.drawInBatch(
                text, x, y, color, shadow, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight
            )
        }
    }

     */

    class Spacer(private val height: Float) : WindowElement {
        override fun getWidth(): Float = 0f
        override fun getHeight(): Float = height
        override fun render(
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            x: Float,
            y: Float,
            availableWidth: Float,
            packedLight: Int
        ) {}
    }

    class Separator(
        private val color: WindowColor,
        private val thickness: Float,
        private val margin: Float
    ) : WindowElement {
        override fun getWidth(): Float = 0f
        override fun getHeight(): Float = thickness + (margin * 2)

        override fun render(
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            x: Float,
            y: Float,
            availableWidth: Float,
            packedLight: Int
        ) {
            val matrix = poseStack.last().pose()
            val lineY = y + margin
            val builder: VertexConsumer = buffer.getBuffer(RenderType.gui())

            addVertex(builder, matrix, x, lineY, 0f, color.resolve(0f, 0f))
            addVertex(builder, matrix, x, lineY + thickness, 0f, color.resolve(0f, 1f))
            addVertex(builder, matrix, x + availableWidth, lineY + thickness, 0f, color.resolve(1f, 1f))
            addVertex(builder, matrix, x + availableWidth, lineY, 0f, color.resolve(1f, 0f))
        }

        private fun addVertex(
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
