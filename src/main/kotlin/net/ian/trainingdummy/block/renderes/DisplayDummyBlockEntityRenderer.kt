package net.ian.trainingdummy.block.renderes

import com.mojang.blaze3d.vertex.PoseStack
import net.ian.trainingdummy.block.custom.DisplayDummyBlock
import net.ian.trainingdummy.block.entityblock.DisplayDummyBlockEntity
import dev.ian.worldpanel.FloatingWindow
import dev.ian.worldpanel.WindowAlignment
import dev.ian.worldpanel.WindowColor
import dev.ian.worldpanel.WindowElement
import net.ian.trainingdummy.entity.DummyEntity
import net.ian.trainingdummy.entity.entityrender.DummyRenderer
import net.ian.trainingdummy.entity.entityrender.DummyWindowProjection
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.properties.AttachFace
import org.joml.Vector3f
import java.util.UUID
import java.util.WeakHashMap

class DisplayDummyBlockEntityRenderer(context: BlockEntityRendererProvider.Context) :
    BlockEntityRenderer<DisplayDummyBlockEntity> {
    private val targetCache = WeakHashMap<DisplayDummyBlockEntity, CachedTarget>()

    override fun render(
        pBlockEntity: DisplayDummyBlockEntity,
        pPartialTick: Float,
        pose: PoseStack,
        buffer: MultiBufferSource,
        pPacketLight: Int,
        pPackedOverlay: Int
    ) {
        renderFloatingWindowDefault(pBlockEntity, pPartialTick, pose, buffer, pPacketLight)
    }

    private fun renderFloatingWindowDefault(
        pBlockEntity: DisplayDummyBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        val state = pBlockEntity.blockState

        // Recupera as propriedades do estado do bloco (com valores de fallback seguros)
        val face =
            if (state.hasProperty(DisplayDummyBlock.FACE)) state.getValue(DisplayDummyBlock.FACE) else AttachFace.FLOOR
        val facing =
            if (state.hasProperty(DisplayDummyBlock.FACING)) state.getValue(DisplayDummyBlock.FACING) else Direction.NORTH

        // Lógica de Deslocamento (Offset) no Espaço Local
        var offsetX = 0.5
        var offsetY = 0.5
        var offsetZ = 0.5

        val distance = 0.15 // Distância para projetar a janela fora do bloco

        when (face) {
            // Bloco no chão -> Janela fica acima dele
            AttachFace.FLOOR -> {
                offsetY += distance // +Y sobe
            }
            // Bloco no teto -> Janela fica abaixo dele
            AttachFace.CEILING -> {
                offsetY -= distance // -Y desce
            }
            // Bloco na parede -> Janela se desloca para fora da parede com base no FACING
            AttachFace.WALL -> {//depois alterar a direçao/rotaçao em um dos eixo em relação ao lado do player
                when (facing) {
                    Direction.NORTH -> offsetZ -= distance
                    Direction.SOUTH -> offsetZ += distance
                    Direction.WEST -> offsetX -= distance
                    Direction.EAST -> offsetX += distance
                    else -> {}
                }
            }
        }

        // --- Renderização Fallback (Sem Entidade Vinculada) ---
        val clientLevel = pBlockEntity.level as? ClientLevel
        val camera = Minecraft.getInstance().gameRenderer.mainCamera
        val cameraRight = Vector3f(1f, 0f, 0f).rotate(camera.rotation())
        val projection = DummyWindowProjection(
            offsetX,
            offsetY,
            offsetZ,
            cameraRight.x.toDouble(),
            cameraRight.z.toDouble()
        )
        val entity = clientLevel?.let { resolveTarget(pBlockEntity, it) } ?: run {
            FloatingWindow.Builder()
                .setOffset(offsetX, offsetY, offsetZ)
                .setSize(16f, 16f)
                .setAlignment(WindowAlignment.CENTER)
                .setPadding(1f)
                .setBorder(WindowColor.Rainbow(speed = 1.0f), width = 0.5f)
                .setBackground(WindowColor.Solid(0xDD000000))
                .setFaceCamera(true)
                .build()
                .render(poseStack, buffer, packedLight)

            return
        }

        DummyRenderer.renderFloatingWindowModo(
            entity,
            partialTick,
            poseStack,
            buffer,
            packedLight,
            projection
        )
    }

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