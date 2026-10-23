package net.ian.trainingdummy.item.custom

import net.ian.trainingdummy.entity.DummyEntity
import net.ian.trainingdummy.entity.ModEntities
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemContainerContents
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.Vec3

class TrainingDummySpawnItem(properties: Properties) : Item(properties) {

    override fun interactLivingEntity(
        stack: ItemStack,
        player: Player,
        interactionTarget: LivingEntity,
        usedHand: InteractionHand
    ): InteractionResult {

        if (interactionTarget is DummyEntity) {

            // No lado do cliente, apenas confirmamos a ação para rodar a animação da mão
            if (interactionTarget.level().isClientSide) {
                return InteractionResult.SUCCESS
            }

            val profilePlayer: ResolvableProfile? = interactionTarget.profilePlayer
            val customName = interactionTarget.customName
            val itemsListDummy = NonNullList.withSize(interactionTarget.dummyInventory.slots, ItemStack.EMPTY)
            for (i in 0 until interactionTarget.dummyInventory.slots) {
                itemsListDummy[i] = interactionTarget.dummyInventory.getStackInSlot(i).copy()
            }

            interactionTarget.updateDummyEquipmentUsingAnItem(stack)
            val stackCustomName = stack.get(DataComponents.CUSTOM_NAME)
            if (stackCustomName != null) {
                interactionTarget.customName = stackCustomName
            } else {
                interactionTarget.customName = null
            }
            stack.get(DataComponents.PROFILE)?.let {
                interactionTarget.profilePlayer = it
            }

            // Salva o inventário, perfil e nome customizado direto no ItemStack
            if (profilePlayer != null) {
                stack.set(DataComponents.PROFILE, profilePlayer)
            } else {
                stack.remove(DataComponents.PROFILE)
            }
            if (customName != null) {
                stack.set(DataComponents.CUSTOM_NAME, customName)
            } else {
                stack.remove(DataComponents.CUSTOM_NAME)
            }
            stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(itemsListDummy))

            interactionTarget.playSound(
                SoundEvents.ARMOR_EQUIP_GENERIC.value(),
                1.0f,
                1.0f
            )

            return InteractionResult.SUCCESS
        }

        return super.interactLivingEntity(stack, player, interactionTarget, usedHand)
    }

    override fun useOn(context: UseOnContext): InteractionResult {

        val direction = context.clickedFace
        if (direction == Direction.DOWN) {
            return InteractionResult.FAIL
        }

        val level = context.level
        val blockplacecontext = BlockPlaceContext(context)
        val blockpos = blockplacecontext.clickedPos
        val itemstack = context.itemInHand
        val vec3 = Vec3.atBottomCenterOf(blockpos)
        val aabb = ModEntities.DUMMY.get().dimensions.makeBoundingBox(vec3)

        if (
            level.noCollision(null as Entity?, aabb) &&
            level.getEntities(null as Entity?, aabb).isEmpty()
        ) {

            if (level is ServerLevel) {

                val serverlevel = level

                val dummy = ModEntities.DUMMY.get().create(serverlevel, EntitySpawnReason.SPAWN_ITEM_USE)
                    ?: return InteractionResult.FAIL

                dummy.updateDummyEquipmentUsingAnItem(itemstack)
                dummy.applyCompoundTagsFromItem(itemstack)

                // Coloca na posição do bloco
                dummy.snapTo(
                    blockpos.x + 0.5,
                    blockpos.y.toDouble(),
                    blockpos.z + 0.5,
                    0.0f,
                    0.0f
                )

                // Mesma lógica de rotação do Armor Stand
                val rotation =
                    Mth.floor(
                        (Mth.wrapDegrees(context.rotation - 180.0f) + 22.5f) / 45.0f
                    ).toFloat() * 45.0f

                dummy.yRot = rotation
                dummy.yBodyRot = rotation
                dummy.yHeadRot = rotation

                // Adiciona no mundo
                serverlevel.addFreshEntityWithPassengers(dummy)
                level.broadcastEntityEvent(dummy, 42)

                level.playSound(
                    null as Player?,
                    dummy.x,
                    dummy.y,
                    dummy.z,
                    SoundEvents.ARMOR_STAND_PLACE,
                    SoundSource.BLOCKS,
                    0.75f,
                    0.8f
                )

                // Evento de colocação
                dummy.gameEvent(
                    GameEvent.ENTITY_PLACE,
                    context.player
                )

                itemstack.shrink(1)
            }

            return InteractionResult.SUCCESS

        } else {
            return InteractionResult.FAIL
        }
    }
}
