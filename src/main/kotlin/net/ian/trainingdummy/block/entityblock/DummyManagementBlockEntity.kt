package net.ian.trainingdummy.block.entityblock

import net.ian.trainingdummy.block.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class DummyManagementBlockEntity(pos: BlockPos,blockState: BlockState) : BlockEntity(ModBlockEntities.DUMMY_MANAGEMENT_BE,pos, blockState) {


    override fun saveAdditional(tag: CompoundTag, registrie: HolderLookup.Provider) {
        super.saveAdditional(tag, registrie)
    }

    override fun loadAdditional(tag: CompoundTag, registrier: HolderLookup.Provider) {
        super.loadAdditional(tag, registrier)
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener?>? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        return saveWithoutMetadata(registries)
    }



}