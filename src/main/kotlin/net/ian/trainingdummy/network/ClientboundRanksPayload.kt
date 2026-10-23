package net.ian.trainingdummy.network

import net.ian.trainingdummy.rank.ClientRankCache
import net.ian.trainingdummy.rank.RankEntry
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.network.handling.IPayloadContext

/**
 * Rankings enviados pelo servidor: uma lista (top 100) por categoria + a posição/valor
 * do próprio jogador em cada uma (posição 0 = ainda sem ranking).
 */
class ClientboundRanksPayload(
    val lists: List<List<RankEntry>>,
    val myPositions: List<Int>,
    val myValues: List<Float>
) : CustomPacketPayload {

    companion object {
        val TYPE = CustomPacketPayload.Type<ClientboundRanksPayload>(
            ResourceLocation.fromNamespaceAndPath("trainingdummy", "ranks")
        )

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, ClientboundRanksPayload> =
            CustomPacketPayload.codec(
                { payload, buf ->
                    buf.writeVarInt(payload.lists.size)
                    payload.lists.forEachIndexed { index, list ->
                        buf.writeVarInt(list.size)
                        list.forEach { entry ->
                            buf.writeUUID(entry.uuid)
                            buf.writeUtf(entry.name, 32)
                            buf.writeFloat(entry.value)
                        }
                        buf.writeVarInt(payload.myPositions.getOrElse(index) { 0 })
                        buf.writeFloat(payload.myValues.getOrElse(index) { 0f })
                    }
                },
                { buf ->
                    val categories = buf.readVarInt()
                    val lists = ArrayList<List<RankEntry>>(categories)
                    val positions = ArrayList<Int>(categories)
                    val values = ArrayList<Float>(categories)
                    repeat(categories) {
                        val size = buf.readVarInt()
                        val list = ArrayList<RankEntry>(size)
                        repeat(size) {
                            list.add(RankEntry(buf.readUUID(), buf.readUtf(32), buf.readFloat()))
                        }
                        lists.add(list)
                        positions.add(buf.readVarInt())
                        values.add(buf.readFloat())
                    }
                    ClientboundRanksPayload(lists, positions, values)
                }
            )

        fun handle(payload: ClientboundRanksPayload, context: IPayloadContext) {
            context.enqueueWork { ClientRankCache.update(payload) }
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}
