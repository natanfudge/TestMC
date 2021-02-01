//package net.fabricmc.example.events;
//
//import net.fabricmc.fabric.api.event.Event;
//import net.fabricmc.fabric.api.event.EventFactory;
//
//@FunctionalInterface
//public interface ClientStartupCrashedEvent {
//    Event<ClientStartupCrashedEvent> EVENT = EventFactory.createArrayBacked(ClientStartupCrashedEvent.class, (susbcribers) -> () -> {
//        for (ClientStartupCrashedEvent subscriber : susbcribers) {
//            subscriber.onClientStartupCrashed();
//        }
//    });
//
//    void onClientStartupCrashed();
//}
