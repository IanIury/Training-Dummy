package net.ian.trainingdummy.client.window

import net.minecraft.util.Mth
import java.awt.Color
import kotlin.math.sin

sealed class WindowColor {
    data class Solid(val argb: Long) : WindowColor()

    data class Gradient(
        val argbStart: Long,
        val argbEnd: Long,
        val isVertical: Boolean = true
    ) : WindowColor()

    data class Fade(
        val colorA: Long,
        val colorB: Long,
        val speed: Float = 1f
    ) : WindowColor()

    data class Rainbow(
        val speed: Float = 1f,
        val saturation: Float = 1f,
        val brightness: Float = 1f
    ) : WindowColor()

    data class AnimatedGradient(
        val argbStart: Long,
        val argbEnd: Long,
        val speed: Float = 1f,
        val scale: Float = 1f
    ) : WindowColor()

    data class Pulse(
        val color: Long,
        val minAlpha: Float = 0.25f,
        val speed: Float = 1f
    ) : WindowColor()

    data class RGBA(val r: Float, val g: Float, val b: Float, val a: Float) {
        fun toArgb(): Int {
            val alpha = (a * 255f).toInt().coerceIn(0, 255)
            val red = (r * 255f).toInt().coerceIn(0, 255)
            val green = (g * 255f).toInt().coerceIn(0, 255)
            val blue = (b * 255f).toInt().coerceIn(0, 255)
            return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
        }
    }

    fun resolve(u: Float = 0f, v: Float = 0f): RGBA {
        val time = System.nanoTime() / 1_000_000_000.0
        return when (this) {
            is Solid -> parse(argb)
            is Gradient -> {
                val factor = if (isVertical) v else u
                mix(parse(argbStart), parse(argbEnd), factor)
            }
            is Fade -> {
                val phase = ((sin(time * speed) + 1.0) / 2.0).toFloat()
                mix(parse(colorA), parse(colorB), phase)
            }
            is Rainbow -> {
                val hue = ((time * speed * 0.1 + u) % 1.0).toFloat().let { if (it < 0f) it + 1f else it }
                val rgb = Color.HSBtoRGB(
                    hue,
                    saturation.coerceIn(0f, 1f),
                    brightness.coerceIn(0f, 1f)
                )
                RGBA(
                    ((rgb shr 16) and 0xFF) / 255f,
                    ((rgb shr 8) and 0xFF) / 255f,
                    (rgb and 0xFF) / 255f,
                    1f
                )
            }
            is AnimatedGradient -> {
                val phase = (time * speed + u * scale * Math.PI * 2.0).let { ((sin(it) + 1.0) / 2.0).toFloat() }
                mix(parse(argbStart), parse(argbEnd), phase)
            }
            is Pulse -> {
                val base = parse(color)
                val minimum = minAlpha.coerceIn(0f, 1f)
                val phase = ((sin(time * speed) + 1.0) / 2.0).toFloat()
                base.copy(a = Mth.lerp(phase, minimum * base.a, base.a))
            }
        }
    }

    private fun mix(a: RGBA, b: RGBA, amount: Float): RGBA {
        val factor = amount.coerceIn(0f, 1f)
        return RGBA(
            Mth.lerp(factor, a.r, b.r),
            Mth.lerp(factor, a.g, b.g),
            Mth.lerp(factor, a.b, b.b),
            Mth.lerp(factor, a.a, b.a)
        )
    }

    private fun parse(argb: Long): RGBA {
        val color = if (argb in 0L..0xFFFFFFL) argb or 0xFF000000L else argb
        return RGBA(
            ((color shr 16) and 0xFF).toFloat() / 255f,
            ((color shr 8) and 0xFF).toFloat() / 255f,
            (color and 0xFF).toFloat() / 255f,
            ((color shr 24) and 0xFF).toFloat() / 255f
        )
    }
}
