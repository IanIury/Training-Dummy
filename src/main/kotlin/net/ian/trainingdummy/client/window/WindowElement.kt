package net.ian.trainingdummy.client.window

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.network.chat.Component

sealed interface WindowElement {
    fun getWidth(): Float
    fun getHeight(): Float
    fun render(poseStack: PoseStack, buffer: MultiBufferSource, x: Float, y: Float, packedLight: Int)

    class Text(
        private val text: Component,
        private val color: Int = 0xFFFFFFFF.toInt(),
        private val shadow: Boolean = false
    ) : WindowElement {
        private val font = Minecraft.getInstance().font

        override fun getWidth(): Float = font.width(text).toFloat()
        override fun getHeight(): Float = font.lineHeight.toFloat()

        override fun render(poseStack: PoseStack, buffer: MultiBufferSource, x: Float, y: Float, packedLight: Int) {
            val matrix = poseStack.last().pose()
            font.drawInBatch(
                text, x, y, color, shadow, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight
            )
        }
    }

    class Spacer(private val height: Float) : WindowElement {
        override fun getWidth(): Float = 0f
        override fun getHeight(): Float = height
        override fun render(poseStack: PoseStack, buffer: MultiBufferSource, x: Float, y: Float, packedLight: Int) {}
    }
}