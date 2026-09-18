package net.ian.trainingdummy.client.window

import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import java.awt.Color
import kotlin.math.sin

sealed class WindowColor {
    data class Solid(val argb: Long) : WindowColor()
    data class Gradient(val argbStart: Long, val argbEnd: Long, val isVertical: Boolean = true) : WindowColor()
    data class Fade(val colorA: Long, val colorB: Long, val speed: Float = 1.0f) : WindowColor()
    data class Rainbow(val speed: Float = 1.0f, val saturation: Float = 1.0f, val brightness: Float = 1.0f) : WindowColor()

    data class RGBA(val r: Float, val g: Float, val b: Float, val a: Float)

    fun resolve(u: Float = 0f, v: Float = 0f): RGBA {

        val time = Minecraft.getInstance().level?.gameTime ?: (System.currentTimeMillis() / 50L)

        return when (this) {
            is Solid -> parse(argb)
            is Gradient -> {
                val factor = if (isVertical) v else u
                val cA = parse(argbStart)
                val cB = parse(argbEnd)
                RGBA(
                    r = Mth.lerp(factor, cA.r, cB.r),
                    g = Mth.lerp(factor, cA.g, cB.g),
                    b = Mth.lerp(factor, cA.b, cB.b),
                    a = Mth.lerp(factor, cA.a, cB.a)
                )
            }
            is Fade -> {
                val factor = ((sin(time * 0.1 * speed) + 1.0) / 2.0).toFloat()
                val cA = parse(colorA)
                val cB = parse(colorB)
                RGBA(
                    r = Mth.lerp(factor, cA.r, cB.r),
                    g = Mth.lerp(factor, cA.g, cB.g),
                    b = Mth.lerp(factor, cA.b, cB.b),
                    a = Mth.lerp(factor, cA.a, cB.a)
                )
            }
            is Rainbow -> {
                val hue = ((time * 0.05 * speed) % 1.0).toFloat()
                val rgbInt = Color.HSBtoRGB(hue, saturation, brightness)
                RGBA(
                    r = ((rgbInt shr 16) and 0xFF) / 255.0f,
                    g = ((rgbInt shr 8) and 0xFF) / 255.0f,
                    b = (rgbInt and 0xFF) / 255.0f,
                    a = 1.0f
                )
            }
        }
    }

    private fun parse(color: Long): RGBA = RGBA(
        r = ((color shr 16) and 0xFF) / 255.0f,
        g = ((color shr 8) and 0xFF) / 255.0f,
        b = (color and 0xFF) / 255.0f,
        a = ((color shr 24) and 0xFF) / 255.0f
    )
}