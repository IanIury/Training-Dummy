package net.ian.trainingdummy.client.window

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f

class FloatingWindow private constructor(
    elements: List<WindowElement>,
    val padding: Float,
    val background: WindowColor,
    val border: WindowColor?,
    val borderWidth: Float,
    val cornerRadius: Float,
    val scale: Float,
    val fixedWidth: Float?,
    val fixedHeight: Float?,
    val autoScaleContent: Boolean,
    val autoFitWidth: Boolean,
    val autoFitHeight: Boolean,
    val alignment: WindowAlignment,
    val faceCamera: Boolean,
    val entityYaw: Float?,
    val offsetWorldX: Double,
    val offsetWorldY: Double,
    val offsetWorldZ: Double,
    private val shadowColor: WindowColor?,
    private val shadowOffset: Float
) {
    enum class ContentFit {
        EXPAND_TO_CONTENT,
        SHRINK_CONTENT,
        KEEP_CONTENT_SIZE
    }

    val elements: List<WindowElement> = elements.toList()

    fun render(poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int) {
        if (elements.isEmpty()) return

        val rawContentWidth = elements.maxOf { it.getWidth() }
        val rawContentHeight = elements.sumOf { it.getHeight().toDouble() }.toFloat()
        val maxWidth = fixedWidth ?: (rawContentWidth + padding * 2f)
        val maxHeight = fixedHeight ?: (rawContentHeight + padding * 2f)
        val availableContentWidth = (maxWidth - padding * 2f).coerceAtLeast(0.1f)
        val availableContentHeight = (maxHeight - padding * 2f).coerceAtLeast(0.1f)
        val fitScale = if (autoScaleContent) {
            minOf(
                1f,
                availableContentWidth / rawContentWidth.coerceAtLeast(0.1f),
                availableContentHeight / rawContentHeight.coerceAtLeast(0.1f)
            )
        } else {
            1f
        }
        val contentWidth = rawContentWidth * fitScale + padding * 2f
        val contentHeight = rawContentHeight * fitScale + padding * 2f
        val width = when {
            fixedWidth == null -> contentWidth
            autoFitWidth -> minOf(fixedWidth, contentWidth)
            else -> fixedWidth
        }.coerceAtLeast(0.1f)
        val height = when {
            fixedHeight == null -> contentHeight
            autoFitHeight -> minOf(fixedHeight, contentHeight)
            else -> fixedHeight
        }.coerceAtLeast(0.1f)
        val left = -width * alignment.xRatio
        val top = -height * alignment.yRatio

        poseStack.pushPose()
        if (faceCamera) {
            poseStack.translate(offsetWorldX, offsetWorldY, offsetWorldZ)
            poseStack.mulPose(Minecraft.getInstance().gameRenderer.mainCamera.rotation())
        } else {
            val yaw = entityYaw ?: 0f
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw))
            poseStack.translate(offsetWorldX, offsetWorldY, offsetWorldZ)
        }
        poseStack.scale(scale, -scale, scale)

        val matrix = poseStack.last().pose()
        val radius = cornerRadius.coerceIn(0f, minOf(width, height) / 2f)
        shadowColor?.let { shadow ->
            drawRoundedFill(
                matrix,
                buffer,
                left + shadowOffset,
                top + shadowOffset,
                left + width + shadowOffset,
                top + height + shadowOffset,
                radius,
                shadow,
                -1f
            )
        }

        val inset = if (border != null) borderWidth.coerceIn(0f, minOf(width, height) / 2f) else 0f
        if (inset > 0f) {
            drawRoundedFill(
                matrix,
                buffer,
                left,
                top,
                left + width,
                top + height,
                radius,
                border!!,
                -0.1f
            )
        }
        drawRoundedFill(
            matrix,
            buffer,
            left + inset,
            top + inset,
            left + width - inset,
            top + height - inset,
            (radius - inset).coerceAtLeast(0f),
            background,
            0f
        )
        poseStack.pushPose()
        poseStack.translate(
            (left + padding).toDouble(),
            (top + padding).toDouble(),
            0.1
        )
        poseStack.scale(fitScale, fitScale, 1f)

        var currentY = 0f
        val elementWidth = (width - padding * 2f) / fitScale
        for (element in elements) {
            element.render(poseStack, buffer, 0f, currentY, elementWidth, packedLight)
            currentY += element.getHeight()
        }
        poseStack.popPose()
        poseStack.popPose()
    }

    private fun drawRoundedFill(
        matrix: Matrix4f,
        buffer: MultiBufferSource,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        requestedRadius: Float,
        color: WindowColor,
        z: Float
    ) {
        if (right <= left || bottom <= top) return
        val radius = requestedRadius.coerceIn(0f, minOf((right - left) / 2f, (bottom - top) / 2f))
        val builder = buffer.getBuffer(RenderType.debugQuads())
        val bandCount = maxOf(1, kotlin.math.ceil((bottom - top) * 2f).toInt())
        for (band in 0 until bandCount) {
            val y0 = top + (bottom - top) * band / bandCount
            val y1 = top + (bottom - top) * (band + 1) / bandCount
            val inset0 = roundedInset(y0, top, bottom, radius)
            val inset1 = roundedInset(y1, top, bottom, radius)
            val left0 = left + inset0
            val left1 = left + inset1
            val right0 = right - inset0
            val right1 = right - inset1
            val c00 = color.resolve((left0 - left) / (right - left), (y0 - top) / (bottom - top))
            val c01 = color.resolve((left1 - left) / (right - left), (y1 - top) / (bottom - top))
            val c11 = color.resolve((right1 - left) / (right - left), (y1 - top) / (bottom - top))
            val c10 = color.resolve((right0 - left) / (right - left), (y0 - top) / (bottom - top))
            addVertex(builder, matrix, left0, y0, z, c00)
            addVertex(builder, matrix, left1, y1, z, c01)
            addVertex(builder, matrix, right1, y1, z, c11)
            addVertex(builder, matrix, right0, y0, z, c10)
        }
    }

    private fun roundedInset(y: Float, top: Float, bottom: Float, radius: Float): Float {
        if (radius <= 0f) return 0f
        val cornerDistance = minOf(y - top, bottom - y)
        if (cornerDistance >= radius) return 0f
        val normalized = (radius - cornerDistance).coerceIn(0f, radius)
        return radius - kotlin.math.sqrt((radius * radius - normalized * normalized).coerceAtLeast(0f))
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

    class Builder {
        private val elements = mutableListOf<WindowElement>()
        private var padding = 4f
        private var background: WindowColor = WindowColor.Solid(0xDD000000)
        private var border: WindowColor? = null
        private var borderWidth = 0.75f
        private var cornerRadius = 4f
        private var shadowColor: WindowColor? = null
        private var shadowOffset = 1f
        private var scale = 0.025f
        private var fixedWidth: Float? = null
        private var fixedHeight: Float? = null
        private var autoScaleContent = false
        private var autoFitWidth = false
        private var autoFitHeight = false
        private var alignment = WindowAlignment.TOP_LEFT
        private var faceCamera = true
        private var entityYaw: Float? = null
        private var offsetX = 0.0
        private var offsetY = 1.3
        private var offsetZ = 0.0

        fun addText(
            text: String,
            color: WindowColor,
            shadow: Boolean = false,
            textScale: Float = 1f,
            alignment: WindowElement.Text.TextAlignment = WindowElement.Text.TextAlignment.LEFT
        ) = apply {
            elements += WindowElement.Text(Component.literal(text), color, shadow, textScale, alignment)
        }

        fun addText(
            component: Component,
            color: WindowColor,
            shadow: Boolean = false,
            textScale: Float = 1f,
            alignment: WindowElement.Text.TextAlignment = WindowElement.Text.TextAlignment.LEFT
        ) = apply {
            elements += WindowElement.Text(component, color, shadow, textScale, alignment)
        }

        fun addText(
            text: String,
            color: Int = 0xFFFFFFFF.toInt(),
            shadow: Boolean = false,
            textScale: Float = 1f,
            alignment: WindowElement.Text.TextAlignment = WindowElement.Text.TextAlignment.LEFT
        ) = apply {
            elements += WindowElement.Text(
                Component.literal(text),
                WindowColor.Solid(color.toLong() and 0xFFFFFFFFL),
                shadow,
                textScale,
                alignment
            )
        }

        fun addText(
            component: Component,
            color: Int = 0xFFFFFFFF.toInt(),
            shadow: Boolean = false,
            textScale: Float = 1f,
            alignment: WindowElement.Text.TextAlignment = WindowElement.Text.TextAlignment.LEFT
        ) = apply {
            elements += WindowElement.Text(
                component,
                WindowColor.Solid(color.toLong() and 0xFFFFFFFFL),
                shadow,
                textScale,
                alignment
            )
        }

        fun addItem(stack: ItemStack, size: Float = 16f) = apply {
            elements += WindowElement.ItemIcon(stack.copy(), size)
        }

        fun addItemText(
            stack: ItemStack,
            text: String,
            color: WindowColor = WindowColor.Solid(0xFFFFFFFFL),
            itemSize: Float = 9f,
            textScale: Float = 1f,
            shadow: Boolean = false,
            spacing: Float = 1f,
            alignment: WindowElement.Text.TextAlignment = WindowElement.Text.TextAlignment.LEFT
        ) = apply {
            elements += WindowElement.Row(
                listOf(
                    WindowElement.ItemIcon(stack.copy(), itemSize),
                    WindowElement.Text(
                        Component.literal(text),
                        color,
                        shadow,
                        textScale
                    )
                ),
                spacing,
                alignment
            )
        }

        fun addItemText(
            stack: ItemStack,
            component: Component,
            color: Int,
            itemSize: Float = 9f,
            textScale: Float = 1f,
            shadow: Boolean = false,
            spacing: Float = 1f,
            alignment: WindowElement.Text.TextAlignment = WindowElement.Text.TextAlignment.LEFT
        ) = addItemText(
            stack,
            component,
            WindowColor.Solid(color.toLong() and 0xFFFFFFFFL),
            itemSize,
            textScale,
            shadow,
            spacing,
            alignment
        )

        fun addItemText(
            stack: ItemStack,
            component: Component,
            color: WindowColor = WindowColor.Solid(0xFFFFFFFFL),
            itemSize: Float = 9f,
            textScale: Float = 1f,
            shadow: Boolean = false,
            spacing: Float = 1f,
            alignment: WindowElement.Text.TextAlignment = WindowElement.Text.TextAlignment.LEFT
        ) = apply {
            elements += WindowElement.Row(
                listOf(
                    WindowElement.ItemIcon(stack.copy(), itemSize),
                    WindowElement.Text(component, color, shadow, textScale)
                ),
                spacing,
                alignment
            )
        }

        fun addItemText(
            stack: ItemStack,
            text: String,
            color: Int,
            itemSize: Float = 9f,
            textScale: Float = 1f,
            shadow: Boolean = false,
            spacing: Float = 1f,
            alignment: WindowElement.Text.TextAlignment = WindowElement.Text.TextAlignment.LEFT
        ) = addItemText(
            stack,
            text,
            WindowColor.Solid(color.toLong() and 0xFFFFFFFFL),
            itemSize,
            textScale,
            shadow,
            spacing,
            alignment
        )

        fun addIconText(
            texture: ResourceLocation,
            text: String,
            color: WindowColor = WindowColor.Solid(0xFFFFFFFFL),
            iconSize: Float = 9f,
            textScale: Float = 1f,
            shadow: Boolean = false,
            spacing: Float = 1f,
            alignment: WindowElement.Text.TextAlignment = WindowElement.Text.TextAlignment.LEFT,
            iconTint: WindowColor = WindowColor.Solid(0xFFFFFFFFL)
        ) = apply {
            elements += WindowElement.Row(
                listOf(
                    WindowElement.ImageIcon(texture, iconSize, iconTint),
                    WindowElement.Text(Component.literal(text), color, shadow, textScale)
                ),
                spacing,
                alignment
            )
        }

        fun addIconText(
            texture: ResourceLocation,
            component: Component,
            color: WindowColor = WindowColor.Solid(0xFFFFFFFFL),
            iconSize: Float = 9f,
            textScale: Float = 1f,
            shadow: Boolean = false,
            spacing: Float = 1f,
            alignment: WindowElement.Text.TextAlignment = WindowElement.Text.TextAlignment.LEFT
        ) = apply {
            elements += WindowElement.Row(
                listOf(
                    WindowElement.ImageIcon(texture, iconSize),
                    WindowElement.Text(component, color, shadow, textScale)
                ),
                spacing,
                alignment
            )
        }

        fun addIconText(
            texture: ResourceLocation,
            text: String,
            color: Int,
            iconSize: Float = 9f,
            textScale: Float = 1f,
            shadow: Boolean = false,
            spacing: Float = 1f,
            alignment: WindowElement.Text.TextAlignment = WindowElement.Text.TextAlignment.LEFT,
            iconTint: WindowColor = WindowColor.Solid(0xFFFFFFFFL)
        ) = addIconText(
            texture,
            text,
            WindowColor.Solid(color.toLong() and 0xFFFFFFFFL),
            iconSize,
            textScale,
            shadow,
            spacing,
            alignment,
            iconTint
        )

        fun addImageText(
            texture: ResourceLocation,
            text: String,
            color: WindowColor = WindowColor.Solid(0xFFFFFFFFL),
            iconSize: Float = 9f,
            textScale: Float = 1f,
            shadow: Boolean = false,
            spacing: Float = 1f,
            alignment: WindowElement.Text.TextAlignment = WindowElement.Text.TextAlignment.LEFT,
            iconTint: WindowColor = WindowColor.Solid(0xFFFFFFFFL)
        ) = addIconText(
            texture, text, color, iconSize, textScale, shadow, spacing, alignment, iconTint
        )

        fun addRow(
            vararg rowElements: WindowElement,
            spacing: Float = 2f,
            alignment: WindowElement.Text.TextAlignment = WindowElement.Text.TextAlignment.LEFT
        ) = apply {
            elements += WindowElement.Row(rowElements.toList(), spacing, alignment)
        }

        fun addElement(element: WindowElement) = apply { elements += element }

        fun addSpacer(height: Float) = apply { elements += WindowElement.Spacer(height) }

        fun addSeparator(
            color: WindowColor = WindowColor.Solid(0xFF808080L),
            thickness: Float = 1f,
            margin: Float = 2f
        ) = apply {
            elements += WindowElement.Separator(color, thickness, margin)
        }

        fun setSize(
            width: Float,
            height: Float,
            autoScaleContent: Boolean = true,
            autoFitWidth: Boolean = false,
            autoFitHeight: Boolean = false
        ) = apply {
            fixedWidth = width.coerceAtLeast(0.1f)
            fixedHeight = height.coerceAtLeast(0.1f)
            this.autoScaleContent = autoScaleContent
            this.autoFitWidth = autoFitWidth
            this.autoFitHeight = autoFitHeight
        }

        fun setContentFit(contentFit: ContentFit) = apply {
            when (contentFit) {
                ContentFit.EXPAND_TO_CONTENT -> {
                    fixedWidth = null
                    fixedHeight = null
                    autoScaleContent = false
                    autoFitWidth = false
                    autoFitHeight = false
                }
                ContentFit.SHRINK_CONTENT -> autoScaleContent = true
                ContentFit.KEEP_CONTENT_SIZE -> autoScaleContent = false
            }
        }

        fun setAlignment(alignment: WindowAlignment) = apply { this.alignment = alignment }

        fun setFaceCamera(faceCamera: Boolean) = apply { this.faceCamera = faceCamera }

        fun attachToEntityRotation(direction: Direction) = apply {
            entityYaw = when (direction) {
                Direction.SOUTH -> 0f
                Direction.NORTH -> 180f
                Direction.EAST -> 90f
                Direction.WEST -> 270f
                else -> 0f
            }
            faceCamera = false
        }

        fun attachToEntityRotation(entity: Entity) = apply {
            entityYaw = entity.yRot
            faceCamera = false
        }

        fun setPadding(padding: Float) = apply { this.padding = padding.coerceAtLeast(0f) }
        fun setBackground(color: WindowColor) = apply { background = color }
        fun setBorder(color: WindowColor, width: Float = 0.75f) = apply {
            border = color
            borderWidth = width.coerceAtLeast(0f)
        }

        fun clearBorder() = apply { border = null }

        fun setCornerRadius(radius: Float) = apply { cornerRadius = radius.coerceAtLeast(0f) }

        fun setShadow(color: WindowColor?, offset: Float = 1f) = apply {
            shadowColor = color
            shadowOffset = offset.coerceAtLeast(0f)
        }

        fun setScale(scale: Float) = apply { this.scale = scale.coerceAtLeast(0.001f) }

        fun setOffset(x: Double, y: Double, z: Double) = apply {
            offsetX = x
            offsetY = y
            offsetZ = z
        }

        fun build() = FloatingWindow(
            elements,
            padding,
            background,
            border,
            borderWidth,
            cornerRadius,
            scale,
            fixedWidth,
            fixedHeight,
            autoScaleContent,
            autoFitWidth,
            autoFitHeight,
            alignment,
            faceCamera,
            entityYaw,
            offsetX,
            offsetY,
            offsetZ,
            shadowColor,
            shadowOffset
        )
    }
}
