package net.ian.trainingdummy.screen.custom

import net.ian.trainingdummy.block.entityblock.DummyManagementBlockEntity
import net.ian.trainingdummy.screen.ModMenuTypes
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.ItemStack

class DummyManagementMenu(
    containerId: Int,
    playerInventory: Inventory,
    val blockEntity: DummyManagementBlockEntity?,
    val blockPos: BlockPos
) : AbstractContainerMenu(ModMenuTypes.DUMMY_MANAGEMENT_MENU.get(), containerId) {

    private constructor(containerId: Int, playerInventory: Inventory, clientData: ClientData) : this(
        containerId,
        playerInventory,
        clientData.blockEntity,
        clientData.blockPos
    )

    constructor(containerId: Int, playerInventory: Inventory, extraData: FriendlyByteBuf) : this(
        containerId,
        playerInventory,
        readClientData(playerInventory, extraData)
    )

    private val clientValues = IntArray(DATA_COUNT)
    private val data = object : ContainerData {
        override fun get(index: Int): Int =
            if (playerInventory.player.level().isClientSide) clientValues[index]
            else blockEntity?.menuValue(index) ?: 0

        override fun set(index: Int, value: Int) {
            clientValues[index] = value
        }

        override fun getCount(): Int = DATA_COUNT
    }

    init {
        addDataSlots(data)
    }

    override fun stillValid(player: Player): Boolean =
        blockEntity != null &&
            player.level() === blockEntity.level &&
            player.distanceToSqr(
                blockPos.x + 0.5,
                blockPos.y + 0.5,
                blockPos.z + 0.5
            ) <= MAX_DISTANCE_SQUARED

    fun value(index: Int): Int = data.get(index)

    override fun quickMoveStack(player: Player, index: Int): ItemStack = ItemStack.EMPTY

    companion object {
        private const val DATA_COUNT = 18
        private const val MAX_DISTANCE_SQUARED = 64.0

        private data class ClientData(
            val blockEntity: DummyManagementBlockEntity?,
            val blockPos: BlockPos
        )

        private fun readClientData(playerInventory: Inventory, buffer: FriendlyByteBuf): ClientData {
            val pos = buffer.readBlockPos()
            return ClientData(
                playerInventory.player.level().getBlockEntity(pos) as? DummyManagementBlockEntity,
                pos
            )
        }
    }
}
