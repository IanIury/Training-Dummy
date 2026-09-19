package net.ian.trainingdummy.entity.entityrender


import com.mojang.blaze3d.vertex.PoseStack
import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.client.layer.DummyCustomHeadLayer
import net.ian.trainingdummy.client.layer.DummyElytraLayer
import net.ian.trainingdummy.client.layer.DummyHumanoidArmorLayer
import net.ian.trainingdummy.client.layer.DummyItemInHandLayer


import net.ian.trainingdummy.client.model.DummyModel

import net.ian.trainingdummy.client.window.FloatingWindow
import net.ian.trainingdummy.client.window.WindowAlignment
import net.ian.trainingdummy.client.window.WindowColor
import net.ian.trainingdummy.entity.DummyEntity
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.HumanoidMobRenderer
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer
import net.minecraft.client.renderer.entity.layers.ElytraLayer
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer
import net.minecraft.resources.ResourceLocation


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

        renderFloatingWindowDefault(entity, poseStack, buffer, packedLight)
    }

    private fun renderFloatingWindowDefault(
        entity: DummyEntity,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        // Exibe a janela apenas se houver registro de dano recente
        if (entity.displayTicks > 0) {

            val damageData = entity.getDamageDataS()

            val window = FloatingWindow.Builder()
                .attachToEntityRotation(entity) // Trava a rotação na direção do Dummy (+180° ajustado)
                .setOffset(1.0, 2.0, 0.0)
                .setSize(
                    24f,
                    24f,
                    autoScaleContent = true,
                    autoFitWidth = true,
                    autoFitHeight = true
                ) // Posição no ombro/lado do Dummy
                .setAlignment(WindowAlignment.CENTER) // Cresce para a direita e para baixo
                .setPadding(1f)
                .setBorder(WindowColor.Rainbow(speed = 1.0f), width = 0.5f)
                .setBackground(WindowColor.Solid(0xDD000000))


                var finaldamage = ""

                if(damageData.isReductions()){
                    window.addText("Raw Damage: §c%.1f".format(damageData.originalDamage))
                        .addSeparator(color = WindowColor.Rainbow(speed = 1.0f), thickness = 0.5f, margin = 2f)
                        .addText("Reductions", color = 0x0087F0.toInt())
                }else{window.setPadding(3f);}

                if(damageData.armor > 0.0f){window.addText("Armor: -§a%.1f".format(damageData.armor))}
                if(damageData.enchantments > 0.0f){window.addText(" Enchantments: -§a%.1f".format(damageData.enchantments))}
                if(damageData.mobEffects > 0.0f){window.addText("Effects: -§a%.1f".format(damageData.mobEffects))}
                if(damageData.absorption > 0.0f){window.addText("Absorption: -§a%.1f".format(damageData.absorption))}
                if(damageData.innateResistance > 0.0f){window.addText("Innate Resistance: -§a%.1f".format(damageData.absorption))}
                if(damageData.invulnerability > 0.0f){window.addText("Invulnerability: -§a%.1f".format(damageData.invulnerability))}

                if(damageData.isReductions()){
                    window.addSeparator(color = WindowColor.Rainbow(speed = 1.0f), thickness = 0.5f, margin = 2f)
                    finaldamage = "Final: %.1f".format(damageData.newDamage)
                }else{finaldamage = "%.1f".format(damageData.newDamage)}




                //window.addText("${if(damageData.isReductions()) "Final:" else ""}%.1f".format(damageData.newDamage) + "${if (damageData.isCriticalHit) {" §c ${damageData.damageMultiplier}x"} else {"§a"}} ")
                if (damageData.isCriticalHit) {
                    //window.addText(finaldamage+" x${damageData.damageMultiplier}",color = WindowColor.Gradient( 0xFFFFFF,0x781500, true))
                    window.addText(
                        "$finaldamage x${damageData.damageMultiplier}",
                        color = WindowColor.AnimatedGradient(
                            argbStart = 0xFF0000, // Cor inicial (Branco)
                            argbEnd = 0xFFFFFF,   // Cor final (Vermelho)
                            speed = 5.0f,          // Velocidade do deslocamento
                            scale = 1.0f           // Densidade/Frequência das ondas no texto
                        )
                    )
                }else{
                    window.addText(finaldamage)
                }



            window.build().render(poseStack, buffer, packedLight)
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

            val damageData = entity.getDamageDataS()

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
