package net.ian.trainingdummy.item.custom

import net.ian.trainingdummy.block.entityblock.DisplayDummyBlockEntity
import net.ian.trainingdummy.entity.DummyEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import java.util.UUID

class DisplayDummyBlockItem(block: Block, properties: Properties) : BlockItem(block, properties) {

    override fun interactLivingEntity(
        stack: ItemStack,
        player: Player,
        interactionTarget: LivingEntity,
        hand: InteractionHand
    ): InteractionResult {
        if (interactionTarget is DummyEntity) {
            if (!player.level().isClientSide) {
                val uuidStr = interactionTarget.uuid.toString()

                //Grava no CUSTOM_DATA do Item (para leitura livre no Item)
                CustomData.update(DataComponents.CUSTOM_DATA, stack) { tag ->
                    tag.putString("dummy_uuid", uuidStr)
                }

                // Grava no BLOCK_ENTITY_DATA contendo o ID registrado do bloco
                CustomData.update(DataComponents.BLOCK_ENTITY_DATA, stack) { tag ->
                    tag.putString("id", "trainingdummy:display_dummy") // ID registrado da sua BlockEntity/Block
                    tag.putString("dummy_uuid", uuidStr)
                }

                player.setItemInHand(hand, stack)

                player.sendSystemMessage(Component.literal("§aDummy $uuidStr vinculado ao item com sucesso!"))
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide)
        }
        return InteractionResult.PASS
    }

    override fun updateCustomBlockEntityTag(
        pos: BlockPos,
        level: Level,
        player: Player?,
        stack: ItemStack,
        state: BlockState
    ): Boolean {
        val result = super.updateCustomBlockEntityTag(pos, level, player, stack, state)

        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos) as? DisplayDummyBlockEntity
            val customData = stack.get(DataComponents.CUSTOM_DATA)

            if (blockEntity != null && customData != null && customData.contains("dummy_uuid")) {
                val uuidStr = customData.copyTag().getString("dummy_uuid")
                blockEntity.target_entity_uuid = UUID.fromString(uuidStr)
                blockEntity.setChanged()
                level.sendBlockUpdated(pos, state, state, 3)
            }
        }
        return result
    }
}