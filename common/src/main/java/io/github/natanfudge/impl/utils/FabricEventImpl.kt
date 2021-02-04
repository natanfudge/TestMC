package io.github.natanfudge.impl.utils

import java.util.*
import java.util.function.Function

abstract class Event<T> {
    /**
     * The invoker field. This should be updated by the implementation to
     * always refer to an instance containing all code that should be
     * executed upon event emission.
     */
    protected var invoker: T? = null

    /**
     * Returns the invoker instance.
     *
     *
     * An "invoker" is an object which hides multiple registered
     * listeners of type T under one instance of type T, executing
     * them and leaving early as necessary.
     *
     * @return The invoker instance.
     */
    fun invoker(): T? {
        return invoker
    }

    /**
     * Register a listener to the event.
     *
     * @param listener The desired listener.
     */
    abstract fun register(listener: T)
}


object EventFactoryImpl {
    private val ARRAY_BACKED_EVENTS = ArrayList<ArrayBackedEvent<*>>()

    fun <T> createArrayBacked(
        type: Class<in T>,
        invokerFactory: Function<Array<T>, T>
    ): Event<T> {
        return createArrayBacked(type, null, invokerFactory)
    }

    fun <T> createArrayBacked(
        type: Class<in T>,
        emptyInvoker: T?,
        invokerFactory: Function<Array<T>, T>
    ):  Event<T> {
        val event: ArrayBackedEvent<T> = ArrayBackedEvent<T>(type, emptyInvoker, invokerFactory)
        ARRAY_BACKED_EVENTS.add(event)
        return event
    }
}

internal class ArrayBackedEvent<T>(
    private val type: Class<in T>,
    private val dummyInvoker: T?,
    private val invokerFactory: Function<Array<T>, T>
) :
    Event<T>() {
    private var handlers: Array<T>? = null
    fun update() {
        if (handlers == null) {
            if (dummyInvoker != null) {
                invoker = dummyInvoker
            } else {
                invoker = invokerFactory.apply(java.lang.reflect.Array.newInstance(type, 0) as Array<T>)
            }
        } else if (handlers!!.size == 1) {
            invoker = handlers!![0]
        } else {
            invoker = invokerFactory.apply(handlers!!)
        }
    }

    override fun register(listener: T) {
        if (listener == null) {
            throw NullPointerException("Tried to register a null listener!")
        }
        if (handlers == null) {
            handlers = java.lang.reflect.Array.newInstance(type, 1) as Array<T>
            handlers!![0] = listener
        } else {
            handlers = Arrays.copyOf(handlers, handlers!!.size + 1)
            handlers!![handlers!!.size - 1] = listener
        }
        update()
    }

    init {
        update()
    }
}
