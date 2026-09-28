package net.ian.trainingdummy.screen.custom

import net.ian.trainingdummy.entity.DummyEntity
import net.ian.trainingdummy.entity.ModEntities
import net.ian.trainingdummy.item.ModItems
import net.ian.trainingdummy.screen.ModMenuTypes
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.ItemStackHandler
import net.neoforged.neoforge.items.SlotItemHandler

class DummyMenu(
    containerId: Int,
    playerInventory: Inventory,
    val dummy: DummyEntity?
) : AbstractContainerMenu(ModMenuTypes.DUMMY_MENU.get(), containerId) {

    // Slot de Upgrade (1 slot separado)
    val upgradeContainer: Container = SimpleContainer(1)

    // Construtor do lado do Cliente (invocado pelo pacote da rede)
    constructor(containerId: Int, playerInventory: Inventory, extraData: FriendlyByteBuf) : this(
        containerId,
        playerInventory,
        playerInventory.player.level().getEntity(extraData.readInt()) as? DummyEntity
    )

    init {

        if (dummy != null) {

            for (i in 0 until 4) {
                this.addSlot(SlotItemHandler(dummy.dummyInventory, i, 8, 8 + i * 18))
            }
            this.addSlot(SlotItemHandler(dummy.dummyInventory, 4, 115, 30))

            this.addSlot(SlotItemHandler(dummy.dummyInventory, 5, 80, 70))
            this.addSlot(SlotItemHandler(dummy.dummyInventory, 6, 98, 70))

        }else {
            // Fallback caso o Dummy seja nulo no cliente temporariamente
            val dummyInventory = object : ItemStackHandler(5) {}
            for (i in 0 until 4) {
                this.addSlot(SlotItemHandler(dummyInventory, i, 8, 8 + i * 18))
            }
            this.addSlot(SlotItemHandler(dummyInventory, 4, 115, 30))
            this.addSlot(SlotItemHandler(dummyInventory, 5, 80, 80))
            this.addSlot(SlotItemHandler(dummyInventory, 6, 98, 80))

        }

        // 3. Inventário do Jogador (3 linhas x 9 colunas)
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                this.addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }

        // 4. Hotbar do Jogador (1 linha x 9 colunas)
        for (col in 0 until 9) {
            this.addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }
    }

    override fun stillValid(player: Player): Boolean {
        val entity = dummy ?: return  false
        return entity.isAlive
    }

    companion object {
        private const val DUMMY_SLOT_COUNT = 7 // 4 Armaduras + 1 Upgrade

        private const val HOTBAR_SLOT_COUNT = 9
        private const val PLAYER_INVENTORY_ROW_COUNT = 3
        private const val PLAYER_INVENTORY_COLUMN_COUNT = 9
        private const val PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT
        private const val VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT

        private const val DUMMY_FIRST_SLOT_INDEX = 0
        private const val VANILLA_FIRST_SLOT_INDEX = DUMMY_FIRST_SLOT_INDEX + DUMMY_SLOT_COUNT
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        val sourceSlot = slots.getOrNull(index) ?: return ItemStack.EMPTY
        if (!sourceSlot.hasItem()) return ItemStack.EMPTY

        val sourceStack = sourceSlot.item
        val copyOfSourceStack = sourceStack.copy()

        if (index < DUMMY_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, true)) {
                return ItemStack.EMPTY
            }
        }

        else if (index in VANILLA_FIRST_SLOT_INDEX until (VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT)) {
            if (!moveItemStackTo(sourceStack, DUMMY_FIRST_SLOT_INDEX, DUMMY_FIRST_SLOT_INDEX + DUMMY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY
            }
        } else {
            return ItemStack.EMPTY
        }

        if (sourceStack.isEmpty) {
            sourceSlot.set(ItemStack.EMPTY)
        } else {
            sourceSlot.setChanged()
        }

        if (sourceStack.count == copyOfSourceStack.count) {
            return ItemStack.EMPTY
        }

        sourceSlot.onTake(player, sourceStack)
        return copyOfSourceStack
    }

    fun hasUpgrade(): Boolean {
        return !upgradeContainer.getItem(0).isEmpty
    }
}