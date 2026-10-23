package net.ian.trainingdummy.block.entityblock

import net.ian.trainingdummy.block.ModBlockEntities
import net.ian.trainingdummy.entity.DummyEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.AABB
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.min
import java.util.concurrent.ConcurrentHashMap

class DummyManagementBlockEntity(pos: BlockPos, blockState: BlockState) :
    BlockEntity(ModBlockEntities.DUMMY_MANAGEMENT_BE, pos, blockState) {

    var mode = DummyManagementMode.HEALTH
        private set
    var maxHealth = 1000
        private set
    var durationSeconds = 5
        private set
    var resetSeconds = 0
        private set
    var autoRegeneration = false
        private set
    var regenerationPerSecond = 10
        private set
    var liveTimedOutput = true
        private set
    var targetAverageDamage = 5.0f
        private set
    var targetDamage = 5.0f
        private set
    var targetHits = 10
        private set
    var dpsWindowSeconds = 10
        private set
    var outputSignal = if (mode == DummyManagementMode.DPS) 0 else 15
        private set
    var invertOutput = false
        private set
    var currentHealth = maxHealth.toFloat()
        private set

    private var linkedDummyUuid: UUID? = null
    private var lookupTicks = 0
    private var timerTicks = 0
    private var resetTicks = 0
    private var timedTestActive = false
    private var timedTestFinished = false
    private var dpsTotalDamage = 0f
    private var dpsHitCount = 0

    fun applyAction(action: DummyManagementAction) {
        when (action) {
            DummyManagementAction.NEXT_MODE -> {
                mode = DummyManagementMode.entries[(mode.ordinal + 1) % DummyManagementMode.entries.size]
            }
            DummyManagementAction.SET_VALUE -> return
            DummyManagementAction.TOGGLE_OUTPUT_INVERSION -> {
                invertOutput = !invertOutput
                setChanged()
                notifySignalChanged()
                return
            }
            DummyManagementAction.MAX_HEALTH_DOWN -> maxHealth = (maxHealth - 100).coerceAtLeast(1)
            DummyManagementAction.MAX_HEALTH_UP -> maxHealth = (maxHealth + 100).coerceAtMost(100_000)
            DummyManagementAction.DURATION_DOWN -> durationSeconds = (durationSeconds - 1).coerceAtLeast(1)
            DummyManagementAction.DURATION_UP -> durationSeconds = (durationSeconds + 1).coerceAtMost(300)
            DummyManagementAction.RESET_DOWN -> resetSeconds = (resetSeconds - 1).coerceAtLeast(0)
            DummyManagementAction.RESET_UP -> resetSeconds = (resetSeconds + 1).coerceAtMost(300)
            DummyManagementAction.TOGGLE_REGEN -> autoRegeneration = !autoRegeneration
            DummyManagementAction.REGEN_RATE_DOWN -> regenerationPerSecond = (regenerationPerSecond - 1).coerceAtLeast(1)
            DummyManagementAction.REGEN_RATE_UP -> regenerationPerSecond = (regenerationPerSecond + 1).coerceAtMost(1000)
            DummyManagementAction.TOGGLE_TIMED_OUTPUT -> liveTimedOutput = !liveTimedOutput
            DummyManagementAction.TARGET_AVERAGE_DOWN ->
                targetAverageDamage = (targetAverageDamage - 0.5f).coerceAtLeast(0.5f)
            DummyManagementAction.TARGET_AVERAGE_UP ->
                targetAverageDamage = (targetAverageDamage + 0.5f).coerceAtMost(1000f)
            DummyManagementAction.TARGET_DAMAGE_DOWN ->
                targetDamage = (targetDamage - 0.5f).coerceAtLeast(0.5f)
            DummyManagementAction.TARGET_DAMAGE_UP ->
                targetDamage = (targetDamage + 0.5f).coerceAtMost(1000f)
            DummyManagementAction.TARGET_HITS_DOWN -> targetHits = (targetHits - 1).coerceAtLeast(1)
            DummyManagementAction.TARGET_HITS_UP -> targetHits = (targetHits + 1).coerceAtMost(1000)
            DummyManagementAction.DPS_WINDOW_DOWN -> dpsWindowSeconds = (dpsWindowSeconds - 1).coerceAtLeast(1)
            DummyManagementAction.DPS_WINDOW_UP -> dpsWindowSeconds = (dpsWindowSeconds + 1).coerceAtMost(300)
        }
        currentHealth = currentHealth.coerceAtMost(maxHealth.toFloat())
        resetTest()
        setChanged()
        syncBlock()
    }

    fun setMenuValue(index: Int, value: Int) {
        when (index) {
            1 -> maxHealth = value.coerceIn(1, 100_000)
            2 -> durationSeconds = value.coerceIn(1, 300)
            3 -> resetSeconds = value.coerceIn(0, 300)
            5 -> regenerationPerSecond = value.coerceIn(1, 1000)
            7 -> targetAverageDamage = value.coerceIn(5, 10_000) / 10f
            8 -> targetHits = value.coerceIn(1, 1000)
            9 -> dpsWindowSeconds = value.coerceIn(1, 300)
            16 -> targetDamage = value.coerceIn(5, 10_000) / 10f
            else -> return
        }
        currentHealth = currentHealth.coerceAtMost(maxHealth.toFloat())
        resetTest()
        setChanged()
        syncBlock()
    }

    fun redstoneSignal(): Int = if (invertOutput) 15 - outputSignal else outputSignal

    fun menuValue(index: Int): Int = when (index) {
        0 -> mode.ordinal
        1 -> maxHealth
        2 -> durationSeconds
        3 -> resetSeconds
        4 -> if (autoRegeneration) 1 else 0
        5 -> regenerationPerSecond
        6 -> if (liveTimedOutput) 1 else 0
        7 -> (targetAverageDamage * 10).toInt()
        8 -> targetHits
        9 -> dpsWindowSeconds
        10 -> redstoneSignal()
        11 -> currentHealth.toInt()
        12 -> dpsHitCount
        13 -> ceil(timerTicks / TICKS_PER_SECOND.toFloat()).toInt()
        14 -> if (dpsHitCount == 0) 0 else (dpsTotalDamage / dpsHitCount * 10f).toInt()
        15 -> if (linkedDummyUuid != null) 1 else 0
        16 -> (targetDamage * 10).toInt()
        17 -> if (invertOutput) 1 else 0
        else -> 0
    }

    fun onDummyDamaged(dummy: DummyEntity, damage: Float) {
        if (damage <= 0f) return
        linkedDummyUuid = dummy.uuid

        when (mode) {
            DummyManagementMode.HEALTH -> {
                currentHealth = (currentHealth - damage).coerceAtLeast(0f)
                if (autoRegeneration) {
                    resetTicks = 0
                } else if (resetSeconds > 0) {
                    resetTicks = resetSeconds * TICKS_PER_SECOND
                }
                updateSignal(healthSignal())
            }
            DummyManagementMode.TIMED_HEALTH -> {
                if (!timedTestActive || timedTestFinished) {
                    currentHealth = maxHealth.toFloat()
                    timerTicks = durationSeconds * TICKS_PER_SECOND
                    resetTicks = 0
                    timedTestActive = true
                    timedTestFinished = false
                    if (!liveTimedOutput) updateSignal(0)
                }
                currentHealth = (currentHealth - damage).coerceAtLeast(0f)
                if (liveTimedOutput) updateSignal(healthSignal())
            }
            DummyManagementMode.SINGLE_HIT -> {
                currentHealth = maxHealth.toFloat()
                currentHealth = (currentHealth - damage).coerceAtLeast(0f)
                resetTicks = resetSeconds * TICKS_PER_SECOND
                updateSignal(healthSignal())
            }
            DummyManagementMode.DPS -> {
                if (!timedTestActive || timedTestFinished) {
                    timerTicks = dpsWindowSeconds * TICKS_PER_SECOND
                    resetTicks = 0
                    dpsTotalDamage = 0f
                    dpsHitCount = 0
                    timedTestActive = true
                    timedTestFinished = false
                }
                dpsTotalDamage += damage
                dpsHitCount++
                updateSignal(dpsSignal())
            }
            DummyManagementMode.DAMAGE_TARGET -> {
                updateSignal(if (kotlin.math.abs(damage - targetDamage) <= HIT_DAMAGE_TOLERANCE) 15 else 0)
                resetTicks = resetSeconds * TICKS_PER_SECOND
            }
        }
        setChanged()
    }

    fun isLinkedTo(dummy: DummyEntity): Boolean {
        linkedDummyUuid?.let { return it == dummy.uuid }
        val centerX = worldPosition.x + 0.5
        val centerY = worldPosition.y + 0.5
        val centerZ = worldPosition.z + 0.5
        return dummy.distanceToSqr(centerX, centerY, centerZ) <= 6.25
    }

    fun tickServer() {
        val currentLevel = level as? ServerLevel ?: return
        if (++lookupTicks >= LINK_LOOKUP_INTERVAL) {
            lookupTicks = 0
            updateLinkedDummy(currentLevel)
        }

        if (mode == DummyManagementMode.HEALTH && autoRegeneration && currentHealth < maxHealth) {
            currentHealth = min(maxHealth.toFloat(), currentHealth + regenerationPerSecond / TICKS_PER_SECOND.toFloat())
            setChanged()
            updateSignal(healthSignal())
        }

        if (timerTicks > 0) {
            timerTicks--
            if (timerTicks % TICKS_PER_SECOND == 0) setChanged()
            if (timerTicks == 0) {
                when (mode) {
                    DummyManagementMode.TIMED_HEALTH -> {
                        timedTestFinished = true
                        if (!liveTimedOutput) updateSignal(healthSignal())
                        if (resetSeconds > 0) resetTicks = resetSeconds * TICKS_PER_SECOND
                    }
                    DummyManagementMode.DPS -> {
                        timedTestFinished = true
                        if (resetSeconds > 0) resetTicks = resetSeconds * TICKS_PER_SECOND
                    }
                    else -> Unit
                }
                setChanged()
            }
        }

        if (resetTicks > 0 && --resetTicks == 0) {
            when (mode) {
                DummyManagementMode.HEALTH, DummyManagementMode.SINGLE_HIT,
                DummyManagementMode.TIMED_HEALTH -> {
                    currentHealth = maxHealth.toFloat()
                    if (mode == DummyManagementMode.TIMED_HEALTH) {
                        timedTestActive = false
                        timedTestFinished = false
                    }
                    updateSignal(if (mode == DummyManagementMode.TIMED_HEALTH && !liveTimedOutput) 0 else 15)
                }
                DummyManagementMode.DPS -> {
                    timedTestActive = false
                    timedTestFinished = false
                    dpsHitCount = 0
                    dpsTotalDamage = 0f
                    updateSignal(0)
                }
                DummyManagementMode.DAMAGE_TARGET -> updateSignal(0)
            }
            setChanged()
        }
    }

    private fun updateLinkedDummy(serverLevel: ServerLevel) {
        val nearbyDummy = serverLevel.getEntitiesOfClass(
            DummyEntity::class.java,
            AABB(worldPosition).inflate(1.5, 2.0, 1.5)
        ).minByOrNull { it.distanceToSqr(worldPosition.x + 0.5, worldPosition.y + 0.5, worldPosition.z + 0.5) }

        val displayTarget = Direction.entries.asSequence()
            .map { worldPosition.relative(it) }
            .mapNotNull { serverLevel.getBlockEntity(it) as? DisplayDummyBlockEntity }
            .mapNotNull { it.target_entity_uuid?.let(serverLevel::getEntity) as? DummyEntity }
            .firstOrNull()

        linkedDummyUuid = displayTarget?.uuid ?: nearbyDummy?.uuid
    }

    private fun healthSignal(): Int =
        ceil(15.0 * (currentHealth / maxHealth.coerceAtLeast(1)).coerceIn(0f, 1f)).toInt().coerceIn(0, 15)

    private fun dpsSignal(): Int {
        val average = if (dpsHitCount == 0) 0f else dpsTotalDamage / dpsHitCount
        val progress = min(
            average / targetAverageDamage.coerceAtLeast(0.1f),
            dpsHitCount.toFloat() / targetHits.coerceAtLeast(1)
        ).coerceIn(0f, 1f)
        return ceil(progress * 15f).toInt().coerceIn(0, 15)
    }

    private fun updateSignal(value: Int) {
        val newSignal = value.coerceIn(0, 15)
        if (outputSignal == newSignal) return
        outputSignal = newSignal
        setChanged()
        notifySignalChanged()
    }

    private fun notifySignalChanged() {
        val currentLevel = level ?: return
        currentLevel.updateNeighborsAt(worldPosition, blockState.block)
        currentLevel.updateNeighbourForOutputSignal(worldPosition, blockState.block)
        syncBlock()
    }

    private fun resetTest() {
        currentHealth = maxHealth.toFloat()
        timerTicks = 0
        resetTicks = 0
        timedTestActive = false
        timedTestFinished = false
        dpsTotalDamage = 0f
        dpsHitCount = 0
        updateSignal(
            when (mode) {
                DummyManagementMode.DPS -> 0
                DummyManagementMode.DAMAGE_TARGET -> 0
                DummyManagementMode.TIMED_HEALTH -> if (liveTimedOutput) 15 else 0
                else -> 15
            }
        )
    }

    private fun syncBlock() {
        val currentLevel = level ?: return
        currentLevel.sendBlockUpdated(worldPosition, blockState, blockState, 3)
    }

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        output.putInt("mode", mode.ordinal)
        output.putInt("max_health", maxHealth)
        output.putInt("duration_seconds", durationSeconds)
        output.putInt("reset_seconds", resetSeconds)
        output.putBoolean("auto_regeneration", autoRegeneration)
        output.putInt("regeneration_per_second", regenerationPerSecond)
        output.putBoolean("live_timed_output", liveTimedOutput)
        output.putFloat("target_average_damage", targetAverageDamage)
        output.putFloat("target_damage", targetDamage)
        output.putInt("target_hits", targetHits)
        output.putInt("dps_window_seconds", dpsWindowSeconds)
        output.putInt("output_signal", outputSignal)
        output.putBoolean("invert_output", invertOutput)
        output.putFloat("current_health", currentHealth)
        output.putInt("timer_ticks", timerTicks)
        output.putInt("reset_ticks", resetTicks)
        output.putBoolean("timed_test_active", timedTestActive)
        output.putBoolean("timed_test_finished", timedTestFinished)
        output.putFloat("dps_total_damage", dpsTotalDamage)
        output.putInt("dps_hit_count", dpsHitCount)
    }

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        mode = input.getInt("mode").map {
            DummyManagementMode.entries.getOrElse(it) { DummyManagementMode.HEALTH }
        }.orElse(DummyManagementMode.HEALTH)
        maxHealth = input.getIntOr("max_health", 1000).coerceIn(1, 100_000)
        durationSeconds = input.getIntOr("duration_seconds", 5).coerceIn(1, 300)
        resetSeconds = input.getIntOr("reset_seconds", 0).coerceIn(0, 300)
        autoRegeneration = input.getBooleanOr("auto_regeneration", false)
        regenerationPerSecond = input.getIntOr("regeneration_per_second", 10).coerceIn(1, 1000)
        liveTimedOutput = input.getBooleanOr("live_timed_output", true)
        targetAverageDamage = input.getFloatOr("target_average_damage", 5f).coerceIn(0.5f, 1000f)
        targetDamage = input.getFloatOr("target_damage", 5f).coerceIn(0.5f, 1000f)
        targetHits = input.getIntOr("target_hits", 10).coerceIn(1, 1000)
        dpsWindowSeconds = input.getIntOr("dps_window_seconds", 10).coerceIn(1, 300)
        outputSignal = input.getIntOr("output_signal", if (mode == DummyManagementMode.DPS) 0 else 15).coerceIn(0, 15)
        invertOutput = input.getBooleanOr("invert_output", false)
        currentHealth = input.getFloatOr("current_health", maxHealth.toFloat()).coerceIn(0f, maxHealth.toFloat())
        timerTicks = input.getIntOr("timer_ticks", 0).coerceAtLeast(0)
        resetTicks = input.getIntOr("reset_ticks", 0).coerceAtLeast(0)
        timedTestActive = input.getBooleanOr("timed_test_active", false)
        timedTestFinished = input.getBooleanOr("timed_test_finished", false)
        dpsTotalDamage = input.getFloatOr("dps_total_damage", 0f).coerceAtLeast(0f)
        dpsHitCount = input.getIntOr("dps_hit_count", 0).coerceAtLeast(0)
        linkedDummyUuid = null
        lookupTicks = 0
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? =
        ClientboundBlockEntityDataPacket.create(this)

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag =
        saveWithoutMetadata(registries)

    override fun onLoad() {
        super.onLoad()
        if (level?.isClientSide == false) loadedManagers += this
    }

    override fun setRemoved() {
        loadedManagers -= this
        super.setRemoved()
    }

    companion object {
        private const val TICKS_PER_SECOND = 20
        private const val HIT_DAMAGE_TOLERANCE = 0.5f
        private const val LINK_LOOKUP_INTERVAL = 20
        private val loadedManagers = ConcurrentHashMap.newKeySet<DummyManagementBlockEntity>()

        fun handleDummyDamage(dummy: DummyEntity, damage: Float) {
            loadedManagers.forEach { manager ->
                if (manager.level === dummy.level() && manager.isLinkedTo(dummy)) {
                    manager.onDummyDamaged(dummy, damage)
                }
            }
        }
    }
}
