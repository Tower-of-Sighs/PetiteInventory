package com.sighs.petiteinventory.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DropItemPayload {
    public static final ResourceLocation ID = new ResourceLocation("petiteinventory", "drop_item");

    private final ItemStack itemStack;

    public DropItemPayload(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public static void encode(DropItemPayload msg, FriendlyByteBuf buf) {
        buf.writeItem(msg.itemStack);
    }

    public static DropItemPayload decode(FriendlyByteBuf buf) {
        return new DropItemPayload(buf.readItem());
    }

    public static void handle(DropItemPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            /* 1. 生成掉落物 */
            ItemEntity itemEntity = new ItemEntity(
                    player.level(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    msg.itemStack.copy());
            itemEntity.setDefaultPickUpDelay();
            player.level().addFreshEntity(itemEntity);

            /* 2. 清空鼠标（安全写法：containerId = -1） */
            player.connection.send(new ClientboundContainerSetSlotPacket(
                    -1, 0, 0, ItemStack.EMPTY));
        });
        ctx.get().setPacketHandled(true);
    }
}