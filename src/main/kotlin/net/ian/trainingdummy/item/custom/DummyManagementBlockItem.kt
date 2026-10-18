package net.ian.trainingdummy.item.custom

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.block.Block

class DummyManagementBlockItem(block: Block, properties: Item.Properties) : BlockItem(block, properties) {

    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(
            Component.translatable("item.trainingdummy.dummy_management_block.tooltip")
                .withStyle(ChatFormatting.GRAY)
        )
    }
}
