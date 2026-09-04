package com.sighs.petiteinventory.platform;

import com.sighs.petiteinventory.platform.inventory.InventoryAdmissionService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Platform transport only; placement policy belongs to the defense module. */
public record PlaceItemPayload(int slotIndex, ItemStack itemStack) implements CustomPacketPayload {
    public static final Type<PlaceItemPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("petiteinventory", "place_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlaceItemPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PlaceItemPayload::slotIndex,
                    ItemStack.STREAM_CODEC, PlaceItemPayload::itemStack,
                    PlaceItemPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlaceItemPayload message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null || message.slotIndex < 0 || message.slotIndex >= player.getInventory().getContainerSize()) {
                return;
            }

            ItemStack normalized = InventoryAdmissionService.normalizeForSlot(message.slotIndex, message.itemStack);
            player.getInventory().setItem(message.slotIndex, normalized);
            player.connection.send(new ClientboundContainerSetSlotPacket(-1, 0, 0, ItemStack.EMPTY));
            player.connection.send(new ClientboundContainerSetSlotPacket(0, 0, message.slotIndex, normalized));
        });
    }
}