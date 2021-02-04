package io.github.natanfudge.impl.utils


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
    val OnServerWorldLoaded = noArgs()
    val OnJoinClientWorld = noArgs()
    val OnServerSafelyClosed = noArgs()
}

