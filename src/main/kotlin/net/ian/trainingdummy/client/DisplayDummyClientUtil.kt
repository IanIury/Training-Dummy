package net.ian.trainingdummy.client

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.entity.DummyEntity
import net.ian.trainingdummy.entity.ModEntities
import net.ian.trainingdummy.item.custom.DisplayDummyBlockItem
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.item.ItemStack
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderHandEvent
import java.util.UUID

@EventBusSubscriber(modid = TrainingDummy.ID, value = [Dist.CLIENT])
object DisplayDummyClientUtil {

    private var cachedDummy: DummyEntity? = null
    var isRenderingHandHologram = false
        private set

    fun isDummyTargetedByPlayer(targetUuid: UUID): Boolean {
        val player = Minecraft.getInstance().player ?: return false
        val uuidStr = targetUuid.toString()
        fun matches(stack: ItemStack): Boolean {
            if (stack.item !is DisplayDummyBlockItem) return false
            val customData = stack.get(DataComponents.CUSTOM_DATA) ?: return false
            return customData.contains("dummy_uuid") && customData.copyTag().getString("dummy_uuid") == uuidStr
        }
        return matches(player.mainHandItem) || matches(player.offhandItem)
    }

    @SubscribeEvent
    fun onRenderHand(event: RenderHandEvent) {
        val stack = event.itemStack
        if (stack.item !is DisplayDummyBlockItem) return

        val customData = stack.get(DataComponents.CUSTOM_DATA) ?: return
        if (!customData.contains("dummy_uuid")) return
        val uuidStr = customData.copyTag().getString("dummy_uuid")
        val targetUuid = try {
            UUID.fromString(uuidStr)
        } catch (_: IllegalArgumentException) {
            return
        }

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val level = mc.level ?: return

        if (isRenderingHandHologram) return
        isRenderingHandHologram = true

        val poseStack = event.poseStack
        poseStack.pushPose()
        try {
            val isMainHand = event.hand == InteractionHand.MAIN_HAND
            val arm = if (isMainHand) player.mainArm else player.mainArm.opposite
            val isRight = arm == HumanoidArm.RIGHT
            val sign = if (isRight) 1 else -1

            // Aplica os movimentos de sway e ataque da mão do jogador
            applyHandTransforms(poseStack, arm, event.equipProgress, event.swingProgress)

            // Centralizado diretamente sobre o topo do item DisplayDummy, com altura reduzida pela metade
            val time = player.tickCount + event.partialTick
            val bob = 0.010f * kotlin.math.sin(time * 0.12f)
            poseStack.translate(sign * 0.185, 0.23 + bob, 0.0)

            // Escala reduzida para holograma
            val scale = 0.22f
            poseStack.scale(scale, scale, scale)

            // Resolve target dummy entity to render
            val dummyToRender = resolveHologramDummy(level, stack, targetUuid)

            // Parado e sempre voltado para o player:
            // LivingEntityRenderer aplica automaticamente (180 - bodyYaw).
            // Ao aplicar (liveBodyYaw + headOffset), o yaw do boneco no mundo é cancelado com precisão matemática,
            // garantindo que a face do dummy fique sempre virada para a câmera, independentemente de
            // estar virado para North, South, East ou West no mapa.
            val liveBodyYaw = Mth.rotLerp(event.partialTick, dummyToRender.yBodyRotO, dummyToRender.yBodyRot)
            val liveHeadYaw = Mth.rotLerp(event.partialTick, dummyToRender.yHeadRotO, dummyToRender.yHeadRot)
            val headOffset = liveHeadYaw - liveBodyYaw
            poseStack.mulPose(Axis.YP.rotationDegrees(liveBodyYaw + headOffset + sign * -12.0f))
            poseStack.mulPose(Axis.XP.rotationDegrees(6.0f))

            val dispatcher = mc.entityRenderDispatcher
            dispatcher.setRenderShadow(false)
            try {
                dispatcher.render(
                    dummyToRender,
                    0.0, 0.0, 0.0,
                    0.0f, event.partialTick,
                    poseStack,
                    event.multiBufferSource,
                    15728880 // Max light (emissive holographic look)
                )
            } finally {
                dispatcher.setRenderShadow(true)
            }
        } finally {
            poseStack.popPose()
            isRenderingHandHologram = false
        }
    }

    private fun applyHandTransforms(
        poseStack: PoseStack,
        arm: HumanoidArm,
        equipProgress: Float,
        swingProgress: Float
    ) {
        val i = if (arm == HumanoidArm.RIGHT) 1 else -1

        val f5 = -0.4f * Mth.sin(Mth.sqrt(swingProgress) * Math.PI.toFloat())
        val f6 = 0.2f * Mth.sin(Mth.sqrt(swingProgress) * (Math.PI.toFloat() * 2f))
        val f10 = -0.2f * Mth.sin(swingProgress * Math.PI.toFloat())
        poseStack.translate(i * f5, f6, f10)

        poseStack.translate(i * 0.56f, -0.52f + equipProgress * -0.6f, -0.72f)

        val f = Mth.sin(swingProgress * swingProgress * Math.PI.toFloat())
        poseStack.mulPose(Axis.YP.rotationDegrees(i * (45.0f + f * -20.0f)))
        val f1 = Mth.sin(Mth.sqrt(swingProgress) * Math.PI.toFloat())
        poseStack.mulPose(Axis.ZP.rotationDegrees(i * f1 * -20.0f))
        poseStack.mulPose(Axis.XP.rotationDegrees(f1 * -80.0f))
        poseStack.mulPose(Axis.YP.rotationDegrees(i * -45.0f))
    }

    private fun resolveHologramDummy(level: ClientLevel, stack: ItemStack, targetUuid: UUID): DummyEntity {
        val live = level.entitiesForRendering().firstOrNull { it.uuid == targetUuid } as? DummyEntity
        if (live != null) return live

        var dummy = cachedDummy
        if (dummy == null || dummy.level() != level) {
            dummy = DummyEntity(ModEntities.DUMMY.get(), level)
            cachedDummy = dummy
        }

        val profile = stack.get(DataComponents.PROFILE)
        if (dummy.profilePlayer != profile) {
            dummy.profilePlayer = profile
        }
        val customData = stack.get(DataComponents.CUSTOM_DATA)
        if (customData != null) {
            val tag = customData.copyTag()
            if (tag.contains("dummy_name")) {
                dummy.customName = Component.literal(tag.getString("dummy_name"))
            }
        }
        return dummy
    }
}
