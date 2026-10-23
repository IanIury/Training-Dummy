package net.ian.trainingdummy.screen.custom

import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.client.gui.WindowShapes
import net.ian.trainingdummy.init.enums.TrainingModos
import net.ian.trainingdummy.init.enums.WindowShapeType
import net.ian.trainingdummy.item.custom.TrainingModuleItem
import net.ian.trainingdummy.network.ServerboundChangeModePayload
import net.ian.trainingdummy.network.ServerboundSetDetailedDamageWindowPayload
import net.ian.trainingdummy.network.ServerboundSetWindowShapePayload
import net.ian.trainingdummy.rank.RankCategory
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory
import net.neoforged.neoforge.client.network.ClientPacketDistributor

class DummyScreen(
    menu: DummyMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<DummyMenu>(menu, playerInventory, title) {

    private val TEXTURE = Identifier.fromNamespaceAndPath(TrainingDummy.ID, "textures/gui/training_dummy_inicial_gui.png")
    private val TRAINING_MODULE_PLACEHOLDER = Identifier.fromNamespaceAndPath(
        TrainingDummy.ID,
        "textures/gui/training_module_placeholder.png"
    )

    private lateinit var btnMode: Button
    private lateinit var btnDetailedDamage: Button
    private lateinit var btnShape: Button
    private lateinit var btnRank: Button

    private var lastMode: TrainingModos? = null
    private var pendingMode: TrainingModos? = null
    private var pendingModeTicks = 0

    private var pendingDetailedDamage: Boolean? = null
    private var pendingDetailedDamageTicks = 0

    private var pendingShape: WindowShapeType? = null
    private var pendingShapeTicks = 0

    override fun init() {
        super.init()

        val modo = currentModuleMode() ?: menu.dummy?.trainingModuleConfig?.trainingModo ?: TrainingModos.DEFAULT

        // Botão de seleção de modo posicionado no topo direito do painel superior (82, 5)
        btnMode = addRenderableWidget(
            object : Button(
                leftPos + 82,
                topPos + 5,
                82,
                20,
                modeLabel(modo),
                OnPress { changeMode(previous = false) },
                CreateNarration { modeLabel(menu.dummy?.trainingModuleConfig?.trainingModo ?: modo).copy() }
            ) {
                override fun mouseClicked(event: MouseButtonEvent, fromTouch: Boolean): Boolean {
                    if (event.button() == 1 && active && visible && isMouseOver(event.x(), event.y())) {
                        changeMode(previous = true)
                        return true
                    }
                    return super.mouseClicked(event, fromTouch)
                }

                override fun extractContents(extractor: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
                    extractDefaultSprite(extractor)
                    extractDefaultLabel(extractor.textRenderer())
                }
            }
        )

        // Botão "D" (Detailed Damage Window) alinhado acima do slot do Módulo de Treino (152, 44)
        btnDetailedDamage = addRenderableWidget(
            object : Button(
                leftPos + DETAILED_BUTTON_X,
                topPos + DETAILED_BUTTON_Y,
                SLOT_SIZE,
                SLOT_SIZE,
                Component.literal(DETAILED_BUTTON_LABEL),
                OnPress { setDetailedDamageWindow(!(pendingDetailedDamage ?: menu.dummy?.detailedDamageWindow ?: false)) },
                CreateNarration { detailedDamageTooltip(menu.dummy?.detailedDamageWindow ?: false) }
            ) {
                override fun extractContents(
                    extractor: GuiGraphicsExtractor,
                    mouseX: Int,
                    mouseY: Int,
                    partialTick: Float
                ) {
                    val enabled = pendingDetailedDamage ?: menu.dummy?.detailedDamageWindow ?: false
                    val hovered = isHovered
                    val background = when {
                        enabled && hovered -> 0xFF3F9B65.toInt()
                        enabled -> 0xFF287A4B.toInt()
                        hovered -> 0xFF626262.toInt()
                        else -> 0xFF414141.toInt()
                    }
                    val border = if (enabled) 0xFF8DE0A8.toInt() else 0xFF999999.toInt()
                    extractor.fill(x, y, x + width, y + height, background)
                    extractor.outline(x, y, width, height, border)
                    extractor.centeredText(
                        Minecraft.getInstance().font,
                        message,
                        x + width / 2,
                        y + (height - 8) / 2,
                        0xFFFFFFFF.toInt()
                    )
                }
            }
        )
        btnDetailedDamage.active = menu.dummy != null

        // Botão de formato geométrico da janela ao lado do slot de Módulo de Treino (134, 62)
        btnShape = addRenderableWidget(
            object : Button(
                leftPos + SHAPE_BUTTON_X,
                topPos + SHAPE_BUTTON_Y,
                SLOT_SIZE,
                SLOT_SIZE,
                Component.empty(),
                OnPress { changeShape(previous = false) },
                CreateNarration { shapeTooltip(currentShape())[0].copy() }
            ) {
                override fun mouseClicked(event: MouseButtonEvent, fromTouch: Boolean): Boolean {
                    if (event.button() == 1 && active && visible && isMouseOver(event.x(), event.y())) {
                        playDownSound(Minecraft.getInstance().soundManager)
                        changeShape(previous = true)
                        return true
                    }
                    return super.mouseClicked(event, fromTouch)
                }

                override fun extractContents(extractor: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
                    drawSlotButton(extractor, x, y, width, isHovered, active)
                    WindowShapes.drawIcon(extractor, currentShape(), x + 2, y + 2, width - 4, 0xFF3A3A3A.toInt())
                    WindowShapes.drawIcon(extractor, currentShape(), x + 1, y + 1, width - 4, 0xFFFFFFFF.toInt())
                }
            }
        )
        btnShape.active = menu.dummy != null

        // Botão do ranking alinhado acima do botão de formato e ao lado do botão de dano detalhado (134, 44)
        btnRank = addRenderableWidget(
            object : Button(
                leftPos + RANK_BUTTON_X,
                topPos + RANK_BUTTON_Y,
                SLOT_SIZE,
                SLOT_SIZE,
                Component.empty(),
                OnPress { openRanking() },
                CreateNarration { Component.translatable("gui.trainingdummy.rank.open").copy() }
            ) {
                override fun extractContents(extractor: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
                    drawSlotButton(extractor, x, y, width, isHovered, active)
                    drawTrophy(extractor, x + (width - TROPHY_SIZE) / 2, y + (height - TROPHY_SIZE) / 2)
                }
            }
        )
        btnRank.visible = menu.hasUpgrade()

        updateButtonState()
    }

    private fun currentShape(): WindowShapeType =
        pendingShape ?: menu.dummy?.windowShape ?: WindowShapeType.DEFAULT

    private fun changeShape(previous: Boolean) {
        if (menu.dummy == null) return
        val newShape = if (previous) currentShape().previous() else currentShape().next()
        pendingShape = newShape
        pendingShapeTicks = 0
        ClientPacketDistributor.sendToServer(ServerboundSetWindowShapePayload(newShape))
    }

    private fun updateShapeButton() {
        val synced = menu.dummy?.windowShape
        pendingShape?.let { pending ->
            if (synced == pending || ++pendingShapeTicks >= SHAPE_SYNC_TIMEOUT_TICKS) {
                pendingShape = null
                pendingShapeTicks = 0
            }
        }
        btnShape.active = menu.dummy != null
    }

    private fun shapeTooltip(shape: WindowShapeType): List<Component> = listOf(
        Component.translatable("gui.trainingdummy.window_shape", Component.translatable(shape.translationKey)),
        Component.translatable("gui.trainingdummy.window_shape.hint").withStyle(ChatFormatting.DARK_GRAY)
    )

    private fun openRanking() {
        if (!menu.hasUpgrade()) return
        val mode = currentModuleMode() ?: menu.dummy?.trainingModuleConfig?.trainingModo
        val category = if (mode == TrainingModos.DPS) RankCategory.BEST_DPS else RankCategory.MAX_HIT
        minecraft.setScreenAndShow(RankScreen(this, category))
    }

    private fun drawSlotButton(g: GuiGraphicsExtractor, x: Int, y: Int, size: Int, hovered: Boolean, active: Boolean) {
        g.fill(x, y, x + size, y + size, 0xFF8B8B8B.toInt())
        g.fill(x, y, x + size - 1, y + 1, 0xFF373737.toInt())
        g.fill(x, y, x + 1, y + size - 1, 0xFF373737.toInt())
        g.fill(x + 1, y + size - 1, x + size, y + size, 0xFFFFFFFF.toInt())
        g.fill(x + size - 1, y + 1, x + size, y + size, 0xFFFFFFFF.toInt())
        if (!active) {
            g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0x80404040.toInt())
        } else if (hovered) {
            g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0x80FFFFFF.toInt())
        }
    }

    private fun drawTrophy(g: GuiGraphicsExtractor, x: Int, y: Int) {
        for ((row, line) in TROPHY_PIXELS.withIndex()) {
            for ((col, ch) in line.withIndex()) {
                if (ch == '.') continue
                val color = when (ch) {
                    '#' -> 0xFFFFD23F.toInt()
                    else -> 0xFFB8860B.toInt()
                }
                g.fill(x + col + 1, y + row + 1, x + col + 2, y + row + 2, 0xFF3A3A3A.toInt())
                g.fill(x + col, y + row, x + col + 1, y + row + 1, color)
            }
        }
    }

    override fun extractLabels(extractor: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
    }

    override fun containerTick() {
        super.containerTick()
        updateButtonState()
    }

    private fun updateButtonState() {
        val hasUpgrade = menu.hasUpgrade()
        btnMode.visible = hasUpgrade
        if (hasUpgrade) {
            val dummyMode = currentModuleMode() ?: menu.dummy?.trainingModuleConfig?.trainingModo
            val syncedMode = pendingMode?.let { pending ->
                if (dummyMode == pending || ++pendingModeTicks >= SYNC_TIMEOUT_TICKS) {
                    pendingMode = null
                    pendingModeTicks = 0
                    dummyMode
                } else {
                    pending
                }
            } ?: dummyMode

            btnMode.active = menu.dummy != null
            if (syncedMode != null) {
                btnMode.message = modeLabel(syncedMode)
                lastMode = syncedMode
            }
        }

        val hasDummy = menu.dummy != null
        btnDetailedDamage.visible = hasDummy
        if (hasDummy) {
            updateDetailedDamageButton()
        }

        btnShape.visible = hasDummy
        if (hasDummy) {
            updateShapeButton()
        }

        btnRank.visible = hasUpgrade
        btnRank.active = hasDummy
    }

    private fun updateDetailedDamageButton() {
        val syncedValue = menu.dummy?.detailedDamageWindow ?: false
        val shownValue = pendingDetailedDamage?.let { pending ->
            if (syncedValue == pending || ++pendingDetailedDamageTicks >= DETAILED_SYNC_TIMEOUT_TICKS) {
                pendingDetailedDamage = null
                pendingDetailedDamageTicks = 0
                syncedValue
            } else {
                pending
            }
        } ?: syncedValue
        btnDetailedDamage.active = menu.dummy != null
        btnDetailedDamage.message = Component.literal(DETAILED_BUTTON_LABEL)
        detailedDamageEnabled = shownValue
    }

    private var detailedDamageEnabled = false

    private fun setDetailedDamageWindow(enabled: Boolean) {
        if (menu.dummy == null) return
        pendingDetailedDamage = enabled
        pendingDetailedDamageTicks = 0
        detailedDamageEnabled = enabled
        ClientPacketDistributor.sendToServer(ServerboundSetDetailedDamageWindowPayload(enabled))
    }

    private fun detailedDamageTooltip(enabled: Boolean): MutableComponent =
        Component.translatable(
            "gui.trainingdummy.detailed_damage_window",
            Component.translatable(if (enabled) "gui.trainingdummy.enabled" else "gui.trainingdummy.disabled")
        )

    private fun changeMode(previous: Boolean) {
        val stack = menu.dummy?.dummyInventory?.getStackInSlot(4) ?: return
        if (stack.isEmpty) return
        val currentMode = pendingMode ?: currentModuleMode() ?: return
        val newMode = if (previous) currentMode.previous() else currentMode.next()
        pendingMode = newMode
        pendingModeTicks = 0
        ClientPacketDistributor.sendToServer(ServerboundChangeModePayload(newMode))
        btnMode.message = modeLabel(newMode)
        lastMode = newMode
    }

    private fun currentModuleMode(): TrainingModos? =
        menu.dummy?.dummyInventory?.getStackInSlot(TRAINING_MODULE_SLOT_INDEX)
            ?.let(TrainingModuleItem::getItemTrainingMode)

    private fun modeLabel(mode: TrainingModos): Component =
        Component.translatable(
            "gui.trainingdummy.dummy_mode",
            Component.translatable(mode.translationKey())
        )

    private fun TrainingModos.translationKey(): String = when (this) {
        TrainingModos.DEFAULT -> "gui.trainingdummy.dummy_mode.default"
        TrainingModos.DPS -> "gui.trainingdummy.dummy_mode.dps"
        TrainingModos.ACCUMULATED -> "gui.trainingdummy.dummy_mode.accumulated"
        TrainingModos.COMPARISON -> "gui.trainingdummy.dummy_mode.comparison"
    }

    override fun extractBackground(extractor: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        val x = this.leftPos
        val y = this.topPos

        // Renderiza a textura da GUI utilizando o pipeline GUI_TEXTURED
        extractor.blit(
            RenderPipelines.GUI_TEXTURED,
            TEXTURE,
            x,
            y,
            0.0f,
            0.0f,
            this.imageWidth,
            this.imageHeight,
            256,
            256
        )

        val dummy = menu.dummy
        if (dummy != null) {
            val x1 = x + 26
            val y1 = y + 8
            val x2 = x + 74
            val y2 = y + 72

            try {
                isRenderingEntityInGui = true
                InventoryScreen.extractEntityInInventoryFollowsMouse(
                    extractor,
                    x1, y1, x2, y2,
                    25,
                    0.0625f,
                    mouseX.toFloat(),
                    mouseY.toFloat(),
                    dummy
                )
            } finally {
                isRenderingEntityInGui = false
            }
        }
    }

    override fun extractRenderState(extractor: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick)
        renderTrainingModulePlaceholder(extractor)
        if (btnDetailedDamage.isHovered) {
            extractor.setTooltipForNextFrame(
                font,
                detailedDamageTooltip(detailedDamageEnabled),
                mouseX,
                mouseY
            )
        }
        if (btnShape.isHovered) {
            extractor.setComponentTooltipForNextFrame(font, shapeTooltip(currentShape()), mouseX, mouseY)
        }
        if (btnRank.visible && btnRank.isHovered) {
            extractor.setComponentTooltipForNextFrame(
                font,
                listOf(
                    Component.translatable("gui.trainingdummy.rank.open"),
                    Component.translatable("gui.trainingdummy.rank.open.hint").withStyle(ChatFormatting.DARK_GRAY)
                ),
                mouseX,
                mouseY
            )
        }
    }

    private fun renderTrainingModulePlaceholder(extractor: GuiGraphicsExtractor) {
        val slot = menu.slots.getOrNull(TRAINING_MODULE_SLOT_INDEX) ?: return
        if (slot.hasItem()) return

        val x = leftPos + slot.x
        val y = topPos + slot.y
        extractor.blit(
            RenderPipelines.GUI_TEXTURED,
            TRAINING_MODULE_PLACEHOLDER,
            x,
            y,
            0.0f,
            0.0f,
            16,
            16,
            16,
            16
        )
    }

    companion object {
        var isRenderingEntityInGui: Boolean = false

        private const val SYNC_TIMEOUT_TICKS = 10
        private const val DETAILED_SYNC_TIMEOUT_TICKS = 10
        private const val SHAPE_SYNC_TIMEOUT_TICKS = 10
        private const val TRAINING_MODULE_SLOT_INDEX = 4
        private const val SLOT_SIZE = 16

        private const val DETAILED_BUTTON_LABEL = "D"
        private const val DETAILED_BUTTON_X = 152
        private const val DETAILED_BUTTON_Y = 44

        /** Posição do botão de formato (à esquerda do slot de módulo de treino em 152,62). */
        private const val SHAPE_BUTTON_X = 134
        private const val SHAPE_BUTTON_Y = 62

        /** Posição do botão do ranking (acima do botão de formato em 134,62). */
        private const val RANK_BUTTON_X = 134
        private const val RANK_BUTTON_Y = 44
        private const val TROPHY_SIZE = 11

        private val TROPHY_PIXELS = arrayOf(
            "..#######..",
            ".#########.",
            "#.##+++##.#",
            "#..#+++#..#",
            ".#..###..#.",
            "..#.###.#..",
            "....###....",
            "....###....",
            "...#####...",
            "..#######..",
            ".#########."
        )
    }
}
