package com.example.hubui.network;

import com.example.hubui.HubUiMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(HubUiMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, OpenHubPacket.class, OpenHubPacket::encode, OpenHubPacket::decode, OpenHubPacket::handle);
        CHANNEL.registerMessage(id++, SetTabPacket.class, SetTabPacket::encode, SetTabPacket::decode, SetTabPacket::handle);
        CHANNEL.registerMessage(id++, SortStoragePacket.class, SortStoragePacket::encode, SortStoragePacket::decode, SortStoragePacket::handle);
        CHANNEL.registerMessage(id++, CraftPacket.class, CraftPacket::encode, CraftPacket::decode, CraftPacket::handle);
        CHANNEL.registerMessage(id++, ReadEntryPacket.class, ReadEntryPacket::encode, ReadEntryPacket::decode, ReadEntryPacket::handle);
        CHANNEL.registerMessage(id++, SyncPacket.class, SyncPacket::encode, SyncPacket::decode, SyncPacket::handle);
    }

    private NetworkHandler() {}
}
