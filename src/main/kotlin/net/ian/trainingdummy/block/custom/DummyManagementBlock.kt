package net.ian.trainingdummy.block.custom

import com.mojang.serialization.MapCodec
import net.ian.trainingdummy.block.entityblock.DummyManagementBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState

class DummyManagementBlock (properties: Properties) : BaseEntityBlock(properties) {


    companion object {
        val CODEC : MapCodec<DummyManagementBlock> = simpleCodec(::DummyManagementBlock)
    }
    override fun codec(): MapCodec<out BaseEntityBlock?> {
       return CODEC
    }

    override fun newBlockEntity(
        p0: BlockPos,
        p1: BlockState
    ): BlockEntity {
        return DummyManagementBlockEntity(p0,p1)
    }

}