package net.ian.trainingdummy.network

import net.ian.trainingdummy.screen.custom.DummyMenu
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.handling.IPayloadContext

class ServerboundSetEffectsWindowPayload(
    val enabled: Boolean
) : CustomPacketPayload {

    companion object {
        val TYPE = CustomPacketPayload.Type<ServerboundSetEffectsWindowPayload>(
            ResourceLocation.fromNamespaceAndPath("trainingdummy", "set_effects_window")
        )

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, ServerboundSetEffectsWindowPayload> =
            CustomPacketPayload.codec(
                { payload, buf -> buf.writeBoolean(payload.enabled) },
                { buf -> ServerboundSetEffectsWindowPayload(buf.readBoolean()) }
            )

        fun handle(payload: ServerboundSetEffectsWindowPayload, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player()
                val menu = player.containerMenu
                if (menu is DummyMenu && menu.stillValid(player)) {
                    val dummy = menu.dummy ?: return@enqueueWork
                    dummy.effectsWindow = payload.enabled
                    if (payload.enabled && player is ServerPlayer) {
                        for (effect in dummy.activeEffects) {
                            player.connection.send(ClientboundUpdateMobEffectPacket(dummy.id, effect, false))
                        }
                    }
                }
            }
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}
