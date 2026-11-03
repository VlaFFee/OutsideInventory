package com.example.hubui.client;

import com.example.hubui.JournalEntry;
import com.example.hubui.ModRegistry;
import com.example.hubui.network.SyncPacket;
import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/** Клиентская копия данных игрока: валюта, журнал. */
@OnlyIn(Dist.CLIENT)
public final class ClientHub {
    public static long balance;
    public static int rows = 6;
    public static boolean limited;
    public static List<JournalEntry> entries = List.of();
    public static long changedAt = -100000L;
    public static long lastDelta;
    private static boolean first = true;
    private static int maxId;

    public static void apply(SyncPacket p) {
        if (!first && p.balance() != balance) {
            lastDelta = p.balance() - balance;
            changedAt = Util.getMillis();
            UiSound.play(ModRegistry.UI_COIN, lastDelta > 0 ? 1f : 0.7f);
        }
        int newMax = 0;
        for (JournalEntry e : p.entries()) newMax = Math.max(newMax, e.id());
        if (!first && newMax > maxId) UiSound.play(ModRegistry.UI_NOTE, 1f);
        balance = p.balance();
        rows = p.rows();
        limited = p.limited();
        entries = p.entries();
        maxId = newMax;
        first = false;
    }

    public static boolean hasUnread() {
        for (JournalEntry e : entries) if (e.unread()) return true;
        return false;
    }

    public static void markReadLocal(int id) {
        List<JournalEntry> l = new ArrayList<>(entries);
        for (int i = 0; i < l.size(); i++) if (l.get(i).id() == id) l.set(i, l.get(i).asRead());
        entries = l;
    }

    public static void reset() {
        first = true;
        balance = 0;
        limited = false;
        entries = List.of();
        maxId = 0;
    }

    private ClientHub() {}
}
