package net.ian.trainingdummy.network

import net.ian.trainingdummy.init.enums.TrainingModos
import net.ian.trainingdummy.item.custom.TrainingModuleItem
import net.ian.trainingdummy.screen.custom.DummyMenu
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.network.handling.IPayloadContext

class ServerboundChangeModePayload(
    val modo: TrainingModos
) : CustomPacketPayload {

    companion object {
        val TYPE = CustomPacketPayload.Type<ServerboundChangeModePayload>(
            ResourceLocation.fromNamespaceAndPath("trainingdummy", "change_mode")
        )

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, ServerboundChangeModePayload> = CustomPacketPayload.codec(
            { payload, buf -> buf.writeEnum(payload.modo) },
            { buf -> ServerboundChangeModePayload(buf.readEnum(TrainingModos::class.java)) }
        )

        fun handle(payload: ServerboundChangeModePayload, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player()
                val container = player.containerMenu

                if (container is DummyMenu) {
                    val dummy = container.dummy ?: return@enqueueWork
                    val slotIndex = 4
                    val stack = dummy.dummyInventory.getStackInSlot(slotIndex)

                    if (!stack.isEmpty) {
                        // 1. Aplica o novo modo na tag NBT
                        TrainingModuleItem.setItemMode(stack, payload.modo)

                        // 2. Atualiza o slot do handler
                        dummy.dummyInventory.setStackInSlot(slotIndex, stack)
                        dummy.trainingModo = payload.modo
                        dummy.resetModoState()

                        // 3. Notifica o servidor que o inventário foi alterado (força persistência e sync)
                        container.broadcastChanges()
                    }
                }
            }
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}