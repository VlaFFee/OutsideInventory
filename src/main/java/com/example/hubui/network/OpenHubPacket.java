package com.example.hubui.network;

import com.example.hubui.HubApi;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Клиент -> сервер: игрок нажал клавишу инвентаря. */
public record OpenHubPacket() {
    public void encode(FriendlyByteBuf buf) {}

    public static OpenHubPacket decode(FriendlyByteBuf buf) {
        return new OpenHubPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            var p = c.getSender();
            if (p != null && !p.isCreative()) HubApi.openHub(p);
        });
        c.setPacketHandled(true);
    }
}
