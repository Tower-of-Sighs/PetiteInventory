package com.sighs.petiteinventory.platform;

import com.sighs.petiteinventory.compat.SophisticatedBackpacksCompat;
import com.sighs.petiteinventory.inventory.Area;
import com.sighs.petiteinventory.platform.inventory.ItemInventoryService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.Set;

/**
 * Server-side commit for a footprint selected from Sophisticated's client-only
 * slot coordinates. Every covered slot is verified before the anchor changes.
 */
public record SophisticatedQuickMovePayload(int sourceSlot, int anchorSlot, int[] footprintSlots, boolean rotated) implements CustomPacketPayload {
    public static final Type<SophisticatedQuickMovePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("petiteinventory", "sophisticated_quick_move"));
    private static final StreamCodec<RegistryFriendlyByteBuf, int[]> INT_ARRAY_CODEC = StreamCodec.of(
            (buffer, array) -> {
                ByteBufCodecs.writeCount(buffer, array.length, 1024);
                for (int value : array) {
                    ByteBufCodecs.VAR_INT.encode(buffer, value);
                }
            },
            buffer -> {
                int[] array = new int[ByteBufCodecs.readCount(buffer, 1024)];
                for (int i = 0; i < array.length; i++) {
                    array[i] = ByteBufCodecs.VAR_INT.decode(buffer);
                }
                return array;
            }
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SophisticatedQuickMovePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SophisticatedQuickMovePayload::sourceSlot,
                    ByteBufCodecs.VAR_INT, SophisticatedQuickMovePayload::anchorSlot,
                    INT_ARRAY_CODEC, SophisticatedQuickMovePayload::footprintSlots,
                    ByteBufCodecs.BOOL, SophisticatedQuickMovePayload::rotated,
                    SophisticatedQuickMovePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SophisticatedQuickMovePayload message, IPayloadContext context) {
        context.enqueueWork(() -> commit((ServerPlayer) context.player(), message));
    }

    private static void commit(ServerPlayer player, SophisticatedQuickMovePayload message) {
        if (player == null) return;
        AbstractContainerMenu menu = player.containerMenu;
        if (!SophisticatedBackpacksCompat.isBackpackMenu(menu)
                || message.sourceSlot < 0 || message.sourceSlot >= menu.slots.size()
                || message.anchorSlot < 0 || message.anchorSlot >= menu.slots.size()) return;

        Slot source = menu.getSlot(message.sourceSlot);
        Slot anchor = menu.getSlot(message.anchorSlot);
        if (!(source.container instanceof Inventory) || source.getItem().isEmpty()) {
            return;
        }

        ItemStack moved = source.getItem().copy();
        ItemInventoryService.ItemRotateHelper.setRotated(moved, message.rotated);
        Area area = ItemInventoryService.getArea(moved);
        if (message.footprintSlots.length != area.width() * area.height()) {
            return;
        }

        Set<Integer> footprint = new HashSet<>();
        for (int index : message.footprintSlots) {
            if (index < 0 || index >= menu.slots.size() || !footprint.add(index)) {
                return;
            }
            Slot slot = menu.getSlot(index);
            if (!SophisticatedBackpacksCompat.isStorageSlot(menu, index) || slot.hasItem() || !slot.mayPlace(moved)) {
                return;
            }
        }
        if (!footprint.contains(message.anchorSlot) || !anchor.mayPlace(moved)) return;

        anchor.set(moved);
        anchor.setChanged();
        source.set(ItemStack.EMPTY);
        source.setChanged();
        source.onTake(player, moved);
        menu.broadcastChanges();
    }
}