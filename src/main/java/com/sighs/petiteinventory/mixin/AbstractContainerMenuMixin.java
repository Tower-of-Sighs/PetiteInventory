package com.sighs.petiteinventory.mixin;

import com.sighs.petiteinventory.api.IAbstractContainerMenu;
import com.sighs.petiteinventory.init.ContainerGrid;
import com.sighs.petiteinventory.utils.ClientUtils;
import com.sighs.petiteinventory.utils.SlotUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin implements IAbstractContainerMenu {
    @Shadow @Final public NonNullList<Slot> slots;

    @Unique
    private Player player;

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void qq(int slot, int p_150401_, ClickType type, Player p_150403_, CallbackInfo ci) {
        if (slot < 0 || slot > slots.size()) return;
        Slot clickedSlot = slots.get(slot);
        ContainerGrid grid = SlotUtils.getContainerGrid((AbstractContainerMenu) (Object) this);
        if (SlotUtils.hasEmptyHotbarSlot(player)) return;
        if ((Object) this instanceof InventoryMenu) {
            if (type == ClickType.QUICK_MOVE && grid.getCell(clickedSlot) == null) {
                SlotUtils.addInventoryItem(getPlayer(), clickedSlot.getItem());
                ci.cancel();
            }
        } else {
            if (type == ClickType.QUICK_MOVE && !(clickedSlot.container instanceof Inventory)) {
                SlotUtils.addInventoryItem(player, clickedSlot.getItem());
                ci.cancel();
            }
        }
    }
}
