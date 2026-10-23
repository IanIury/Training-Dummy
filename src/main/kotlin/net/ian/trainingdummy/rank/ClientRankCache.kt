package net.ian.trainingdummy.rank

import net.ian.trainingdummy.network.ClientboundRanksPayload

/** Cache do lado do cliente com os últimos rankings recebidos (sem referências a classes só-cliente). */
object ClientRankCache {

    private var lists: List<List<RankEntry>> = emptyList()
    private var myPositions: List<Int> = emptyList()
    private var myValues: List<Float> = emptyList()

    /** `true` depois que o servidor respondeu ao menos uma vez. */
    var loaded: Boolean = false
        private set

    fun update(payload: ClientboundRanksPayload) {
        lists = payload.lists
        myPositions = payload.myPositions
        myValues = payload.myValues
        loaded = true
    }

    fun entries(category: RankCategory): List<RankEntry> = lists.getOrNull(category.ordinal) ?: emptyList()

    fun myPosition(category: RankCategory): Int = myPositions.getOrNull(category.ordinal) ?: 0

    fun myValue(category: RankCategory): Float = myValues.getOrNull(category.ordinal) ?: 0f
}
