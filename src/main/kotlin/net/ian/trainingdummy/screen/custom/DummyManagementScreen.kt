package net.ian.trainingdummy.screen.custom

import net.ian.trainingdummy.block.entityblock.DummyManagementAction
import net.ian.trainingdummy.block.entityblock.DummyManagementMode
import net.ian.trainingdummy.network.ServerboundDummyManagementActionPayload
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.neoforged.neoforge.client.network.ClientPacketDistributor
import org.lwjgl.glfw.GLFW
import java.util.Locale
import kotlin.math.roundToInt

class DummyManagementScreen(
    menu: DummyManagementMenu,
    inventory: Inventory,
    title: Component
) : AbstractContainerScreen<DummyManagementMenu>(menu, inventory, title, 286, 220) {
    private var modeButton: Button? = null
    private var inversionButton: Button? = null
    private val toggleButtons = mutableListOf<Pair<Button, Int>>()
    private val valueFields = mutableListOf<Pair<EditBox, Control>>()
    private var displayedMode = -1

    init {
        inventoryLabelY = imageHeight + 20
    }

    override fun init() {
        super.init()
        toggleButtons.clear()
        valueFields.clear()
        displayedMode = menu.value(0)

        modeButton = addRenderableWidget(
            Button.builder(modeLabel()) { send(DummyManagementAction.NEXT_MODE) }
                .bounds(leftPos + 10, topPos + 22, imageWidth - 20, 22)
                .build()
        )

        controlsFor(currentMode()).forEachIndexed { index, control ->
            val y = topPos + 52 + index * ROW_HEIGHT
            if (control.increase == null) {
                val button = Button.builder(toggleLabel(control.dataIndex)) {
                    send(control.decrease)
                }.bounds(leftPos + imageWidth - 78, y, 68, 20).build()
                toggleButtons += button to control.dataIndex
                addRenderableWidget(button)
            } else {
                val field = EditBox(
                    font,
                    leftPos + imageWidth - 125,
                    y,
                    72,
                    20,
                    Component.translatable(control.labelKey)
                )
                field.setMaxLength(10)
                field.value = formatValue(control.dataIndex, control.decimalPlaces)
                valueFields += field to control
                addRenderableWidget(field)
                addRenderableWidget(
                    Button.builder(Component.translatable("gui.trainingdummy.apply")) {
                        submitValue(field, control)
                    }.bounds(leftPos + imageWidth - 48, y, 40, 20).build()
                )
            }
        }

        inversionButton = addRenderableWidget(
            object : Button(
                leftPos + imageWidth - 27,
                topPos + 2,
                22,
                16,
                Component.translatable("gui.trainingdummy.invert_short"),
                OnPress { send(DummyManagementAction.TOGGLE_OUTPUT_INVERSION) },
                CreateNarration { inversionLabel().copy() }
            ) {
                override fun extractContents(
                    extractor: GuiGraphicsExtractor,
                    mouseX: Int,
                    mouseY: Int,
                    partialTick: Float
                ) {
                    val inverted = menu.value(17) == 1
                    val hovered = isMouseOver(mouseX.toDouble(), mouseY.toDouble())
                    val background = when {
                        inverted && hovered -> 0xFF2F765F.toInt()
                        inverted -> 0xFF245544.toInt()
                        hovered -> 0xFF34485B.toInt()
                        else -> 0xFF263747.toInt()
                    }
                    val border = if (inverted) 0xFF75E6B4.toInt() else 0xFF66849A.toInt()
                    val text = if (inverted) 0xFFFFFFFF.toInt() else 0xFFC9D8E3.toInt()

                    extractor.fill(x, y, x + width, y + height, background)
                    extractor.outline(x, y, width, height, border)
                    extractor.centeredText(font, message, x + width / 2, y + 4, text)
                }
            }.also {
                it.setTooltip(Tooltip.create(inversionLabel()))
            }
        )
    }

    override fun containerTick() {
        super.containerTick()
        if (displayedMode != menu.value(0)) rebuildWidgets()
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            valueFields.firstOrNull { (field, _) -> field.isFocused }?.let { (field, control) ->
                submitValue(field, control)
                return true
            }
        }
        return super.keyPressed(event)
    }

    override fun extractBackground(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xF0101724.toInt())
        guiGraphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + 20, 0xFF1C2A3C.toInt())
        guiGraphics.outline(leftPos, topPos, imageWidth, imageHeight, 0xFF54C8E8.toInt())
        guiGraphics.text(
            font,
            title,
            leftPos + 10,
            topPos + 8,
            0xFFEAF6FF.toInt()
        )

        controlsFor(currentMode()).forEachIndexed { index, control ->
            val y = topPos + 58 + index * ROW_HEIGHT
            guiGraphics.text(
                font,
                Component.translatable(control.labelKey),
                leftPos + 12,
                y,
                0xFFE4EDF5.toInt()
            )
        }
        guiGraphics.text(
            font,
            Component.translatable("gui.trainingdummy.redstone_output", menu.value(10)),
            leftPos + 12,
            topPos + imageHeight - 26,
            0xFF8BE9A8.toInt()
        )
        when (currentMode()) {
            DummyManagementMode.DPS -> guiGraphics.text(
                font,
                Component.translatable(
                    "gui.trainingdummy.dps_status",
                    menu.value(13),
                    menu.value(12),
                    menu.value(8),
                    formatValue(14, 1),
                    formatValue(7, 1)
                ),
                leftPos + 12,
                topPos + imageHeight - 40,
                0xFFB9C9DC.toInt()
            )
            DummyManagementMode.DAMAGE_TARGET -> guiGraphics.text(
                font,
                Component.translatable("gui.trainingdummy.damage_tolerance"),
                leftPos + 12,
                topPos + imageHeight - 40,
                0xFFB9C9DC.toInt()
            )
            else -> guiGraphics.text(
                font,
                Component.translatable(
                    "gui.trainingdummy.virtual_health",
                    menu.value(11),
                    menu.value(1)
                ),
                leftPos + 12,
                topPos + imageHeight - 40,
                0xFFB9C9DC.toInt()
            )
        }
        guiGraphics.text(
            font,
            if (menu.value(15) == 1) {
                Component.translatable("gui.trainingdummy.dummy_connected")
            } else {
                Component.translatable("gui.trainingdummy.dummy_disconnected")
            },
            leftPos + imageWidth - 12 - font.width(
                if (menu.value(15) == 1) Component.translatable("gui.trainingdummy.dummy_connected")
                else Component.translatable("gui.trainingdummy.dummy_disconnected")
            ),
            topPos + imageHeight - 26,
            if (menu.value(15) == 1) 0xFF8BE9A8.toInt() else 0xFFFFB86C.toInt()
        )
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        modeButton?.message = modeLabel()
        inversionButton?.setTooltip(Tooltip.create(inversionLabel()))
        toggleButtons.forEach { (button, index) -> button.message = toggleLabel(index) }
        valueFields.forEach { (field, control) ->
            if (!field.isFocused) field.value = formatValue(control.dataIndex, control.decimalPlaces)
        }
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick)
    }

    override fun extractLabels(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) = Unit

    private fun currentMode(): DummyManagementMode =
        DummyManagementMode.entries.getOrElse(menu.value(0)) { DummyManagementMode.HEALTH }

    private fun modeLabel(): Component =
        Component.translatable("gui.trainingdummy.mode", Component.translatable(currentMode().translationKey))

    private fun inversionLabel(): Component = Component.translatable(
        "gui.trainingdummy.invert_output",
        Component.translatable(
            if (menu.value(17) == 1) "gui.trainingdummy.enabled" else "gui.trainingdummy.disabled"
        )
    )

    private fun controlsFor(mode: DummyManagementMode): List<Control> = when (mode) {
        DummyManagementMode.HEALTH -> listOf(
            Control("gui.trainingdummy.max_health", 1, DummyManagementAction.MAX_HEALTH_DOWN, DummyManagementAction.MAX_HEALTH_UP),
            Control("gui.trainingdummy.reset_delay", 3, DummyManagementAction.RESET_DOWN, DummyManagementAction.RESET_UP),
            Control("gui.trainingdummy.auto_regeneration", 4, DummyManagementAction.TOGGLE_REGEN),
            Control("gui.trainingdummy.regeneration_rate", 5, DummyManagementAction.REGEN_RATE_DOWN, DummyManagementAction.REGEN_RATE_UP)
        )
        DummyManagementMode.TIMED_HEALTH -> listOf(
            Control("gui.trainingdummy.max_health", 1, DummyManagementAction.MAX_HEALTH_DOWN, DummyManagementAction.MAX_HEALTH_UP),
            Control("gui.trainingdummy.duration", 2, DummyManagementAction.DURATION_DOWN, DummyManagementAction.DURATION_UP),
            Control("gui.trainingdummy.reset_delay", 3, DummyManagementAction.RESET_DOWN, DummyManagementAction.RESET_UP),
            Control("gui.trainingdummy.live_output", 6, DummyManagementAction.TOGGLE_TIMED_OUTPUT)
        )
        DummyManagementMode.SINGLE_HIT -> listOf(
            Control("gui.trainingdummy.health_per_hit", 1, DummyManagementAction.MAX_HEALTH_DOWN, DummyManagementAction.MAX_HEALTH_UP),
            Control("gui.trainingdummy.single_hit_reset", 3, DummyManagementAction.RESET_DOWN, DummyManagementAction.RESET_UP)
        )
        DummyManagementMode.DPS -> listOf(
            Control("gui.trainingdummy.dps_window", 9, DummyManagementAction.DPS_WINDOW_DOWN, DummyManagementAction.DPS_WINDOW_UP),
            Control("gui.trainingdummy.target_average", 7, DummyManagementAction.TARGET_AVERAGE_DOWN, DummyManagementAction.TARGET_AVERAGE_UP, 1),
            Control("gui.trainingdummy.target_hits", 8, DummyManagementAction.TARGET_HITS_DOWN, DummyManagementAction.TARGET_HITS_UP),
            Control("gui.trainingdummy.reset_delay", 3, DummyManagementAction.RESET_DOWN, DummyManagementAction.RESET_UP)
        )
        DummyManagementMode.DAMAGE_TARGET -> listOf(
            Control("gui.trainingdummy.target_damage", 16, DummyManagementAction.TARGET_DAMAGE_DOWN, DummyManagementAction.TARGET_DAMAGE_UP, 1),
            Control("gui.trainingdummy.signal_reset", 3, DummyManagementAction.RESET_DOWN, DummyManagementAction.RESET_UP)
        )
    }

    private fun formatValue(index: Int, decimalPlaces: Int = 0): String {
        val rawValue = when (index) {
            7, 16 -> menu.value(index) / 10f
            else -> menu.value(index).toFloat()
        }
        return if (decimalPlaces == 1) String.format(Locale.ROOT, "%.1f", rawValue) else rawValue.toInt().toString()
    }

    private fun toggleLabel(index: Int): Component = Component.translatable(
        "gui.trainingdummy.toggle_value",
        Component.translatable(if (menu.value(index) == 1) "gui.trainingdummy.enabled" else "gui.trainingdummy.disabled")
    )

    private fun submitValue(field: EditBox, control: Control) {
        val parsed = field.value.replace(',', '.').toDoubleOrNull() ?: return
        if (!parsed.isFinite() || parsed < 0.0) return
        val scale = if (control.decimalPlaces == 1) 10.0 else 1.0
        val scaled = (parsed * scale).roundToInt()
        ClientPacketDistributor.sendToServer(
            ServerboundDummyManagementActionPayload(menu.blockPos, DummyManagementAction.SET_VALUE, control.dataIndex, scaled)
        )
        field.isFocused = false
    }

    private fun send(action: DummyManagementAction) {
        ClientPacketDistributor.sendToServer(ServerboundDummyManagementActionPayload(menu.blockPos, action))
    }

    private data class Control(
        val labelKey: String,
        val dataIndex: Int,
        val decrease: DummyManagementAction,
        val increase: DummyManagementAction? = null,
        val decimalPlaces: Int = 0
    )

    companion object {
        private const val ROW_HEIGHT = 25
    }
}
