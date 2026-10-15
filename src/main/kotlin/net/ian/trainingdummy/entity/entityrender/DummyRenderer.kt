package net.ian.trainingdummy.entity.entityrender


import com.mojang.blaze3d.vertex.PoseStack
import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.client.layer.DummyCustomHeadLayer
import net.ian.trainingdummy.client.layer.DummyElytraLayer
import net.ian.trainingdummy.client.layer.DummyFaceLayer
import net.ian.trainingdummy.client.layer.DummyHumanoidArmorLayer
import net.ian.trainingdummy.client.layer.DummyItemInHandLayer


import net.ian.trainingdummy.client.model.DummyModel

import net.ian.trainingdummy.client.window.FloatingWindow
import net.ian.trainingdummy.client.window.FloatingWindow.*
import net.ian.trainingdummy.client.window.WindowAlignment
import net.ian.trainingdummy.client.window.WindowColor
import net.ian.trainingdummy.client.window.WindowColor.*
import net.ian.trainingdummy.client.window.WindowElement
import net.ian.trainingdummy.entity.DummyEntity
import net.ian.trainingdummy.event.utils.DamageData
import net.ian.trainingdummy.init.enums.TrainingModos
import net.ian.trainingdummy.item.custom.TrainingModuleItem
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.HumanoidMobRenderer
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer
import net.minecraft.client.renderer.entity.layers.ElytraLayer
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import java.awt.Color
import java.util.Locale


class DummyRenderer(context: EntityRendererProvider.Context) : HumanoidMobRenderer<DummyEntity, DummyModel>(
    context,
    DummyModel(context.bakeLayer(DummyModel.LAYER_LOCATION)),
    0.5f
) {

    init {

        this.layers.removeIf { it is CustomHeadLayer<*, *>}
        this.layers.removeIf { it is ItemInHandLayer<*, *>}
        this.layers.removeIf { it is ElytraLayer<*, *>}

        val innerArmor = HumanoidModel<DummyEntity>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR))
        val outerArmor = HumanoidModel<DummyEntity>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR))

        this.addLayer(DummyHumanoidArmorLayer(this, innerArmor, outerArmor, context.modelManager))
        this.addLayer(DummyItemInHandLayer(this,context.itemInHandRenderer))
        this.addLayer(DummyCustomHeadLayer(this, context.modelSet, context.itemInHandRenderer))
        this.addLayer(DummyElytraLayer(this,context.modelSet))
        this.addLayer(DummyFaceLayer(this,context.modelSet))

        //this.addLayer(DummyGlowLayer(this))
        //this.addLayer(DummyOutlineLayer(this))
    }

    // Textura da entidade (.png em assets/modid/textures/entity/dummy.png)
    override fun getTextureLocation(entity: DummyEntity): ResourceLocation {
        return ResourceLocation.fromNamespaceAndPath(TrainingDummy.ID, "textures/entity/training_dummy.png")
    }

    override fun render(
        entity: DummyEntity,
        entityYaw: Float,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight)



        renderFloatingWindowModo(entity, partialTicks, poseStack, buffer, packedLight)
    }


    private fun renderFloatingWindowModo(
        entity: DummyEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int

    ) {


        val modoAtual = entity.trainingModuleConfig.trainingModo
        if (modoAtual != TrainingModos.DPS && entity.displayTicks <= 0) return

        val damageData = entity.damageData
        val damageDataOld = entity.damageDataOld

        val oldStack = entity.damageStackOld
        val atualStack = entity.damageStack

        val window = FloatingWindow.Builder()

        when(modoAtual){

            TrainingModos.DEFAULT -> {windowConfigDefault(window, entity)}
            TrainingModos.DPS -> {
                window.attachToEntityRotation(entity)
                    .setOffset(0.9, 2.0, 0.0)
                    .setAlignment(WindowAlignment.CENTER)
                    .setPadding(2f)
                    .setSize(38f, 30f, autoScaleContent = true, autoFitWidth = true, autoFitHeight = true)
                    .setCornerRadius(3f)

                    .setBorder(
                        when (entity.dpsTestState) {
                            DummyEntity.DPS_STATE_READY -> AnimatedGradient(
                                argbStart = 0xFF65E8A0,
                                argbEnd = 0xFF21B6A8,
                                speed = 0.8f,
                                scale = 0.8f
                            )
                            DummyEntity.DPS_STATE_COOLDOWN -> AnimatedGradient(
                                argbStart = 0xFFFFB454,
                                argbEnd = 0xFFFF5A5A,
                                speed = 0.8f,
                                scale = 0.8f
                            )
                            else -> AnimatedGradient(
                                argbStart = 0xFFC5A5FB,
                                argbEnd = 0xFF5700AC,
                                speed = 1.2f,
                                scale = 0.8f
                            )
                        },
                        width = 0.75f
                    )
                    .setBackground(
                        Gradient(
                            argbStart = 0xF0181C2A,
                            argbEnd = 0xF02B233D
                        )
                    )

                val formatDamage: (Float) -> String = { value ->
                    String.format(Locale.ROOT, "%.1f", value)
                }

                when (entity.dpsTestState) {
                    DummyEntity.DPS_STATE_MEASURING -> {
                        val remainingSeconds =
                            (entity.dpsTicksRemaining - partialTicks.coerceIn(0f, 1f)).coerceAtLeast(0f) / 20f
                        val elapsedTicks = DummyEntity.DPS_TEST_DURATION_TICKS - entity.dpsTicksRemaining
                        val currentDps = if (elapsedTicks >= 20) {
                            entity.dpsTotalDamage / (elapsedTicks / 20f)
                        } else {
                            null
                        }

                            window.addText(
                                Component.translatable(
                                    "message.trainingdummy.dps.countdown",
                                    formatDamage(remainingSeconds),
                                    currentDps?.let(formatDamage) ?: "--"
                                ),
                                color = 0xFF9BE7FF.toInt(),
                                shadow = true,
                                textScale = 0.9f,
                                alignment = WindowElement.Text.TextAlignment.CENTER
                            )
                                .addText(
                                    Component.translatable(
                                        "message.trainingdummy.dps.summary",
                                        formatDamage(entity.dpsTotalDamage),
                                        entity.dpsHitCount
                                    ),
                                    color = 0xFFFFFFFF.toInt(),
                                    textScale = 0.9f,
                                    alignment = WindowElement.Text.TextAlignment.CENTER
                                )
                    }

                    DummyEntity.DPS_STATE_COOLDOWN -> {
                            val averageHit = if (entity.dpsHitCount > 0) {
                                entity.dpsTotalDamage / entity.dpsHitCount
                            } else {
                                0f
                            }

                            window.addText(
                                Component.translatable(
                                    "message.trainingdummy.dps.final",
                                    formatDamage(entity.dpsTotalDamage / 5f)
                                ),
                                color = 0xFFE5D4FF.toInt(),
                                shadow = true,
                                textScale = 0.95f,
                                alignment = WindowElement.Text.TextAlignment.CENTER
                            )
                                .addText(
                                    Component.translatable(
                                        "message.trainingdummy.dps.summary",
                                        formatDamage(entity.dpsTotalDamage),
                                        entity.dpsHitCount
                                    ),
                                    color = 0xFFFFFFFF.toInt(),
                                    textScale = 0.9f,
                                    alignment = WindowElement.Text.TextAlignment.CENTER
                                )
                                .addText(
                                    Component.translatable(
                                        "message.trainingdummy.dps.hitStats",
                                        formatDamage(averageHit),
                                        formatDamage(entity.dpsHighestHit)
                                    ),
                                    color = 0xFFD7C8FF.toInt(),
                                    textScale = 0.85f,
                                    alignment = WindowElement.Text.TextAlignment.CENTER
                                )
                    }

                    else -> {
                            window.addText(
                                Component.translatable("message.trainingdummy.dps.ready"),
                                color = 0xFFB7F7C5.toInt(),
                                shadow = true,
                                textScale = 0.9f,
                                alignment = WindowElement.Text.TextAlignment.CENTER
                            )
                    }
                }
            }
            TrainingModos.ACCUMULATED -> {
                window.attachToEntityRotation(entity)
                    .setOffset(0.6, 2.0, 0.0)
                    .setSize(24f, 24f, autoScaleContent = true, autoFitWidth = true, autoFitHeight = true)
                    .setAlignment(WindowAlignment.CENTER)
                    .setCornerRadius(3f)
                    .setPadding(1f)
                    .setBorder(Rainbow(speed = 1.0f), width = 0.5f)
                    .setBackground(Solid(0xDD000000))

                    .addText(" • ${damageData.newDamage}")
            }
            TrainingModos.COMPARISON -> {
                windowDefault(window, damageDataOld, entity)
                window.addItemText(entity.damageStackOld, "  ${damageDataOld.newDamage}")
                val window2 = Builder()

                windowDefault(window2,damageData, entity)
                window2.setOffset(-0.6, 2.0, 0.0)
                    .addItemText(entity.damageStack, "  ${damageData.newDamage}")
                window2.build().render(poseStack, buffer, packedLight)//coloca nome (previous)
            }

            //null -> {return}
        }

        window.build().render(poseStack, buffer, packedLight)

    }

    private val armorHudIcon = ResourceLocation.fromNamespaceAndPath(
        "minecraft",
        "textures/gui/sprites/hud/armor_full.png"
    )

    private val absorptionHudIcon = ResourceLocation.fromNamespaceAndPath(
        "minecraft",
        "textures/gui/sprites/hud/heart/absorbing_full.png"
    )

    private val enchantItem = Items.ENCHANTED_BOOK.defaultInstance
    private val potionItem = Items.POTION.defaultInstance.also {
        it.set(
            DataComponents.POTION_CONTENTS,
            PotionContents(Potions.HEALING)
        )
    }
    private val innateResistanceItem = Items.STRUCTURE_VOID.defaultInstance
    private val invulnerabilityItem = Items.BARRIER.defaultInstance




    private fun windowConfigDefault(window : FloatingWindow.Builder, entity : DummyEntity, detailed : Boolean = false){
        val damageData = entity.damageData
        window.attachToEntityRotation(entity)
            .setOffset(0.9, 2.0, 0.0)
            .setSize(30f, 30f, autoScaleContent = true, autoFitWidth = true, autoFitHeight = true)
            .setAlignment(WindowAlignment.CENTER)
            .setPadding(3f)

            .setBorder(
                AnimatedGradient(
                    argbStart = 0xC5A5FB,
                    argbEnd = 0x5700ac,
                    speed = 1.2f,
                    scale = 0.8f
                ),
                width = 0.75f
            )
            .setBackground(
                Gradient(
                    argbStart = 0xF0181C2A,
                    argbEnd = 0xF02B233D
                )
            )

        if(detailed){
            window.addText(Component.translatable("message.trainingdummy.original_damage").append(":"),
                textScale = 0.8f,
                shadow = true,
                color = 0xDCDCDCFF.toInt()
            )
                .addText("${damageData.originalDamage}",
                    color = 0xFF9BE7FF.toInt(), shadow = true, alignment = WindowElement.Text.TextAlignment.CENTER, textScale = 2.5f)
                .addSeparator(
                    color = AnimatedGradient(
                        argbStart = 0xFF67E8F9,
                        argbEnd = 0xFF8B5CF6,
                        speed = 1.2f,
                        scale = 0.8f
                    ),
                    thickness = 0.5f,
                    margin = 2f
                )

                .addText(Component.translatable("message.trainingdummy.final_damage"),
                    textScale = 0.8f,
                    shadow = true,
                    color = 0xDCDCDCFF.toInt()
                )
        }

        window.addText( "${ if(detailed) {damageData.newDamage}else{"%.2f".format(damageData.newDamage)} }",
            color = if(damageData.isCriticalHit){AnimatedGradient(
                argbStart = 0xFF0000,
                argbEnd = 0xff8c00,
                speed = 5.0f,
                scale = 0.8f
            )}else{
                Pulse(0xFF9BE7FF)} ,
            shadow = true,
            alignment = WindowElement.Text.TextAlignment.CENTER,
            textScale = 2.5f)

        if(damageData.isCriticalHit){
            window.addText("x${damageData.damageMultiplier}crit",
                shadow = true,
                alignment = WindowElement.Text.TextAlignment.CENTER,
                textScale = 0.8f,
                color = Pulse(color = 0xff1a1a, speed = 1.5f))

        }

        if(damageData.isReductions()){

            window.addSeparator(
                color = AnimatedGradient(
                    argbStart = 0xFF67E8F9,
                    argbEnd = 0xFF8B5CF6,
                    speed = 1.2f,
                    scale = 0.8f
                ),
                thickness = 0.5f,
                margin = 2f
            )

            if (damageData.armor > 0.0f) {
                window.addIconText(
                    armorHudIcon,
                    ": -§a%.1f".format(damageData.armor),
                    alignment = WindowElement.Text.TextAlignment.CENTER
                )
            }
            if (damageData.enchantments > 0.0f) {
                window.addItemText(
                    enchantItem,
                    ": -§a%.1f".format(damageData.enchantments),
                    alignment = WindowElement.Text.TextAlignment.CENTER
                )
            }
            if (damageData.mobEffects > 0.0f) {
                window.addItemText(
                    potionItem,
                    ": -§a%.1f".format(damageData.mobEffects),
                    alignment = WindowElement.Text.TextAlignment.CENTER
                )
            }
            if (damageData.absorption > 0.0f) {
                window.addIconText(
                    absorptionHudIcon,
                    ": -§a%.1f".format(damageData.absorption),
                    alignment = WindowElement.Text.TextAlignment.CENTER
                )
            }
            if (damageData.innateResistance > 0.0f) {
                window.addItemText(
                    innateResistanceItem,
                    ": -§a%.1f".format(damageData.innateResistance),
                    alignment = WindowElement.Text.TextAlignment.CENTER
                )
            }
            if (damageData.invulnerability > 0.0f) {
                window.addItemText(
                    invulnerabilityItem,
                    ": -§a%.1f".format(damageData.invulnerability),
                    alignment = WindowElement.Text.TextAlignment.CENTER
                )
            }

            window.addSeparator(
                color = AnimatedGradient(
                    argbStart = 0xFF67E8F9,
                    argbEnd = 0xFF8B5CF6,
                    speed = 1.2f,
                    scale = 0.8f
                ),
                thickness = 0.5f,
                margin = 2f
            )

        }

    }

    private fun windowD(window: FloatingWindow.Builder,damageData: DamageData, entity: DummyEntity){
        window.attachToEntityRotation(entity)
            .setOffset(1.0, 2.0, 0.0)
            .setSize(24f, 24f, autoScaleContent = true, autoFitWidth = true, autoFitHeight = true)
            .setAlignment(WindowAlignment.CENTER)

            .setPadding(3f)
            .setBorder(Rainbow(speed = 1.0f), width = 0.5f)
            .setBackground(Solid(0xDD000000))

        var finaldamage = ""

        if(damageData.isReductions()){
            window.addText("Raw Damage: §c%.1f".format(damageData.originalDamage))
                .addSeparator(color = Rainbow(speed = 1.0f), thickness = 0.5f, margin = 2f)
                .addText("Reductions", color = 0x0087F0.toInt())
        }else{window.setPadding(3f);}

        if(damageData.armor > 0.0f){window.addText("Armor: -§a%.1f".format(damageData.armor))}
        if(damageData.enchantments > 0.0f){window.addText(" Enchantments: -§a%.1f".format(damageData.enchantments))}
        if(damageData.mobEffects > 0.0f){window.addText("Effects: -§a%.1f".format(damageData.mobEffects))}
        if(damageData.absorption > 0.0f){window.addText("Absorption: -§a%.1f".format(damageData.absorption))}
        if(damageData.innateResistance > 0.0f){window.addText("Innate Resistance: -§a%.1f".format(damageData.absorption))}
        if(damageData.invulnerability > 0.0f){window.addText("Invulnerability: -§a%.1f".format(damageData.invulnerability))}

        if(damageData.isReductions()){
            window.addSeparator(color = Rainbow(speed = 1.0f), thickness = 0.5f, margin = 2f)
            finaldamage = "Final: %.1f".format(damageData.newDamage)
        }else{finaldamage = "%.1f".format(damageData.newDamage)}

        if (damageData.isCriticalHit) {
            window.addText(
                "$finaldamage x${damageData.damageMultiplier}",
                color = AnimatedGradient(
                    argbStart = 0xFF0000, // Cor inicial (Branco)
                    argbEnd = 0xFFFFFF,   // Cor final (Vermelho)
                    speed = 5.0f,          // Velocidade do deslocamento
                    scale = 1.0f           // Densidade/Frequência das ondas no texto
                )
            )
        }else{
            window.addText(finaldamage)
        }
    }

    private fun windowDefault(window: FloatingWindow.Builder,damageData: DamageData, entity: DummyEntity,){
        window.attachToEntityRotation(entity)
            .setOffset(0.6, 2.0, 0.0)
            .setSize(24f, 24f, autoScaleContent = true, autoFitWidth = true, autoFitHeight = true)
            .setAlignment(WindowAlignment.CENTER)
            .setPadding(1f)
            .setCornerRadius(3f)
            .setBorder(
                AnimatedGradient(
                    argbStart = 0xFF67E8F9,
                    argbEnd = 0xFF8B5CF6,
                    speed = 1.2f,
                    scale = 0.8f
                ),
                width = 0.75f
            )
            .setBackground(
                Gradient(
                    argbStart = 0xF0181C2A,
                    argbEnd = 0xF02B233D
                )
            )
            .addText("DANO", color = 0xFF9BE7FF.toInt(), shadow = true)
            .addSeparator(
                color = AnimatedGradient(
                    argbStart = 0xFF67E8F9,
                    argbEnd = 0xFF8B5CF6,
                    speed = 1.2f,
                    scale = 0.8f
                ),
                thickness = 0.5f,
                margin = 2f
            )

        var finaldamage = ""

        if(damageData.isReductions()){
            window.addText("Raw Damage: §c%.1f".format(damageData.originalDamage))
                .addSeparator(color = Rainbow(speed = 1.0f), thickness = 0.5f, margin = 2f)
                .addText("Reductions", color = 0x0087F0.toInt())
        }else{
            window.setPadding(3f);
        }

        if(damageData.armor > 0.0f){window.addText("Armor: -§a%.1f".format(damageData.armor))}
        if(damageData.enchantments > 0.0f){window.addText(" Enchantments: -§a%.1f".format(damageData.enchantments))}
        if(damageData.mobEffects > 0.0f){window.addText("Effects: -§a%.1f".format(damageData.mobEffects))}
        if(damageData.absorption > 0.0f){window.addText("Absorption: -§a%.1f".format(damageData.absorption))}
        if(damageData.innateResistance > 0.0f){window.addText("Innate Resistance: -§a%.1f".format(damageData.innateResistance))}
        if(damageData.invulnerability > 0.0f){window.addText("Invulnerability: -§a%.1f".format(damageData.invulnerability))}

        if(damageData.isReductions()){
            window.addSeparator(color = Rainbow(speed = 1.0f), thickness = 0.5f, margin = 2f)
            finaldamage = "Final: %.1f".format(damageData.newDamage)
        }else{finaldamage = "%.1f".format(damageData.newDamage)}

        if (damageData.isCriticalHit) {
            window.addText(
                "$finaldamage x${damageData.damageMultiplier}",
                color = AnimatedGradient(
                    argbStart = 0xFF0000, // Cor inicial (Branco)
                    argbEnd = 0xFFFFFF,   // Cor final (Vermelho)
                    speed = 5.0f,          // Velocidade do deslocamento
                    scale = 1.0f           // Densidade/Frequência das ondas no texto
                )
            )
        }else{
            window.addText(finaldamage)
        }
    }


    private fun renderFloatingWindow(
        entity: DummyEntity,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        // Exibe a janela apenas se houver registro de dano recente
        if (entity.displayTicks > 0) {

            val damageData = entity.damageData

            val window = FloatingWindow.Builder()
                .attachToEntityRotation(entity) // Trava a rotação na direção do Dummy (+180° ajustado)
                .setOffset(1.0, 2.0, 0.0)
                .setSize(16f,16f, autoScaleContent = true)// Posição no ombro/lado do Dummy
                .setAlignment(WindowAlignment.CENTER) // Cresce para a direita e para baixo
                .setPadding(1f)

                .setBorder(WindowColor.Rainbow(speed = 1.0f), width = 0.5f)
                .setBackground(WindowColor.Solid(0xDD000000))

                .addText("Raw Damage: §c%.1f".format(damageData.originalDamage))
                .addSeparator(color = WindowColor.Rainbow(speed = 1.0f), thickness = 0.5f, margin = 2f)
                .addText("Reductions", color = 0x0087F0.toInt())


                .addText("Armor: §a%.1f".format(damageData.armor))
                .addSeparator(color = WindowColor.Rainbow(speed = 1.0f), thickness = 0.5f, margin = 2f)
                .addText("Final: %.1f".format(damageData.newDamage) + "${if (damageData.isCriticalHit) {" §cCrit ${damageData.damageMultiplier}x"} else {"§a"}} ")


                // Conteúdo
                .addText("§lESTATÍSTICAS", color = 0xFFFFAA00.toInt())
                .addSpacer(2f)
                .addText("Raw Damage: §c%.1f".format(damageData.originalDamage))
                .addText("Blocked: §b%.1f".format(damageData.blockedDamage))
                .addText("Shiel: §a%.1f".format(damageData.shieldDamage))
                .addText("Dano Final: ${if (damageData.isCriticalHit) "§c" else "§a"}%.1f".format(damageData.newDamage))
                .addText("Mob: §a%.1f".format(damageData.mobEffects))
                .addText("Absorp: §a%.1f".format(damageData.absorption))
                .addText("Armor: §a%.1f".format(damageData.armor))
                .addText("Enchant: §a%.1f".format(damageData.enchantments))
                .addText("Innate: §a%.1f".format(damageData.innateResistance))
                .addText("Invul: §a%.1f".format(damageData.invulnerability))
                //.addText("Crítico: " + if (entity.lastCrit>0f) "§aSIM" else "§cNÃO")


                .build()

            window.render(poseStack, buffer, packedLight)
        }
    }
}
