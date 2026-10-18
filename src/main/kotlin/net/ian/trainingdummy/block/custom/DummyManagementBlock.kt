package net.ian.trainingdummy.block.custom

import com.mojang.serialization.MapCodec
import net.ian.trainingdummy.block.entityblock.DummyManagementBlockEntity
import net.ian.trainingdummy.screen.custom.DummyManagementMenu
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.MenuProvider
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

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

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult {
        val blockEntity = level.getBlockEntity(pos) as? DummyManagementBlockEntity
            ?: return InteractionResult.PASS
        if (!level.isClientSide && player is ServerPlayer) {
            val provider: MenuProvider = SimpleMenuProvider(
                { containerId, inventory, _ ->
                    DummyManagementMenu(containerId, inventory, blockEntity, pos)
                },
                Component.translatable("container.trainingdummy.dummy_management")
            )
            player.openMenu(provider) { buffer -> buffer.writeBlockPos(pos) }
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun hasAnalogOutputSignal(state: BlockState): Boolean = true

    override fun isSignalSource(state: BlockState): Boolean = true

    override fun getAnalogOutputSignal(state: BlockState, level: Level, pos: BlockPos): Int =
        (level.getBlockEntity(pos) as? DummyManagementBlockEntity)?.redstoneSignal() ?: 0

    override fun getSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: net.minecraft.core.Direction): Int =
        (level.getBlockEntity(pos) as? DummyManagementBlockEntity)?.redstoneSignal() ?: 0

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        return BlockEntityTicker<T> { _, _, _, blockEntity ->
            (blockEntity as? DummyManagementBlockEntity)?.tickServer()
        }
    }

}