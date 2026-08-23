package com.sighs.petiteinventory.inventory;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.inventory.Slot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Applies Petite footprints to automated container insertion paths.
 *
 * Hopper insertion does not have a Screen or menu, so it cannot use the normal
 * screen-level grid. This service builds the same logical row-major grid from
 * the target Container and only handles stacks whose configured footprint is
 * larger than one slot. A null result means that vanilla should handle the
 * insertion; a non-null result is the remaining stack after Petite handled it.
 */
public final class ContainerAutomationService {
    private ContainerAutomationService() {
    }

    /**
     * Attempts to insert an automated stack. Returns null to delegate to
     * vanilla, ItemStack.EMPTY when the whole stack was accepted, or a
     * non-empty remainder when the stack was handled but could not fit fully.
     */
    @Nullable
    public static ItemStack tryInsertFromHopper(Container target, ItemStack incoming,
                                                @Nullable Direction direction) {
        if (target == null || incoming.isEmpty()) {
            return null;
        }

        Area area = ItemInventoryService.getArea(incoming);
        if (area.width() == 1 && area.height() == 1) {
            return null;
        }

        List<Slot> slots = createLogicalSlots(target);
        if (slots.isEmpty()) {
            return incoming;
        }

        // Preserve vanilla stacking semantics before occupying a new footprint.
        stackIntoExisting(target, slots, incoming, direction);
        if (incoming.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int width = inferWidth(target);
        Slot anchor = findEmptyFootprint(target, slots, incoming, area, width, direction);
        if (anchor == null) {
            boolean wasRotated = ItemInventoryService.ItemRotateHelper.isRotated(incoming);
            ItemInventoryService.ItemRotateHelper.setRotated(incoming, !wasRotated);
            Area rotatedArea = ItemInventoryService.getArea(incoming);
            anchor = findEmptyFootprint(target, slots, incoming, rotatedArea, width, direction);
            if (anchor == null) {
                ItemInventoryService.ItemRotateHelper.setRotated(incoming, wasRotated);
                return incoming;
            }
        }

        target.setItem(anchor.getContainerSlot(), incoming.copy());
        target.setChanged();
        incoming.setCount(0);
        return ItemStack.EMPTY;
    }

    /**
     * Handles Forge's capability-based hopper path for targets that still
     * expose the vanilla Container contract. Forge checks this path before
     * calling HopperBlockEntity.tryMoveInItem, so both paths must share the
     * same footprint rules.
     *
     * @return null to delegate to Forge, or true when a sized stack was
     * handled (including the no-space case, which must not fall back to a
     * one-slot insertion).
     */
    @Nullable
    public static Boolean tryInsertFromForgeHook(HopperBlockEntity hopper) {
        if (hopper == null || hopper.getLevel() == null) return null;

        Direction facing = hopper.getBlockState().getValue(HopperBlock.FACING);
        Direction targetFace = facing.getOpposite();
        BlockPos targetPos = hopper.getBlockPos().relative(facing);
        BlockEntity blockEntity = hopper.getLevel().getBlockEntity(targetPos);
        if (!(blockEntity instanceof Container target)) return null;

        int sourceSlot = -1;
        for (int index = 0; index < hopper.getContainerSize(); index++) {
            if (!hopper.getItem(index).isEmpty()) {
                sourceSlot = index;
                break;
            }
        }
        if (sourceSlot < 0) return null;

        ItemStack source = hopper.getItem(sourceSlot);
        Area area = ItemInventoryService.getArea(source);
        if (area.width() == 1 && area.height() == 1) return null;

        // Forge would otherwise put a sized item into one capability slot.
        // Keep the source untouched when no complete footprint is available.
        ItemStack incoming = source.copy();
        ItemStack remainder = tryInsertFromHopper(target, incoming, targetFace);
        if (remainder == null) return null;
        hopper.setItem(sourceSlot, remainder);
        return Boolean.TRUE;
    }

    private static void stackIntoExisting(Container target, List<Slot> slots, ItemStack incoming,
                                          @Nullable Direction direction) {
        for (Slot slot : slots) {
            ItemStack existing = slot.getItem();
            if (existing.isEmpty() || !ItemInventoryService.isSameItemIgnoreRotate(existing, incoming)
                    || !canPlace(target, slot.getContainerSlot(), incoming, direction)) {
                continue;
            }

            int maximum = Math.min(target.getMaxStackSize(), incoming.getMaxStackSize());
            int amount = Math.min(incoming.getCount(), maximum - existing.getCount());
            if (amount <= 0) continue;

            existing.grow(amount);
            incoming.shrink(amount);
            target.setChanged();
            if (incoming.isEmpty()) return;
        }
    }

    private static Slot findEmptyFootprint(Container target, List<Slot> slots, ItemStack incoming,
                                           Area area, int width, @Nullable Direction direction) {
        if (width <= 0) return null;

        ContainerGrid grid = ContainerGrid.parse(slots);
        List<ContainerGrid.Cell> ordered = new ArrayList<>(grid.getCells());
        ordered.sort(Comparator.comparingInt(cell -> cell.slot().index));
        Map<ContainerGrid.Cell, ContainerGrid.Cell> occupied = grid.getCellMap();

        for (ContainerGrid.Cell anchor : ordered) {
            List<ContainerGrid.Cell> footprint = grid.getCellsBySlotOrder(anchor, area, width);
            if (footprint.size() != area.width() * area.height()) continue;

            boolean valid = true;
            for (ContainerGrid.Cell cell : footprint) {
                if (!cell.isEmpty() || occupied.containsKey(cell)
                        || !canPlace(target, cell.slot().getContainerSlot(), incoming, direction)) {
                    valid = false;
                    break;
                }
            }
            if (valid) return anchor.slot();
        }
        return null;
    }

    private static boolean canPlace(Container target, int slot, ItemStack stack,
                                    @Nullable Direction direction) {
        if (!target.canPlaceItem(slot, stack)) return false;
        if (target instanceof WorldlyContainer worldly
                && !worldly.canPlaceItemThroughFace(slot, stack, direction)) {
            return false;
        }
        return true;
    }

    private static List<Slot> createLogicalSlots(Container target) {
        int size = target.getContainerSize();
        if (size <= 0) return List.of();

        int width = inferWidth(target);
        List<Slot> slots = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            slots.add(new Slot(target, index, (index % width) * 18, (index / width) * 18));
        }
        return slots;
    }

    /**
     * Most vanilla storage containers use nine columns. For other containers
     * without screen coordinates, choose the factor at or above the square root
     * so even-column storages remain rectangular instead of degenerating to one
     * long row.
     */
    private static int inferWidth(Container target) {
        int size = target.getContainerSize();
        if (target instanceof Inventory || (size >= 9 && size % 9 == 0)) return 9;
        if (size <= 9) return size;

        int root = (int) Math.ceil(Math.sqrt(size));
        for (int width = Math.max(1, root); width <= Math.min(9, size); width++) {
            if (size % width == 0) return width;
        }
        return Math.min(9, size);
    }
}
