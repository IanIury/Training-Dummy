package net.ian.trainingdummy.block.custom

import com.mojang.serialization.MapCodec
import net.ian.trainingdummy.block.entityblock.DisplayDummyBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.AttachFace
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

class DisplayDummyBlock(properties: Properties) : BaseEntityBlock(properties) {

    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.FLOOR)
        )
    }

    companion object {
        val CODEC: MapCodec<DisplayDummyBlock> = simpleCodec(::DisplayDummyBlock)

        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING
        val FACE: EnumProperty<AttachFace> = BlockStateProperties.ATTACH_FACE

        // VoxelShape base apontando para o Norte no Chão
        private val BASE_SHAPE = Block.box(3.0, 0.0, 5.0, 13.0, 1.0, 11.0)
        private val POLE_SHAPE = Block.box(7.0, 1.0, 7.0, 9.0, 3.0, 9.0)
        private val TOP_SHAPE = Block.box(4.5, 3.0, 6.5, 11.5, 3.1, 9.5)
        val SHAPE_BASE: VoxelShape = Shapes.or(BASE_SHAPE, POLE_SHAPE, TOP_SHAPE)


        fun getShapeForState(state: BlockState): VoxelShape {
            val facing = state.getValue(FACING)
            val face = state.getValue(FACE)

            var shape = SHAPE_BASE

            // Rotação da superfície (Teto ou Parede)
            shape = when (face) {
                AttachFace.CEILING -> rotateVoxel(shape, Direction.DOWN)
                AttachFace.WALL -> rotateVoxel(shape, Direction.NORTH)
                AttachFace.FLOOR -> shape
            }

            // Rotação horizontal (Norte, Sul, Leste, Oeste)
            val steps = when (facing) {
                Direction.SOUTH -> 2
                Direction.WEST -> 3
                Direction.EAST -> 1
                else -> 0
            }

            for (i in 0 until steps) {
                shape = rotateY(shape)
            }

            return shape
        }

        private fun rotateY(shape: VoxelShape): VoxelShape {
            val rotated = mutableListOf<VoxelShape>()
            shape.forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
                rotated.add(Block.box((1.0 - maxZ) * 16, minY * 16, minX * 16, (1.0 - minZ) * 16, maxY * 16, maxX * 16))
            }
            return rotated.reduceOrNull { a, b -> Shapes.or(a, b) } ?: shape
        }

        private fun rotateVoxel(shape: VoxelShape, target: Direction): VoxelShape {
            val rotated = mutableListOf<VoxelShape>()
            shape.forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
                when (target) {
                    Direction.DOWN -> rotated.add(Block.box(minX * 16, (1.0 - maxY) * 16, minZ * 16, maxX * 16, (1.0 - minY) * 16, maxZ * 16))
                    Direction.NORTH -> rotated.add(Block.box(minX * 16, minZ * 16, (1.0 - maxY) * 16, maxX * 16, maxZ * 16, (1.0 - minY) * 16))
                    else -> rotated.add(shape)
                }
            }
            return rotated.reduceOrNull { a, b -> Shapes.or(a, b) } ?: shape
        }
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, FACE)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val clickedFace = context.clickedFace

        val face = when (clickedFace) {
            Direction.UP -> AttachFace.FLOOR
            Direction.DOWN -> AttachFace.CEILING
            else -> AttachFace.WALL
        }

        val facing = if (clickedFace.axis.isHorizontal) {
            clickedFace
        } else {
            context.horizontalDirection.opposite
        }

        return defaultBlockState()
            .setValue(FACE, face)
            .setValue(FACING, facing)
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return getShapeForState(state)
    }

    override fun rotate(state: BlockState, level: LevelAccessor, pos: BlockPos, direction: Rotation): BlockState {
        return state.setValue(FACING, direction.rotate(state.getValue(FACING)))
    }

    override fun mirror(state: BlockState, mirror: Mirror): BlockState {
        return state.rotate(mirror.getRotation(state.getValue(FACING)))
    }

    override fun codec(): MapCodec<out BaseEntityBlock> {
        return CODEC
    }

    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun newBlockEntity(blockPos: BlockPos, state: BlockState): BlockEntity? {
        return DisplayDummyBlockEntity(blockPos, state)
    }

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hitResult: BlockHitResult
    ): ItemInteractionResult {
        if(level.isClientSide){return ItemInteractionResult.SUCCESS}
        val blockEntity = level.getBlockEntity(pos) as? DisplayDummyBlockEntity ?: return ItemInteractionResult.SUCCESS
        player.sendSystemMessage(Component.literal("[UUID Dummy: ${blockEntity.target_entity_uuid.toString()}]"))
        return ItemInteractionResult.SUCCESS
    }

    override fun playerWillDestroy(level: Level, pos: BlockPos, state: BlockState, player: Player): BlockState {
        val blockEntity = level.getBlockEntity(pos) as? DisplayDummyBlockEntity
        if (!level.isClientSide && blockEntity != null && !player.isCreative) {
            val stack = ItemStack(this)

            blockEntity.saveToItem(stack, level.registryAccess())

            val itemEntity = ItemEntity(
                level,
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
                stack
            )
            itemEntity.setDefaultPickUpDelay()
            level.addFreshEntity(itemEntity)
        }
        return super.playerWillDestroy(level, pos, state, player)
    }
}