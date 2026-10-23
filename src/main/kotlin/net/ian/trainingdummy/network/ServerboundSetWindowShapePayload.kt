package net.ian.trainingdummy.network

import net.ian.trainingdummy.init.enums.WindowShapeType
import net.ian.trainingdummy.screen.custom.DummyMenu
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.network.handling.IPayloadContext

/** O cliente escolhe o formato geométrico da janela do dummy aberto no menu. */
class ServerboundSetWindowShapePayload(
    val shape: WindowShapeType
) : CustomPacketPayload {

    companion object {
        val TYPE = CustomPacketPayload.Type<ServerboundSetWindowShapePayload>(
            ResourceLocation.fromNamespaceAndPath("trainingdummy", "set_window_shape")
        )

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, ServerboundSetWindowShapePayload> =
            CustomPacketPayload.codec(
                { payload, buf -> buf.writeVarInt(payload.shape.ordinal) },
                { buf -> ServerboundSetWindowShapePayload(WindowShapeType.byId(buf.readVarInt())) }
            )

        fun handle(payload: ServerboundSetWindowShapePayload, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player()
                val menu = player.containerMenu
                if (menu is DummyMenu && menu.stillValid(player)) {
                    menu.dummy?.windowShape = payload.shape
                }
            }
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}
