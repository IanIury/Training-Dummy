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

class DisplayDummyBlockEntity(pos: BlockPos,blockState: BlockState) : BlockEntity(ModBlockEntities.DISPLAY_DUMMY_BE,pos, blockState){

    var target_entity_uuid : UUID? = null

    override fun saveAdditional(tag: CompoundTag, registrie: HolderLookup.Provider) {
        super.saveAdditional(tag, registrie)
        target_entity_uuid?.let { uuid -> tag.putString("dummy_uuid",uuid.toString()) }

    }

    override fun loadAdditional(tag: CompoundTag, registrier: HolderLookup.Provider) {
        super.loadAdditional(tag,registrier)
        target_entity_uuid = null
        val rawUuid = tag.getString("dummy_uuid")
        if (rawUuid.isNotBlank()) {
            try {
                target_entity_uuid = UUID.fromString(rawUuid)
            } catch (_: IllegalArgumentException) {
                TrainingDummy.LOGGER.warn("Ignoring invalid dummy UUID '{}' at {}", rawUuid, worldPosition)
            }
        }

    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener?>? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        return saveWithoutMetadata(registries)
    }

}