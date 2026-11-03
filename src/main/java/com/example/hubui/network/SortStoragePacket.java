package com.example.hubui.network;

import com.example.hubui.HubMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SortStoragePacket() {
    public void encode(FriendlyByteBuf buf) {}

    public static SortStoragePacket decode(FriendlyByteBuf buf) {
        return new SortStoragePacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            var p = c.getSender();
            if (p != null && p.containerMenu instanceof HubMenu m) m.sortStorage();
        });
        c.setPacketHandled(true);
    }
}
