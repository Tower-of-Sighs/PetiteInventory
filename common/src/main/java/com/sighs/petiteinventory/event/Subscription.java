package com.sighs.petiteinventory.event;

/** Handle returned by an event subscription. */
public interface Subscription extends AutoCloseable {
    void unsubscribe();

    boolean isActive();

    @Override
    default void close() {
        unsubscribe();
    }
}
