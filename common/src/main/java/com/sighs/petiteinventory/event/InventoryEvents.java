package com.sighs.petiteinventory.event;

import com.sighs.petiteinventory.service.AdmissionResult;

/** Shared event entry points used by target adapters and common subscribers. */
public final class InventoryEvents {
    public static final InternalEventBus BUS = new InternalEventBus();

    private InventoryEvents() {
    }

    public static void publish(InternalEvent event) {
        BUS.publish(event);
    }

    public static final class Admission implements InternalEvent {
        private final Object player;
        private final Object stack;
        private AdmissionResult result = AdmissionResult.DEFER_TO_VANILLA;
        private boolean handled;

        public Admission(Object player, Object stack) {
            this.player = player;
            this.stack = stack;
        }

        public Object player() {
            return player;
        }

        public Object stack() {
            return stack;
        }

        public AdmissionResult result() {
            return result;
        }

        public boolean isHandled() {
            return handled;
        }

        public void handled(AdmissionResult result) {
            this.result = result == null ? AdmissionResult.DEFER_TO_VANILLA : result;
            this.handled = true;
        }
    }

    public static final class ContainerClose implements InternalEvent {
        private final Object player;
        private final Object carried;
        private boolean handled;

        public ContainerClose(Object player, Object carried) {
            this.player = player;
            this.carried = carried;
        }

        public Object player() {
            return player;
        }

        public Object carried() {
            return carried;
        }

        public boolean isHandled() {
            return handled;
        }

        public void handled() {
            this.handled = true;
        }
    }

    public static final class HopperInsert implements InternalEvent {
        private final Object target;
        private final Object incoming;
        private final Object direction;
        private Object remainder;
        private boolean handled;

        public HopperInsert(Object target, Object incoming, Object direction) {
            this.target = target;
            this.incoming = incoming;
            this.direction = direction;
        }

        public Object target() {
            return target;
        }

        public Object incoming() {
            return incoming;
        }

        public Object direction() {
            return direction;
        }

        public Object remainder() {
            return remainder;
        }

        public boolean isHandled() {
            return handled;
        }

        public void handled(Object remainder) {
            this.remainder = remainder;
            this.handled = true;
        }
    }

    /** Target adapter entry point for a menu's quick-move action. */
    public static final class QuickMove implements InternalEvent {
        private final Object menu;
        private final int slot;
        private final int button;
        private final Object player;
        private boolean handled;

        public QuickMove(Object menu, int slot, int button, Object player) {
            this.menu = menu;
            this.slot = slot;
            this.button = button;
            this.player = player;
        }

        public Object menu() { return menu; }
        public int slot() { return slot; }
        public int button() { return button; }
        public Object player() { return player; }
        public boolean isHandled() { return handled; }
        public void handled() { this.handled = true; }
    }

    /** Quick-move entry point for storage mods with private menu APIs. */
    public static final class StorageQuickMove implements InternalEvent {
        private final Object menu;
        private final int slot;
        private final Object player;
        private Object result;
        private boolean handled;

        public StorageQuickMove(Object menu, int slot, Object player) {
            this.menu = menu;
            this.slot = slot;
            this.player = player;
        }

        public Object menu() { return menu; }
        public int slot() { return slot; }
        public Object player() { return player; }
        public Object result() { return result; }
        public boolean isHandled() { return handled; }
        public void handled(Object result) { this.result = result; this.handled = true; }
    }
}
