package com.sighs.petiteinventory.platform;

import com.sighs.petiteinventory.client.ClientEditMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-to-client synchronization for the editor toggle. */
public record EditModePayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<EditModePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("petiteinventory", "edit_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EditModePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, EditModePayload::enabled, EditModePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EditModePayload message, IPayloadContext context) {
        context.enqueueWork(() -> ClientEditMode.setEnabled(message.enabled));
    }
}