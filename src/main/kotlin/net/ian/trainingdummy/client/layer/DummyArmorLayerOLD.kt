package net.ian.trainingdummy.client.layer

/*
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis

import net.ian.trainingdummy.entity.DummyEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.model.ElytraModel
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.EntityModelSet
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.resources.model.ModelManager
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorMaterial
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.AbstractSkullBlock
import net.neoforged.neoforge.client.ClientHooks
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions

class DummyArmorLayerOLD(
    renderer: RenderLayerParent<DummyEntity, DummyModelOLD<DummyEntity>>,
    modelSet: EntityModelSet,
    private val modelManager: ModelManager
) : RenderLayer<DummyEntity, DummyModelOLD<DummyEntity>>(renderer) {

    private val innerModel = HumanoidModel<DummyEntity>(modelSet.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR))
    private val outerModel = HumanoidModel<DummyEntity>(modelSet.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR))
    private val elytraModel = ElytraModel<DummyEntity>(modelSet.bakeLayer(ModelLayers.ELYTRA))

    private val ELYTRA_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/elytra.png")

    private val HEAD_SCALE = 1.3f
    private val CHEST_SCALE = 1.8f
    private val LEGS_FEET_SCALE = 1.5f

    private val HEAD_OFFSET_Y = -1.1
    private val CHEST_OFFSET_Y = -2.1
    private val LEGS_FEET_OFFSET_Y = -2.3

    override fun render(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        entity: DummyEntity,
        limbSwing: Float,
        limbSwingAmount: Float,
        partialTicks: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.HEAD, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch)
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.CHEST, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch)
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.LEGS, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch)
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.FEET, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch)
    }

    private fun renderSlot(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        entity: DummyEntity,
        slot: EquipmentSlot,
        limbSwing: Float,
        limbSwingAmount: Float,
        partialTick: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
        val stack = entity.getItemBySlot(slot)
        if (stack.isEmpty) return

        val dummyModel = parentModel
        poseStack.pushPose()

        when (slot) {
            EquipmentSlot.HEAD -> {
                dummyModel.pila2.translateAndRotate(poseStack)
                dummyModel.body.translateAndRotate(poseStack)
                dummyModel.head.translateAndRotate(poseStack)

                poseStack.translate(0.0, HEAD_OFFSET_Y, 0.0)
                poseStack.scale(HEAD_SCALE, HEAD_SCALE, HEAD_SCALE)

                val item = stack.item

                // 1. Cabeças de Mobs
                if (item is BlockItem && item.block is AbstractSkullBlock) {
                    val skullBlock = item.block as AbstractSkullBlock
                    val skullType = skullBlock.type
                    val profile = stack.get(DataComponents.PROFILE)

                    poseStack.translate(0.5, 0.815, -0.5)
                    poseStack.scale(-1.0f, -1.0f, 1.0f)

                    val skullModel = SkullBlockRenderer.createSkullRenderers(Minecraft.getInstance().entityModels)[skullType]
                    val renderType = SkullBlockRenderer.getRenderType(skullType, profile)

                    if (skullModel != null) {
                        SkullBlockRenderer.renderSkull(null, 0.0f, 0.0f, poseStack, buffer, packedLight, skullModel, renderType)
                    }
                }
                // 2. Armaduras Padrão (Vanilla e Mods)
                else if (item is ArmorItem) {
                    renderStandardArmor(poseStack, buffer, packedLight, entity, stack, slot, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch)
                }
                // 3. Fallback: Abóboras, Blocos ou Itens customizados de Mods que não estendem ArmorItem
                else {
                    poseStack.translate(0.0, 0.57, 0.0)
                    poseStack.mulPose(Axis.YP.rotationDegrees(180.0f))
                    poseStack.scale(0.5f, -0.5f, -0.5f)

                    Minecraft.getInstance().itemRenderer.renderStatic(
                        entity, stack, ItemDisplayContext.HEAD,
                        false, poseStack, buffer, entity.level(), packedLight, OverlayTexture.NO_OVERLAY, 0
                    )
                }
            }

            EquipmentSlot.CHEST -> {
                dummyModel.pila2.translateAndRotate(poseStack)
                dummyModel.body.translateAndRotate(poseStack)

                poseStack.translate(0.0, CHEST_OFFSET_Y, 0.0)
                poseStack.scale(CHEST_SCALE, CHEST_SCALE, CHEST_SCALE)

                if (stack.`is`(Items.ELYTRA)) {
                    renderElytra(poseStack, buffer, packedLight, entity, stack)
                } else if (stack.item is ArmorItem) {
                    renderStandardArmor(poseStack, buffer, packedLight, entity, stack, slot, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch)
                }
            }

            EquipmentSlot.LEGS, EquipmentSlot.FEET -> {
                dummyModel.pila2.translateAndRotate(poseStack)

                poseStack.translate(0.0, LEGS_FEET_OFFSET_Y, 0.0)
                poseStack.scale(LEGS_FEET_SCALE, LEGS_FEET_SCALE, LEGS_FEET_SCALE)

                if (stack.item is ArmorItem) {
                    renderStandardArmor(poseStack, buffer, packedLight, entity, stack, slot, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch)
                }
            }
            else -> {}
        }
        poseStack.popPose()
    }

    private fun renderStandardArmor(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        entity: DummyEntity,
        stack: ItemStack,
        slot: EquipmentSlot,
        limbSwing: Float,
        limbSwingAmount: Float,
        partialTick: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
        val item = stack.item as? ArmorItem ?: return
        val defaultModel = if (slot == EquipmentSlot.LEGS) innerModel else outerModel

        // 1. Obtém o modelo customizado (vital para mods que sobrescrevem o modelo padrão)
        val customModel = ClientHooks.getArmorModel(entity, stack, slot, defaultModel)

        // 2. Chama os hooks de animação do mod
        val extensions = IClientItemExtensions.of(stack)
        extensions.setupModelAnimations(entity, stack, slot, customModel, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch)

        // 3. SE o modelo for HumanoidModel (99% dos mods incluindo Create, Botania, Mekanism, etc.)
        if (customModel is HumanoidModel<*>) {

            // CORREÇÃO CRÍTICA 1: Aplicar visibilidade na instância CORRETA do modelo
            setPartVisibility(customModel, slot)

            customModel.young = entity.isBaby
            customModel.crouching = entity.isCrouching
            customModel.riding = entity.isPassenger

            // CORREÇÃO CRÍTICA 2: Zerar rotações e posições das partes.
            // Como você já moveu o PoseStack usando `dummyModel.head.translateAndRotate`,
            // se o modelo do mod também aplicar a rotação dele, a armadura girará 2x e sairá voando.
            when (slot) {
                EquipmentSlot.HEAD -> {
                    customModel.head.setPos(0f, 0f, 0f)
                    customModel.hat.setPos(0f, 0f, 0f)
                    customModel.head.xRot = 0f
                    customModel.head.yRot = 0f
                    customModel.head.zRot = 0f
                    customModel.hat.xRot = 0f
                    customModel.hat.yRot = 0f
                    customModel.hat.zRot = 0f
                }
                EquipmentSlot.CHEST -> {
                    customModel.body.setPos(0f, 0f, 0f)
                    customModel.body.xRot = 0f
                    customModel.body.yRot = 0f
                    customModel.body.zRot = 0f
                    // Braços mantém posição padrão
                    customModel.rightArm.setPos(-5f, 2f, 0f)
                    customModel.leftArm.setPos(5f, 2f, 0f)
                }
                EquipmentSlot.LEGS, EquipmentSlot.FEET -> {
                    customModel.rightLeg.setPos(-1.9f, 12f, 0f)
                    customModel.leftLeg.setPos(1.9f, 12f, 0f)
                    customModel.rightLeg.xRot = 0f
                    customModel.rightLeg.yRot = 0f
                    customModel.rightLeg.zRot = 0f
                    customModel.leftLeg.xRot = 0f
                    customModel.leftLeg.yRot = 0f
                    customModel.leftLeg.zRot = 0f
                }
                else -> {}
            }
        }

        // 4. Lógica de renderização de Textura
        val armorMaterial = item.material.value()
        val isInnerLayer = slot == EquipmentSlot.LEGS
        val fallbackColor = extensions.getDefaultDyeColor(stack)
        val layers = armorMaterial.layers()

        // CORREÇÃO CRÍTICA 3: Modos exóticos podem retornar 0 layers. Nesse caso ignoramos o loop e forçamos a renderização base.
        if (layers.isEmpty()) {

            val texture = ClientHooks.getArmorTexture(entity, stack, ArmorMaterial.Layer(ELYTRA_LOCATION), isInnerLayer, slot)
            val vertexConsumer = buffer.getBuffer(RenderType.armorCutoutNoCull(texture))
            customModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, fallbackColor)
        } else {
            for (layerIdx in layers.indices) {
                val layer = layers[layerIdx]
                val color = extensions.getArmorLayerTintColor(stack, entity, layer, layerIdx, fallbackColor)
                if (color != 0) {
                    val texture = ClientHooks.getArmorTexture(entity, stack, layer, isInnerLayer, slot)
                    val vertexConsumer = buffer.getBuffer(RenderType.armorCutoutNoCull(texture))
                    customModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, color)
                }
            }
        }

        // 5. Trims (Ornamentos) e Enchantment Glint
        val trim = stack.get(DataComponents.TRIM)
        if (trim != null) {
            val trimAtlas = modelManager.getAtlas(Sheets.ARMOR_TRIMS_SHEET)
            val sprite = trimAtlas.getSprite(if (isInnerLayer) trim.innerTexture(item.material) else trim.outerTexture(item.material))
            val trimConsumer = sprite.wrap(buffer.getBuffer(Sheets.armorTrimsSheet(trim.pattern().value().decal())))
            customModel.renderToBuffer(poseStack, trimConsumer, packedLight, OverlayTexture.NO_OVERLAY)
        }

        if (stack.hasFoil()) {
            val glintConsumer = buffer.getBuffer(RenderType.armorEntityGlint())
            customModel.renderToBuffer(poseStack, glintConsumer, packedLight, OverlayTexture.NO_OVERLAY)
        }
    }

    private fun renderElytra(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        entity: DummyEntity,
        stack: ItemStack
    ) {
        poseStack.pushPose()
        poseStack.translate(0.0, 0.0, 0.125)
        val vertexConsumer = ItemRenderer.getArmorFoilBuffer(
            buffer,
            RenderType.armorCutoutNoCull(ELYTRA_LOCATION),
            stack.hasFoil()
        )
        elytraModel.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY)
        poseStack.popPose()
    }

    // Nota: Mudamos HumanoidModel<DummyEntity> para HumanoidModel<*> (Star Projection)
    // para conseguir aplicar as mudanças em instâncias de mods com segurança
    private fun setPartVisibility(model: HumanoidModel<*>, slot: EquipmentSlot) {
        model.setAllVisible(false)
        when (slot) {
            EquipmentSlot.HEAD -> {
                model.head.visible = true
                model.hat.visible = true
            }
            EquipmentSlot.CHEST -> {
                model.body.visible = true
                model.rightArm.visible = true
                model.leftArm.visible = true
            }
            EquipmentSlot.LEGS -> {
                model.body.visible = true
                model.rightLeg.visible = true
                model.leftLeg.visible = true
            }
            EquipmentSlot.FEET -> {
                model.rightLeg.visible = true
                model.leftLeg.visible = true
            }
            else -> {}
        }
    }
}
*/

/*
import com.mojang.blaze3d.vertex.PoseStack
import net.ian.trainingdummy.client.model.DummyModel
import net.ian.trainingdummy.entity.DummyEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.EntityModelSet
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.entity.ArmorStandRenderer
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorMaterial
import net.minecraft.world.item.ArmorMaterials
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.armortrim.ArmorTrim
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions

class DummyArmorLayer(
    renderer: RenderLayerParent<DummyEntity, DummyModel<DummyEntity>>,
    modelSet: EntityModelSet
) : RenderLayer<DummyEntity, DummyModel<DummyEntity>>(renderer) {

    private val innerModel = HumanoidModel<DummyEntity>(modelSet.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR))
    private val outerModel = HumanoidModel<DummyEntity>(modelSet.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR))

    override fun render(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        entity: DummyEntity,
        limbSwing: Float,
        limbSwingAmount: Float,
        partialTicks: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.HEAD)
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.CHEST)
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.LEGS)
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.FEET)
    }

    private fun renderSlot(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        entity: DummyEntity,
        slot: EquipmentSlot
    ) {
        val itemStack: ItemStack = entity.getItemBySlot(slot)
        if (itemStack.isEmpty) return

        val dummyModel = parentModel
        val armorModel = if (slot == EquipmentSlot.LEGS) innerModel else outerModel

        // 1. Limpa exibição das partes do modelo da armadura
        armorModel.head.visible = false
        armorModel.hat.visible = false
        armorModel.body.visible = false
        armorModel.rightArm.visible = false
        armorModel.leftArm.visible = false
        armorModel.rightLeg.visible = false
        armorModel.leftLeg.visible = false

        // 2. Reseta rotações
        armorModel.head.setRotation(0f, 0f, 0f)
        armorModel.body.setRotation(0f, 0f, 0f)
        armorModel.rightArm.setRotation(0f, 0f, 0f)
        armorModel.leftArm.setRotation(0f, 0f, 0f)
        armorModel.rightLeg.setRotation(0f, 0f, 0f)
        armorModel.leftLeg.setRotation(0f, 0f, 0f)

        // 3. Texturas e Cores
        val armorTexture = getArmorTexture(itemStack, slot)
        val armorColor = getArmorColor(entity, itemStack, slot)
        val vertexConsumer = buffer.getBuffer(armorModel.renderType(armorTexture))
        val overlay = OverlayTexture.NO_OVERLAY

        // 4. Obtém o componente de Armor Trim (se existir no item)
        val trim: ArmorTrim? = itemStack.get(DataComponents.TRIM)

        poseStack.pushPose()

        // --- CALÇAS E BOTAS ---
        if (slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) {
            dummyModel.pila2.translateAndRotate(poseStack)

            armorModel.rightLeg.visible = true
            armorModel.rightLeg.setPos(0f, 0f, 0f)

            poseStack.pushPose()
            poseStack.translate(0.0, -0.60, 0.0)
            poseStack.scale(0.81f, 0.81f, 0.81f)

            // Desenha a armadura base
            armorModel.rightLeg.render(poseStack, vertexConsumer, packedLight, overlay, armorColor)

            // Desenha o Trim por cima
            if (trim != null) {
                renderTrim(itemStack, slot, trim, poseStack, buffer, packedLight, armorModel)
            }

            poseStack.popPose()
        }

        // --- PEITORAL ---
        if (slot == EquipmentSlot.CHEST) {
            dummyModel.pila2.translateAndRotate(poseStack)
            dummyModel.body.translateAndRotate(poseStack)

            armorModel.body.visible = true
            armorModel.rightArm.visible = true
            armorModel.leftArm.visible = true

            armorModel.body.setPos(0f, 0f, 0f)
            armorModel.rightArm.setPos(-5f, 2f, 0f)
            armorModel.leftArm.setPos(5f, 2f, 0f)

            poseStack.pushPose()
            poseStack.translate(0.0, -0.75, 0.0)
            poseStack.scale(1.01f, 1.01f, 1.01f)

            // Desenha a armadura base
            armorModel.body.render(poseStack, vertexConsumer, packedLight, overlay, armorColor)
            armorModel.rightArm.render(poseStack, vertexConsumer, packedLight, overlay, armorColor)
            armorModel.leftArm.render(poseStack, vertexConsumer, packedLight, overlay, armorColor)

            // Desenha o Trim por cima
            if (trim != null) {
                renderTrim(itemStack, slot, trim, poseStack, buffer, packedLight, armorModel)
            }

            poseStack.popPose()
        }

        // --- CAPACETE ---
        if (slot == EquipmentSlot.HEAD) {
            dummyModel.pila2.translateAndRotate(poseStack)
            dummyModel.body.translateAndRotate(poseStack)
            dummyModel.head.translateAndRotate(poseStack)

            armorModel.head.visible = true
            armorModel.hat.visible = true

            armorModel.head.setPos(0f, 0f, 0f)
            armorModel.hat.setPos(0f, 0f, 0f)

            poseStack.pushPose()
            poseStack.translate(0.0, -0.10, 0.0)
            poseStack.scale(1.02f, 1.02f, 1.02f)

            // Desenha a armadura base
            armorModel.head.render(poseStack, vertexConsumer, packedLight, overlay, armorColor)
            armorModel.hat.render(poseStack, vertexConsumer, packedLight, overlay, armorColor)

            // Desenha o Trim por cima
            if (trim != null) {
                renderTrim(itemStack, slot, trim, poseStack, buffer, packedLight, armorModel)
            }

            poseStack.popPose()
        }

        poseStack.popPose()
    }

    /**
     * Renderiza o Armor Trim usando o Atlas de Armaduras do Minecraft
     */
    private fun renderTrim(
        stack: ItemStack,
        slot: EquipmentSlot,
        trim: ArmorTrim,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        armorModel: HumanoidModel<DummyEntity>
    ) {
        val isLegs = slot == EquipmentSlot.LEGS

        // Obtém o atlas de armaduras via Minecraft Client Instance
        val modelManager = Minecraft.getInstance().modelManager
        val armorAtlas = modelManager.getAtlas(Sheets.ARMOR_TRIMS_SHEET)

        // Localização da textura do Trim
        val textureLocation: ResourceLocation = if (isLegs) {
            trim.innerTexture(stack.itemHolder as Holder<ArmorMaterial?>)
        } else {
            trim.outerTexture(stack.itemHolder as Holder<ArmorMaterial?>)
        }
        ArmorStandRenderer

        val sprite: TextureAtlasSprite = armorAtlas.getSprite(textureLocation)
        val trimVertexConsumer = sprite.wrap(buffer.getBuffer(Sheets.armorTrimsSheet(trim.pattern().value().decal())))

        armorModel.renderToBuffer(poseStack, trimVertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, -1)
    }

    private fun getArmorTexture(stack: ItemStack, slot: EquipmentSlot): ResourceLocation {
        val item = stack.item
        if (item is ArmorItem) {
            val material = item.material.value()
            val layer = material.layers().firstOrNull()
            if (layer != null) {
                return layer.texture(slot == EquipmentSlot.LEGS)
            }
        }
        val layerIndex = if (slot == EquipmentSlot.LEGS) "2" else "1"
        return ResourceLocation.withDefaultNamespace("textures/models/armor/iron_layer_$layerIndex.png")
    }

    private fun getArmorColor(entity: DummyEntity, stack: ItemStack, slot: EquipmentSlot): Int {
        val item = stack.item
        if (item is ArmorItem) {
            val materialHolder = item.material

            // 1. Caso o item tenha sido tingido pelo jogador (DyedItemColor)
            val dyedColor = stack.get(DataComponents.DYED_COLOR)
            if (dyedColor != null) {
                val colorRgb = dyedColor.rgb
                return colorRgb or -0x1000000 // Adiciona o canal Alpha 255 (0xFF000000)
            }

            // 2. Extensões de cliente NeoForge/Forge
            val layer = materialHolder.value().layers().firstOrNull()
            if (layer != null && layer.dyeable()) {
                val extensions = IClientItemExtensions.of(stack)
                val tint = extensions.getArmorLayerTintColor(stack, entity, layer, 0, -1)
                if (tint != -1) return tint
            }

            // 3. COR PADRÃO DO COURO: Compara usando .is(...) no Holder do material
            if (materialHolder.`is`(ArmorMaterials.LEATHER)) {
                return -0x5f9ab0 // 0xFFA06540 (Marrom natural do couro)
            }
        }

        // Para as demais armaduras (Branco pleno ARGB: 0xFFFFFFFF / -1)
        return -1
    }
}
*/