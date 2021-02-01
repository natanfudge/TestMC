package io.github.natanfudge.impl.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

@FunctionalInterface
public interface WorldLoadedEvent {
    Event<WorldLoadedEvent> EVENT = EventFactory.createArrayBacked(WorldLoadedEvent.class, (susbcribers) -> () -> {
        for (WorldLoadedEvent subscriber : susbcribers) {
            subscriber.onWorldLoaded();
        }
    });

    void onWorldLoaded();
}
