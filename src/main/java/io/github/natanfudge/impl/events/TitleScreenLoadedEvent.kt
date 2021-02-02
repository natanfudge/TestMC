//package io.github.natanfudge.impl.events
//
//import io.github.natanfudge.impl.events.TitleScreenLoadedEvent
//import net.fabricmc.fabric.api.event.Event
//import net.fabricmc.fabric.api.event.EventFactory
//
//internal fun interface TitleScreenLoadedEvent {
//    fun onTitleScreenLoaded()
//
//    companion object {
//        @JvmStatic
//        val EVENT: Event<TitleScreenLoadedEvent> = EventFactory.createArrayBacked(TitleScreenLoadedEvent::class.java) { susbcribers ->
//            TitleScreenLoadedEvent {
//                for (subscriber in susbcribers) {
//                    subscriber.onTitleScreenLoaded()
//                }
//            }
//        }
//    }
//}