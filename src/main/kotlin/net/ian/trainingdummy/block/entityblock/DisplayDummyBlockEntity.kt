package net.ian.trainingdummy.block.entityblock

import com.mojang.serialization.Codec
import net.ian.trainingdummy.block.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import java.util.UUID

class DisplayDummyBlockEntity(pos: BlockPos, blockState: BlockState) : BlockEntity(ModBlockEntities.DISPLAY_DUMMY_BE, pos, blockState) {

    var target_entity_uuid: UUID? = null
    var dummy_name: String? = null
    var dummy_x: Double? = null
    var dummy_y: Double? = null
    var dummy_z: Double? = null
    var dummy_dim: String? = null
    var dummy_mode: String? = null

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        output.storeNullable("dummy_uuid", UUIDUtil.CODEC, target_entity_uuid)
        dummy_name?.let { output.putString("dummy_name", it) }
        dummy_x?.let { output.putDouble("dummy_x", it) }
        dummy_y?.let { output.putDouble("dummy_y", it) }
        dummy_z?.let { output.putDouble("dummy_z", it) }
        dummy_dim?.let { output.putString("dummy_dimension", it) }
        dummy_mode?.let { output.putString("dummy_mode", it) }
    }

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        target_entity_uuid = input.read("dummy_uuid", UUIDUtil.CODEC).orElse(null)
        dummy_name = input.getString("dummy_name").orElse(null)
        dummy_x = input.read("dummy_x", Codec.DOUBLE).orElse(null)
        dummy_y = input.read("dummy_y", Codec.DOUBLE).orElse(null)
        dummy_z = input.read("dummy_z", Codec.DOUBLE).orElse(null)
        dummy_dim = input.getString("dummy_dimension").orElse(null)
        dummy_mode = input.getString("dummy_mode").orElse(null)
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        return saveWithoutMetadata(registries)
    }
}
