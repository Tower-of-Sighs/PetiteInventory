package com.sighs.petiteinventory.platform;

import com.sighs.petiteinventory.platform.inventory.ItemInventoryService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RotateAreaPayload(int slotIndex, boolean rotated) implements CustomPacketPayload {
    public static final Type<RotateAreaPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("petiteinventory", "rotate_area"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RotateAreaPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, RotateAreaPayload::slotIndex,
                    ByteBufCodecs.BOOL, RotateAreaPayload::rotated,
                    RotateAreaPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RotateAreaPayload msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            AbstractContainerMenu menu = player.containerMenu;
            ItemStack stack;
            int containerId;
            int slotId;

            if (msg.slotIndex == -1) {
                stack = menu.getCarried();
                containerId = -1;
                slotId = 0;
            } else if (msg.slotIndex >= 0 && msg.slotIndex < menu.slots.size()) {
                stack = menu.getSlot(msg.slotIndex).getItem();
                containerId = menu.containerId;
                slotId = msg.slotIndex;
            } else {
                return;
            }

            if (stack.isEmpty()) return;
            ItemInventoryService.ItemRotateHelper.setRotated(stack, msg.rotated);
            player.connection.send(new ClientboundContainerSetSlotPacket(
                    containerId,
                    menu.incrementStateId(),
                    slotId,
                    stack
            ));
        });
    }
}