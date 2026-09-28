package net.ian.trainingdummy.block.entityblock

import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.block.ModBlockEntities
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BooleanProperty
import java.util.UUID

class DisplayDummyBlockEntity(pos: BlockPos,blockState: BlockState) : BlockEntity(ModBlockEntities.DISPLAY_DUMMY_BE,pos, blockState){

    var target_entity_uuid : UUID? = null

    override fun saveAdditional(tag: CompoundTag, registrie: HolderLookup.Provider) {
        super.saveAdditional(tag, registrie)
        target_entity_uuid?.let { uuid -> tag.putString("dummy_uuid",uuid.toString()) }

    }

    override fun loadAdditional(tag: CompoundTag, registrier: HolderLookup.Provider) {
        super.loadAdditional(tag,registrier)
        tag.getString("dummy_uuid").let { uuid -> if(!uuid.isNullOrBlank()){target_entity_uuid= UUID.fromString(uuid) } }

    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener?>? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        return saveWithoutMetadata(registries)
    }

    fun getEntityByUUID() : Entity? {
        val currentLevel = level ?: return null

        if (currentLevel is ServerLevel) {
            return target_entity_uuid?.let { uuid ->
                currentLevel.getEntity(uuid)
            }
        }

        return target_entity_uuid?.let { uuid ->

            (currentLevel as ClientLevel)
                .entitiesForRendering()
                .firstOrNull { it.uuid == uuid }
        }
    }

}