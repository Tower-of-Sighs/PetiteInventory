package com.sighs.petiteinventory.platform;

import com.sighs.petiteinventory.platform.inventory.InventoryAdmissionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Platform transport only; placement policy belongs to the defense module. */
public class PlaceItemPayload {
    public static final ResourceLocation ID = new ResourceLocation("petiteinventory", "place_item");

    private final int slotIndex;
    private final ItemStack itemStack;

    public PlaceItemPayload(int slotIndex, ItemStack itemStack) {
        this.slotIndex = slotIndex;
        this.itemStack = itemStack;
    }

    public static void encode(PlaceItemPayload message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.slotIndex);
        buffer.writeItem(message.itemStack);
    }

    public static PlaceItemPayload decode(FriendlyByteBuf buffer) {
        return new PlaceItemPayload(buffer.readInt(), buffer.readItem());
    }

    public static void handle(PlaceItemPayload message, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> {
            ServerPlayer player = contextSupplier.get().getSender();
            if (player == null || message.slotIndex < 0 || message.slotIndex >= player.getInventory().getContainerSize()) {
                return;
            }

            ItemStack normalized = InventoryAdmissionService.normalizeForSlot(message.slotIndex, message.itemStack);
            player.getInventory().setItem(message.slotIndex, normalized);
            player.connection.send(new ClientboundContainerSetSlotPacket(-1, 0, 0, ItemStack.EMPTY));
            player.connection.send(new ClientboundContainerSetSlotPacket(0, 0, message.slotIndex, normalized));
        });
        contextSupplier.get().setPacketHandled(true);
    }
}
