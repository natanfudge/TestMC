package io.github.natanfudge

import io.github.natanfudge.impl.mixinhandlers.ServerMixinHandler
import io.github.natanfudge.impl.utils.CrossLoaderObjectImpl
import io.github.natanfudge.impl.utils.Events
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.minecraft.client.MinecraftClient
import net.minecraft.server.MinecraftServer
import net.minecraft.util.registry.DynamicRegistryManager
import net.minecraft.world.gen.GeneratorOptions
import kotlin.concurrent.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

enum class Side {
    Client,Server
}

fun interface TestCode {
    // Called reflectively
    @Suppress("unused")
    fun MinecraftContext.run()
}

interface Platform {
    fun startMcInstance(testCode: TestCode, envType: Side)
}

//TODO: figure out ways of sharing test code between platforms


//object MinecraftLifecycle {

    /**
     * Starts the Minecraft client, and runs [testCode] in the correct (knot) class loader.
     * WARNING: do NOT interact with mod or Minecraft code outside of [testCode]. The state will not sync up with what is happening in-game.
     * WARNING2: do NOT capture any variables in the [testCode] lambda. Doing so will result in a NoSuchMethodException.
     */
    inline fun Platform.startClient(crossinline testCode: suspend MinecraftContext.Client.() -> Unit) {
        startMcInstance(Side.Client) {
            testCode(MinecraftContext.Client())
        }
    }

    inline fun Platform.startServer(crossinline testCode: suspend MinecraftContext.Server.() -> Unit) {
        startMcInstance(Side.Server) {
            testCode(MinecraftContext.Server())
        }
    }

    //TODO: allow launching client and server together by launching the server in a new process and then printing a special line when it errors,
    // and parse that line from the original process, and throw if it was written.
    // OR maybe by running two tests at the same time?
    @PublishedApi
    internal inline fun Platform.startMcInstance(envType: Side, crossinline testCode: suspend MinecraftContext.() -> Unit) {
        val code = TestCode {
            launch {
                testCode()
                println("end!")
                closeServer()
                println("close!")
                unlock()
                println("unlock!")
//                closeMinecraft()
            }
        }
        startMcInstance(code, envType)
    }






    @PublishedApi internal suspend fun closeServer() {
        ServerMixinHandler.server!!.stop(false)
        suspendCoroutine<Unit> {
            Events.OnServerSafelyClosed.register {
                it.resume(Unit)
            }
        }
    }

    @PublishedApi internal  fun unlock() {
        val clo = CrossLoaderObjectImpl.getInstance()!!
        clo.lock.withLock {
            clo.condition.signal()
        }
    }
//}

class MinecraftContext @PublishedApi internal constructor() {
    @PublishedApi
    internal fun launch(testCode: suspend () -> Unit) {
        GlobalScope.launch(Dispatchers.Unconfined) {
            testCode()
        }
    }

    class Client @PublishedApi internal constructor() {
        suspend fun waitForGameToLoad() = suspendCoroutine<Unit> { cont ->
            Events.OnTitleScreenLoaded.register {
                cont.resume(Unit)
            }
        }

        suspend fun openDemoWorld() = suspendCoroutine<Unit> { cont ->
            val registryManager = DynamicRegistryManager.create()

            Events.OnJoinClientWorld.register {
                cont.resume(Unit)
            }
            MinecraftClient.getInstance().method_29607(
                "Demo_World",
                MinecraftServer.DEMO_LEVEL_INFO,
                registryManager,
                GeneratorOptions.method_31112(registryManager)
            )
        }
    }

    class Server @PublishedApi internal constructor() {
        suspend fun waitForWorldToLoad() = suspendCoroutine<Unit> { cont ->
            Events.OnServerWorldLoaded.register {
                cont.resume(Unit)
            }
        }
    }


}

