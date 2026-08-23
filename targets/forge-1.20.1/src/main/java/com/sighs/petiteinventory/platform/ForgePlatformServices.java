package com.sighs.petiteinventory.platform;

import com.sighs.petiteinventory.event.InternalEventBus;
import com.sighs.petiteinventory.event.InventoryEvents;
import com.sighs.petiteinventory.platform.inventory.ContainerAutomationService;
import com.sighs.petiteinventory.platform.inventory.QuickMoveService;
import com.sighs.petiteinventory.platform.inventory.SophisticatedQuickMoveService;
import com.sighs.petiteinventory.service.InventoryRuntime;
import com.sighs.petiteinventory.spi.PlatformServiceProvider;
import com.sighs.petiteinventory.platform.spi.ForgeInventoryPort;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/** Forge target wiring. Feature behavior remains in common or feature services. */
public final class ForgePlatformServices implements PlatformServiceProvider {
    // Keep the runtime alive for the lifetime of the target; it owns event subscriptions.
    private static InventoryRuntime<?, ?> runtime;
    private static boolean initialized;

    @Override
    public String id() {
        return "forge-1.20.1";
    }

    @Override
    public synchronized void initialize(InternalEventBus eventBus) {
        if (initialized) {
            return;
        }
        InventoryRuntime<net.minecraft.world.entity.player.Player, ItemStack> inventoryRuntime =
                new InventoryRuntime<>(new ForgeInventoryPort(), eventBus);
        inventoryRuntime.start();
        runtime = inventoryRuntime;

        eventBus.subscribe(InventoryEvents.HopperInsert.class, 100, event -> {
            if (event.incoming() == null) {
                Boolean handled = ContainerAutomationService.tryInsertFromForgeHook(
                        (HopperBlockEntity) event.target());
                if (handled != null) {
                    event.handled(handled);
                }
                return;
            }
            ItemStack remainder = ContainerAutomationService.tryInsertFromHopper(
                    (Container) event.target(), (ItemStack) event.incoming(),
                    (Direction) event.direction());
            if (remainder != null) {
                event.handled(remainder);
            }
        });
        eventBus.subscribe(InventoryEvents.QuickMove.class, 100, event -> {
            if (QuickMoveService.handle((AbstractContainerMenu) event.menu(), event.slot(),
                    event.button(), ClickType.QUICK_MOVE,
                    (net.minecraft.world.entity.player.Player) event.player())) {
                event.handled();
            }
        });
        eventBus.subscribe(InventoryEvents.StorageQuickMove.class, 100, event -> {
            ItemStack result = SophisticatedQuickMoveService.handle(
                    (AbstractContainerMenu) event.menu(),
                    (net.minecraft.world.entity.player.Player) event.player(), event.slot());
            if (result != null) event.handled(result);
        });
        initialized = true;
    }
}
