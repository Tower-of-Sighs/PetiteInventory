package com.sighs.petiteinventory.service;

import com.sighs.petiteinventory.event.InternalEventBus;
import com.sighs.petiteinventory.event.InventoryEvents;
import com.sighs.petiteinventory.event.Subscription;
import com.sighs.petiteinventory.spi.InventoryPort;

import java.util.ArrayList;
import java.util.List;

/** Installs common inventory listeners once for a target runtime. */
public final class InventoryRuntime<P, S> implements AutoCloseable {
    private final InventoryAdmissionService<P, S> admission;
    private final InternalEventBus events;
    private final List<Subscription> subscriptions = new ArrayList<>();
    private boolean started;

    public InventoryRuntime(InventoryPort<P, S> inventory, InternalEventBus events) {
        this.admission = new InventoryAdmissionService<>(inventory);
        if (events == null) {
            throw new IllegalArgumentException("events");
        }
        this.events = events;
    }

    public synchronized void start() {
        if (started) {
            return;
        }
        subscriptions.add(events.subscribe(InventoryEvents.Admission.class, 100, event -> {
            AdmissionResult result = admission.admit(player(event), stack(event));
            if (result != AdmissionResult.DEFER_TO_VANILLA) {
                event.handled(result);
            }
        }));
        subscriptions.add(events.subscribe(InventoryEvents.ContainerClose.class, 100,
                event -> {
                    admission.returnCarriedItem(player(event), stack(event));
                    event.handled();
                }));
        started = true;
    }

    public InventoryAdmissionService<P, S> admission() {
        return admission;
    }

    @SuppressWarnings("unchecked")
    private P player(InventoryEvents.Admission event) {
        return (P) event.player();
    }

    @SuppressWarnings("unchecked")
    private S stack(InventoryEvents.Admission event) {
        return (S) event.stack();
    }

    @SuppressWarnings("unchecked")
    private P player(InventoryEvents.ContainerClose event) {
        return (P) event.player();
    }

    @SuppressWarnings("unchecked")
    private S stack(InventoryEvents.ContainerClose event) {
        return (S) event.carried();
    }

    @Override
    public synchronized void close() {
        for (Subscription subscription : subscriptions) {
            subscription.unsubscribe();
        }
        subscriptions.clear();
        started = false;
    }
}
