package net.ian.trainingdummy.util

import net.neoforged.neoforge.registries.DeferredHolder
import java.util.function.Supplier
import kotlin.reflect.KProperty

operator fun <T> DeferredHolder<*, out T>.getValue(thisRef: Any?, property: KProperty<*>): T = get()
operator fun <T> Supplier<out T>.getValue(thisRef: Any?, property: KProperty<*>): T = get()
