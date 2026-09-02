package com.sighs.petiteinventory.platform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DropItemPayload(ItemStack itemStack) implements CustomPacketPayload {
    public static final Type<DropItemPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("petiteinventory", "drop_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DropItemPayload> STREAM_CODEC =
            StreamCodec.composite(ItemStack.STREAM_CODEC, DropItemPayload::itemStack, DropItemPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DropItemPayload message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            ItemEntity itemEntity = new ItemEntity(
                    player.level(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    message.itemStack.copy());
            itemEntity.setDefaultPickUpDelay();
            player.level().addFreshEntity(itemEntity);

            player.connection.send(new ClientboundContainerSetSlotPacket(
                    -1, 0, 0, ItemStack.EMPTY));
        });
    }
}