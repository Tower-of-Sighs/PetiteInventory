package com.sighs.petiteinventory.platform;

import com.sighs.petiteinventory.platform.inventory.ContainerOverlapService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client-to-server request: run the overlap tidy for the player's current menu. */
public class TidyOverlapPayload {
    public static final ResourceLocation ID = new ResourceLocation("petiteinventory", "tidy_overlap");

    private final boolean unused;

    public TidyOverlapPayload(boolean unused) {
        this.unused = unused;
    }

    public static void encode(TidyOverlapPayload message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.unused);
    }

    public static TidyOverlapPayload decode(FriendlyByteBuf buffer) {
        return new TidyOverlapPayload(buffer.readBoolean());
    }

    public static void handle(TidyOverlapPayload message, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> {
            ServerPlayer player = contextSupplier.get().getSender();
            if (player != null) {
                ContainerOverlapService.tidy(player.containerMenu);
            }
        });
        contextSupplier.get().setPacketHandled(true);
    }
}
