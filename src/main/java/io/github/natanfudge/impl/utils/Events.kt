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


object Events {
    val OnTitleScreenLoaded = noArgs()
    //TODO: maybe use this for server tests, NOTE: currently only triggers based off of the integratedServer (client-only)
    val OnServerWorldLoaded = noArgs()
    val OnJoinClientWorld = noArgs()
//    val WorldLoaded = noArgs()
}