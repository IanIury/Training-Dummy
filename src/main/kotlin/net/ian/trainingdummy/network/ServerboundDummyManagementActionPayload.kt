package net.ian.trainingdummy.network

import net.ian.trainingdummy.block.entityblock.DummyManagementAction
import net.ian.trainingdummy.block.entityblock.DummyManagementBlockEntity
import net.ian.trainingdummy.screen.custom.DummyManagementMenu
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.network.handling.IPayloadContext

class ServerboundDummyManagementActionPayload(
    val blockPos: BlockPos,
    val action: DummyManagementAction,
    val valueIndex: Int = -1,
    val value: Int = 0
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ServerboundDummyManagementActionPayload>(
            ResourceLocation.fromNamespaceAndPath("trainingdummy", "dummy_management_action")
        )

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, ServerboundDummyManagementActionPayload> =
            CustomPacketPayload.codec(
                { payload, buffer ->
                    buffer.writeBlockPos(payload.blockPos)
                    buffer.writeEnum(payload.action)
                    buffer.writeVarInt(payload.valueIndex)
                    buffer.writeVarInt(payload.value)
                },
                { buffer ->
                    ServerboundDummyManagementActionPayload(
                        buffer.readBlockPos(),
                        buffer.readEnum(DummyManagementAction::class.java),
                        buffer.readVarInt(),
                        buffer.readVarInt()
                    )
                }
            )

        fun handle(payload: ServerboundDummyManagementActionPayload, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player()
                val menu = player.containerMenu as? DummyManagementMenu ?: return@enqueueWork
                if (!menu.stillValid(player) || menu.blockPos != payload.blockPos) return@enqueueWork

                val blockEntity = player.level().getBlockEntity(payload.blockPos)
                    as? DummyManagementBlockEntity ?: return@enqueueWork
                if (payload.action == DummyManagementAction.SET_VALUE) {
                    blockEntity.setMenuValue(payload.valueIndex, payload.value)
                } else {
                    blockEntity.applyAction(payload.action)
                }
                menu.broadcastChanges()
            }
        }
    }
}
