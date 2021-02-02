//package io.github.natanfudge.impl.events
//
//import io.github.natanfudge.impl.events.WorldLoadedEvent
//import net.fabricmc.fabric.api.event.EventFactory
//
//internal fun interface WorldLoadedEvent {
//    fun onWorldLoaded()
//
//    companion object {
//        val EVENT = EventFactory.createArrayBacked(WorldLoadedEvent::class.java) { susbcribers ->
//            WorldLoadedEvent {
//                for (subscriber in susbcribers) {
//                    subscriber.onWorldLoaded()
//                }
//            }
//        }
//    }
//}