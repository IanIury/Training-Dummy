package net.ian.trainingdummy.block.custom

import com.mojang.serialization.MapCodec
import net.ian.trainingdummy.block.entityblock.DisplayDummyBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.AttachFace
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.phys.Vec3
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

        val FACING: EnumProperty<Direction> = BlockStateProperties.HORIZONTAL_FACING
        val FACE: EnumProperty<AttachFace> = BlockStateProperties.ATTACH_FACE

        // VoxelShape base apontando para o Norte no Chão, alinhado aos elementos do modelo.
        private val BASE_SHAPE = Block.box(3.0, 0.0, 5.0, 13.0, 1.0, 11.0)
        private val BASE_CONNECTOR_SHAPE = Block.box(6.5, 1.0, 7.0, 9.5, 1.3, 9.0)
        private val POLE_SHAPE = Block.box(7.5, 1.3, 7.5, 8.5, 3.3, 8.5)
        private val TOP_SHAPE = Block.box(0.5, 3.3, 7.5, 15.5, 3.8, 8.5)
        val SHAPE_BASE: VoxelShape = Shapes.or(
            BASE_SHAPE,
            BASE_CONNECTOR_SHAPE,
            POLE_SHAPE,
            TOP_SHAPE
        )

        fun getShapeForState(state: BlockState): VoxelShape {
            val facing = state.getValue(FACING)
            val face = state.getValue(FACE)

            var shape = SHAPE_BASE

            shape = when (face) {
                AttachFace.CEILING -> rotateVoxel(shape, Direction.DOWN)
                AttachFace.WALL -> rotateVoxel(shape, Direction.NORTH)
                AttachFace.FLOOR -> shape
            }

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

        fun getProjectionSurface(state: BlockState): Pair<Vec3, Direction> {
            var shape = TOP_SHAPE
            val face = state.getValue(FACE)
            val facing = state.getValue(FACING)

            shape = when (face) {
                AttachFace.CEILING -> rotateVoxel(shape, Direction.DOWN)
                AttachFace.WALL -> rotateVoxel(shape, Direction.NORTH)
                AttachFace.FLOOR -> shape
            }

            val steps = when (facing) {
                Direction.SOUTH -> 2
                Direction.WEST -> 3
                Direction.EAST -> 1
                else -> 0
            }
            repeat(steps) { shape = rotateY(shape) }

            val bounds = shape.bounds()
            val center = Vec3(
                (bounds.minX + bounds.maxX) / 2.0,
                (bounds.minY + bounds.maxY) / 2.0,
                (bounds.minZ + bounds.maxZ) / 2.0
            )
            val normal = when (face) {
                AttachFace.FLOOR -> Direction.UP
                AttachFace.CEILING -> Direction.DOWN
                AttachFace.WALL -> facing
            }
            val halfExtent = when (normal.axis) {
                Direction.Axis.X -> (bounds.maxX - bounds.minX) / 2.0
                Direction.Axis.Y -> (bounds.maxY - bounds.minY) / 2.0
                Direction.Axis.Z -> (bounds.maxZ - bounds.minZ) / 2.0
            }
            val surfaceCenter = center.add(
                normal.stepX * halfExtent,
                normal.stepY * halfExtent,
                normal.stepZ * halfExtent
            )
            return surfaceCenter to normal
        }

        private fun rotateY(shape: VoxelShape): VoxelShape {
            val rotated = mutableListOf<VoxelShape>()
            shape.forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
                rotated.add(Block.box((1.0 - maxZ) * 16, minY * 16, minX * 16, (1.0 - minZ) * 16, maxY * 16, maxX * 16))
            }
            return rotated.reduceOrNull { a: VoxelShape, b: VoxelShape -> Shapes.or(a, b) } ?: shape
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
            return rotated.reduceOrNull { a: VoxelShape, b: VoxelShape -> Shapes.or(a, b) } ?: shape
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

    override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
    }

    override fun rotate(state: BlockState, level: LevelAccessor, pos: BlockPos, direction: Rotation): BlockState {
        return rotate(state, direction)
    }

    override fun mirror(state: BlockState, mirror: Mirror): BlockState {
        return rotate(state, mirror.getRotation(state.getValue(FACING)))
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

    override fun playerWillDestroy(level: Level, pos: BlockPos, state: BlockState, player: Player): BlockState {
        val blockEntity = level.getBlockEntity(pos) as? DisplayDummyBlockEntity
        if (!level.isClientSide && blockEntity != null && !player.isCreative) {
            val stack = ItemStack(this)
            stack.applyComponents(blockEntity.collectComponents())
            blockEntity.target_entity_uuid?.let { uuid ->
                CustomData.update(DataComponents.CUSTOM_DATA, stack) { tag ->
                    tag.putString("dummy_uuid", uuid.toString())
                    blockEntity.dummy_name?.let { tag.putString("dummy_name", it) }
                    blockEntity.dummy_x?.let { tag.putDouble("dummy_x", it) }
                    blockEntity.dummy_y?.let { tag.putDouble("dummy_y", it) }
                    blockEntity.dummy_z?.let { tag.putDouble("dummy_z", it) }
                    blockEntity.dummy_dim?.let { tag.putString("dummy_dimension", it) }
                    blockEntity.dummy_mode?.let { tag.putString("dummy_mode", it) }
                }
            }

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
