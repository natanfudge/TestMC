package io.github.natanfudge.impl.utils

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock


internal interface CrossLoaderObject {
    val testMethods: List<Method>
    var testDone: Boolean
    val lock: Lock
    val condition: Condition
}

internal class CrossLoaderObjectImpl(override val testMethods: List<Method>) : CrossLoaderObject {
    override val lock: Lock = ReentrantLock()
    override val condition: Condition = lock.newCondition()


    override var testDone: Boolean = false

    companion object {
        @JvmStatic
        private var instance: CrossLoaderObject? = null

        /**
         * Retrieve some TRUELY singleton Object. There can only be one, even across different class loaders.
         */
        @Synchronized
        fun getInstance(): CrossLoaderObject? {
            val myClassLoader = CrossLoaderObject::class.java.classLoader
            // The point is that we will only use ONE classloader.
            // the instance will be stored in the "root class loader", aka the appClassLoader.
            val rootClassLoader = getRootClassLoader()
            if (myClassLoader === rootClassLoader) {
                // If this is the appClassLoader then we can just use normal java to do this, woohoo.
//                instance = ctr()
//                return instance
                return instance
            } else {
                // If this is not appClassLoader, then we need to get the instance from appClassLoader using reflection.
                // If there is no instance yet, we will put one there.
                val absoluteSingletonClassInAppLoader =
                    rootClassLoader.loadClass(CrossLoaderObjectImpl::class.java.name)
                val instanceFieldInAppLoader = absoluteSingletonClassInAppLoader.getDeclaredField("instance")
                instanceFieldInAppLoader.isAccessible = true
                val instanceInAppLoader = instanceFieldInAppLoader.get(null)
//                if (instanceInAppLoader == null) {
//                    val newInstance = ctr()
//                    instanceFieldInAppLoader[null] = newInstance
//                    instance = newInstance
//                } else {
                // But, we can't cast it to our own interface directly because classes loaded from
                // different classloaders implement different versions of an interface.
                // So instead, we use java.lang.reflect.Proxy to wrap it in an object that *does*
                // support our interface, and the proxy will use reflection to pass through all calls
                // to the object.
                return if (instanceInAppLoader == null) null else Proxy.newProxyInstance(
                    myClassLoader,
                    arrayOf(CrossLoaderObject::class.java),
                    PassThroughProxyHandler(instanceInAppLoader)
                )
                        as CrossLoaderObject
//                }

            }

//        return instance
        }

        @Synchronized
        fun setInstance(value: CrossLoaderObject) {
            val myClassLoader = CrossLoaderObject::class.java.classLoader
            // The point is that we will only use ONE classloader.
            // the instance will be stored in the "root class loader", aka the appClassLoader.
            val rootClassLoader = getRootClassLoader()
            if (myClassLoader === rootClassLoader) {
                // If this is the appClassLoader then we can just use normal java to do this, woohoo.
                this.instance = value
            } else {
                // If this is not appClassLoader, then we need to get the instance from appClassLoader using reflection.
                // If there is no instance yet, we will put one there.
                val absoluteSingletonClassInAppLoader =
                    rootClassLoader.loadClass(CrossLoaderObjectImpl::class.java.name)
                val instanceFieldInAppLoader = absoluteSingletonClassInAppLoader.getDeclaredField("instance")
                instanceFieldInAppLoader.isAccessible = true
                instanceFieldInAppLoader.set(null, value)
            }
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
            var currentLoader = this::class.java.classLoader
            while (!isRootClassLoader(currentLoader)) {
                currentLoader = currentLoader.parent
            }
            return currentLoader
        }

        private fun isRootClassLoader(loader: ClassLoader): Boolean {
            return loader.parent.parent == null
        }
    }


}

/**
 * An invocation handler that passes on any calls made to it directly to its delegate.
 * This is useful to handle identical classes loaded in different classloaders - the
 * VM treats them as different classes, but they have identical signatures.
 *
 * Note this is using class.getMethod, which will only work on public methods.
 */
internal class PassThroughProxyHandler(private val delegate: Any?) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any? {
        val delegateMethod = delegate?.javaClass?.getMethod(method.name, *method.parameterTypes)

        return if (args == null) delegateMethod?.invoke(delegate)
        else delegateMethod?.invoke(delegate, args)
    }
}


//internal val TestMethod = CrossLoaderObject<Method?>() { null }