package com.sighs.petiteinventory.spi;

import com.sighs.petiteinventory.event.InternalEventBus;

import java.util.Iterator;
import java.util.ServiceLoader;

/** Resolves the target implementation without a compile-time loader dependency. */
public final class PlatformServices {
    private static volatile PlatformServiceProvider provider;

    private PlatformServices() {
    }

    public static PlatformServiceProvider load() {
        PlatformServiceProvider current = provider;
        if (current != null) {
            return current;
        }
        synchronized (PlatformServices.class) {
            current = provider;
            if (current == null) {
                Iterator<PlatformServiceProvider> providers = ServiceLoader
                        .load(PlatformServiceProvider.class, PlatformServices.class.getClassLoader())
                        .iterator();
                if (!providers.hasNext()) {
                    current = new NoopPlatformServiceProvider();
                } else {
                    current = providers.next();
                }
                provider = current;
            }
            return current;
        }
    }

    /** Test and embedded-launch hook; target code normally uses {@link #load()}. */
    public static void install(PlatformServiceProvider serviceProvider) {
        if (serviceProvider == null) {
            throw new IllegalArgumentException("serviceProvider");
        }
        provider = serviceProvider;
    }

    public static void initialize(InternalEventBus eventBus) {
        load().initialize(eventBus);
    }

    private static final class NoopPlatformServiceProvider implements PlatformServiceProvider {
        @Override
        public String id() {
            return "none";
        }

        @Override
        public void initialize(InternalEventBus eventBus) {
            // Common can be used by pure Java integrations without a game loader.
        }
    }
}
