package com.example.hubui.network;

import com.example.hubui.JournalEntry;
import com.example.hubui.client.ClientHub;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Сервер -> клиент: валюта, открытые ряды хранилища, режим ограниченного инвентаря и журнал. */
public record SyncPacket(long balance, int rows, boolean limited, List<JournalEntry> entries) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(balance);
        buf.writeVarInt(rows);
        buf.writeBoolean(limited);
        buf.writeVarInt(entries.size());
        for (JournalEntry e : entries) e.write(buf);
    }

    public static SyncPacket decode(FriendlyByteBuf buf) {
        long b = buf.readLong();
        int rows = buf.readVarInt();
        boolean limited = buf.readBoolean();
        int n = buf.readVarInt();
        List<JournalEntry> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add(JournalEntry.read(buf));
        return new SyncPacket(b, rows, limited, l);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHub.apply(this)));
        c.setPacketHandled(true);
    }
}
