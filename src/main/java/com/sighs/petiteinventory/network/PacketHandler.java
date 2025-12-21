package com.sighs.petiteinventory.network;

import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            RotateAreaPayload.ID,
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, RotateAreaPayload.class,
                RotateAreaPayload::encode,
                RotateAreaPayload::decode,
                RotateAreaPayload::handle);

        // 注册新数据包
        CHANNEL.registerMessage(id++, PlaceItemPayload.class,
                PlaceItemPayload::encode,
                PlaceItemPayload::decode,
                PlaceItemPayload::handle);

        CHANNEL.registerMessage(id++, DropItemPayload.class,
                DropItemPayload::encode,
                DropItemPayload::decode,
                DropItemPayload::handle);
    }
}