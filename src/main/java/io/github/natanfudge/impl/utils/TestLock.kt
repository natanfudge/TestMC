package io.github.natanfudge.impl.utils

import io.github.natanfudge.impl.utils.TestLock
import java.lang.ClassNotFoundException
import java.lang.NoSuchFieldException
import java.lang.IllegalAccessException

object TestLock {
    private var instance: Any? = null

    /**
     * Retrieve some TRUELY singleton Object. There can only be one, even across different class loaders.
     */
    @Synchronized
    fun getInstance(): Any? {
        val myClassLoader = Any::class.java.classLoader
        if (instance == null) {
            // The point is that we will only use ONE classloader.
            // the instance will be stored in the "root class loader", aka the appClassLoader.
            val rootClassLoader = getRootClassLoader()
            if (myClassLoader === rootClassLoader) {
                // If this is the appClassLoader then we can just use normal java to do this, woohoo.
                instance = Any()
            } else {
                // If this is not appClassLoader, then we need to get the instance from appClassLoader using reflection.
                // If there is no instance yet, we will put one there.
                try {
                    val absoluteSingletonClassInAppLoader = rootClassLoader.loadClass(TestLock::class.java.name)
                    val instanceFieldInAppLoader = absoluteSingletonClassInAppLoader.getField("instance")
                    val instanceInAppLoader = instanceFieldInAppLoader[null]
                    if (instanceInAppLoader == null) {
                        val newInstance = Any()
                        instanceFieldInAppLoader[null] = newInstance
                        instance = newInstance
                    } else {
                        instance = instanceInAppLoader
                    }
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                } catch (e: NoSuchFieldException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            }
        }
        return instance
    }

    // Minecraft class loader parent hierarchy looks like this:
    // null -> platformClassLoader -> appClassLoader -> someFLoaderBsLoader1 -> someFLoaderBsLoader2 -> minecraftClassLoader
    // Test class loader parent hierarchy looks like this:
    // null -> platformClassLoader -> appClassLoader.
    // Therefore the best way to find the common ancestor (appClassLoader) is going up and up until loader.getParent().getParent() == null
    private fun getRootClassLoader(): ClassLoader {
        // Minecraft class loader parent hierarchy looks like this:
        // null -> platformClassLoader -> appClassLoader -> someFLoaderBsLoader1 -> someFLoaderBsLoader2 -> minecraftClassLoader
        // Test class loader parent hierarchy looks like this:
        // null -> platformClassLoader -> appClassLoader.
        // Therefore the best way to find the common ancestor (appClassLoader) is going up and up until loader.getParent().getParent() == null
        var currentLoader = TestLock::class.java.classLoader
        while (!isRootClassLoader(currentLoader)) {
            currentLoader = currentLoader.parent
        }
        return currentLoader
    }

    private fun isRootClassLoader(loader: ClassLoader): Boolean {
        return loader.parent.parent == null
    }
}