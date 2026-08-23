package com.sighs.petiteinventory.platform;

import com.sighs.petiteinventory.platform.inventory.ItemInventoryService;
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

    private final int slotIndex;   // -1 鐞涖劎銇氭Η鐘崇垼娑撳﹦娈戦悧鈺佹惂
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

            // 1. 绾喖鐣鹃悧鈺佹惂閸滃苯褰傞崠鍛棘閺?
            int containerId;
            int slotId;

            if (msg.slotIndex == -1) {
                // 姒х姵鐖ｆ稉濠勬畱閻椻晛鎼?閳?韫囧懘銆忛悽?containerId = -1
                stack = menu.getCarried();
                containerId = -1;  // 閳?閸忔娊鏁敍浣姐€冪粈?閹煎搫鐢悧鈺佹惂"
                slotId = 0;        // 閳?鏉╂瑤閲滈崐闂寸窗鐞氼偄鎷烽悾?
            } else if (msg.slotIndex >= 0 && msg.slotIndex < menu.slots.size()) {
                // 鐎圭懓娅掗崘鍛畱閻椻晛鎼?
                stack = menu.getSlot(msg.slotIndex).getItem();
                containerId = menu.containerId;
                slotId = msg.slotIndex;
            } else {
                return; // 闂堢偞纭剁槐銏犵穿
            }

            if (stack.isEmpty()) return;

            // 2. 閸愭瑥鍙嗛弮瀣祮閻樿埖鈧?
            ItemInventoryService.ItemRotateHelper.setRotated(stack, msg.rotated);

            // 3. 閸欐垿鈧礁鎮撳銉ュ瘶閿涘牆鍙ч柨顔芥Ц containerId 韫囧懘銆忓锝団€橀敍?
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
