package net.ian.trainingdummy.network

import net.ian.trainingdummy.screen.custom.DummyMenu
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.network.handling.IPayloadContext

class ServerboundSetDetailedDamageWindowPayload(
    val enabled: Boolean
) : CustomPacketPayload {

    companion object {
        val TYPE = CustomPacketPayload.Type<ServerboundSetDetailedDamageWindowPayload>(
            ResourceLocation.fromNamespaceAndPath("trainingdummy", "set_detailed_damage_window")
        )

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, ServerboundSetDetailedDamageWindowPayload> =
            CustomPacketPayload.codec(
                { payload, buf -> buf.writeBoolean(payload.enabled) },
                { buf -> ServerboundSetDetailedDamageWindowPayload(buf.readBoolean()) }
            )

        fun handle(payload: ServerboundSetDetailedDamageWindowPayload, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player()
                val menu = player.containerMenu
                if (menu is DummyMenu && menu.stillValid(player)) {
                    menu.dummy?.detailedDamageWindow = payload.enabled
                }
            }
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}
