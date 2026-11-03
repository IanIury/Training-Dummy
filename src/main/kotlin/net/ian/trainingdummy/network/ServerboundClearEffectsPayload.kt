package net.ian.trainingdummy.network

import net.ian.trainingdummy.screen.custom.DummyMenu
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.network.handling.IPayloadContext

class ServerboundClearEffectsPayload : CustomPacketPayload {

    companion object {
        val TYPE = CustomPacketPayload.Type<ServerboundClearEffectsPayload>(
            Identifier.fromNamespaceAndPath("trainingdummy", "clear_effects")
        )

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, ServerboundClearEffectsPayload> =
            StreamCodec.unit(ServerboundClearEffectsPayload())

        fun handle(payload: ServerboundClearEffectsPayload, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player()
                val menu = player.containerMenu
                if (menu is DummyMenu && menu.stillValid(player)) {
                    menu.dummy?.clearAllEffects()
                }
            }
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}
