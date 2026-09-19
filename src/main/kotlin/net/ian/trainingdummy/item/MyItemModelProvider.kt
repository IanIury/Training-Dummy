package net.ian.trainingdummy.item

import net.ian.trainingdummy.TrainingDummy
import net.minecraft.data.PackOutput
import net.neoforged.neoforge.client.model.generators.ItemModelProvider
import net.neoforged.neoforge.common.data.ExistingFileHelper

class MyItemModelProvider(output: PackOutput, existingFileHelper: ExistingFileHelper) :
    ItemModelProvider(output, TrainingDummy.ID, existingFileHelper) {
    override fun registerModels() {

        //withExistingParent(ModItems.DUMMY_ITEM_SPAWN.descriptionId, modLoc("item/generated")).texture("9","entity/training_dummy")
        //basicItem(ModItems.DUMMY_ITEM_SPAWN)

    }
}