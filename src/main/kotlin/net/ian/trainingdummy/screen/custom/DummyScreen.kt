package net.ian.trainingdummy.screen.custom

import com.mojang.blaze3d.systems.RenderSystem
import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.item.custom.TrainingModuleItem
import net.ian.trainingdummy.network.ServerboundChangeModePayload
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.network.PacketDistributor
import java.util.function.Supplier

class DummyScreen(
    menu: DummyMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<DummyMenu>(menu, playerInventory, title) {

    private val TEXTURE = ResourceLocation.fromNamespaceAndPath(TrainingDummy.ID, "textures/gui/training_dummy_inicial_gui.png")

    private lateinit var btnMode: Button

    private var lastMode: TrainingModuleItem.TrainingModos? = null


    override fun init() {
        super.init()
        this.imageWidth = 176
        this.imageHeight = 166



        val stack = menu.dummy?.dummyInventory?.getStackInSlot(4) ?: return
        val modo = TrainingModuleItem.getItemMode(stack)
        this.lastMode = modo

        println("MODO: " + modo.name)

        btnMode = this.addRenderableWidget(

            object : Button(
                this.leftPos + 85,
                this.topPos + 5,
                80,
                20,
                Component.literal("Modo : ${modo.nameD()}"),
                OnPress {},
                CreateNarration { Component.empty() }
            ){

                override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {

                    val isHovered = this.isMouseOver(mouseX.toDouble(), mouseY.toDouble())

                    val backgroundColor = if (isHovered) 0xDD3C3C3C.toInt() else 0xDD1E1E1E.toInt() // Cinza escuro com transparência
                    val borderColor = if (isHovered) 0xFF55FF55.toInt() else 0xFFAAAAAA.toInt() // Verde se hovered, Cinza claro padrão
                    val textColor = if (isHovered) 0xFFFFFF55.toInt() else 0xFFFFFFFF.toInt() // Amarelo se hovered, Branco padrão

                    guiGraphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, backgroundColor)

                    guiGraphics.renderOutline(this.x, this.y, this.width, this.height, borderColor)

                    guiGraphics.drawCenteredString(
                        Minecraft.getInstance().font,
                        this.message,
                        this.x + this.width / 2,
                        this.y + (this.height - 8) / 2,
                        textColor
                    )
                }

                override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
                    if (this.active && this.visible && this.clicked(mouseX, mouseY)) {
                        val isDireito = button == 1
                        val isEsquerdo = button == 0

                        if (isEsquerdo || isDireito) {

                            val stack = menu.dummy?.dummyInventory?.getStackInSlot(4) ?: return true
                            if(stack.isEmpty) return true
                            val modoAtual = TrainingModuleItem.getItemMode(stack)
                            val newModo = if (isDireito) modoAtual.previous() else modoAtual.next()


                            PacketDistributor.sendToServer(ServerboundChangeModePayload(newModo))

                            this.message = Component.literal("Modo: ${newModo.nameD()}")
                            this.createNarration.createNarrationMessage { Component.literal(newModo.nameD()) }
                            return true
                        }
                    }
                    return super.mouseClicked(mouseX, mouseY, button)
                }
            }

        )

        updateButtonState()
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
            val stack = menu.dummy?.dummyInventory?.getStackInSlot(4) ?: ItemStack.EMPTY
            val modoAtual = TrainingModuleItem.getItemMode(stack)

            // Se o item mudou ou o modo do item mudou, atualiza a mensagem do botão
            if (modoAtual != lastMode) {
                lastMode = modoAtual
                btnMode.message = Component.literal("Modo: ${modoAtual.nameD()}")
            }
        } else {
            lastMode = null
        }
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
        this.renderTooltip(guiGraphics, mouseX, mouseY)
    }
}