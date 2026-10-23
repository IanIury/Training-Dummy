package net.ian.trainingdummy.block.renderes

import com.mojang.blaze3d.vertex.PoseStack
import dev.ian.worldpanel.FloatingWindow
import dev.ian.worldpanel.WindowAlignment
import dev.ian.worldpanel.WindowColor
import net.ian.trainingdummy.block.custom.DisplayDummyBlock
import net.ian.trainingdummy.block.entityblock.DisplayDummyBlockEntity
import net.ian.trainingdummy.entity.DummyEntity
import net.ian.trainingdummy.entity.entityrender.DummyRenderer
import net.ian.trainingdummy.entity.entityrender.DummyWindowProjection
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.AttachFace
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import java.util.UUID
import java.util.WeakHashMap

class DisplayDummyBlockEntityRenderState : BlockEntityRenderState() {
    var blockState: BlockState = Blocks.AIR.defaultBlockState()
    var targetEntity: DummyEntity? = null
    var partialTick: Float = 0f
}

class DisplayDummyBlockEntityRenderer(context: BlockEntityRendererProvider.Context) :
    BlockEntityRenderer<DisplayDummyBlockEntity, DisplayDummyBlockEntityRenderState> {
    private val targetCache = WeakHashMap<DisplayDummyBlockEntity, CachedTarget>()

    override fun createRenderState(): DisplayDummyBlockEntityRenderState =
        DisplayDummyBlockEntityRenderState()

    override fun extractRenderState(
        blockEntity: DisplayDummyBlockEntity,
        state: DisplayDummyBlockEntityRenderState,
        partialTick: Float,
        cameraPos: Vec3,
        overlay: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        BlockEntityRenderState.extractBase(blockEntity, state, overlay)
        state.blockState = blockEntity.blockState
        state.partialTick = partialTick
        val clientLevel = blockEntity.level as? ClientLevel
        state.targetEntity = clientLevel?.let { resolveTarget(blockEntity, it) }
    }

    override fun submit(
        state: DisplayDummyBlockEntityRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        cameraState: CameraRenderState
    ) {
        val blockState = state.blockState
        val face =
            if (blockState.hasProperty(DisplayDummyBlock.FACE)) blockState.getValue(DisplayDummyBlock.FACE) else AttachFace.FLOOR
        val facing =
            if (blockState.hasProperty(DisplayDummyBlock.FACING)) blockState.getValue(DisplayDummyBlock.FACING) else Direction.NORTH

        var offsetX = 0.5
        var offsetY = 0.5
        var offsetZ = 0.5
        val distance = 0.15

        when (face) {
            AttachFace.FLOOR -> offsetY += distance
            AttachFace.CEILING -> offsetY -= distance
            AttachFace.WALL -> {
                when (facing) {
                    Direction.NORTH -> offsetZ -= distance
                    Direction.SOUTH -> offsetZ += distance
                    Direction.WEST -> offsetX -= distance
                    Direction.EAST -> offsetX += distance
                    else -> {}
                }
            }
        }

        val cameraRight = Vector3f(1f, 0f, 0f).rotate(cameraState.orientation)
        val projection = DummyWindowProjection(
            offsetX,
            offsetY,
            offsetZ,
            cameraRight.x.toDouble(),
            cameraRight.z.toDouble()
        )

        val entity = state.targetEntity ?: run {
            FloatingWindow.Builder()
                .setOffset(offsetX, offsetY, offsetZ)
                .setSize(16f, 16f)
                .setAlignment(WindowAlignment.CENTER)
                .setPadding(1f)
                .setBorder(WindowColor.Rainbow(speed = 1.0f), width = 0.5f)
                .setBackground(WindowColor.Solid(0xDD000000L))
                .setFaceCamera(true)
                .build()
                .render(poseStack, collector, state.lightCoords)
            return
        }

        DummyRenderer.renderFloatingWindowModo(
            entity,
            poseStack,
            collector,
            state.lightCoords,
            projection,
            state.partialTick
        )
    }

    override fun shouldRenderOffScreen(): Boolean = true

    override fun getViewDistance(): Int = 128

    private fun resolveTarget(blockEntity: DisplayDummyBlockEntity, level: ClientLevel): DummyEntity? {
        val targetUuid = blockEntity.target_entity_uuid ?: run {
            targetCache.remove(blockEntity)
            return null
        }
        val cached = targetCache[blockEntity]
            ?.takeIf { it.uuid == targetUuid }
            ?: CachedTarget(targetUuid).also { targetCache[blockEntity] = it }

        cached.entity?.takeIf {
            it.uuid == targetUuid && it.level() === level && it.isAlive
        }?.let { return it }

        if (cached.lastLookupGameTime == level.gameTime) return null
        cached.lastLookupGameTime = level.gameTime
        cached.entity = level.entitiesForRendering()
            .firstOrNull { it.uuid == targetUuid } as? DummyEntity
        return cached.entity
    }

    private data class CachedTarget(
        val uuid: UUID,
        var entity: DummyEntity? = null,
        var lastLookupGameTime: Long = Long.MIN_VALUE
    )
}
