package net.ian.trainingdummy.rank

import java.util.Locale

/** Categorias do ranking de treino. A ordem define o índice usado na rede. */
enum class RankCategory(val translationKey: String, val decimals: Boolean) {
    /** Melhor DPS de um teste completo (modo DPS). */
    BEST_DPS("gui.trainingdummy.rank.category.dps", true),

    /** Maior dano causado em um único golpe. */
    MAX_HIT("gui.trainingdummy.rank.category.max_hit", true),

    /** Quantidade total de golpes dados nos dummies deste mundo/servidor. */
    TOTAL_HITS("gui.trainingdummy.rank.category.total_hits", false);

    fun next(): RankCategory = entries[(ordinal + 1) % entries.size]

    fun previous(): RankCategory = entries[(ordinal - 1 + entries.size) % entries.size]

    fun format(value: Float): String =
        if (decimals) String.format(Locale.ROOT, "%.1f", value) else value.toLong().toString()

    companion object {
        fun byId(id: Int): RankCategory = entries.getOrElse(id) { BEST_DPS }
    }
}
