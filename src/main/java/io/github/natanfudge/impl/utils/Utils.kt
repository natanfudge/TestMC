package io.github.natanfudge.impl.utils

internal fun <T> flatten(obj : T, nextGetter: T.() -> T?) : List<T> = mutableListOf<T>().apply {
    var current : T? = obj
    while(current != null){
        add(current)
        current = current.nextGetter()
    }
}