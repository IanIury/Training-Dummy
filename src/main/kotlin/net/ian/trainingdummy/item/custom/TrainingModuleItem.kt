package net.ian.trainingdummy.item.custom

import net.ian.trainingdummy.init.components.ModDataComponents
import net.ian.trainingdummy.init.components.TrainingModuleConfig
import net.ian.trainingdummy.init.enums.TrainingModos
import net.ian.trainingdummy.item.ModItems
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay
import java.util.function.Consumer

class TrainingModuleItem(properties: Properties) : Item(properties) {

    @Suppress("OVERRIDE_DEPRECATION")
    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltipDisplay: TooltipDisplay,
        tooltipComponents: Consumer<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.accept(
            Component.translatable("item.trainingdummy.training_module_item.tooltip")
                .withStyle(ChatFormatting.GRAY)
        )
    }

    companion object {
        const val MODE_KEY = "training_mode"

        fun setItemMode(stack: ItemStack, mode: TrainingModos): ItemStack {
            val config = stack.getOrDefault(
                ModDataComponents.TRAINING_MODULE_CONFIG,
                TrainingModuleConfig()
            )
            stack.set(
                ModDataComponents.TRAINING_MODULE_CONFIG,
                config.copy(trainingModo = mode)
            )
            return stack
        }

        fun getItemTrainingMode(stack: ItemStack): TrainingModos? {
            if (!stack.`is`(ModItems.TRAINING_MODULE_ITEM)) { return null }
            return stack.getOrDefault(ModDataComponents.TRAINING_MODULE_CONFIG, TrainingModuleConfig()).trainingModo
        }

        fun getItemTrainingModuleConfig(stack: ItemStack): TrainingModuleConfig? {
            if (!stack.`is`(ModItems.TRAINING_MODULE_ITEM)) { return null }
            return stack.getOrDefault(ModDataComponents.TRAINING_MODULE_CONFIG, TrainingModuleConfig())
        }
    }
}
