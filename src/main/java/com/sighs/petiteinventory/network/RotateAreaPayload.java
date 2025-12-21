package com.sighs.petiteinventory.network;

import com.sighs.petiteinventory.utils.ItemUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RotateAreaPayload {
    public static final ResourceLocation ID = new ResourceLocation("petiteinventory", "rotate_area");

    private final int slotIndex;   // -1 表示鼠标上的物品
    private final boolean rotated;

    public RotateAreaPayload(int slotIndex, boolean rotated) {
        this.slotIndex = slotIndex;
        this.rotated   = rotated;
    }

    public static void encode(RotateAreaPayload msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.slotIndex);
        buf.writeBoolean(msg.rotated);
    }

    public static RotateAreaPayload decode(FriendlyByteBuf buf) {
        return new RotateAreaPayload(buf.readInt(), buf.readBoolean());
    }

    public static void handle(RotateAreaPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            AbstractContainerMenu menu = player.containerMenu;
            ItemStack stack;

            // 1. 确定物品和发包参数
            int containerId;
            int slotId;

            if (msg.slotIndex == -1) {
                // 鼠标上的物品 → 必须用 containerId = -1
                stack = menu.getCarried();
                containerId = -1;  // ← 关键！表示"携带物品"
                slotId = 0;        // ← 这个值会被忽略
            } else if (msg.slotIndex >= 0 && msg.slotIndex < menu.slots.size()) {
                // 容器内的物品
                stack = menu.getSlot(msg.slotIndex).getItem();
                containerId = menu.containerId;
                slotId = msg.slotIndex;
            } else {
                return; // 非法索引
            }

            if (stack.isEmpty()) return;

            // 2. 写入旋转状态
            ItemUtils.ItemRotateHelper.setRotated(stack, msg.rotated);

            // 3. 发送同步包（关键是 containerId 必须正确）
            player.connection.send(new ClientboundContainerSetSlotPacket(
                    containerId,
                    menu.incrementStateId(),
                    slotId,
                    stack
            ));
        });
        ctx.get().setPacketHandled(true);
    }
}