package net.ian.trainingdummy.client.window

import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import java.awt.Color
import kotlin.math.sin

sealed class WindowColor {
    data class Solid(val argb: Long) : WindowColor()
    data class Gradient(val argbStart: Long, val argbEnd: Long, val isVertical: Boolean = true) : WindowColor()
    data class Fade(val colorA: Long, val colorB: Long, val speed: Float = 1.0f) : WindowColor()
    data class Rainbow(val speed: Float = 1.0f, val saturation: Float = 1.0f, val brightness: Float = 1.0f) :
        WindowColor()

    data class AnimatedGradient(
        val argbStart: Long,
        val argbEnd: Long,
        val speed: Float = 1.0f,
        val scale: Float = 1.0f // Frequência do ciclo das ondas do gradiente
    ) : WindowColor()

    //data class RGBA(val r: Float, val g: Float, val b: Float, val a: Float)
    data class RGBA(val r: Float, val g: Float, val b: Float, val a: Float) {
        fun toArgb(): Int {
            val ai = (a * 255f).toInt().coerceIn(0, 255)
            val ri = (r * 255f).toInt().coerceIn(0, 255)
            val gi = (g * 255f).toInt().coerceIn(0, 255)
            val bi = (b * 255f).toInt().coerceIn(0, 255)
            return (ai shl 24) or (ri shl 16) or (gi shl 8) or bi
        }
    }

    fun resolve(u: Float = 0f, v: Float = 0f): RGBA {
        val time = Minecraft.getInstance().level?.gameTime ?: (System.currentTimeMillis() / 50L)

        return when (this) {
            is Solid -> parse(argb)
            is Gradient -> {
                val factor = if (isVertical) v else u
                val cA = parse(argbStart)
                val cB = parse(argbEnd)
                lerpRGBA(cA, cB, factor.coerceIn(0f, 1f))
            }

            is Fade -> {
                val factor = ((sin(time * 0.1 * speed) + 1.0) / 2.0).toFloat()
                val cA = parse(colorA)
                val cB = parse(colorB)
                lerpRGBA(cA, cB, factor)
            }

            is Rainbow -> {
                val hue = ((time * 0.05 * speed + u * 0.2) % 1.0).toFloat()
                val rgbInt = Color.HSBtoRGB(hue, saturation, brightness)
                RGBA(
                    r = ((rgbInt shr 16) and 0xFF) / 255.0f,
                    g = ((rgbInt shr 8) and 0xFF) / 255.0f,
                    b = (rgbInt and 0xFF) / 255.0f,
                    a = 1.0f
                )
            }

            is AnimatedGradient -> {
                // Onda senoidal baseada em tempo e posição (U) estilo After Effects
                val wave = sin((time * 0.1 * speed) + (u * scale * Math.PI * 2.0))
                val factor = ((wave + 1.0) / 2.0).toFloat()
                val cA = parse(argbStart)
                val cB = parse(argbEnd)
                lerpRGBA(cA, cB, factor)
            }
        }
    }

    private fun lerpRGBA(cA: RGBA, cB: RGBA, factor: Float): RGBA = RGBA(
        r = Mth.lerp(factor, cA.r, cB.r),
        g = Mth.lerp(factor, cA.g, cB.g),
        b = Mth.lerp(factor, cA.b, cB.b),
        a = Mth.lerp(factor, cA.a, cB.a)
    )

    private fun parse(color: Long): RGBA {
        // Se a cor for passada sem Alpha (ex: 0xFFFFFF em vez de 0xFFFFFFFF), assume Alpha 255 (FF)
        val fullColor = if (color in 0x000000..0xFFFFFF) color or 0xFF000000L else color
        return RGBA(
            r = ((fullColor shr 16) and 0xFF) / 255.0f,
            g = ((fullColor shr 8) and 0xFF) / 255.0f,
            b = (fullColor and 0xFF) / 255.0f,
            a = ((fullColor shr 24) and 0xFF) / 255.0f
        )
    }
}