package io.github.natanfudge

import io.github.natanfudge.impl.utils.CrossLoaderObjectImpl
import io.github.natanfudge.impl.utils.Events
import net.minecraft.client.MinecraftClient
import net.minecraft.server.MinecraftServer
import net.minecraft.util.registry.DynamicRegistryManager
import net.minecraft.world.gen.GeneratorOptions
import kotlin.concurrent.withLock

fun interface TestCode {
    fun MinecraftContext.run()
}

object MinecraftLifecycle {
    /**
     * Starts the Minecraft client, and runs [testCode] in the correct (knot) class loader.
     * WARNING: do NOT interact with mod or Minecraft code outside of [testCode]. The state will not sync up with what is happening in-game.
     */
    fun startClient(testCode: TestCode) {
        System.setProperty("fabric.dli.config", "launch.cfg")
        System.setProperty("fabric.dli.env", "client")
        System.setProperty("fabric.dli.main", "net.fabricmc.loader.launch.knot.KnotClient")

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

class MinecraftContext {
    fun onGameLoaded(callback: GameLoaded.() -> Unit) {
        Events.OnTitleScreenLoaded.register {
            GameLoaded().callback()
        }
    }

    class GameLoaded {
        fun openDemoWorld(onLoaded: () -> Unit) {
            val registryManager = DynamicRegistryManager.create()

            Events.OnJoinClientWorld.register {
                onLoaded()
            }
            MinecraftClient.getInstance().method_29607(
                "Demo_World",
                MinecraftServer.DEMO_LEVEL_INFO,
                registryManager,
                GeneratorOptions.method_31112(registryManager)
            )
        }
    }

    fun closeMinecraft() {
        val clo = CrossLoaderObjectImpl.getInstance()!!
        clo.lock.withLock {
            clo.condition.signal()
        }
    }
}

