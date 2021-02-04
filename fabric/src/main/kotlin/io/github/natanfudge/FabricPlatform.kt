package io.github.natanfudge

import io.github.natanfudge.impl.utils.CrossLoaderObjectImpl
import kotlin.concurrent.withLock

class FabricPlatform : Platform {
    override fun startMcInstance(testCode: TestCode, envType: Side) {
        val env = when (envType) {
            Side.Client -> "client"
            Side.Server -> "server"
        }
        val knotClass = when (envType) {
            Side.Client -> "net.fabricmc.loader.launch.knot.KnotClient"
            Side.Server -> "net.fabricmc.loader.launch.knot.KnotServer"
        }
        System.setProperty("fabric.dli.config", "launch.cfg")
        System.setProperty("fabric.dli.env", env)
        System.setProperty("fabric.dli.main", knotClass)

        //TODO: get ride of this prob
        val frameworkCode = TestCode {}

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