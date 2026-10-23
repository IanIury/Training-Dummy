package net.ian.trainingdummy.item.custom

import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.block.ModBlockEntities
import net.ian.trainingdummy.block.entityblock.DisplayDummyBlockEntity
import net.ian.trainingdummy.entity.DummyEntity
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemContainerContents
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.item.component.TypedEntityData
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import java.util.UUID
import java.util.function.Consumer

class DisplayDummyBlockItem(block: Block, properties: Properties) : BlockItem(block, properties) {

    @Suppress("OVERRIDE_DEPRECATION")
    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltipDisplay: TooltipDisplay,
        tooltipComponents: Consumer<Component>,
        tooltipFlag: TooltipFlag
    ) {
        val customData = stack.get(DataComponents.CUSTOM_DATA)
        if (customData != null && customData.contains("dummy_uuid")) {
            val tag = customData.copyTag()
            val name = tag.getStringOr("dummy_name", "Training Dummy")
            val x = if (tag.contains("dummy_x")) tag.getDoubleOr("dummy_x", 0.0) else null
            val y = if (tag.contains("dummy_y")) tag.getDoubleOr("dummy_y", 0.0) else null
            val z = if (tag.contains("dummy_z")) tag.getDoubleOr("dummy_z", 0.0) else null
            val dim = if (tag.contains("dummy_dimension")) tag.getStringOr("dummy_dimension", "") else null
            val mode = if (tag.contains("dummy_mode")) tag.getStringOr("dummy_mode", "") else null
            val uuidStr = tag.getStringOr("dummy_uuid", "")

            tooltipComponents.accept(
                Component.translatable("item.trainingdummy.display_dummy.tooltip.linked_header")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            )
            tooltipComponents.accept(
                Component.literal("  ")
                    .append(Component.translatable("item.trainingdummy.display_dummy.tooltip.name").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
            )
            if (x != null && y != null && z != null) {
                tooltipComponents.accept(
                    Component.literal("  ")
                        .append(Component.translatable("item.trainingdummy.display_dummy.tooltip.location").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(String.format("X: %.1f, Y: %.1f, Z: %.1f", x, y, z)).withStyle(ChatFormatting.YELLOW))
                )

                val clientPlayer = try { net.minecraft.client.Minecraft.getInstance().player } catch (_: Throwable) { null }
                if (clientPlayer != null && dim != null && clientPlayer.level().dimension().identifier().toString() == dim) {
                    val dist = kotlin.math.sqrt(clientPlayer.distanceToSqr(x, y, z)).toInt()
                    tooltipComponents.accept(
                        Component.literal("  ")
                            .append(Component.translatable("item.trainingdummy.display_dummy.tooltip.distance").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal("${dist}m").withStyle(ChatFormatting.WHITE))
                    )
                } else if (dim != null) {
                    val dimShort = dim.substringAfter(":")
                    tooltipComponents.accept(
                        Component.literal("  ")
                            .append(Component.translatable("item.trainingdummy.display_dummy.tooltip.dimension").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(dimShort).withStyle(ChatFormatting.LIGHT_PURPLE))
                    )
                }
            }
            if (!mode.isNullOrEmpty()) {
                tooltipComponents.accept(
                    Component.literal("  ")
                        .append(Component.translatable("item.trainingdummy.display_dummy.tooltip.mode").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(mode).withStyle(ChatFormatting.GREEN))
                )
            }
            val shortUuid = if (uuidStr.length >= 8) uuidStr.substring(0, 8) + "..." else uuidStr
            tooltipComponents.accept(
                Component.literal("  §8UUID: $shortUuid")
            )
            tooltipComponents.accept(CommonComponents.EMPTY)
        } else {
            tooltipComponents.accept(
                Component.translatable("item.trainingdummy.display_dummy.tooltip.link")
                    .withStyle(ChatFormatting.GRAY)
            )
        }
        tooltipComponents.accept(
            Component.translatable("item.trainingdummy.display_dummy.tooltip.place")
                .withStyle(ChatFormatting.DARK_GRAY)
        )
    }

    override fun interactLivingEntity(
        stack: ItemStack,
        player: Player,
        interactionTarget: LivingEntity,
        hand: InteractionHand
    ): InteractionResult {
        if (interactionTarget is DummyEntity) {
            if (!player.level().isClientSide) {
                val uuidStr = interactionTarget.uuid.toString()
                val dummyName = if (interactionTarget.hasCustomName()) {
                    interactionTarget.customName?.string ?: "Training Dummy"
                } else {
                    "Boneco de Treino"
                }
                val mode = interactionTarget.trainingModuleConfig.trainingModo.name
                val dim = player.level().dimension().identifier().toString()
                val posX = interactionTarget.x
                val posY = interactionTarget.y
                val posZ = interactionTarget.z

                // Grava no CUSTOM_DATA do Item (para leitura livre no Item)
                CustomData.update(DataComponents.CUSTOM_DATA, stack) { tag ->
                    tag.putString("dummy_uuid", uuidStr)
                    tag.putString("dummy_name", dummyName)
                    tag.putDouble("dummy_x", posX)
                    tag.putDouble("dummy_y", posY)
                    tag.putDouble("dummy_z", posZ)
                    tag.putString("dummy_dimension", dim)
                    tag.putString("dummy_mode", mode)
                }

                // Grava no BLOCK_ENTITY_DATA contendo o ID registrado do bloco
                val beTag = CompoundTag()
                beTag.putString("dummy_uuid", uuidStr)
                beTag.putString("dummy_name", dummyName)
                beTag.putDouble("dummy_x", posX)
                beTag.putDouble("dummy_y", posY)
                beTag.putDouble("dummy_z", posZ)
                beTag.putString("dummy_dimension", dim)
                beTag.putString("dummy_mode", mode)
                stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(ModBlockEntities.DISPLAY_DUMMY_BE, beTag))

                val profile = interactionTarget.profilePlayer
                if (profile != null) {
                    stack.set(DataComponents.PROFILE, profile)
                } else {
                    stack.remove(DataComponents.PROFILE)
                }

                val itemsList = NonNullList.withSize(interactionTarget.dummyInventory.slots, ItemStack.EMPTY)
                for (i in 0 until interactionTarget.dummyInventory.slots) {
                    itemsList[i] = interactionTarget.dummyInventory.getStackInSlot(i).copy()
                }
                stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(itemsList))

                player.setItemInHand(hand, stack)

                // Som de confirmação
                player.level().playSound(
                    null,
                    interactionTarget.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.2f
                )

                // Partículas visuais em volta do dummy
                val serverLevel = player.level() as? ServerLevel
                serverLevel?.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    posX, posY + 1.0, posZ,
                    15,
                    0.3, 0.4, 0.3,
                    0.05
                )
                serverLevel?.sendParticles(
                    ParticleTypes.WAX_ON,
                    posX, posY + 1.0, posZ,
                    10,
                    0.25, 0.35, 0.25,
                    0.02
                )

                // Mensagem formatada no chat e na barra de ação
                val coordsFormatted = String.format("X: %.1f, Y: %.1f, Z: %.1f", posX, posY, posZ)
                player.sendOverlayMessage(
                    Component.translatable("item.trainingdummy.display_dummy.linked_actionbar", dummyName)
                        .withStyle(ChatFormatting.GREEN)
                )
                player.sendSystemMessage(
                    Component.translatable("item.trainingdummy.display_dummy.linked_chat", dummyName, coordsFormatted)
                        .withStyle(ChatFormatting.GREEN)
                )
            }
            return InteractionResult.SUCCESS
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
                val tag = customData.copyTag()
                val uuidStr = tag.getStringOr("dummy_uuid", "")
                blockEntity.target_entity_uuid = try {
                    UUID.fromString(uuidStr)
                } catch (_: IllegalArgumentException) {
                    TrainingDummy.LOGGER.warn("Ignoring invalid dummy UUID '{}' in display block item", uuidStr)
                    null
                }
                if (tag.contains("dummy_name")) blockEntity.dummy_name = tag.getStringOr("dummy_name", "")
                if (tag.contains("dummy_x")) blockEntity.dummy_x = tag.getDoubleOr("dummy_x", 0.0)
                if (tag.contains("dummy_y")) blockEntity.dummy_y = tag.getDoubleOr("dummy_y", 0.0)
                if (tag.contains("dummy_z")) blockEntity.dummy_z = tag.getDoubleOr("dummy_z", 0.0)
                if (tag.contains("dummy_dimension")) blockEntity.dummy_dim = tag.getStringOr("dummy_dimension", "")
                if (tag.contains("dummy_mode")) blockEntity.dummy_mode = tag.getStringOr("dummy_mode", "")

                blockEntity.setChanged()
                level.sendBlockUpdated(pos, state, state, 3)
            }
        }
        return result
    }
}
