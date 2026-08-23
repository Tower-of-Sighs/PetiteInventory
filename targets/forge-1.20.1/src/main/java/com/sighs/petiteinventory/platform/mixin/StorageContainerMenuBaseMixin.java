package com.sighs.petiteinventory.platform.mixin;

import com.sighs.petiteinventory.inventory.Area;
import com.sighs.petiteinventory.inventory.ContainerGrid;
import com.sighs.petiteinventory.inventory.ItemInventoryService;
import com.sighs.petiteinventory.platform.NetworkChannel;
import com.sighs.petiteinventory.platform.SophisticatedQuickMovePayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Sophisticated Core implements quick-move itself and otherwise tries each
 * storage slot independently. A sized stack must instead be committed only to
 * the anchor selected by Petite's footprint grid.
 */
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase", remap = false)
public abstract class StorageContainerMenuBaseMixin {
    @Shadow
    public abstract boolean isStorageInventorySlot(int slot);

    @Shadow
    public abstract int getNumberOfRows();

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true, remap = false)
    private void moveSizedStackIntoStorage(Player player, int slotIndex,
                                           CallbackInfoReturnable<ItemStack> callback) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        List<Slot> slots = menu.slots;
        if (slotIndex < 0 || slotIndex >= slots.size()) return;

        if (isStorageInventorySlot(slotIndex)) {
            moveStackToPlayerMainInventory(player, slots, slotIndex, callback);
            return;
        }

        Slot sourceSlot = slots.get(slotIndex);
        if (!(sourceSlot.container instanceof Inventory) || !sourceSlot.hasItem()) return;

        ItemStack sourceStack = sourceSlot.getItem();
        Area area = ItemInventoryService.getArea(sourceStack);

        List<Slot> storageSlots = new ArrayList<>();
        for (int index = 0; index < slots.size(); index++) {
            if (isStorageInventorySlot(index)) storageSlots.add(slots.get(index));
        }
        if (storageSlots.isEmpty()) return;

        // The client selects the destination from the screen's current slot
        // layout and sends that exact footprint to the server. Do not run
        // Core's server-side placeholder layout a second time: all storage
        // slots have x/y == 0 there, which can select a different anchor.
        if (!player.level().isClientSide) {
            callback.setReturnValue(ItemStack.EMPTY);
            return;
        }

        ContainerGrid storageGrid = ContainerGrid.parse(storageSlots);
        int storageWidth = resolveStorageWidth(storageGrid, storageSlots);
        ContainerGrid.Cell target = storageGrid.findAreaBySlotOrder(area, storageWidth);
        boolean rotatedForTarget = false;
        boolean originalRotation = false;
        if (target == null) {
            originalRotation = ItemInventoryService.ItemRotateHelper.isRotated(sourceStack);
            ItemInventoryService.ItemRotateHelper.setRotated(sourceStack, !originalRotation);
            target = storageGrid.findAreaBySlotOrder(ItemInventoryService.getArea(sourceStack), storageWidth);
            if (target == null) {
                ItemInventoryService.ItemRotateHelper.setRotated(sourceStack, originalRotation);
                callback.setReturnValue(ItemStack.EMPTY);
                return;
            }
            rotatedForTarget = true;
        }

        Slot targetSlot = target.slot();
        if (!targetSlot.mayPlace(sourceStack)) {
            if (rotatedForTarget) {
                ItemInventoryService.ItemRotateHelper.setRotated(sourceStack, originalRotation);
            }
            callback.setReturnValue(ItemStack.EMPTY);
            return;
        }

        Area targetArea = ItemInventoryService.getArea(sourceStack);
        int[] footprint = storageGrid.getCellsBySlotOrder(target, targetArea, storageWidth).stream()
                .mapToInt(cell -> slots.indexOf(cell.slot()))
                .toArray();
        if (footprint.length != targetArea.width() * targetArea.height()) {
            callback.setReturnValue(ItemStack.EMPTY);
            return;
        }
        boolean rotated = ItemInventoryService.ItemRotateHelper.isRotated(sourceStack);

        ItemStack movedStack = sourceStack.copy();
        targetSlot.set(movedStack);
        targetSlot.setChanged();
        sourceSlot.set(ItemStack.EMPTY);
        sourceSlot.setChanged();
        sourceSlot.onTake(player, movedStack);
        if (player.level().isClientSide) {
            NetworkChannel.CHANNEL.sendToServer(new SophisticatedQuickMovePayload(slotIndex, slots.indexOf(targetSlot), footprint, rotated));
        }
        callback.setReturnValue(movedStack);
    }

    @Unique
    private int resolveStorageWidth(ContainerGrid grid, List<Slot> storageSlots) {
        int rows = getNumberOfRows();
        if (rows > 0 && storageSlots.size() % rows == 0) {
            // The server-side menu can expose every storage slot at the same
            // placeholder coordinate. Derive the logical width from its rows so
            // 2/4/6/8-column storages use the same layout on both sides.
            return Math.max(1, storageSlots.size() / rows);
        }

        int visualWidth = grid.getWidth();
        if (visualWidth > 1) return visualWidth;
        return rows > 0 ? Math.max(1, (storageSlots.size() + rows - 1) / rows) : storageSlots.size();
    }

    @Unique
    private void moveStackToPlayerMainInventory(Player player, List<Slot> slots, int slotIndex,
                                                CallbackInfoReturnable<ItemStack> callback) {
        Slot source = slots.get(slotIndex);
        if (!source.hasItem()) return;
        ItemStack stack = source.getItem();
        ItemStack original = stack.copy();
        int firstPlayerSlot = 0;
        while (firstPlayerSlot < slots.size() && isStorageInventorySlot(firstPlayerSlot)) firstPlayerSlot++;
        List<Slot> mainInventorySlots = new ArrayList<>(slots.subList(
                firstPlayerSlot, Math.min(firstPlayerSlot + 27, slots.size())));
        int hotbarStart = Math.min(firstPlayerSlot + 27, slots.size());
        List<Slot> hotbarSlots = new ArrayList<>(slots.subList(
                hotbarStart, Math.min(hotbarStart + 9, slots.size())));
        List<Slot> targets = new ArrayList<>(hotbarSlots.size() + mainInventorySlots.size());
        targets.addAll(hotbarSlots);
        targets.addAll(mainInventorySlots);
        if (targets.isEmpty()) {
            callback.setReturnValue(ItemStack.EMPTY);
            return;
        }

        for (Slot target : targets) {
            ItemStack existing = target.getItem();
            if (existing.isEmpty() || !ItemInventoryService.isSameItemIgnoreRotate(existing, stack)) continue;
            int capacity = Math.min(target.getMaxStackSize(), stack.getMaxStackSize()) - existing.getCount();
            int added = Math.min(stack.getCount(), capacity);
            if (added <= 0) continue;
            existing.grow(added);
            stack.shrink(added);
            target.setChanged();
            if (stack.isEmpty()) {
                source.set(ItemStack.EMPTY);
                source.setChanged();
                source.onTake(player, original);
                callback.setReturnValue(original);
                return;
            }
        }

        boolean wasRotated = ItemInventoryService.ItemRotateHelper.isRotated(stack);
        Area area = ItemInventoryService.getArea(stack);
        Slot target = findPlayerInventoryAnchor(hotbarSlots, mainInventorySlots, area);
        if (target == null) {
            boolean rotated = ItemInventoryService.ItemRotateHelper.isRotated(stack);
            ItemInventoryService.ItemRotateHelper.setRotated(stack, !rotated);
            area = ItemInventoryService.getArea(stack);
            target = findPlayerInventoryAnchor(hotbarSlots, mainInventorySlots, area);
            if (target == null) ItemInventoryService.ItemRotateHelper.setRotated(stack, wasRotated);
        }
        if (target == null) {
            callback.setReturnValue(ItemStack.EMPTY);
            return;
        }

        if (!target.mayPlace(stack)) {
            callback.setReturnValue(ItemStack.EMPTY);
            return;
        }
        target.set(stack.copy());
        target.setChanged();
        source.set(ItemStack.EMPTY);
        source.setChanged();
        source.onTake(player, original);
        callback.setReturnValue(original);
    }

    @Unique
    private Slot findPlayerInventoryAnchor(List<Slot> hotbarSlots, List<Slot> mainInventorySlots, Area area) {
        Slot hotbarTarget = findHotbarInventoryAnchor(hotbarSlots, area);
        return hotbarTarget != null ? hotbarTarget : findMainInventoryAnchor(mainInventorySlots, area);
    }

    @Unique
    private Slot findHotbarInventoryAnchor(List<Slot> slots, Area area) {
        if (slots.size() < 9 || area.height() > 1 || area.width() > 9) return null;

        for (int column = 0; column + area.width() <= 9; column++) {
            boolean valid = true;
            for (int occupiedIndex = 0; occupiedIndex < 9 && valid; occupiedIndex++) {
                ItemStack existing = slots.get(occupiedIndex).getItem();
                if (existing.isEmpty()) continue;
                Area existingArea = ItemInventoryService.getArea(existing);
                boolean overlaps = column < occupiedIndex + existingArea.width()
                        && column + area.width() > occupiedIndex;
                if (overlaps) {
                    valid = false;
                }
            }
            if (valid) return slots.get(column);
        }
        return null;
    }

    @Unique
    private Slot findMainInventoryAnchor(List<Slot> slots, Area area) {
        if (slots.size() < 27) return null;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                if (column + area.width() > 9 || row + area.height() > 3) continue;
                boolean valid = true;
                for (int otherIndex = 0; otherIndex < 27 && valid; otherIndex++) {
                    ItemStack existing = slots.get(otherIndex).getItem();
                    if (existing.isEmpty()) continue;
                    int otherRow = otherIndex / 9;
                    int otherColumn = otherIndex % 9;
                    Area existingArea = ItemInventoryService.getArea(existing);
                    int existingRight = otherColumn + existingArea.width();
                    int existingBottom = otherRow + existingArea.height();
                    boolean overlaps = column < existingRight && column + area.width() > otherColumn
                            && row < existingBottom && row + area.height() > otherRow;
                    if (overlaps) valid = false;
                }
                if (valid) return slots.get(row * 9 + column);
            }
        }
        return null;
    }

}
