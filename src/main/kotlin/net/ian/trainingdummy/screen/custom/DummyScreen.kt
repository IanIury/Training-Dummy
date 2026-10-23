package net.ian.trainingdummy.screen.custom

import com.mojang.blaze3d.systems.RenderSystem
import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.client.gui.WindowShapes
import net.ian.trainingdummy.init.enums.TrainingModos
import net.ian.trainingdummy.init.enums.WindowShapeType
import net.ian.trainingdummy.rank.RankCategory
import net.ian.trainingdummy.item.custom.TrainingModuleItem
import net.ian.trainingdummy.network.ServerboundChangeModePayload
import net.ian.trainingdummy.network.ServerboundSetDetailedDamageWindowPayload
import net.ian.trainingdummy.network.ServerboundSetWindowShapePayload
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.neoforged.neoforge.network.PacketDistributor

class DummyScreen(
    menu: DummyMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<DummyMenu>(menu, playerInventory, title) {

    private val TEXTURE = ResourceLocation.fromNamespaceAndPath(TrainingDummy.ID, "textures/gui/training_dummy_inicial_gui.png")
    private val TRAINING_MODULE_PLACEHOLDER = ResourceLocation.fromNamespaceAndPath(
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
        this.imageWidth = 176
        this.imageHeight = 166



        val modo = currentModuleMode() ?: menu.dummy?.trainingModuleConfig?.trainingModo ?: TrainingModos.DEFAULT

        this.lastMode = modo

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
                override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
                    if (button == 1 && active && visible && clicked(mouseX, mouseY)) {
                        changeMode(previous = true)
                        return true
                    }
                    return super.mouseClicked(mouseX, mouseY, button)
                }
            }
        )

        val moduleSlot = menu.slots[TRAINING_MODULE_SLOT_INDEX]
        btnDetailedDamage = addRenderableWidget(
            object : Button(
                leftPos + moduleSlot.x + (SLOT_SIZE - DETAILED_BUTTON_SIZE) / 2,
                topPos + moduleSlot.y - DETAILED_BUTTON_SIZE - DETAILED_BUTTON_GAP,
                DETAILED_BUTTON_SIZE,
                DETAILED_BUTTON_SIZE,
                Component.literal(DETAILED_BUTTON_LABEL),
                OnPress { setDetailedDamageWindow(!(pendingDetailedDamage ?: menu.dummy?.detailedDamageWindow ?: false)) },
                CreateNarration { detailedDamageTooltip(menu.dummy?.detailedDamageWindow ?: false) }
            ) {
                override fun renderWidget(
                    guiGraphics: GuiGraphics,
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
                    guiGraphics.fill(x, y, x + width, y + height, background)
                    guiGraphics.renderOutline(x, y, width, height, border)
                    guiGraphics.drawCenteredString(
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

        // Botão do tamanho de um slot: troca o formato geométrico da janela (ícone = formato atual)
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
                override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
                    if (button == 1 && active && visible && clicked(mouseX, mouseY)) {
                        playDownSound(Minecraft.getInstance().soundManager)
                        changeShape(previous = true)
                        return true
                    }
                    return super.mouseClicked(mouseX, mouseY, button)
                }

                override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
                    drawSlotButton(guiGraphics, x, y, width, isHovered, active)
                    WindowShapes.drawIcon(guiGraphics, currentShape(), x + 2, y + 2, width - 4, 0xFF3A3A3A.toInt())
                    WindowShapes.drawIcon(guiGraphics, currentShape(), x + 1, y + 1, width - 4, 0xFFFFFFFF.toInt())
                }
            }
        )
        btnShape.active = menu.dummy != null

        // Botão do ranking (aparece somente com o módulo de treino instalado)
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
                override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
                    drawSlotButton(guiGraphics, x, y, width, isHovered, active)
                    drawTrophy(guiGraphics, x + (width - TROPHY_SIZE) / 2, y + (height - TROPHY_SIZE) / 2)
                }
            }
        )
        btnRank.visible = menu.hasUpgrade()

        updateButtonState()
    }

    // ------------------------------------------------------------------------
    // Formato da janela + ranking
    // ------------------------------------------------------------------------

    private fun currentShape(): WindowShapeType =
        pendingShape ?: menu.dummy?.windowShape ?: WindowShapeType.DEFAULT

    private fun changeShape(previous: Boolean) {
        if (menu.dummy == null) return
        val newShape = if (previous) currentShape().previous() else currentShape().next()
        pendingShape = newShape
        pendingShapeTicks = 0
        PacketDistributor.sendToServer(ServerboundSetWindowShapePayload(newShape))
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
        minecraft?.setScreen(RankScreen(this, category))
    }

    /** Fundo no estilo de um slot do inventário. */
    private fun drawSlotButton(g: GuiGraphics, x: Int, y: Int, size: Int, hovered: Boolean, active: Boolean) {
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

    /** Pequeno troféu em pixel art. */
    private fun drawTrophy(g: GuiGraphics, x: Int, y: Int) {
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

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
    }

    override fun containerTick() {
        super.containerTick()
        updateButtonState()
    }

    private fun updateButtonState() {
        val hasUpgrade = menu.hasUpgrade()
        btnMode.visible = hasUpgrade

        if (hasUpgrade) {
            menu.dummy?.trainingModuleConfig?.trainingModo?.let { syncedMode ->
                val modoAtual = pendingMode?.let { pending ->
                    if (syncedMode == pending || ++pendingModeTicks >= MODE_SYNC_TIMEOUT_TICKS) {
                        pendingMode = null
                        pendingModeTicks = 0
                        syncedMode
                    } else {
                        pending
                    }
                } ?: currentModuleMode() ?: syncedMode

                if (modoAtual != lastMode) {
                    lastMode = modoAtual
                    btnMode.message = modeLabel(modoAtual)
                }
            }

        } else {
            pendingMode = null
            pendingModeTicks = 0
            lastMode = null
        }

        btnRank.visible = hasUpgrade
        updateShapeButton()
        updateDetailedDamageButton()
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
        PacketDistributor.sendToServer(ServerboundSetDetailedDamageWindowPayload(enabled))
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
        PacketDistributor.sendToServer(ServerboundChangeModePayload(newMode))
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

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.setShaderTexture(0, TEXTURE)
        val x = this.leftPos
        val y = this.topPos

        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight)

        val dummy = menu.dummy
        if (dummy != null) {
            val x1 = x + 26
            val y1 = y + 8
            val x2 = x + 74
            val y2 = y + 72


            InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                x1, y1, x2, y2,
                25, // Escala (Tamanho do Dummy)
                0.0625f, // Y Offset
                mouseX.toFloat(),
                mouseY.toFloat(),
                dummy
            )
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTrainingModulePlaceholder(guiGraphics)
        this.renderTooltip(guiGraphics, mouseX, mouseY)
        if (btnDetailedDamage.isHovered) {
            guiGraphics.renderTooltip(
                font,
                detailedDamageTooltip(detailedDamageEnabled),
                mouseX,
                mouseY
            )
        }
        if (btnShape.isHovered) {
            guiGraphics.renderComponentTooltip(font, shapeTooltip(currentShape()), mouseX, mouseY)
        }
        if (btnRank.visible && btnRank.isHovered) {
            guiGraphics.renderComponentTooltip(
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

    private fun renderTrainingModulePlaceholder(guiGraphics: GuiGraphics) {
        val slot = menu.slots.getOrNull(TRAINING_MODULE_SLOT_INDEX) ?: return
        if (slot.hasItem()) return

        val x = leftPos + slot.x
        val y = topPos + slot.y
        guiGraphics.blit(
            TRAINING_MODULE_PLACEHOLDER,
            x,
            y,
            0f,
            0f,
            SLOT_SIZE,
            SLOT_SIZE,
            SLOT_SIZE,
            SLOT_SIZE
        )
    }

    companion object {
        private const val TRAINING_MODULE_SLOT_INDEX = 4
        private const val SLOT_SIZE = 16
        private const val DETAILED_BUTTON_SIZE = 14
        private const val DETAILED_BUTTON_GAP = 2
        private const val DETAILED_BUTTON_LABEL = "D"
        private const val MODE_SYNC_TIMEOUT_TICKS = 40
        private const val DETAILED_SYNC_TIMEOUT_TICKS = 40
        private const val SHAPE_SYNC_TIMEOUT_TICKS = 40

        // Botões do tamanho de um slot, alinhados à grade de 18px ao lado do slot do módulo (152, 62)
        private const val SHAPE_BUTTON_X = 134
        private const val SHAPE_BUTTON_Y = 62
        private const val RANK_BUTTON_X = 134
        private const val RANK_BUTTON_Y = 44

        private const val TROPHY_SIZE = 11
        private val TROPHY_PIXELS = listOf(
            "..#######..",
            "##.#####.##",
            "#..#####..#",
            "##.#####.##",
            "...#####...",
            "....###....",
            ".....#.....",
            ".....#.....",
            "....###....",
            "...#####...",
            "...#####..."
        )
    }
}