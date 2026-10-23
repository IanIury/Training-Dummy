package net.ian.trainingdummy.screen.custom

import net.ian.trainingdummy.client.gui.RankSkins
import net.ian.trainingdummy.network.ServerboundRequestRanksPayload
import net.ian.trainingdummy.rank.ClientRankCache
import net.ian.trainingdummy.rank.RankCategory
import net.ian.trainingdummy.rank.RankEntry
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.PlayerFaceExtractor
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.client.network.ClientPacketDistributor
import kotlin.math.ceil
import kotlin.math.max

/**
 * Tela de ranking (top 100) do treino, aberta a partir do menu do dummy.
 *
 * Layout: botão de voltar + título/categoria no topo, pódio com o top 3 (rosto, nome e pontos)
 * e, logo abaixo, três blocos compactos com os demais jogadores (paginados).
 */
class RankScreen(
    private val parent: Screen?,
    initialCategory: RankCategory
) : Screen(Component.translatable("gui.trainingdummy.rank.title")) {

    private var category = initialCategory
    private var page = 0
    private var left = 0
    private var top = 0
    private var refreshTicks = 0

    private lateinit var btnPagePrev: Button
    private lateinit var btnPageNext: Button

    override fun init() {
        left = (width - PANEL_W) / 2
        top = (height - PANEL_H) / 2

        addRenderableWidget(
            Button.builder(Component.literal("←")) { onClose() }
                .bounds(left + 6, top + 6, 16, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.trainingdummy.rank.back")))
                .build()
        )

        val titleX = left + (PANEL_W - TITLE_W) / 2
        addRenderableWidget(
            Button.builder(Component.literal("<")) { changeCategory(previous = true) }
                .bounds(titleX - 18, top + 6, 16, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.trainingdummy.rank.previous_category")))
                .build()
        )
        addRenderableWidget(
            Button.builder(Component.literal(">")) { changeCategory(previous = false) }
                .bounds(titleX + TITLE_W + 2, top + 6, 16, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.trainingdummy.rank.next_category")))
                .build()
        )

        btnPagePrev = addRenderableWidget(
            Button.builder(Component.literal("<")) { changePage(-1) }
                .bounds(left + PANEL_W / 2 - 62, top + NAV_Y, 18, 14)
                .build()
        )
        btnPageNext = addRenderableWidget(
            Button.builder(Component.literal(">")) { changePage(1) }
                .bounds(left + PANEL_W / 2 + 44, top + NAV_Y, 18, 14)
                .build()
        )

        requestRanks()
        updateNavButtons()
    }

    override fun tick() {
        super.tick()
        if (++refreshTicks >= REFRESH_INTERVAL_TICKS) {
            refreshTicks = 0
            requestRanks()
        }
        updateNavButtons()
    }

    private fun requestRanks() {
        ClientPacketDistributor.sendToServer(ServerboundRequestRanksPayload())
    }

    private fun changeCategory(previous: Boolean) {
        category = if (previous) category.previous() else category.next()
        page = 0
        updateNavButtons()
    }

    private fun changePage(delta: Int) {
        page = (page + delta).coerceIn(0, pageCount() - 1)
        updateNavButtons()
    }

    private fun restEntries(): List<RankEntry> = ClientRankCache.entries(category).drop(PODIUM_SIZE)

    private fun pageCount(): Int = max(1, ceil(restEntries().size / PAGE_SIZE.toDouble()).toInt())

    private fun updateNavButtons() {
        val pages = pageCount()
        page = page.coerceIn(0, pages - 1)
        btnPagePrev.active = page > 0
        btnPageNext.active = page < pages - 1
    }

    override fun onClose() {
        parent?.let { minecraft.setScreenAndShow(it) }
    }

    override fun isPauseScreen(): Boolean = false

    // ------------------------------------------------------------------------
    // RENDERIZAÇÃO
    // ------------------------------------------------------------------------

    override fun extractBackground(extractor: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick)
        drawPanel(extractor)
        drawTitle(extractor)
        drawPodium(extractor)
        drawLists(extractor)
        drawFooter(extractor)
    }

    private fun drawPanel(g: GuiGraphicsExtractor) {
        val x = left
        val y = top
        g.fill(x, y, x + PANEL_W, y + PANEL_H, 0xFF000000.toInt())
        g.fill(x + 1, y + 1, x + PANEL_W - 1, y + PANEL_H - 1, 0xFFC6C6C6.toInt())
        g.fill(x + 1, y + 1, x + PANEL_W - 2, y + 2, 0xFFFFFFFF.toInt())
        g.fill(x + 1, y + 1, x + 2, y + PANEL_H - 2, 0xFFFFFFFF.toInt())
        g.fill(x + 2, y + PANEL_H - 2, x + PANEL_W - 1, y + PANEL_H - 1, 0xFF555555.toInt())
        g.fill(x + PANEL_W - 2, y + 2, x + PANEL_W - 1, y + PANEL_H - 1, 0xFF555555.toInt())
    }

    private fun drawTitle(g: GuiGraphicsExtractor) {
        val x = left + (PANEL_W - TITLE_W) / 2
        val y = top + 6
        g.fill(x, y, x + TITLE_W, y + 16, 0xFF000000.toInt())
        g.outline(x, y, TITLE_W, 16, 0xFF6B6B6B.toInt())
        val title = Component.translatable(category.translationKey).string
        drawScaledCentered(g, title, x + TITLE_W / 2, y + 4, 1f, 0xFFFFD84A.toInt(), TITLE_W - 6)
    }

    private class PodiumSpec(
        val rank: Int,
        val centerOffset: Int,
        val pedestalHeight: Int,
        val headSize: Int,
        val topColor: Int,
        val baseColor: Int,
        val numberColor: Int
    )

    private fun drawPodium(g: GuiGraphicsExtractor) {
        val boxX = left + 8
        val boxY = top + PODIUM_TOP
        val boxW = PANEL_W - 16
        val boxH = PODIUM_BOTTOM - PODIUM_TOP + 2
        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF12121A.toInt())
        g.outline(boxX, boxY, boxW, boxH, 0xFF555555.toInt())

        val entries = ClientRankCache.entries(category)
        if (!ClientRankCache.loaded) {
            drawScaledCentered(
                g, Component.translatable("gui.trainingdummy.rank.loading").string,
                left + PANEL_W / 2, boxY + boxH / 2 - 4, 1f, 0xFFAAAAAA.toInt(), boxW - 8
            )
            return
        }

        val specs = listOf(
            PodiumSpec(2, -PODIUM_SPACING, 20, 18, 0xFFDDE3EA.toInt(), 0xFFAEB6C1.toInt(), 0xFF5C6470.toInt()),
            PodiumSpec(1, 0, 26, 20, 0xFFFFE680.toInt(), 0xFFE0AE1F.toInt(), 0xFF7A5A00.toInt()),
            PodiumSpec(3, PODIUM_SPACING, 14, 18, 0xFFE0A064.toInt(), 0xFFB06F2C.toInt(), 0xFF5E3A12.toInt())
        )
        for (spec in specs) {
            drawPodiumSlot(g, spec, entries.getOrNull(spec.rank - 1))
        }
    }

    private fun drawPodiumSlot(g: GuiGraphicsExtractor, spec: PodiumSpec, entry: RankEntry?) {
        val centerX = left + PANEL_W / 2 + spec.centerOffset
        val bottom = top + PODIUM_BOTTOM
        val pedestalTop = bottom - spec.pedestalHeight
        val half = PODIUM_COLUMN_W / 2

        g.fill(centerX - half, pedestalTop, centerX + half, bottom, spec.baseColor)
        g.fill(centerX - half, pedestalTop, centerX + half, pedestalTop + 2, spec.topColor)
        g.fill(centerX - half, pedestalTop + 2, centerX - half + 1, bottom, spec.topColor)
        val numberScale = 1.2f
        val numberY = pedestalTop + 2 + (spec.pedestalHeight - 2 - (8 * numberScale).toInt()) / 2
        drawScaled(g, spec.rank.toString(), centerX, numberY, numberScale, spec.numberColor, shadow = false, centered = true)

        val headY = pedestalTop - spec.headSize - 2
        val headX = centerX - spec.headSize / 2
        val isMe = entry != null && entry.uuid == Minecraft.getInstance().player?.uuid
        val frameColor = if (isMe) 0xFF55FFFF.toInt() else spec.topColor
        g.fill(headX - 1, headY - 1, headX + spec.headSize + 1, headY + spec.headSize + 1, frameColor)
        if (entry != null) {
            PlayerFaceExtractor.extractRenderState(g, RankSkins.skin(entry.uuid), headX, headY, spec.headSize)
        } else {
            g.fill(headX, headY, headX + spec.headSize, headY + spec.headSize, 0xFF2A2A36.toInt())
            drawScaled(g, "?", centerX, headY + (spec.headSize - 8) / 2, 1f, 0xFF777777.toInt(), shadow = true, centered = true)
        }

        val scoreY = headY - 9
        val nameY = scoreY - 9
        val nameColor = if (isMe) 0xFF55FFFF.toInt() else 0xFFFFFFFF.toInt()
        if (entry != null) {
            drawScaledCentered(g, entry.name, centerX, nameY, 0.9f, nameColor, PODIUM_COLUMN_W - 2)
            drawScaledCentered(g, category.format(entry.value), centerX, scoreY, 0.9f, spec.topColor, PODIUM_COLUMN_W - 2)
        } else {
            drawScaledCentered(g, "---", centerX, scoreY, 0.9f, 0xFF666666.toInt(), PODIUM_COLUMN_W - 2)
        }
    }

    private fun drawLists(g: GuiGraphicsExtractor) {
        val rest = restEntries()
        val myUuid = Minecraft.getInstance().player?.uuid
        val pageStart = page * PAGE_SIZE
        val boxY = top + LIST_TOP

        for (column in 0 until LIST_COLUMNS) {
            val boxX = left + 8 + column * (LIST_COLUMN_W + LIST_GAP)
            g.fill(boxX, boxY, boxX + LIST_COLUMN_W, boxY + LIST_BOX_H, 0xFF000000.toInt())
            g.outline(boxX, boxY, LIST_COLUMN_W, LIST_BOX_H, 0xFF555555.toInt())

            for (row in 0 until ROWS_PER_COLUMN) {
                val index = pageStart + column * ROWS_PER_COLUMN + row
                val rowY = boxY + 3 + row * ROW_H
                val entry = rest.getOrNull(index)

                if (entry == null) {
                    if (index < PAGE_SIZE * pageCount()) {
                        drawScaled(g, (index + PODIUM_SIZE + 1).toString(), boxX + 4, rowY + 2, 0.7f, 0xFF3A3A3A.toInt(), shadow = false, centered = false)
                    }
                    continue
                }

                val isMe = entry.uuid == myUuid
                if (isMe) {
                    g.fill(boxX + 2, rowY, boxX + LIST_COLUMN_W - 2, rowY + ROW_H - 1, 0x6055FFFF)
                } else if (row % 2 == 1) {
                    g.fill(boxX + 2, rowY, boxX + LIST_COLUMN_W - 2, rowY + ROW_H - 1, 0x18FFFFFF)
                }

                val position = index + PODIUM_SIZE + 1
                val textY = rowY + 2
                drawScaled(g, position.toString(), boxX + 4, textY, 0.7f, 0xFF9A9A9A.toInt(), shadow = false, centered = false)

                val valueText = category.format(entry.value)
                val valueWidth = (font.width(valueText) * 0.7f).toInt()
                drawScaled(
                    g, valueText, boxX + LIST_COLUMN_W - 4 - valueWidth, textY, 0.7f,
                    0xFFFFD84A.toInt(), shadow = false, centered = false
                )

                val nameX = boxX + 4 + 11
                val nameMaxWidth = LIST_COLUMN_W - 4 - 11 - valueWidth - 6
                val nameColor = if (isMe) 0xFF55FFFF.toInt() else 0xFFE6E6E6.toInt()
                drawScaled(
                    g, trimToWidth(entry.name, nameMaxWidth, 0.7f), nameX, textY, 0.7f,
                    nameColor, shadow = false, centered = false
                )
            }
        }
    }

    private fun drawFooter(g: GuiGraphicsExtractor) {
        val centerX = left + PANEL_W / 2
        val pages = pageCount()
        drawScaledCentered(
            g,
            Component.translatable("gui.trainingdummy.rank.page", page + 1, pages).string,
            centerX, top + NAV_Y + 3, 1f, 0xFF404040.toInt(), 80, shadow = false
        )

        val position = ClientRankCache.myPosition(category)
        val line = if (position > 0) {
            Component.translatable(
                "gui.trainingdummy.rank.your_position",
                position,
                category.format(ClientRankCache.myValue(category))
            )
        } else {
            Component.translatable("gui.trainingdummy.rank.unranked")
        }
        val color = if (position > 0) 0xFF006B6B.toInt() else 0xFF555555.toInt()
        drawScaledCentered(g, line.string, centerX, top + SELF_Y, 0.9f, color, PANEL_W - 16, shadow = false)
    }

    private fun trimToWidth(text: String, maxWidth: Int, scale: Float): String {
        val limit = (maxWidth / scale).toInt()
        if (font.width(text) <= limit) return text
        val ellipsis = "…"
        return font.plainSubstrByWidth(text, (limit - font.width(ellipsis)).coerceAtLeast(0)) + ellipsis
    }

    private fun drawScaledCentered(
        g: GuiGraphicsExtractor,
        text: String,
        centerX: Int,
        y: Int,
        scale: Float,
        color: Int,
        maxWidth: Int,
        shadow: Boolean = true
    ) {
        drawScaled(g, trimToWidth(text, maxWidth, scale), centerX, y, scale, color, shadow, centered = true)
    }

    private fun drawScaled(
        g: GuiGraphicsExtractor,
        text: String,
        x: Int,
        y: Int,
        scale: Float,
        color: Int,
        shadow: Boolean,
        centered: Boolean
    ) {
        val pose = g.pose()
        pose.pushMatrix()
        pose.translate(x.toFloat(), y.toFloat())
        pose.scale(scale, scale)
        val offsetX = if (centered) -font.width(text) / 2 else 0
        g.text(font, text, offsetX, 0, color, shadow)
        pose.popMatrix()
    }

    companion object {
        private const val PANEL_W = 264
        private const val PANEL_H = 228
        private const val TITLE_W = 116
        private const val PODIUM_TOP = 26
        private const val PODIUM_BOTTOM = 98
        private const val PODIUM_SPACING = 74
        private const val PODIUM_COLUMN_W = 60
        private const val LIST_TOP = 104
        private const val LIST_BOX_H = 76
        private const val LIST_COLUMNS = 3
        private const val ROWS_PER_COLUMN = 6
        private const val ROW_H = 12
        private const val LIST_COLUMN_W = 78
        private const val LIST_GAP = 7
        private const val NAV_Y = 186
        private const val SELF_Y = 208
        private const val REFRESH_INTERVAL_TICKS = 40
        private const val PODIUM_SIZE = 3
        private const val PAGE_SIZE = 18
    }
}
