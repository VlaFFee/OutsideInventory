package com.example.hubui.network;

import com.example.hubui.HubApi;
import com.example.hubui.HubData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Клиент -> сервер: запись журнала прочитана. */
public record ReadEntryPacket(int id) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(id);
    }

    public static ReadEntryPacket decode(FriendlyByteBuf buf) {
        return new ReadEntryPacket(buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            var p = c.getSender();
            if (p != null) {
                HubData.markRead(p, id);
                HubApi.sync(p);
            }
        });
        c.setPacketHandled(true);
    }
}
