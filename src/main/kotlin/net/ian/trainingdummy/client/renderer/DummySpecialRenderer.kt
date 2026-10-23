package net.ian.trainingdummy.client.renderer

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.serialization.MapCodec
import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.client.model.DummyModel
import net.ian.trainingdummy.entity.DummyEntity
import net.ian.trainingdummy.entity.ModEntities
import net.ian.trainingdummy.entity.entityrender.DummyRenderer
import net.ian.trainingdummy.item.ModItems
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.PlayerSkinRenderCache
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.special.SpecialModelRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemContainerContents
import net.minecraft.world.item.component.ResolvableProfile
import org.joml.Vector3fc
import java.util.function.Consumer

class DummySpecialRenderer(
    private val playerSkinRenderCache: PlayerSkinRenderCache,
    private val model: DummyModel
) : SpecialModelRenderer<DummySpecialRenderer.ItemData> {

    data class ItemData(
        val profile: ResolvableProfile?,
        val customName: Component?,
        val container: ItemContainerContents
    )

    override fun extractArgument(stack: ItemStack): ItemData {
        val profile: ResolvableProfile? = stack.get(DataComponents.PROFILE)
        val customName: Component? = stack.get(DataComponents.CUSTOM_NAME)
        val container: ItemContainerContents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
        return ItemData(profile, customName, container)
    }

    override fun submit(
        argument: ItemData?,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        light: Int,
        overlay: Int,
        hasFoil: Boolean,
        color: Int
    ) {
        val mc = Minecraft.getInstance()
        val level = mc.level
        if (level != null && renderDepth < MAX_RENDER_DEPTH) {
            if (dummyCache == null || dummyCache?.level() != level) {
                dummyCache = ModEntities.DUMMY.get().create(level, EntitySpawnReason.LOAD)?.apply {
                    id = -4200
                }
            }
            val dummyToRender = dummyCache
            val renderer = if (dummyToRender != null) {
                mc.entityRenderDispatcher.getRenderer(dummyToRender) as? DummyRenderer
            } else {
                null
            }

            if (dummyToRender != null && renderer != null) {
                dummyToRender.tickCount = mc.player?.tickCount ?: 0
                dummyToRender.yRot = 0f
                dummyToRender.yRotO = 0f
                dummyToRender.yBodyRot = 0f
                dummyToRender.yBodyRotO = 0f
                dummyToRender.yHeadRot = 0f
                dummyToRender.yHeadRotO = 0f
                dummyToRender.xRot = 0f
                dummyToRender.xRotO = 0f

                for (i in 0 until dummyToRender.dummyInventory.slots) {
                    dummyToRender.dummyInventory.setStackInSlot(i, ItemStack.EMPTY)
                }
                dummyToRender.applyCustomName(null)
                dummyToRender.profilePlayer = null

                if (argument != null) {
                    val itemsList = NonNullList.withSize(dummyToRender.dummyInventory.slots, ItemStack.EMPTY)
                    argument.container.copyInto(itemsList)
                    for (i in 0 until dummyToRender.dummyInventory.slots) {
                        val slotStack = itemsList.getOrElse(i) { ItemStack.EMPTY }.copy()
                        if ((i == 5 || i == 6) && slotStack.`is`(ModItems.DUMMY_ITEM_SPAWN)) {
                            slotStack.remove(DataComponents.PROFILE)
                            slotStack.remove(DataComponents.CONTAINER)
                        }
                        dummyToRender.dummyInventory.setStackInSlot(i, slotStack)
                    }
                    if (argument.customName != null) {
                        dummyToRender.customName = argument.customName
                    }
                    if (argument.profile != null) {
                        dummyToRender.profilePlayer = argument.profile
                    }
                }

                val partialTick = mc.deltaTracker.getGameTimeDeltaPartialTick(true)
                val state = renderer.createRenderState()
                renderer.extractRenderState(dummyToRender, state, partialTick)
                state.lightCoords = light
                state.outlineColor = color
                state.isGui = true
                state.nameTag = null
                state.shadowRadius = 0f
                state.shadowPieces.clear()

                poseStack.pushPose()
                poseStack.translate(0.5, 0.0, 0.5)
                poseStack.scale(0.45f, 0.45f, 0.45f)
                renderDepth++
                try {
                    renderer.submit(state, poseStack, collector, CameraRenderState())
                    if (hasFoil) {
                        poseStack.pushPose()
                        poseStack.scale(-1.0f, -1.0f, 1.0f)
                        poseStack.translate(0.0, -1.501, 0.0)
                        collector.order(1).submitModelPart(
                            model.modelRoot,
                            poseStack,
                            RenderTypes.entityGlint(),
                            light,
                            overlay,
                            null
                        )
                        poseStack.popPose()
                    }
                } finally {
                    renderDepth--
                    poseStack.popPose()
                }
                return
            }
        }

        poseStack.pushPose()
        model.modelRoot.allParts.forEach { it.resetPose() }

        poseStack.translate(0.5, 0.0, 0.5)
        poseStack.scale(0.45f, -0.45f, -0.45f)
        poseStack.translate(0.0, -1.5, 0.0)

        val renderType = model.renderType(Identifier.fromNamespaceAndPath(TrainingDummy.ID, "textures/entity/training_dummy.png"))
        val tintColor = if (color != 0) color else -1

        collector.submitModelPart(
            model.modelRoot,
            poseStack,
            renderType,
            light,
            overlay,
            null,
            tintColor,
            null
        )

        if (hasFoil) {
            collector.order(1).submitModelPart(
                model.modelRoot,
                poseStack,
                RenderTypes.entityGlint(),
                light,
                overlay,
                null
            )
        }
        poseStack.popPose()
    }

    override fun getExtents(consumer: Consumer<Vector3fc>) {
        val poseStack = PoseStack()
        model.modelRoot.allParts.forEach { it.resetPose() }
        poseStack.translate(0.5, 0.0, 0.5)
        poseStack.scale(0.45f, -0.45f, -0.45f)
        poseStack.translate(0.0, -1.5, 0.0)
        model.modelRoot.getExtentsForGui(poseStack, consumer)
    }

    class Unbaked : SpecialModelRenderer.Unbaked<ItemData> {
        override fun type(): MapCodec<Unbaked> = MAP_CODEC

        override fun bake(context: SpecialModelRenderer.BakingContext): SpecialModelRenderer<ItemData> {
            val model = DummyModel(context.entityModelSet().bakeLayer(DummyModel.LAYER_LOCATION))
            return DummySpecialRenderer(context.playerSkinRenderCache(), model)
        }

        companion object {
            val MAP_CODEC: MapCodec<Unbaked> = MapCodec.unit(::Unbaked)
        }
    }

    companion object {
        private const val MAX_RENDER_DEPTH = 1
        private var renderDepth = 0
        private var dummyCache: DummyEntity? = null
    }
}
