package com.example.hubui;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Все данные мода лежат в постоянных данных игрока (сохраняются вместе с ним и переживают смерть). */
public final class HubData {
    public static final String ROOT = "hubui";
    public static final int DEFAULT_ROWS = 6, MAX_ROWS = 7, MAX_ENTRIES = 100;

    public static CompoundTag root(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(ROOT, Tag.TAG_COMPOUND)) pd.put(ROOT, new CompoundTag());
        return pd.getCompound(ROOT);
    }

    // ---------- валюта ----------
    public static long getBalance(Player p) {
        return root(p).getLong("Balance");
    }

    public static void setBalance(Player p, long v) {
        root(p).putLong("Balance", Math.max(0L, Math.min(v, 1_000_000_000_000L)));
    }

    // ---------- хранилище ----------
    public static int getRows(Player p) {
        CompoundTag r = root(p);
        return r.contains("Rows") ? Mth.clamp(r.getInt("Rows"), 0, MAX_ROWS) : DEFAULT_ROWS;
    }

    public static void setRows(Player p, int v) {
        root(p).putInt("Rows", Mth.clamp(v, 0, MAX_ROWS));
    }

    public static void loadStorage(Player p, Container c) {
        NonNullList<ItemStack> l = NonNullList.withSize(c.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(root(p).getCompound("Storage"), l);
        for (int i = 0; i < l.size(); i++) c.setItem(i, l.get(i));
    }

    public static void saveStorage(Player p, Container c) {
        NonNullList<ItemStack> l = NonNullList.withSize(c.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < l.size(); i++) l.set(i, c.getItem(i));
        CompoundTag t = new CompoundTag();
        ContainerHelper.saveAllItems(t, l, true);
        root(p).put("Storage", t);
    }

    // ---------- журнал ----------
    public static List<JournalEntry> getEntries(Player p) {
        ListTag list = root(p).getList("Journal", Tag.TAG_COMPOUND);
        List<JournalEntry> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) out.add(JournalEntry.fromTag(list.getCompound(i)));
        return out;
    }

    private static void putEntries(Player p, List<JournalEntry> entries) {
        ListTag list = new ListTag();
        for (JournalEntry e : entries) list.add(e.toTag());
        root(p).put("Journal", list);
    }

    public static void addEntry(Player p, String author, String title, String text, int day) {
        CompoundTag r = root(p);
        int id = r.getInt("NextId") + 1;
        r.putInt("NextId", id);
        List<JournalEntry> list = getEntries(p);
        list.add(new JournalEntry(id, cut(author, 60), cut(title, 120), cut(text, 3000), day, true));
        while (list.size() > MAX_ENTRIES) list.remove(0);
        putEntries(p, list);
    }

    public static void markRead(Player p, int id) {
        List<JournalEntry> list = getEntries(p);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id() == id) list.set(i, list.get(i).asRead());
        }
        putEntries(p, list);
    }

    public static void clearJournal(Player p) {
        root(p).put("Journal", new ListTag());
    }

    private static String cut(String s, int n) {
        return s.length() > n ? s.substring(0, n) : s;
    }

    private HubData() {}
}
