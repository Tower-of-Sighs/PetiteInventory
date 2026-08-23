package com.sighs.petiteinventory.spi;

import com.sighs.petiteinventory.event.InternalEventBus;

/**
 * Loader/version entry point discovered through {@link java.util.ServiceLoader}.
 * A target owns this implementation; common owns the lifecycle contract.
 */
public interface PlatformServiceProvider {
    String id();

    void initialize(InternalEventBus eventBus);
}
