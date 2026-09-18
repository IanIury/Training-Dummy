package net.ian.trainingdummy.item.custom

import net.ian.trainingdummy.entity.ModEntities
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ArmorStandItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.Vec3

class TrainingDummySpawnItem(properties: Properties) : Item(properties) {

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

                // Cria sua DummyEntityO
                val dummy = ModEntities.DUMMY.get().create(serverlevel)
                    ?: return InteractionResult.FAIL

                // Coloca na posição do bloco
                dummy.moveTo(
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

                // Som
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



            return InteractionResult.sidedSuccess(level.isClientSide)

        } else {
            return InteractionResult.FAIL
        }
    }
}