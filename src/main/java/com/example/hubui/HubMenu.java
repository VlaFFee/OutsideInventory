package com.example.hubui;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Единое меню хаба. Слоты разных вкладок существуют одновременно, но "активны" только слоты текущей вкладки.
 * Индексы: 0-4 хотбар, 5-8 боковые карманы, 9-12 броня (голова..ботинки), 13 левая рука, 14.. хранилище.
 */
public class HubMenu extends AbstractContainerMenu {
    public static final int TAB_INV = 0, TAB_STORAGE = 1, TAB_CRAFT = 2, TAB_JOURNAL = 3;
    public static final int COLS = 6, STORAGE_SIZE = 42;
    public static final int HOTBAR_COUNT = 5, SIDE_START = 5, INV_END = 9, ARMOR_START = 9, OFFHAND = 13, STORAGE_START = 14;

    // раскладка (координаты предмета относительно панели)
    public static final int ROW_Y = 178, HOT_X = 102, SIDE_L_X = 60, SIDE_R_X = 204, STEP = 20;
    public static final int ARMOR_X = 78, ARMOR_Y = 34, ARMOR_STEP = 26;
    public static final int OFF_X = 206, OFF_Y = 112;
    public static final int ST_X = 15, ST_Y = 35, ST_STEP = 18;

    private static final int M_INV = 1, M_STORAGE = 2, M_ROW = 1 | 2 | 4;

    public int tab;
    public final SimpleContainer storage;
    public final int unlockedRows;
    private final Player owner;

    public HubMenu(int id, Inventory inv, SimpleContainer storage, int unlockedRows) {
        super(ModRegistry.HUB_MENU.get(), id);
        this.owner = inv.player;
        this.storage = storage;
        this.unlockedRows = Mth.clamp(unlockedRows, 0, 7);

        for (int i = 0; i < HOTBAR_COUNT; i++) addSlot(new HubSlot(this, inv, i, HOT_X + i * STEP, ROW_Y, M_ROW));
        addSlot(new HubSlot(this, inv, 5, SIDE_L_X, ROW_Y, M_ROW));
        addSlot(new HubSlot(this, inv, 6, SIDE_L_X + STEP, ROW_Y, M_ROW));
        addSlot(new HubSlot(this, inv, 7, SIDE_R_X, ROW_Y, M_ROW));
        addSlot(new HubSlot(this, inv, 8, SIDE_R_X + STEP, ROW_Y, M_ROW));

        EquipmentSlot[] order = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++)
            addSlot(new ArmorSlot(this, inv, 39 - i, ARMOR_X, ARMOR_Y + i * ARMOR_STEP, order[i], owner));
        addSlot(new OffhandSlot(this, inv, 40, OFF_X, OFF_Y));

        for (int i = 0; i < STORAGE_SIZE; i++) {
            int row = i / COLS, col = i % COLS;
            addSlot(new StorageSlot(this, storage, i, ST_X + col * ST_STEP, ST_Y + row * ST_STEP, row < this.unlockedRows));
        }
    }

    public static HubMenu clientCreate(int id, Inventory inv, FriendlyByteBuf buf) {
        return new HubMenu(id, inv, new SimpleContainer(STORAGE_SIZE), buf.readInt());
    }

    // ---------------- слоты ----------------
    public static class HubSlot extends Slot {
        final HubMenu menu;
        final int mask;

        HubSlot(HubMenu menu, Container c, int index, int x, int y, int mask) {
            super(c, index, x, y);
            this.menu = menu;
            this.mask = mask;
        }

        @Override
        public boolean isActive() {
            return (mask & (1 << menu.tab)) != 0;
        }
    }

    public static class ArmorSlot extends HubSlot {
        private final EquipmentSlot type;
        private final Player owner;

        ArmorSlot(HubMenu menu, Container c, int index, int x, int y, EquipmentSlot type, Player owner) {
            super(menu, c, index, x, y, M_INV);
            this.type = type;
            this.owner = owner;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.canEquip(type, owner);
        }

        @Override
        public boolean mayPickup(Player player) {
            ItemStack s = getItem();
            return (s.isEmpty() || player.isCreative() || !EnchantmentHelper.hasBindingCurse(s)) && super.mayPickup(player);
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            ResourceLocation icon = switch (type) {
                case HEAD -> InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;
                case CHEST -> InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;
                case LEGS -> InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;
                default -> InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;
            };
            return Pair.of(InventoryMenu.BLOCK_ATLAS, icon);
        }
    }

    public static class OffhandSlot extends HubSlot {
        OffhandSlot(HubMenu menu, Container c, int index, int x, int y) {
            super(menu, c, index, x, y, M_INV);
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
        }
    }

    public static class StorageSlot extends HubSlot {
        private final boolean unlocked;

        StorageSlot(HubMenu menu, Container c, int index, int x, int y, boolean unlocked) {
            super(menu, c, index, x, y, M_STORAGE);
            this.unlocked = unlocked;
        }

        public boolean isUnlocked() {
            return unlocked;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return unlocked;
        }

        @Override
        public boolean mayPickup(Player player) {
            return unlocked;
        }
    }

    // ---------------- логика ----------------
    public void setTab(int t) {
        this.tab = Mth.clamp(t, 0, 3);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int idx) {
        Slot slot = slots.get(idx);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (idx >= STORAGE_START) {
            if (!moveItemStackTo(stack, 0, INV_END, true)) return ItemStack.EMPTY;
        } else if (idx < INV_END) {
            if (tab == TAB_STORAGE) {
                if (!moveItemStackTo(stack, STORAGE_START, STORAGE_START + unlockedRows * COLS, false)) return ItemStack.EMPTY;
            } else if (!moveToArmor(stack)) {
                if (idx < SIDE_START) {
                    if (!moveItemStackTo(stack, SIDE_START, INV_END, false)) return ItemStack.EMPTY;
                } else if (!moveItemStackTo(stack, 0, SIDE_START, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (!moveItemStackTo(stack, 0, INV_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    private boolean moveToArmor(ItemStack stack) {
        for (int i = ARMOR_START; i < ARMOR_START + 4; i++) {
            Slot s = slots.get(i);
            if (!s.hasItem() && s.mayPlace(stack)) {
                s.set(stack.split(1));
                return true;
            }
        }
        return false;
    }

    /** Слоты, из которых берутся материалы для крафта: хотбар, карманы, левая рука, открытая часть хранилища. */
    private List<Slot> sources() {
        List<Slot> l = new ArrayList<>();
        for (int i = 0; i < INV_END; i++) l.add(slots.get(i));
        l.add(slots.get(OFFHAND));
        for (int i = 0; i < unlockedRows * COLS; i++) l.add(slots.get(STORAGE_START + i));
        return l;
    }

    public int countItems(Ingredient ing) {
        int n = 0;
        for (Slot s : sources()) {
            ItemStack st = s.getItem();
            if (!st.isEmpty() && ing.test(st)) n += st.getCount();
        }
        return n;
    }

    private void removeItems(Ingredient ing, int count) {
        for (Slot s : sources()) {
            if (count <= 0) return;
            ItemStack st = s.getItem();
            if (!st.isEmpty() && ing.test(st)) {
                int take = Math.min(count, st.getCount());
                st.shrink(take);
                count -= take;
                s.setChanged();
            }
        }
    }

    public boolean canCraft(HubRecipe r) {
        for (HubRecipe.Req req : r.reqs) {
            if (countItems(req.ingredient()) < req.count()) return false;
        }
        return true;
    }

    /** Серверный крафт: проверка, списание, выдача результата. */
    public void craft(ServerPlayer p, HubRecipe r) {
        if (!canCraft(r)) return;
        for (HubRecipe.Req req : r.reqs) removeItems(req.ingredient(), req.count());
        ItemStack out = r.getResultItem(p.level().registryAccess()).copy();
        ItemStack rest = AllowedInv.insert(p.getInventory(), out);
        if (!rest.isEmpty()) p.drop(rest, false);
        broadcastChanges();
    }

    /** Склеивает одинаковые стопки и сортирует хранилище по названию предмета. */
    public void sortStorage() {
        int n = unlockedRows * COLS;
        List<ItemStack> merged = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ItemStack s = storage.getItem(i).copy();
            storage.setItem(i, ItemStack.EMPTY);
            if (s.isEmpty()) continue;
            for (ItemStack m : merged) {
                if (ItemStack.isSameItemSameTags(m, s) && m.getCount() < m.getMaxStackSize()) {
                    int move = Math.min(s.getCount(), m.getMaxStackSize() - m.getCount());
                    m.grow(move);
                    s.shrink(move);
                    if (s.isEmpty()) break;
                }
            }
            if (!s.isEmpty()) merged.add(s);
        }
        merged.sort(Comparator.comparing((ItemStack s) -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString())
                .thenComparing(s -> -s.getCount()));
        for (int i = 0; i < merged.size() && i < n; i++) storage.setItem(i, merged.get(i));
        broadcastChanges();
    }
}
