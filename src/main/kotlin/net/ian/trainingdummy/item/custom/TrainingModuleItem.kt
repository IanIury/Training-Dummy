package net.ian.trainingdummy.item.custom

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import kotlin.rem

class TrainingModuleItem (properties: Properties) : Item(properties) {

    init {
        setItemMode(defaultInstance, TrainingModos.DEFAULT)
    }

    enum class TrainingModos {
        DEFAULT, // SINGLE_HIT
        DPS,
        ACCUMULATED,
        COMPARISON;

        companion object {
            fun fromString(name: String): TrainingModos {
                return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DEFAULT
            }

        }

        fun next(): TrainingModos {
            return entries[(this.ordinal + 1) % entries.size]
        }

        fun previous(): TrainingModos {
            return entries[(this.ordinal - 1 + entries.size) % entries.size]
        }

        fun nameD(): String {return name.replace('_',' ').lowercase().replaceFirstChar { it.uppercase() }}

    }

    companion object {
        const val MODE_KEY = "training_mode"

        fun setItemMode(stack: ItemStack, mode: TrainingModos) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack) { tag ->
                tag.putString(MODE_KEY, mode.name)
            }
        }

        fun getItemMode(stack: ItemStack): TrainingModos {
            val customData = stack.get(DataComponents.CUSTOM_DATA) ?: return TrainingModos.DEFAULT
            val tag = customData.copyTag()

            if (!tag.contains(MODE_KEY)) return TrainingModos.DEFAULT
            return TrainingModos.fromString(tag.getString(MODE_KEY))
        }
    }
}