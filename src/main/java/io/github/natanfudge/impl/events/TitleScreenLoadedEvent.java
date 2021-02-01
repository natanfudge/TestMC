package io.github.natanfudge.impl.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

@FunctionalInterface
public interface TitleScreenLoadedEvent {
    Event<TitleScreenLoadedEvent> EVENT = EventFactory.createArrayBacked(TitleScreenLoadedEvent.class, (susbcribers) -> () -> {
        for (TitleScreenLoadedEvent subscriber : susbcribers) {
            subscriber.onTitleScreenLoaded();
        }
    });

    void onTitleScreenLoaded();
}
