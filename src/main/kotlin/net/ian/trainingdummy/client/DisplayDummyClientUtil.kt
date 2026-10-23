package net.ian.trainingdummy.client

import com.mojang.math.Axis
import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.entity.DummyEntity
import net.ian.trainingdummy.entity.ModEntities
import net.ian.trainingdummy.entity.entityrender.DummyRenderer
import net.ian.trainingdummy.item.ModItems
import net.ian.trainingdummy.item.custom.DisplayDummyBlockItem
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemContainerContents
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderHandEvent
import java.util.UUID
import kotlin.math.sin

@EventBusSubscriber(modid = TrainingDummy.ID, value = [Dist.CLIENT])
object DisplayDummyClientUtil {

    private var fallbackDummy: DummyEntity? = null

    /**
     * Retorna se o dummy com [targetUuid] está vinculado ao item [DisplayDummyBlockItem]
     * atualmente segurado na mão principal ou secundária do jogador local.
     * Utilizado para ativar o efeito de brilho (glow) de destaque no dummy no mundo.
     */
    fun isDummyTargetedByPlayer(targetUuid: UUID): Boolean {
        val player = Minecraft.getInstance().player ?: return false
        val uuidStr = targetUuid.toString()
        fun matches(stack: ItemStack): Boolean {
            if (stack.item !is DisplayDummyBlockItem) return false
            val customData = stack.get(DataComponents.CUSTOM_DATA) ?: return false
            return customData.contains("dummy_uuid") && customData.copyTag().getStringOr("dummy_uuid", "") == uuidStr
        }
        return matches(player.mainHandItem) || matches(player.offhandItem)
    }

    @SubscribeEvent
    fun onRenderHand(event: RenderHandEvent) {
        val stack = event.itemStack
        if (stack.item !is DisplayDummyBlockItem) return

        val customData = stack.get(DataComponents.CUSTOM_DATA) ?: return
        if (!customData.contains("dummy_uuid")) return

        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val player = mc.player ?: return

        val tag = customData.copyTag()
        val uuidStr = tag.getStringOr("dummy_uuid", "")
        val targetUuid = try {
            UUID.fromString(uuidStr)
        } catch (_: IllegalArgumentException) {
            null
        }

        val worldDummy = if (targetUuid != null) {
            level.entitiesForRendering().firstOrNull { it is DummyEntity && it.uuid == targetUuid } as? DummyEntity
        } else {
            null
        }

        val dummyToRender: DummyEntity = worldDummy ?: run {
            if (fallbackDummy == null || fallbackDummy?.level() != level) {
                fallbackDummy = ModEntities.DUMMY.get().create(level, EntitySpawnReason.LOAD)?.apply {
                    id = -4210
                }
            }
            fallbackDummy?.also { dummy ->
                val container = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                val itemsList = NonNullList.withSize(dummy.dummyInventory.slots, ItemStack.EMPTY)
                container.copyInto(itemsList)
                for (i in 0 until dummy.dummyInventory.slots) {
                    val slotStack = itemsList.getOrElse(i) { ItemStack.EMPTY }.copy()
                    if ((i == 5 || i == 6) && slotStack.`is`(ModItems.DUMMY_ITEM_SPAWN)) {
                        slotStack.remove(DataComponents.PROFILE)
                        slotStack.remove(DataComponents.CONTAINER)
                    }
                    dummy.dummyInventory.setStackInSlot(i, slotStack)
                }

                val savedName = tag.getStringOr("dummy_name", "")
                if (savedName.isNotBlank() && savedName != "Boneco de Treino" && savedName != "Training Dummy") {
                    dummy.customName = Component.literal(savedName)
                } else {
                    dummy.applyCustomName(null)
                    dummy.profilePlayer = null
                }
                stack.get(DataComponents.PROFILE)?.let {
                    dummy.profilePlayer = it
                }
            }
        } ?: return

        val renderer = mc.entityRenderDispatcher.getRenderer(dummyToRender) as? DummyRenderer ?: return

        val poseStack = event.poseStack
        poseStack.pushPose()

        val isRight = (event.hand == InteractionHand.MAIN_HAND && mc.options.mainHand().get() == HumanoidArm.RIGHT) ||
            (event.hand == InteractionHand.OFF_HAND && mc.options.mainHand().get() == HumanoidArm.LEFT)
        val sideOffset = if (isRight) 1.0f else -1.0f

        // Posiciona a miniatura flutuando logo acima do item DisplayDummy segurado na mão
        poseStack.translate(sideOffset * 0.42f, -0.32f + event.equipProgress * -0.4f, -0.60f)

        val time = player.tickCount
        val floatOffset = sin((time + event.partialTick) * 0.1f) * 0.015f
        poseStack.translate(0.0f, floatOffset, 0.0f)

        val scale = 0.16f
        poseStack.scale(scale, scale, scale)
        poseStack.mulPose(Axis.YP.rotationDegrees((time + event.partialTick) * 2.5f))

        val state = renderer.createRenderState()
        renderer.extractRenderState(dummyToRender, state, event.partialTick)
        state.lightCoords = event.packedLight
        state.isGui = true
        state.nameTag = null
        state.shadowRadius = 0f
        state.shadowPieces.clear()
        state.bodyRot = 0f
        state.yRot = 0f
        state.xRot = 0f

        try {
            DummyRenderer.isRenderingHandHologram = true
            renderer.submit(state, poseStack, event.submitNodeCollector, CameraRenderState())
        } finally {
            DummyRenderer.isRenderingHandHologram = false
            poseStack.popPose()
        }
    }
}
