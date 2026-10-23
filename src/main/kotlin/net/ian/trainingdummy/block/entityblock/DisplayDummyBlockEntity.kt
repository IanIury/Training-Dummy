package net.ian.trainingdummy.block.entityblock

import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.block.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.util.UUID

class DisplayDummyBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlockEntities.DISPLAY_DUMMY_BE, pos, blockState) {

    var target_entity_uuid: UUID? = null
    var dummy_name: String? = null
    var dummy_x: Double? = null
    var dummy_y: Double? = null
    var dummy_z: Double? = null
    var dummy_dim: String? = null
    var dummy_mode: String? = null

    override fun saveAdditional(tag: CompoundTag, registrie: HolderLookup.Provider) {
        super.saveAdditional(tag, registrie)
        target_entity_uuid?.let { uuid -> tag.putString("dummy_uuid", uuid.toString()) }
        dummy_name?.let { tag.putString("dummy_name", it) }
        dummy_x?.let { tag.putDouble("dummy_x", it) }
        dummy_y?.let { tag.putDouble("dummy_y", it) }
        dummy_z?.let { tag.putDouble("dummy_z", it) }
        dummy_dim?.let { tag.putString("dummy_dimension", it) }
        dummy_mode?.let { tag.putString("dummy_mode", it) }
    }

    override fun loadAdditional(tag: CompoundTag, registrier: HolderLookup.Provider) {
        super.loadAdditional(tag, registrier)
        target_entity_uuid = null
        val rawUuid = tag.getString("dummy_uuid")
        if (rawUuid.isNotBlank()) {
            try {
                target_entity_uuid = UUID.fromString(rawUuid)
            } catch (_: IllegalArgumentException) {
                TrainingDummy.LOGGER.warn("Ignoring invalid dummy UUID '{}' at {}", rawUuid, worldPosition)
            }
        }
        dummy_name = if (tag.contains("dummy_name")) tag.getString("dummy_name") else null
        dummy_x = if (tag.contains("dummy_x")) tag.getDouble("dummy_x") else null
        dummy_y = if (tag.contains("dummy_y")) tag.getDouble("dummy_y") else null
        dummy_z = if (tag.contains("dummy_z")) tag.getDouble("dummy_z") else null
        dummy_dim = if (tag.contains("dummy_dimension")) tag.getString("dummy_dimension") else null
        dummy_mode = if (tag.contains("dummy_mode")) tag.getString("dummy_mode") else null
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener?>? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        return saveWithoutMetadata(registries)
    }
}
