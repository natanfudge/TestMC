package io.github.natanfudge.impl.mixinhandlers

import io.github.natanfudge.MinecraftContext
import io.github.natanfudge.impl.utils.CrossLoaderObjectImpl
import java.lang.reflect.Method

object MainMixinHandler {
    fun asSoonAsPossible() {
        val instance = CrossLoaderObjectImpl.getInstance() ?: return
        for (oldClassloaderMethod in instance.testMethods) {
            val clazz = oldClassloaderMethod.declaringClass.getInCurrentClassLoader()
            val constructor = clazz.getDeclaredConstructor()
            constructor.isAccessible = true
            val newClassloaderMethod = oldClassloaderMethod.getInCurrentClassLoader()
            newClassloaderMethod.isAccessible = true
            newClassloaderMethod.invoke(constructor.newInstance(), MinecraftContext())
        }
    }

    private fun <T> Class<T>.getInCurrentClassLoader() :Class<T>  = Class.forName(name) as Class<T>
    private fun Method.getInCurrentClassLoader() : Method {
        val parameterTypesInCurrentLoader = parameterTypes.map { it.getInCurrentClassLoader() }
        return declaringClass.getInCurrentClassLoader().getDeclaredMethod(name, *parameterTypesInCurrentLoader.toTypedArray())
    }
}