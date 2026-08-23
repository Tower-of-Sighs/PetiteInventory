package com.sighs.petiteinventory.platform.spi;

import com.sighs.petiteinventory.core.ItemSize;
import com.sighs.petiteinventory.inventory.Area;
import com.sighs.petiteinventory.platform.inventory.InventorySlotService;
import com.sighs.petiteinventory.platform.inventory.ItemInventoryService;
import com.sighs.petiteinventory.spi.InventoryPort;
import com.sighs.petiteinventory.spi.StackPort;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Forge 1.20.1 translation layer for the common admission policy. */
public final class ForgeInventoryPort implements InventoryPort<Player, ItemStack> {
    private static final ForgeStackPort STACKS = new ForgeStackPort();

    public static StackPort<ItemStack> stackPort() { return STACKS; }

    @Override
    public StackPort<ItemStack> stacks() { return STACKS; }

    @Override
    public int size(Player player) { return player.getInventory().getContainerSize(); }

    @Override
    public ItemStack get(Player player, int slot) { return player.getInventory().getItem(slot); }

    @Override
    public void set(Player player, int slot, ItemStack stack) { player.getInventory().setItem(slot, stack); }

    @Override
    public int maxStackSize(Player player) { return player.getInventory().getMaxStackSize(); }

    @Override
    public int findSlotFor(Player player, ItemSize footprint) {
        return InventorySlotService.findSlotIndexForArea(player,
                new Area(footprint.width(), footprint.height(), ItemStack.EMPTY));
    }

    @Override
    public boolean placeFallback(Player player, ItemStack stack) {
        Inventory inventory = player.getInventory();
        for (int slot = 9; slot < 36; slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                inventory.setItem(slot, stack.copy());
                return true;
            }
        }
        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                inventory.setItem(slot, stack.copy());
                return true;
            }
        }
        return false;
    }

    @Override
    public void drop(Player player, ItemStack stack) { player.drop(stack, false); }

    private static final class ForgeStackPort implements StackPort<ItemStack> {
        @Override public boolean isEmpty(ItemStack stack) { return stack == null || stack.isEmpty(); }
        @Override public boolean isStackable(ItemStack stack) { return stack != null && stack.isStackable(); }
        @Override public ItemStack copy(ItemStack stack) { return stack.copy(); }
        @Override public int count(ItemStack stack) { return stack.getCount(); }
        @Override public void setCount(ItemStack stack, int count) { stack.setCount(count); }
        @Override public void shrink(ItemStack stack, int amount) { stack.shrink(amount); }
        @Override public void grow(ItemStack stack, int amount) { stack.grow(amount); }
        @Override public int maxStackSize(ItemStack stack) { return stack.getMaxStackSize(); }
        @Override public boolean sameItemIgnoringRotation(ItemStack first, ItemStack second) {
            return ItemInventoryService.isSameItemIgnoreRotate(first, second);
        }
        @Override public boolean isRotated(ItemStack stack) {
            return ItemInventoryService.ItemRotateHelper.isRotated(stack);
        }
        @Override public void setRotated(ItemStack stack, boolean rotated) {
            ItemInventoryService.ItemRotateHelper.setRotated(stack, rotated);
        }
        @Override public ItemSize footprint(ItemStack stack) {
            return ItemInventoryService.getArea(stack).size();
        }
    }
}
