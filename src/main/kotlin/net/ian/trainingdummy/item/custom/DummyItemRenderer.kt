package net.ian.trainingdummy.item.custom

import com.mojang.blaze3d.vertex.PoseStack
import net.ian.trainingdummy.entity.DummyEntity
import net.ian.trainingdummy.entity.ModEntities
import net.ian.trainingdummy.init.ModDataComponents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Quaternionf

class DummyItemRenderer : BlockEntityWithoutLevelRenderer(
    Minecraft.getInstance().blockEntityRenderDispatcher,
    Minecraft.getInstance().entityModels
) {
    private var dummyCache: DummyEntity? = null

    override fun renderByItem(
        stack: ItemStack,
        displayContext: ItemDisplayContext,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val level = Minecraft.getInstance().level ?: return

        // 1. Instancia a entidade de teste/cache se não existir
        if (dummyCache == null || dummyCache?.level() != level) {
            dummyCache = DummyEntity(ModEntities.DUMMY.get(), level)
        }

        val dummy = dummyCache ?: return

        // 2. Transfere os equipamentos do ItemStack para a Entidade fictícia
        // Adapte com base em como você salva os dados no ItemStack (Data Components)
        updateDummyEquipment(dummy, stack)

        // 3. Ajusta Transformações da PoseStack dependendo de onde o item está renderizando
        poseStack.pushPose()

        when (displayContext) {
            ItemDisplayContext.GUI -> {
                poseStack.translate(0.5, 0.05, 0.0)
                poseStack.scale(0.45f, 0.45f, 0.45f)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(35f))
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(15f))
            }
            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.FIRST_PERSON_LEFT_HAND -> {
                poseStack.translate(0.5, 0.3, 0.5)
                poseStack.scale(0.4f, 0.4f, 0.4f)
            }
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.5, 0.2, 0.5)
                poseStack.scale(0.35f, 0.35f, 0.35f)
            }
            ItemDisplayContext.GROUND -> {
                poseStack.translate(0.5, 0.3, 0.5)
                poseStack.scale(0.25f, 0.25f, 0.25f)
            }

            else -> {

                poseStack.translate(0.5, 0.0, 0.5)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f))
                poseStack.scale(0.4f, 0.4f, 0.4f)
            }
        }

        // 4. Renderiza a Entidade usando o seu EntityRenderDispatcher oficial
        val entityRenderDispatcher = Minecraft.getInstance().entityRenderDispatcher
        entityRenderDispatcher.render(
            dummy,
            0.0, 0.0, 0.0, // x, y, z locais na PoseStack
            0.0f,          // Yaw
            1.0f,          // PartialTicks
            poseStack,
            buffer,
            packedLight
        )

        poseStack.popPose()
    }

    private fun updateDummyEquipment(dummy: DummyEntity, stack: ItemStack) {
        // Limpa slots anteriores
        EquipmentSlot.entries.forEach { slot -> dummy.setItemSlot(slot, ItemStack.EMPTY) }

        // Leia as armaduras/itens salvos no CustomData / DataComponent do 'stack'

        val armorContainer = stack.get(ModDataComponents.DUMMY_DATA) ?: return
        dummy.setItemSlot(EquipmentSlot.FEET, armorContainer.armor[0])
        dummy.setItemSlot(EquipmentSlot.LEGS, armorContainer.armor[1])
        dummy.setItemSlot(EquipmentSlot.CHEST, armorContainer.armor[2])
        dummy.setItemSlot(EquipmentSlot.HEAD, armorContainer.armor[3])

        dummy.setItemSlot(EquipmentSlot.MAINHAND, armorContainer.hands[0])
        dummy.setItemSlot(EquipmentSlot.OFFHAND, armorContainer.hands[1])


    }
}