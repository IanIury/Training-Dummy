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
import net.minecraft.util.Mth
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
                dummy.tickCount = player.tickCount
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

        val isMainHand = event.hand == InteractionHand.MAIN_HAND
        val arm = if (isMainHand) player.mainArm else player.mainArm.opposite
        val isRight = arm == HumanoidArm.RIGHT
        val sideOffset = if (isRight) 1.0f else -1.0f

        // 1. Transformação exata do braço em primeira pessoa (ItemInHandRenderer.applyItemArmTransform + swingArm)
        poseStack.translate(sideOffset * 0.56f, -0.52f + event.equipProgress * -0.6f, -0.72f)
        val swing = event.swingProgress
        if (swing > 0.0f) {
            val f = sin(swing * swing * Math.PI.toFloat())
            val f1 = sin(Mth.sqrt(swing) * Math.PI.toFloat())
            val f2 = sin(Mth.sqrt(swing) * (Math.PI.toFloat() * 2.0f))
            poseStack.translate(sideOffset * (-0.4f * f1), 0.2f * f2, -0.2f * f)
            poseStack.mulPose(Axis.YP.rotationDegrees(sideOffset * (45.0f + f * -20.0f)))
            poseStack.mulPose(Axis.ZP.rotationDegrees(sideOffset * f1 * -20.0f))
            poseStack.mulPose(Axis.XP.rotationDegrees(f1 * -80.0f))
            poseStack.mulPose(Axis.YP.rotationDegrees(sideOffset * -45.0f))
        }

        // 2. Transformação firstperson_righthand / firstperson_lefthand do modelo display_dummy.json:
        // translation = [3, 6, 0] pixels (3/16, 6/16, 0), scale = [0.68, 0.68, 0.68], seguida do offset de modelo (-0.5, -0.5, -0.5)
        poseStack.translate(sideOffset * (3.0f / 16.0f), 6.0f / 16.0f, 0.0f)
        poseStack.scale(0.68f, 0.68f, 0.68f)
        poseStack.translate(-0.5f, -0.5f, -0.5f)

        // 3. Centraliza exatamente no ponto de pivot [8, 0, 8] do modelo display_dummy (origin = [8, 0, 8] em pixels),
        // posicionando a base do holograma logo acima da haste/barra superior do projetor (topo em y = 3.8/16)
        val pivotX = 8.0f / 16.0f
        val pivotY = 4.1f / 16.0f
        val pivotZ = 8.0f / 16.0f

        val time = player.tickCount + event.partialTick
        val floatOffset = sin(time * 0.1f) * 0.02f
        poseStack.translate(pivotX, pivotY + floatOffset, pivotZ)

        val holoScale = 0.235f
        poseStack.scale(holoScale, holoScale, holoScale)
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 2.5f))

        val state = renderer.createRenderState()
        renderer.extractRenderState(dummyToRender, state, event.partialTick)
        state.lightCoords = 0xF000F0
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
