package net.ian.trainingdummy.screen

import net.ian.trainingdummy.TrainingDummy
import net.ian.trainingdummy.screen.custom.DummyMenu
import net.minecraft.core.registries.Registries
import net.minecraft.world.inventory.MenuType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModMenuTypes {

    val MENUS: DeferredRegister<MenuType<*>> =
        DeferredRegister.create(Registries.MENU, TrainingDummy.ID)

    val DUMMY_MENU : DeferredHolder<MenuType<*>, MenuType<DummyMenu>> =
        MENUS.register("training_dummy_menu", Supplier {
        IMenuTypeExtension.create { containerId, playerInventory, extraData ->
            DummyMenu(containerId, playerInventory, extraData)
        }
    })


    fun register(eventBus: IEventBus){MENUS.register(eventBus)}
}