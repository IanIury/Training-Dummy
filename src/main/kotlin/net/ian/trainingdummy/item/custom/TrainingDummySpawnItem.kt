package net.ian.trainingdummy.item.custom

import net.ian.trainingdummy.entity.DummyEntity
import net.ian.trainingdummy.entity.ModEntities
import net.ian.trainingdummy.init.DummyData
import net.ian.trainingdummy.init.ModDataComponents
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ArmorStandItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.Vec3

class TrainingDummySpawnItem(properties: Properties) : Item(properties) {

    private fun updateDummyEquipment(dummy: DummyEntity, stack: ItemStack) {
        val data = stack.get(ModDataComponents.DUMMY_DATA.get()) ?: DummyData()

        dummy.setItemSlot(EquipmentSlot.FEET, data.armor[0])
        dummy.setItemSlot(EquipmentSlot.LEGS, data.armor[1])
        dummy.setItemSlot(EquipmentSlot.CHEST, data.armor[2])
        dummy.setItemSlot(EquipmentSlot.HEAD, data.armor[3])

        dummy.setItemSlot(EquipmentSlot.MAINHAND, data.hands[0])
        dummy.setItemSlot(EquipmentSlot.OFFHAND, data.hands[1])
    }

    override fun interactLivingEntity(
        stack: ItemStack,
        player: Player,
        interactionTarget: LivingEntity,
        usedHand: InteractionHand
    ): InteractionResult {
        // 1. Garante que o alvo é o DummyEntity
        if (interactionTarget is DummyEntity) {

            // No lado do cliente, apenas confirmamos a ação para rodar a animação da mão
            if (interactionTarget.level().isClientSide) {
                return InteractionResult.SUCCESS
            }

            // 2. Pega os dados atuais do Item na mão (se não houver componente, usa valores vazios padrão)
            val dataInItem = stack.get(ModDataComponents.DUMMY_DATA.get()) ?: DummyData()

            // 3. Salva os equipamentos ATUAIS do Dummy da entidade
            val currentEntityArmor = NonNullList.withSize(4, ItemStack.EMPTY).apply {
                this[0] = interactionTarget.getItemBySlot(EquipmentSlot.FEET).copy()
                this[1] = interactionTarget.getItemBySlot(EquipmentSlot.LEGS).copy()
                this[2] = interactionTarget.getItemBySlot(EquipmentSlot.CHEST).copy()
                this[3] = interactionTarget.getItemBySlot(EquipmentSlot.HEAD).copy()
            }

            val currentEntityHands = NonNullList.withSize(2, ItemStack.EMPTY).apply {
                this[0] = interactionTarget.getItemBySlot(EquipmentSlot.MAINHAND).copy()
                this[1] = interactionTarget.getItemBySlot(EquipmentSlot.OFFHAND).copy()
            }

            // 4. Aplica os itens do Item para a Entidade Dummy
            interactionTarget.setItemSlot(EquipmentSlot.FEET, dataInItem.armor.getOrElse(0) { ItemStack.EMPTY }.copy())
            interactionTarget.setItemSlot(EquipmentSlot.LEGS, dataInItem.armor.getOrElse(1) { ItemStack.EMPTY }.copy())
            interactionTarget.setItemSlot(EquipmentSlot.CHEST, dataInItem.armor.getOrElse(2) { ItemStack.EMPTY }.copy())
            interactionTarget.setItemSlot(EquipmentSlot.HEAD, dataInItem.armor.getOrElse(3) { ItemStack.EMPTY }.copy())

            interactionTarget.setItemSlot(EquipmentSlot.MAINHAND, dataInItem.hands.getOrElse(0) { ItemStack.EMPTY }.copy())
            interactionTarget.setItemSlot(EquipmentSlot.OFFHAND, dataInItem.hands.getOrElse(1) { ItemStack.EMPTY }.copy())

            // 5. Atualiza o NBT / DataComponent do Item na mão com o que estava na entidade
            val newDummyData = DummyData(
                armor = currentEntityArmor,
                hands = currentEntityHands,
            )

            stack.set(ModDataComponents.DUMMY_DATA.get(), newDummyData)

            // Toca um som de equipamento para dar feedback sonoro
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

                // Cria sua DummyEntityO
                val dummy = ModEntities.DUMMY.get().create(serverlevel)
                    ?: return InteractionResult.FAIL

                updateDummyEquipment(dummy,itemstack)

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
                level.broadcastEntityEvent(dummy,42)

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