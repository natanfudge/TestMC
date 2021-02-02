package io.github.natanfudge.impl.utils

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.impl.base.event.EventFactoryImpl
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.world.World

private inline fun <reified T> event(noinline invokerFactory: (Array<T>) -> T): Event<T> {
    return EventFactoryImpl.createArrayBacked(T::class.java, invokerFactory)
}

private inline operator fun <T> Array<T>.invoke(callback: T.() -> Unit) = forEach(callback)

private fun noArgs() = event<() -> Unit> { listeners ->
    {
        listeners { invoke() }
    }
}

//private val subscribedHudListeners = mutableListOf<HudRenderFunction>()
//private var subscribedToHud = false


operator fun <T> Event<T>.invoke(listener: T) = register(listener)

//typealias HudRenderFunction = (MatrixStack, Float) -> Unit
//
//operator fun Event<HudRenderCallback>.invoke(listener: HudRenderFunction): HudRenderFunction {
//    if (!subscribedToHud) {
//        subscribedToHud = true
//        register { stack, delta ->
//            for (existingListener in subscribedHudListeners) {
//                existingListener(stack, delta)
//            }
//        }
//    }
//    subscribedHudListeners.add(listener)
//    return listener
//}
//
//fun HudRenderFunction.unsubscribe(): Boolean = (subscribedHudListeners as MutableCollection<*>).remove(this)
//

object Events {
    val OnTitleScreenLoaded = noArgs()
//    val WorldLoaded = noArgs()
}