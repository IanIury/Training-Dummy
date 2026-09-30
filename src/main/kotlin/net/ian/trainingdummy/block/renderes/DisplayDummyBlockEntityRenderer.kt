package net.ian.trainingdummy.block.renderes

import com.mojang.blaze3d.vertex.PoseStack
import net.ian.trainingdummy.block.custom.DisplayDummyBlock
import net.ian.trainingdummy.block.entityblock.DisplayDummyBlockEntity
import net.ian.trainingdummy.client.window.FloatingWindow
import net.ian.trainingdummy.client.window.WindowAlignment
import net.ian.trainingdummy.client.window.WindowColor
import net.ian.trainingdummy.entity.DummyEntity
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.properties.AttachFace

class DisplayDummyBlockEntityRenderer(context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<DisplayDummyBlockEntity> {

    override fun render(
        pBlockEntity: DisplayDummyBlockEntity,
        pPartialTick: Float,
        pose: PoseStack,
        buffer: MultiBufferSource,
        pPacketLight: Int,
        pPackedOverlay: Int
    ) {
        renderFloatingWindowDefault(pBlockEntity, pose, buffer, pPacketLight)
    }

    private fun renderFloatingWindowDefault(
        pBlockEntity: DisplayDummyBlockEntity,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        val state = pBlockEntity.blockState

        // Recupera as propriedades do estado do bloco (com valores de fallback seguros)
        val face = if (state.hasProperty(DisplayDummyBlock.FACE)) state.getValue(DisplayDummyBlock.FACE) else AttachFace.FLOOR
        val facing = if (state.hasProperty(DisplayDummyBlock.FACING)) state.getValue(DisplayDummyBlock.FACING) else Direction.NORTH

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
                    Direction.WEST  -> offsetX -= distance
                    Direction.EAST  -> offsetX += distance
                    else -> {}
                }
            }
        }

        // --- Renderização Fallback (Sem Entidade Vinculada) ---
        val entity = pBlockEntity.getEntityByUUID() as? DummyEntity ?: run {
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

        val damageData = entity.damageData

        val window = FloatingWindow.Builder()
            .setOffset(offsetX, offsetY, offsetZ)
            .setSize(
                24f,
                24f,
                autoScaleContent = true,
                autoFitWidth = true,
                autoFitHeight = true
            )
            .setAlignment(WindowAlignment.CENTER)
            .setPadding(1f)
            .setBorder(
                WindowColor.AnimatedGradient(
                    argbStart = 0xFFF154,
                    argbEnd = 0xDCE5FC,
                    speed = 5.0f,
                    scale = 1.0f
                )
            )
            .setBackground(WindowColor.Solid(0xDD000000))

        var finaldamage = ""

        if (damageData.isReductions()) {
            window.addText("Raw Damage: §c%.1f".format(damageData.originalDamage))
                .addSeparator(color = WindowColor.Rainbow(speed = 1.0f), thickness = 0.5f, margin = 2f)
                .addText("Reductions", color = 0x0087F0)
        } else {
            window.setPadding(3f)
        }

        if (damageData.armor > 0.0f) { window.addText("Armor: -§a%.1f".format(damageData.armor)) }
        if (damageData.enchantments > 0.0f) { window.addText(" Enchantments: -§a%.1f".format(damageData.enchantments)) }
        if (damageData.mobEffects > 0.0f) { window.addText("Effects: -§a%.1f".format(damageData.mobEffects)) }
        if (damageData.absorption > 0.0f) { window.addText("Absorption: -§a%.1f".format(damageData.absorption)) }
        if (damageData.innateResistance > 0.0f) { window.addText("Innate Resistance: -§a%.1f".format(damageData.absorption)) }
        if (damageData.invulnerability > 0.0f) { window.addText("Invulnerability: -§a%.1f".format(damageData.invulnerability)) }

        if (damageData.isReductions()) {
            window.addSeparator(color = WindowColor.Rainbow(speed = 1.0f), thickness = 0.5f, margin = 2f)
            finaldamage = "Final: %.1f".format(damageData.newDamage)
        } else {
            finaldamage = "%.1f".format(damageData.newDamage)
        }

        if (damageData.isCriticalHit) {
            window.addText(
                "$finaldamage x${damageData.damageMultiplier}",
                color = WindowColor.AnimatedGradient(
                    argbStart = 0xFF0000,
                    argbEnd = 0xFFFFFF,
                    speed = 5.0f,
                    scale = 1.0f
                )
            )
        } else {
            window.addText(finaldamage)
        }

        window.build().render(poseStack, buffer, packedLight)
    }
}