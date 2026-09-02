package com.sighs.petiteinventory.platform;

import com.sighs.petiteinventory.Petiteinventory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Petiteinventory.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class PayloadRegistrationEvent {
    private PayloadRegistrationEvent() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(RotateAreaPayload.TYPE, RotateAreaPayload.STREAM_CODEC, RotateAreaPayload::handle);
        registrar.playToServer(PlaceItemPayload.TYPE, PlaceItemPayload.STREAM_CODEC, PlaceItemPayload::handle);
        registrar.playToServer(DropItemPayload.TYPE, DropItemPayload.STREAM_CODEC, DropItemPayload::handle);
        registrar.playToServer(SophisticatedQuickMovePayload.TYPE, SophisticatedQuickMovePayload.STREAM_CODEC, SophisticatedQuickMovePayload::handle);

        registrar.playToClient(EditModePayload.TYPE, EditModePayload.STREAM_CODEC, EditModePayload::handle);
        registrar.playToClient(ScreenLayoutModePayload.TYPE, ScreenLayoutModePayload.STREAM_CODEC, ScreenLayoutModePayload::handle);
    }
}