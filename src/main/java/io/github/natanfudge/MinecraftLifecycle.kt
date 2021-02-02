package io.github.natanfudge

import io.github.natanfudge.impl.EndTestException
import io.github.natanfudge.impl.utils.CrossLoaderObjectImpl
import io.github.natanfudge.impl.utils.Events
import io.github.natanfudge.impl.utils.flatten
import io.github.natanfudge.impl.utils.invoke
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.locks.ReentrantLock
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
//            with(MinecraftContext()){
//                testCode()
//            }
//            TitleScreenLoadedEvent.EVENT.register(TitleScreenLoadedEvent { throw EndTestException() })
        }

//        CrossLoaderObjectImpl.setInstance(CrossLoaderObjectImpl(methods))

        runInKnotClassLoader(listOf(testCode, frameworkCode))

        try {
            //TODO: consider PR'ing a change to Fabric Loader that allows just straight up crashing instead of showing the swing GUI when there's an error.
            net.fabricmc.devlaunchinjector.Main.main(arrayOf())
        } catch (e: Throwable) {
            val involved = allInvolvedExceptions(e)
            if (involved.any { it.javaClass.name == "io.github.natanfudge.impl.EndTestException" }) return
            throw e
        }
    }

    fun MinecraftContext.frameworkCode() {

    }


    private fun allInvolvedExceptions(e: Throwable) = flatten(e) { cause }

    private fun runInKnotClassLoader(lambdas: List<TestCode>) {
        val methods = lambdas.map { it::class.java.getDeclaredMethod("run", MinecraftContext::class.java) }

        CrossLoaderObjectImpl.setInstance(CrossLoaderObjectImpl(methods))
    }
}

class MinecraftContext {
    val lock = ReentrantLock()
    val condition = lock.newCondition()
    fun launch(testCode:  () -> Unit) {
//        runBlocking {
            testCode()
//            closeMinecraft()
//        }
//        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
//            //THROW
//            throw throwable
//        }
//        // Create a new coroutine to avoid blocking the game;
//        // Use Dispatchers.Unconfined to stay in the game thread.
//        GlobalScope.launch(exceptionHandler + Dispatchers.Unconfined) {
//            Thread.currentThread().setUncaughtExceptionHandler { _, throwable ->
//                // FUCKING THROW!!!
//                throw throwable
//            }
//            testCode()
//
//            closeMinecraft()
//        }
    }

    fun waitForGameToLoad(callback : () -> Unit) {
        Events.OnTitleScreenLoaded {
            callback()
//            lock.withLock {
//                condition.signal()
//            }
        }
//        lock.withLock {
//            condition.await()
//        }
    }

//    suspend fun openDemoWorld() = suspendCoroutine<Unit> { continuation ->
//        val registryManager = DynamicRegistryManager.create()
//        MinecraftClient.getInstance()
//            .method_29607(
//                "Demo_World",
//                MinecraftServer.DEMO_LEVEL_INFO,
//                registryManager,
//                GeneratorOptions.method_31112(registryManager)
//            )
//        ClientLifecycleEvent.CLIENT_WORLD_LOAD.register(ClientLifecycleEvent.ClientWorldState {
//            continuation.resume(Unit)
//        })
//    }
//
//    suspend fun waitForGameToLoad() = suspendCoroutine<Unit> { continuation ->
//        Events.OnTitleScreenLoaded {
//            continuation.resume(Unit)
//        }
//    }

    fun closeMinecraft() {
        throw EndTestException()
    }
}

fun main() = runBlocking {
    val handler = Thread.getDefaultUncaughtExceptionHandler()
    val x = 2
    GlobalScope.launch {
        throw IndexOutOfBoundsException()
    }.join()
}
