package net.ian.trainingdummy.item.custom

import net.ian.trainingdummy.init.enums.TrainingModos
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

class TrainingModuleItem (properties: Properties) : Item(properties) {

    init {
        //setItemMode(defaultInstance, TrainingModos.DEFAULT)
    }

    companion object {
        const val MODE_KEY = "training_mode"

        fun setItemMode(stack: ItemStack, mode: TrainingModos) : ItemStack {
            CustomData.update(DataComponents.CUSTOM_DATA, stack) { tag ->
                tag.putString(MODE_KEY, mode.name)
            }
            return stack
        }

        fun getItemMode(stack: ItemStack): TrainingModos {
            val customData = stack.get(DataComponents.CUSTOM_DATA) ?: return TrainingModos.DEFAULT
            val tag = customData.copyTag()

            if (!tag.contains(MODE_KEY)) return TrainingModos.DEFAULT
            return TrainingModos.fromString(tag.getString(MODE_KEY))
        }
    }
}