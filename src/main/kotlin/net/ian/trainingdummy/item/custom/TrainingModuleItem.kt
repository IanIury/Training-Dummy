package net.ian.trainingdummy.item.custom

import net.ian.trainingdummy.init.components.ModDataComponents
import net.ian.trainingdummy.init.components.TrainingModuleConfig
import net.ian.trainingdummy.init.enums.TrainingModos
import net.ian.trainingdummy.item.ModItems
import net.minecraft.core.component.DataComponents
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.CustomData

class TrainingModuleItem (properties: Properties) : Item(properties) {

    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(
            Component.translatable("item.trainingdummy.training_module_item.tooltip")
                .withStyle(ChatFormatting.GRAY)
        )
    }

    init {
        //setItemMode(defaultInstance, TrainingModos.DEFAULT)
    }

    companion object {
        const val MODE_KEY = "training_mode"

        fun setItemMode(stack: ItemStack, mode: TrainingModos) : ItemStack {
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

        fun getItemTrainingMode(stack: ItemStack) : TrainingModos? {
            if(!stack.`is`(ModItems.TRAINING_MODULE_ITEM)){return null}
            return stack.getOrDefault(ModDataComponents.TRAINING_MODULE_CONFIG, TrainingModuleConfig()).trainingModo
        }
        fun getItemTrainingModuleConfig(stack: ItemStack) : TrainingModuleConfig? {
            if(!stack.`is`(ModItems.TRAINING_MODULE_ITEM)){return null}
            return stack.getOrDefault(ModDataComponents.TRAINING_MODULE_CONFIG, TrainingModuleConfig())
        }
    }
}