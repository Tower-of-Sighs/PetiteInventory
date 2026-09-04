package com.sighs.petiteinventory.platform.inventory;

import com.sighs.petiteinventory.inventory.Area;
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
import java.util.List;

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
    private static final com.sighs.petiteinventory.inventory.ContainerAutomationPolicy.Port<Container, ItemStack, Slot, Direction> PORT =
            new com.sighs.petiteinventory.inventory.ContainerAutomationPolicy.Port<>() {
                @Override public List<Slot> slots(Container target) { return createLogicalSlots(target); }
                @Override public com.sighs.petiteinventory.spi.GridSlotPort<ItemStack, Slot> gridSlots() {
                    return new com.sighs.petiteinventory.spi.GridSlotPort<>() {
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
                @Override public com.sighs.petiteinventory.spi.SlotPort<ItemStack, Slot> slotPort() {
                    return new com.sighs.petiteinventory.spi.SlotPort<>() {
                        @Override public ItemStack stack(Slot slot) { return slot.getItem(); }
                        @Override public boolean canPlace(Slot slot, ItemStack stack) { return slot.mayPlace(stack); }
                        @Override public int maxStackSize(Slot slot) { return slot.getMaxStackSize(); }
                        @Override public void changed(Slot slot) { slot.setChanged(); }
                    };
                }
                @Override public com.sighs.petiteinventory.spi.StackPort<ItemStack> stacks() {
                    return ItemInventoryService.stackPort();
                }
                @Override public boolean canPlace(Container target, Slot slot, ItemStack stack, Direction direction) {
                    return ContainerAutomationService.canPlace(target, slot.getContainerSlot(), stack, direction);
                }
                @Override public void set(Container target, Slot anchor, ItemStack stack) {
                    target.setItem(anchor.getContainerSlot(), stack);
                }
                @Override public void changed(Container target) { target.setChanged(); }
                @Override public int width(Container target, List<Slot> slots) { return inferWidth(target); }
            };
    private static final com.sighs.petiteinventory.inventory.ContainerAutomationPolicy<Container, ItemStack, Slot, Direction> COMMON =
            new com.sighs.petiteinventory.inventory.ContainerAutomationPolicy<>(PORT);

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

        ItemStack remainder = COMMON.insert(target, incoming, direction);
        return remainder == null ? null : (remainder.isEmpty() ? ItemStack.EMPTY : remainder);
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
