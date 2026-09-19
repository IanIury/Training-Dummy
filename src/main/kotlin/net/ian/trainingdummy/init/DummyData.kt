package net.ian.trainingdummy.init

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.NonNullList
import net.minecraft.world.item.ItemStack
data class DummyData(
    val armor: NonNullList<ItemStack> = NonNullList.withSize(4, ItemStack.EMPTY),
    val hands: NonNullList<ItemStack> = NonNullList.withSize(2, ItemStack.EMPTY),
    //val customConfig: Int = 0
) {
    companion object {
        val CODEC: Codec<DummyData> = RecordCodecBuilder.create { instance ->
            instance.group(
                ItemStack.OPTIONAL_CODEC.listOf().fieldOf("armor").forGetter { it.armor.toList() },
                ItemStack.OPTIONAL_CODEC.listOf().fieldOf("hands").forGetter { it.hands.toList() },
              //  Codec.INT.fieldOf("custom_config").orElse(0).forGetter { it.customConfig }
            ).apply(instance) { armorList, handsList /* ,config   aqui masi configs */ ->
                val armorNonNull = NonNullList.withSize(4, ItemStack.EMPTY)
                // Usando take() para pegar no máximo 4 itens de forma limpa
                armorList.take(4).forEachIndexed { i, stack -> armorNonNull[i] = stack }

                val handsNonNull = NonNullList.withSize(2, ItemStack.EMPTY)
                // Usando take() para pegar no máximo 2 itens
                handsList.take(2).forEachIndexed { i, stack -> handsNonNull[i] = stack }

                DummyData(armorNonNull, handsNonNull)//, config)
            }
        }
    }
}