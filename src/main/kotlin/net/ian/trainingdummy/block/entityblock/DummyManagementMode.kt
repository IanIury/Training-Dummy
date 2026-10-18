package net.ian.trainingdummy.block.entityblock

enum class DummyManagementMode(val translationKey: String) {
    HEALTH("gui.trainingdummy.mode.health"),
    TIMED_HEALTH("gui.trainingdummy.mode.timed_health"),
    SINGLE_HIT("gui.trainingdummy.mode.single_hit"),
    DPS("gui.trainingdummy.mode.dps"),
    DAMAGE_TARGET("gui.trainingdummy.mode.damage_target")
}
