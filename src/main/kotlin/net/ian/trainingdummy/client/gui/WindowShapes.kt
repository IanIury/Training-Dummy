package net.ian.trainingdummy.client.gui

import dev.ian.worldpanel.shape.PanelShape
import net.ian.trainingdummy.init.enums.WindowShapeType
import net.minecraft.client.gui.GuiGraphics

/**
 * Ponte entre [WindowShapeType] (enum comum) e [PanelShape] (WorldPanel), além de um
 * rasterizador simples para desenhar o formato como ícone em GUIs 2D.
 */
object WindowShapes {

    /** Retorna a forma geométrica de retângulo arredondado do WorldPanel. */
    fun roundedRectangle(radius: Float = 8f): PanelShape = PanelShape.roundedRectangle(radius)

    fun toPanelShape(type: WindowShapeType): PanelShape = when (type) {
        WindowShapeType.ROUNDED -> PanelShape.roundedRectangle(3f)
        WindowShapeType.ROUNDED_RECTANGLE -> PanelShape.roundedRectangle(8f)
        WindowShapeType.RECTANGLE -> PanelShape.rectangle()
        WindowShapeType.CIRCLE -> PanelShape.circle()
        WindowShapeType.ELLIPSE -> PanelShape.ellipse()
        WindowShapeType.TRIANGLE -> PanelShape.equilateralTriangle()
        WindowShapeType.RHOMBUS -> PanelShape.rhombus()
        WindowShapeType.TRAPEZOID -> PanelShape.trapezoid()
        WindowShapeType.PARALLELOGRAM -> PanelShape.parallelogram()
        WindowShapeType.PENTAGON -> PanelShape.pentagon()
        WindowShapeType.HEXAGON -> PanelShape.hexagon()
        WindowShapeType.OCTAGON -> PanelShape.polygon(8, 22.5f)
    }

    /** Caixa (largura, altura) usada para desenhar o ícone de cada formato. */
    private fun iconBox(type: WindowShapeType, size: Int): Pair<Int, Int> = when (type) {
        WindowShapeType.ROUNDED, WindowShapeType.ROUNDED_RECTANGLE, WindowShapeType.RECTANGLE, WindowShapeType.ELLIPSE ->
            size to (size * 0.68f).toInt().coerceAtLeast(3)
        WindowShapeType.TRAPEZOID, WindowShapeType.PARALLELOGRAM ->
            size to (size * 0.75f).toInt().coerceAtLeast(3)
        else -> size to size
    }

    /**
     * Desenha o formato [type] centralizado na área ([x], [y], [areaSize] x [areaSize]).
     * O polígono é rasterizado linha a linha (scanline), o que funciona para qualquer formato do WorldPanel.
     */
    fun drawIcon(guiGraphics: GuiGraphics, type: WindowShapeType, x: Int, y: Int, areaSize: Int, color: Int) {
        val shape = when (type) {
            WindowShapeType.ROUNDED -> PanelShape.roundedRectangle(areaSize * 0.18f)
            WindowShapeType.ROUNDED_RECTANGLE -> PanelShape.roundedRectangle(areaSize * 0.38f)
            else -> toPanelShape(type)
        }
        val (w, h) = iconBox(type, areaSize)
        val polygon = shape.toPolygon(w.toFloat(), h.toFloat()) ?: return
        val vertices = polygon.vertices
        if (vertices.size < 3) return

        val originX = x + (areaSize - w) / 2
        val originY = y + (areaSize - h) / 2
        val crossings = ArrayList<Float>(8)

        for (row in 0 until h) {
            val scanY = row + 0.5f
            crossings.clear()
            for (i in vertices.indices) {
                val p1 = vertices[i]
                val p2 = vertices[(i + 1) % vertices.size]
                if (p1.y == p2.y) continue
                val minY = minOf(p1.y, p2.y)
                val maxY = maxOf(p1.y, p2.y)
                if (scanY < minY || scanY >= maxY) continue
                val t = (scanY - p1.y) / (p2.y - p1.y)
                crossings.add(p1.x + t * (p2.x - p1.x))
            }
            if (crossings.size < 2) continue
            crossings.sort()
            var i = 0
            while (i + 1 < crossings.size) {
                val startX = Math.round(crossings[i])
                val endX = maxOf(Math.round(crossings[i + 1]), startX + 1)
                guiGraphics.fill(originX + startX, originY + row, originX + endX, originY + row + 1, color)
                i += 2
            }
        }
    }
}
