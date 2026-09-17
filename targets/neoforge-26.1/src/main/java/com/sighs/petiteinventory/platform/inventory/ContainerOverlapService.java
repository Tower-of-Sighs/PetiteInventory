package com.sighs.petiteinventory.platform.inventory;

import com.sighs.petiteinventory.config.ModConfig;
import com.sighs.petiteinventory.inventory.ContainerOverlapPlanner;
import com.sighs.petiteinventory.spi.GridSlotPort;
import com.sighs.petiteinventory.spi.StackPort;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Applies the overlap-rearrangement policy to newly opened container menus. */
public final class ContainerOverlapService {
    private static final ContainerOverlapPlanner.Port<ItemStack, Slot> PORT =
            new ContainerOverlapPlanner.Port<>() {
                @Override public GridSlotPort<ItemStack, Slot> gridSlots() {
                    return new GridSlotPort<>() {
                        @Override public int x(Slot slot) { return slot.x; }
                        @Override public int y(Slot slot) { return slot.y; }
                        @Override public int index(Slot slot) { return slot.index; }
                        @Override public Object container(Slot slot) { return slot.container; }
                        @Override public boolean isEmpty(Slot slot) { return !slot.hasItem(); }
                        @Override public ItemStack stack(Slot slot) { return slot.getItem(); }
                        @Override public com.sighs.petiteinventory.core.ItemSize footprint(ItemStack stack) {
                            return ItemInventoryService.getArea(stack).size();
                        }
                    };
                }

                @Override public StackPort<ItemStack> stacks() { return ItemInventoryService.stackPort(); }

                @Override public boolean sameContainer(Slot left, Slot right) {
                    if (left.container == right.container) return true;
                    return left.container != null && left.container.equals(right.container);
                }

                @Override public boolean canMove(Slot slot, ItemStack stack) {
                    return slot.mayPlace(stack);
                }
            };
    private static final ContainerOverlapPlanner<ItemStack, Slot> PLANNER =
            new ContainerOverlapPlanner<>();

    private ContainerOverlapService() {
    }

    public static void tidy(AbstractContainerMenu menu) {
        if (menu == null || !ModConfig.ENABLE_OVERLAP_TIDY.get()) {
            return;
        }

        List<Slot> slots = collectSlots(menu);
        List<ContainerOverlapPlanner.Move<ItemStack, Slot>> moves = PLANNER.plan(slots, PORT);
        if (moves.isEmpty()) {
            return;
        }

        Map<Slot, ItemStack> targetStacks = new HashMap<>();
        for (ContainerOverlapPlanner.Move<ItemStack, Slot> move : moves) {
            targetStacks.put(move.to, move.stack);
        }

        for (ContainerOverlapPlanner.Move<ItemStack, Slot> move : moves) {
            move.from.set(ItemStack.EMPTY);
        }
        for (Map.Entry<Slot, ItemStack> entry : targetStacks.entrySet()) {
            entry.getKey().set(entry.getValue());
        }

        Set<Slot> touched = new HashSet<>();
        for (ContainerOverlapPlanner.Move<ItemStack, Slot> move : moves) {
            touched.add(move.from);
            touched.add(move.to);
        }
        for (Slot slot : touched) {
            slot.setChanged();
        }
        menu.broadcastChanges();
    }

    public static boolean needsTidy(AbstractContainerMenu menu) {
        if (menu == null || !ModConfig.ENABLE_OVERLAP_TIDY.get()) {
            return false;
        }
        return !PLANNER.plan(collectSlots(menu), PORT).isEmpty();
    }

    private static List<Slot> collectSlots(AbstractContainerMenu menu) {
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (isTidyCandidate(slot)) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private static boolean isTidyCandidate(Slot slot) {
        if (slot == null || slot.container instanceof Inventory || isResultLike(slot)) {
            return false;
        }
        return true;
    }

    private static boolean isResultLike(Slot slot) {
        String name = slot.getClass().getName().toLowerCase(Locale.ROOT);
        return name.endsWith("resultslot") || name.endsWith("outputslot")
                || name.contains("merchantresult") || name.contains("traderesult");
    }
}
