package net.ian.trainingdummy.item.renderer

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.entity.DummyEntity
import net.ian.trainingdummy.entity.DummyEntity.Companion.DUMMY_DATA
import net.ian.trainingdummy.entity.ModEntities
import net.ian.trainingdummy.init.ModDataComponents
import net.ian.trainingdummy.item.ModItems
import net.ian.trainingdummy.item.custom.TrainingDummySpawnItem
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemContainerContents

class DummyItemRenderer : BlockEntityWithoutLevelRenderer(
    Minecraft.getInstance().blockEntityRenderDispatcher,
    Minecraft.getInstance().entityModels
) {
    private var dummyCache: DummyEntity? = null
    private var isRendering: Boolean = false

    //private var lastRenderedStack: ItemStack = ItemStack.EMPTY

    override fun renderByItem(
        stack: ItemStack,
        displayContext: ItemDisplayContext,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val level = Minecraft.getInstance().level ?: return

        val dummyToRender = if(isRendering) {
            DummyEntity(ModEntities.DUMMY.get(), level)
        } else {
            if (dummyCache == null || dummyCache?.level() != level) {
                dummyCache = DummyEntity(ModEntities.DUMMY.get(), level)
            }
            dummyCache!!
        }
        val previousRenderingState = isRendering
        isRendering = true

        try {
            val profileOnItem = stack.get(DataComponents.PROFILE)
            if (dummyToRender.profilePlayer != profileOnItem) {
                dummyToRender.profilePlayer = profileOnItem
            }
            applyEquipmentSafely(dummyToRender, stack)
            poseStack.pushPose()

            applyPoseStackTransforms(displayContext, poseStack)

            val entityRenderDispatcher = Minecraft.getInstance().entityRenderDispatcher
            entityRenderDispatcher.render(
                dummyToRender,
                0.0, 0.0, 0.0,
                0.0f, 1.0f,
                poseStack, buffer, packedLight
            )

            poseStack.popPose()

        } finally {
            isRendering = previousRenderingState
        }

    }

    private fun applyPoseStackTransforms(displayContext: ItemDisplayContext, poseStack: PoseStack) {
        when (displayContext) {
            ItemDisplayContext.GUI -> {
                poseStack.translate(0.5, 0.05, 0.0)
                poseStack.scale(0.45f, 0.45f, 0.45f)
                poseStack.mulPose(Axis.YP.rotationDegrees(35f))
                poseStack.mulPose(Axis.ZP.rotationDegrees(15f))
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
                poseStack.mulPose(Axis.YP.rotationDegrees(180f))
                poseStack.scale(0.4f, 0.4f, 0.4f)
            }
        }
    }

    private fun applyEquipmentSafely(dummy: DummyEntity, stack: ItemStack) {

        stack.get(DataComponents.PROFILE)?.let { if(dummy.profilePlayer != it) dummy.profilePlayer = it }
        stack.get(DataComponents.CUSTOM_NAME)?.let { if(dummy.customName!=it) dummy.applyCustomName(it) }

        val containerContents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
        val itemsList = NonNullList.withSize(dummy.dummyInventory.slots, ItemStack.EMPTY)
        containerContents.copyInto(itemsList)

        for (i in 0 until dummy.dummyInventory.slots) {
            val originalItem = itemsList.getOrElse(i) { ItemStack.EMPTY }

            if (originalItem.`is`(ModItems.DUMMY_ITEM_SPAWN)) {

                val safeCopy = originalItem.copy()
                safeCopy.remove(DataComponents.PROFILE)
                dummy.dummyInventory.setStackInSlot(i, safeCopy)
            } else {
                dummy.dummyInventory.setStackInSlot(i, originalItem)
            }
        }

        dummy.setItemSlot(EquipmentSlot.HEAD, dummy.dummyInventory.getStackInSlot(0))
        dummy.setItemSlot(EquipmentSlot.CHEST, dummy.dummyInventory.getStackInSlot(1))
        dummy.setItemSlot(EquipmentSlot.LEGS, dummy.dummyInventory.getStackInSlot(2))
        dummy.setItemSlot(EquipmentSlot.FEET, dummy.dummyInventory.getStackInSlot(3))
        dummy.setItemSlot(EquipmentSlot.MAINHAND, dummy.dummyInventory.getStackInSlot(5))
        dummy.setItemSlot(EquipmentSlot.OFFHAND, dummy.dummyInventory.getStackInSlot(6))
    }

}