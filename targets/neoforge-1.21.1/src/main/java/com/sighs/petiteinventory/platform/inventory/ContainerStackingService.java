package com.sighs.petiteinventory.platform.inventory;

import com.sighs.petiteinventory.platform.spi.ForgeInventoryPort;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Forge Slot adapter for the loader-neutral stacking policy. */
public final class ContainerStackingService {
    private static final com.sighs.petiteinventory.inventory.ContainerStackingService<ItemStack, Slot> COMMON =
            new com.sighs.petiteinventory.inventory.ContainerStackingService<>(
                    ForgeInventoryPort.stackPort(), new ForgeSlotPort());

    private ContainerStackingService() {
    }

    public static boolean stackIntoExisting(ItemStack source, List<Slot> targets) {
        return COMMON.stackIntoExisting(source, targets);
    }

    public static boolean stack(ItemStack source, ItemStack target, Slot targetSlot) {
        return COMMON.stack(source, target, targetSlot);
    }

    private static final class ForgeSlotPort implements com.sighs.petiteinventory.spi.SlotPort<ItemStack, Slot> {
        @Override public ItemStack stack(Slot slot) { return slot.getItem(); }
        @Override public boolean canPlace(Slot slot, ItemStack stack) { return slot.mayPlace(stack); }
        @Override public int maxStackSize(Slot slot) { return slot.getMaxStackSize(); }
        @Override public void changed(Slot slot) { slot.setChanged(); }
    }
}
