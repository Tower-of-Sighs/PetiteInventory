package com.sighs.petiteinventory.network;

import com.sighs.petiteinventory.utils.ItemUtils;  // ← 新增这行
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlaceItemPayload {
    public static final ResourceLocation ID = new ResourceLocation("petiteinventory", "place_item");

    private final int slotIndex;
    private final ItemStack itemStack;

    public PlaceItemPayload(int slotIndex, ItemStack itemStack) {
        this.slotIndex = slotIndex;
        this.itemStack = itemStack;
    }

    public static void encode(PlaceItemPayload msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.slotIndex);
        buf.writeItem(msg.itemStack);
    }

    public static PlaceItemPayload decode(FriendlyByteBuf buf) {
        return new PlaceItemPayload(buf.readInt(), buf.readItem());
    }

    public static void handle(PlaceItemPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            /* 1. 放置到背包指定槽位 */
            if (msg.slotIndex >= 0 && msg.slotIndex < player.getInventory().getContainerSize()) {
                ItemStack toPlace = msg.itemStack.copy();

                /* 快捷栏强制 1×1 */
                if (msg.slotIndex <= 8) {
                    ItemUtils.ItemRotateHelper.setRotated(toPlace, false);
                }

                player.getInventory().setItem(msg.slotIndex, toPlace);

                /* 2. 清空鼠标（安全写法：containerId = -1） */
                player.connection.send(new ClientboundContainerSetSlotPacket(
                        -1, 0, 0, ItemStack.EMPTY));

                /* 3. 刷新客户端背包槽位 */
                player.connection.send(new ClientboundContainerSetSlotPacket(
                        0, 0, msg.slotIndex, toPlace));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}