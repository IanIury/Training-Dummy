package net.ian.trainingdummy.screen.custom

import com.mojang.blaze3d.systems.RenderSystem
import net.ian.trainingdummy.TrainingDummy
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class DummyScreen(
    menu: DummyMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<DummyMenu>(menu, playerInventory, title) {

    private val TEXTURE = ResourceLocation.fromNamespaceAndPath(TrainingDummy.ID, "textures/gui/training_dummy_inicial_gui.png")

    private lateinit var btnDPSMode: Button
    private lateinit var btnTTKMode: Button

    override fun init() {
        super.init()
        this.imageWidth = 176
        this.imageHeight = 166

        // Adiciona botões de navegação para os novos modos (só ativos com upgrade)
        btnDPSMode = this.addRenderableWidget(
            Button.builder(Component.literal("Modo DPS")) {
                // Ação: Abre a janela do modo DPS ou envia pacote para o servidor
            }.bounds(this.leftPos + 106, this.topPos + 18, 60, 20).build()
        )

        btnTTKMode = this.addRenderableWidget(
            Button.builder(Component.literal("Modo Vida")) {
                // Ação: Abre a janela do modo de tempo até matar (TTK)
            }.bounds(this.leftPos + 106, this.topPos + 42, 60, 20).build()
        )

        // Atualiza a visibilidade inicial baseada no upgrade
        updateButtonVisibility()
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
    }

    override fun containerTick() {
        super.containerTick()
        updateButtonVisibility()
    }

    private fun updateButtonVisibility() {
        val hasUpgrade = menu.hasUpgrade()
        btnDPSMode.visible = hasUpgrade
        btnTTKMode.visible = hasUpgrade
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.setShaderTexture(0, TEXTURE)
        val x = this.leftPos
        val y = this.topPos

        // 1. Desenha a imagem de fundo do inventário
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight)

        // 2. Renderiza a Entidade no espaço preto[cite: 1]
        val dummy = menu.dummy
        if (dummy != null) {
            // Coordenadas da caixa preta da sua textura:
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
        this.renderTooltip(guiGraphics, mouseX, mouseY)
    }
}