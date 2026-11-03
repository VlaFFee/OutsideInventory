package com.example.hubui.network;

import com.example.hubui.HubMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Клиент -> сервер: какая вкладка открыта (нужно для правил shift-клика). */
public record SetTabPacket(int tab) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(tab);
    }

    public static SetTabPacket decode(FriendlyByteBuf buf) {
        return new SetTabPacket(buf.readByte());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            var p = c.getSender();
            if (p != null && p.containerMenu instanceof HubMenu m) m.setTab(tab);
        });
        c.setPacketHandled(true);
    }
}
