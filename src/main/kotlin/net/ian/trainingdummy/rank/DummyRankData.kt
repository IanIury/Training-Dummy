package net.ian.trainingdummy.rank

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.ian.trainingdummy.network.ClientboundRanksPayload
import net.minecraft.core.UUIDUtil
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.saveddata.SavedDataType
import java.util.UUID

/** Linha do ranking enviada ao cliente. */
data class RankEntry(val uuid: UUID, val name: String, val value: Float)

data class SerializedPlayerEntry(
    val uuid: UUID,
    val name: String,
    val bestDps: Float,
    val maxHit: Float,
    val totalHits: Long
)

/**
 * Ranking persistente do treino. Fica salvo no mundo (`data/trainingdummy_ranks.dat`), portanto cada
 * mapa/servidor possui o seu próprio ranking, que é carregado automaticamente ao abrir o mundo.
 */
class DummyRankData : SavedData() {

    class PlayerStats(
        var name: String,
        var bestDps: Float = 0f,
        var maxHit: Float = 0f,
        var totalHits: Long = 0L
    ) {
        fun value(category: RankCategory): Float = when (category) {
            RankCategory.BEST_DPS -> bestDps
            RankCategory.MAX_HIT -> maxHit
            RankCategory.TOTAL_HITS -> totalHits.toFloat()
        }
    }

    private val players = HashMap<UUID, PlayerStats>()

    private fun stats(uuid: UUID, name: String): PlayerStats {
        val stats = players.getOrPut(uuid) { PlayerStats(name) }
        stats.name = name
        return stats
    }

    /** Registra um golpe dado em um dummy com módulo de treino. */
    fun recordHit(uuid: UUID, name: String, damage: Float) {
        val stats = stats(uuid, name)
        stats.totalHits++
        if (damage > stats.maxHit) stats.maxHit = damage
        setDirty()
    }

    /** Registra o resultado de um teste de DPS. Retorna `true` se for um novo recorde pessoal. */
    fun recordDps(uuid: UUID, name: String, dps: Float): Boolean {
        val stats = stats(uuid, name)
        setDirty()
        if (dps > stats.bestDps) {
            stats.bestDps = dps
            return true
        }
        return false
    }

    /** Top [limit] jogadores da categoria, em ordem decrescente (ignora valores zerados). */
    fun top(category: RankCategory, limit: Int = TOP_LIMIT): List<RankEntry> =
        players.entries
            .filter { it.value.value(category) > 0f }
            .sortedWith(
                compareByDescending<Map.Entry<UUID, PlayerStats>> { it.value.value(category) }
                    .thenBy { it.value.name.lowercase() }
            )
            .take(limit)
            .map { RankEntry(it.key, it.value.name, it.value.value(category)) }

    /** Posição (1-based) do jogador na categoria, ou 0 se não ranqueado. */
    fun position(category: RankCategory, uuid: UUID): Int {
        val mine = players[uuid]?.value(category) ?: return 0
        if (mine <= 0f) return 0
        return players.values.count { it.value(category) > mine } + 1
    }

    fun valueOf(category: RankCategory, uuid: UUID): Float = players[uuid]?.value(category) ?: 0f

    fun buildPayload(viewer: UUID): ClientboundRanksPayload {
        val categories = RankCategory.entries
        return ClientboundRanksPayload(
            lists = categories.map { top(it) },
            myPositions = categories.map { position(it, viewer) },
            myValues = categories.map { valueOf(it, viewer) }
        )
    }

    companion object {
        const val TOP_LIMIT = 100

        private val ENTRY_CODEC: Codec<SerializedPlayerEntry> = RecordCodecBuilder.create { instance ->
            instance.group(
                UUIDUtil.CODEC.fieldOf("uuid").forGetter(SerializedPlayerEntry::uuid),
                Codec.STRING.fieldOf("name").forGetter(SerializedPlayerEntry::name),
                Codec.FLOAT.fieldOf("best_dps").forGetter(SerializedPlayerEntry::bestDps),
                Codec.FLOAT.fieldOf("max_hit").forGetter(SerializedPlayerEntry::maxHit),
                Codec.LONG.fieldOf("total_hits").forGetter(SerializedPlayerEntry::totalHits)
            ).apply(instance, ::SerializedPlayerEntry)
        }

        val CODEC: Codec<DummyRankData> = RecordCodecBuilder.create { instance ->
            instance.group(
                ENTRY_CODEC.listOf().fieldOf("players").forGetter { data ->
                    data.players.map { (uuid, stats) ->
                        SerializedPlayerEntry(uuid, stats.name, stats.bestDps, stats.maxHit, stats.totalHits)
                    }
                }
            ).apply(instance) { entries ->
                val data = DummyRankData()
                for (entry in entries) {
                    data.players[entry.uuid] = PlayerStats(
                        name = entry.name,
                        bestDps = entry.bestDps,
                        maxHit = entry.maxHit,
                        totalHits = entry.totalHits
                    )
                }
                data
            }
        }

        val TYPE: SavedDataType<DummyRankData> = SavedDataType(
            Identifier.fromNamespaceAndPath("trainingdummy", "ranks"),
            ::DummyRankData,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
        )

        fun get(server: MinecraftServer): DummyRankData =
            server.overworld().dataStorage.computeIfAbsent(TYPE)
    }
}
