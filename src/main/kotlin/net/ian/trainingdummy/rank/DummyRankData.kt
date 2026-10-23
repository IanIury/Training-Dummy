package net.ian.trainingdummy.rank

import net.ian.trainingdummy.network.ClientboundRanksPayload
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID

/** Linha do ranking enviada ao cliente. */
data class RankEntry(val uuid: UUID, val name: String, val value: Float)

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

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        val list = ListTag()
        players.forEach { (uuid, stats) ->
            val entry = CompoundTag()
            entry.putUUID("uuid", uuid)
            entry.putString("name", stats.name)
            entry.putFloat("best_dps", stats.bestDps)
            entry.putFloat("max_hit", stats.maxHit)
            entry.putLong("total_hits", stats.totalHits)
            list.add(entry)
        }
        tag.put("players", list)
        return tag
    }

    companion object {
        const val TOP_LIMIT = 100
        private const val DATA_NAME = "trainingdummy_ranks"

        private fun load(tag: CompoundTag, registries: HolderLookup.Provider): DummyRankData {
            val data = DummyRankData()
            val list = tag.getList("players", Tag.TAG_COMPOUND.toInt())
            for (i in 0 until list.size) {
                val entry = list.getCompound(i)
                if (!entry.hasUUID("uuid")) continue
                data.players[entry.getUUID("uuid")] = PlayerStats(
                    name = entry.getString("name"),
                    bestDps = entry.getFloat("best_dps"),
                    maxHit = entry.getFloat("max_hit"),
                    totalHits = entry.getLong("total_hits")
                )
            }
            return data
        }

        fun get(server: MinecraftServer): DummyRankData =
            server.overworld().dataStorage.computeIfAbsent(
                Factory({ DummyRankData() }, { tag, registries -> load(tag, registries) }, null),
                DATA_NAME
            )
    }
}
