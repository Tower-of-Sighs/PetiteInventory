package com.sighs.petiteinventory.platform;

import com.sighs.petiteinventory.client.ClientInventoryContext;
import com.sighs.petiteinventory.client.ScreenLayoutSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Synchronizes the global default used by the per-screen layout overrides. */
public record ScreenLayoutModePayload(boolean defaultEnabled) implements CustomPacketPayload {
    public static final Type<ScreenLayoutModePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("petiteinventory", "screen_layout_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScreenLayoutModePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, ScreenLayoutModePayload::defaultEnabled, ScreenLayoutModePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ScreenLayoutModePayload message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ScreenLayoutSettings.setDefaultEnabled(message.defaultEnabled);
            ClientInventoryContext.invalidate();
        });
    }
}