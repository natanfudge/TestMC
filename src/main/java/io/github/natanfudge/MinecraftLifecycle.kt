package io.github.natanfudge

import io.github.natanfudge.impl.mixinhandlers.ServerMixinHandler
import io.github.natanfudge.impl.utils.CrossLoaderObjectImpl
import io.github.natanfudge.impl.utils.Events
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.fabricmc.api.EnvType
import net.minecraft.client.MinecraftClient
import net.minecraft.server.MinecraftServer
import net.minecraft.util.registry.DynamicRegistryManager
import net.minecraft.world.gen.GeneratorOptions
import kotlin.concurrent.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun interface TestCode {
    // Called reflectively
    @Suppress("unused")
    fun MinecraftContext.run()
}

object MinecraftLifecycle {

    /**
     * Starts the Minecraft client, and runs [testCode] in the correct (knot) class loader.
     * WARNING: do NOT interact with mod or Minecraft code outside of [testCode]. The state will not sync up with what is happening in-game.
     * WARNING2: do NOT capture any variables in the [testCode] lambda. Doing so will result in a NoSuchMethodException.
     */
    inline fun startClient(crossinline testCode: suspend MinecraftContext.Client.() -> Unit) {
        startMcInstance(EnvType.CLIENT) {
            testCode(MinecraftContext.Client())
        }
    }

    inline fun startServer(crossinline testCode: suspend MinecraftContext.Server.() -> Unit) {
        startMcInstance(EnvType.SERVER) {
            testCode(MinecraftContext.Server())
            // Give the server some time to close properly
            ServerMixinHandler.server!!.stop(false)
            //TODO: more advanced mechanism for figuring out when it is safe to attempt to close the process.
            delay(2000)
        }
    }

    //TODO: allow launching client and server together by launching the server in a new process and then printing a special line when it errors,
    // and parse that line from the original process, and throw if it was written.
    @PublishedApi
    internal inline fun startMcInstance(envType: EnvType, crossinline testCode: suspend MinecraftContext.() -> Unit) {
        val code = TestCode {
            launch {
                testCode()
                closeMinecraft()
            }
        }
        startMcInstance(code, envType)
    }

    @PublishedApi
    internal fun startMcInstance(testCode: TestCode, envType: EnvType) {
        val env = when (envType) {
            EnvType.CLIENT -> "client"
            EnvType.SERVER -> "server"
        }
        val knotClass = when (envType) {
            EnvType.CLIENT -> "net.fabricmc.loader.launch.knot.KnotClient"
            EnvType.SERVER -> "net.fabricmc.loader.launch.knot.KnotServer"
        }
        System.setProperty("fabric.dli.config", "launch.cfg")
        System.setProperty("fabric.dli.env", env)
        System.setProperty("fabric.dli.main", knotClass)

        val frameworkCode = TestCode {
            println("Framework code in classloader: ${MinecraftLifecycle.javaClass.classLoader}")
        }

        runInKnotClassLoader(listOf(testCode, frameworkCode))

        val clo = CrossLoaderObjectImpl.getInstance()!!
        var error: Throwable? = null

        Thread {
            //TODO: consider PR'ing a change to Fabric Loader that allows just straight up crashing instead of showing the swing GUI when there's an error.

            try {
                net.fabricmc.devlaunchinjector.Main.main(arrayOf())
            } catch (e: Throwable) {
                error = e
                clo.lock.withLock {
                    clo.condition.signal()
                }
            }

        }.start()


        clo.lock.withLock {
            clo.condition.await()
        }
        if (error != null) throw error!!
    }


    private fun runInKnotClassLoader(lambdas: List<TestCode>) {
        val methods = lambdas.map { it::class.java.getDeclaredMethod("run", MinecraftContext::class.java) }

        CrossLoaderObjectImpl.setInstance(CrossLoaderObjectImpl(methods))
    }
}

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

    fun closeMinecraft() {
        val clo = CrossLoaderObjectImpl.getInstance()!!
        clo.lock.withLock {
            clo.condition.signal()
        }

    }
}

