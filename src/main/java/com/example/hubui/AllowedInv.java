package com.example.hubui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Ограниченный инвентарь: используются только ячейки 0..8 ванильного инвентаря.
 * 0..4 - хотбар (виден на экране), 5..8 - боковые карманы (только в окне инвентаря).
 * Ячейки 9..35 всегда пусты: всё, что туда попало, переносится в 0..8 или выпадает на землю.
 */
public final class AllowedInv {
    public static final int HOTBAR = 5, SIDE = 4, SLOTS = 9;

    /** Сколько предметов st поместится в разрешённые ячейки. */
    public static int room(Inventory inv, ItemStack st) {
        int room = 0;
        int max = Math.min(st.getMaxStackSize(), inv.getMaxStackSize());
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = inv.items.get(i);
            if (s.isEmpty()) room += max;
            else if (ItemStack.isSameItemSameTags(s, st)) room += Math.max(0, Math.min(s.getMaxStackSize(), inv.getMaxStackSize()) - s.getCount());
        }
        return room;
    }

    /** Кладёт предмет в ячейки 0..8 (сначала доукладывает в существующие стопки). Возвращает остаток. */
    public static ItemStack insert(Inventory inv, ItemStack st) {
        ItemStack rest = st.copy();
        int max = inv.getMaxStackSize();
        for (int i = 0; i < SLOTS && !rest.isEmpty(); i++) {
            ItemStack s = inv.items.get(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameTags(s, rest)) {
                int can = Math.min(s.getMaxStackSize(), max) - s.getCount();
                int move = Math.min(can, rest.getCount());
                if (move > 0) {
                    s.grow(move);
                    s.setPopTime(5);
                    rest.shrink(move);
                }
            }
        }
        for (int i = 0; i < SLOTS && !rest.isEmpty(); i++) {
            if (inv.items.get(i).isEmpty()) {
                int move = Math.min(rest.getMaxStackSize(), Math.min(rest.getCount(), max));
                ItemStack put = rest.copy();
                put.setCount(move);
                put.setPopTime(5);
                inv.items.set(i, put);
                rest.shrink(move);
            }
        }
        return rest;
    }

    private AllowedInv() {}
}
