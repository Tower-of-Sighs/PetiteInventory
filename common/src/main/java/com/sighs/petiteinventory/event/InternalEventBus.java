package com.sighs.petiteinventory.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Small synchronous event bus used by common services. Loader event buses are
 * adapters only; feature logic subscribes here so it can be shared by targets.
 */
public final class InternalEventBus {
    private final Map<Class<?>, CopyOnWriteArrayList<Handler<?>>> handlers = new ConcurrentHashMap<>();
    private final Consumer<Throwable> errorHandler;

    public InternalEventBus() {
        this(throwable -> {
            // Keep one faulty optional integration from stopping game events.
        });
    }

    public InternalEventBus(Consumer<Throwable> errorHandler) {
        this.errorHandler = errorHandler == null ? throwable -> { } : errorHandler;
    }

    public <E> Subscription subscribe(Class<E> type, Consumer<? super E> listener) {
        return subscribe(type, 0, listener);
    }

    public <E> Subscription subscribe(Class<E> type, int priority, Consumer<? super E> listener) {
        if (type == null || listener == null) {
            throw new IllegalArgumentException("event type and listener are required");
        }
        Handler<E> handler = new Handler<>(priority, listener);
        handlers.computeIfAbsent(type, ignored -> new CopyOnWriteArrayList<>()).add(handler);
        return handler;
    }

    /** Publishes to the exact event type and its assignable supertypes. */
    public void publish(Object event) {
        if (event == null) {
            return;
        }
        List<Handler<?>> matching = new ArrayList<>();
        for (Map.Entry<Class<?>, CopyOnWriteArrayList<Handler<?>>> entry : handlers.entrySet()) {
            if (entry.getKey().isInstance(event)) {
                matching.addAll(entry.getValue());
            }
        }
        Collections.sort(matching, (left, right) -> Integer.compare(right.priority(), left.priority()));
        for (Handler<?> handler : matching) {
            try {
                handler.invoke(event);
            } catch (Throwable throwable) {
                errorHandler.accept(throwable);
            }
        }
    }

    public void clear() {
        handlers.clear();
    }

    private static final class Handler<E> implements Subscription {
        private final int priority;
        private final Consumer<? super E> listener;
        private volatile boolean active = true;

        private Handler(int priority, Consumer<? super E> listener) {
            this.priority = priority;
            this.listener = listener;
        }

        private int priority() {
            return priority;
        }

        @SuppressWarnings("unchecked")
        private void invoke(Object event) {
            if (active) {
                listener.accept((E) event);
            }
        }

        @Override
        public void unsubscribe() {
            active = false;
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }
}
