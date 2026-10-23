package net.ian.trainingdummy.network

import net.ian.trainingdummy.rank.DummyRankData
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.handling.IPayloadContext

/** O cliente pede ao servidor os rankings atuais (todas as categorias). */
class ServerboundRequestRanksPayload : CustomPacketPayload {

    companion object {
        val TYPE = CustomPacketPayload.Type<ServerboundRequestRanksPayload>(
            Identifier.fromNamespaceAndPath("trainingdummy", "request_ranks")
        )

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, ServerboundRequestRanksPayload> =
            CustomPacketPayload.codec(
                { _, _ -> },
                { ServerboundRequestRanksPayload() }
            )

        fun handle(payload: ServerboundRequestRanksPayload, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player() as? ServerPlayer ?: return@enqueueWork
                val data = DummyRankData.get(player.level().server)
                PacketDistributor.sendToPlayer(player, data.buildPayload(player.uuid))
            }
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}
