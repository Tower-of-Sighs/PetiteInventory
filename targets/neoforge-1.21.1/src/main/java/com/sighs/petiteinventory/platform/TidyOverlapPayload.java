package com.sighs.petiteinventory.platform;

import com.sighs.petiteinventory.platform.inventory.ContainerOverlapService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client-to-server request: run the overlap tidy for the player's current menu. */
public record TidyOverlapPayload(boolean unused) implements CustomPacketPayload {
    public static final Type<TidyOverlapPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("petiteinventory", "tidy_overlap"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TidyOverlapPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, TidyOverlapPayload::unused, TidyOverlapPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TidyOverlapPayload message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player != null) {
                ContainerOverlapService.tidy(player.containerMenu);
            }
        });
    }
}
