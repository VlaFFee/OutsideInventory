package com.example.hubui;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/** Запись журнала. Пишет её сюжетный персонаж (автор), игрок только читает. */
public record JournalEntry(int id, String author, String title, String text, int day, boolean unread) {

    public CompoundTag toTag() {
        CompoundTag t = new CompoundTag();
        t.putInt("Id", id);
        t.putString("Author", author);
        t.putString("Title", title);
        t.putString("Text", text);
        t.putInt("Day", day);
        t.putBoolean("Unread", unread);
        return t;
    }

    public static JournalEntry fromTag(CompoundTag t) {
        return new JournalEntry(t.getInt("Id"), t.getString("Author"), t.getString("Title"),
                t.getString("Text"), t.getInt("Day"), t.getBoolean("Unread"));
    }

    public JournalEntry asRead() {
        return new JournalEntry(id, author, title, text, day, false);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(id);
        buf.writeUtf(author, 100);
        buf.writeUtf(title, 200);
        buf.writeUtf(text, 6000);
        buf.writeVarInt(day);
        buf.writeBoolean(unread);
    }

    public static JournalEntry read(FriendlyByteBuf buf) {
        return new JournalEntry(buf.readVarInt(), buf.readUtf(100), buf.readUtf(200), buf.readUtf(6000),
                buf.readVarInt(), buf.readBoolean());
    }
}
