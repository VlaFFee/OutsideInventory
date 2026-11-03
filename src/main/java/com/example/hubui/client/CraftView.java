package com.example.hubui.client;

import com.example.hubui.HubRecipe;
import com.example.hubui.ModRegistry;
import com.example.hubui.network.CraftPacket;
import com.example.hubui.network.NetworkHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Вкладка "Создание". Рецепты берутся из JSON (тип hubui:hub_crafting), пока их нет - показывается заглушка.
 * Материалы считаются по инвентарю и открытой части хранилища.
 */
@OnlyIn(Dist.CLIENT)
final class CraftView {
    static final String[] CAT_IDS = {"all", "clothing", "tools", "food", "building", "tech", "misc"};
    static final String[] CAT_NAMES = {"Все", "Одежда", "Инструменты", "Еда", "Строительство", "Техника", "Прочее"};
    private static final int CARD_W = 69, CARD_H = 54, ROW_H = 57;
    private static final int LIST_X = 12, LIST_Y = 58, LIST_W = 141, LIST_H = 114;

    private final ItemStack[] catIcons = {
            new ItemStack(Items.CRAFTING_TABLE), new ItemStack(Items.LEATHER_CHESTPLATE),
            new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.APPLE), new ItemStack(Items.BRICKS),
            new ItemStack(Items.REDSTONE), new ItemStack(Items.AMETHYST_SHARD)};
    private final ItemStack anvil = new ItemStack(Items.ANVIL);
    private final HubScreen s;
    private final float[] catHover = new float[7];
    private final Map<ResourceLocation, Float> cardHover = new HashMap<>();

    boolean dirty = true;
    private int category;
    private float scroll, scrollTarget, craftHover;
    private long flashAt = -100000L;
    private ResourceLocation selected;
    private List<HubRecipe> filtered = List.of();

    CraftView(HubScreen s) {
        this.s = s;
    }

    private void refresh() {
        dirty = false;
        Level lvl = s.mc().level;
        if (lvl == null) {
            filtered = List.of();
            return;
        }
        String q = s.craftQuery.trim().toLowerCase(Locale.ROOT);
        List<HubRecipe> out = new ArrayList<>();
        for (HubRecipe r : lvl.getRecipeManager().getAllRecipesFor(ModRegistry.HUB_RECIPE_TYPE.get())) {
            if (category != 0 && !CAT_IDS[category].equals(r.category)) continue;
            String name = r.getResultItem(lvl.registryAccess()).getHoverName().getString().toLowerCase(Locale.ROOT);
            if (!q.isEmpty() && !name.contains(q)) continue;
            out.add(r);
        }
        out.sort(Comparator.comparing((HubRecipe r) -> r.getResultItem(lvl.registryAccess()).getHoverName().getString()));
        filtered = out;
        if (selected != null && out.stream().noneMatch(r -> r.getId().equals(selected))) selected = null;
        if (selected == null && !out.isEmpty()) selected = out.get(0).getId();
    }

    private HubRecipe selectedRecipe() {
        for (HubRecipe r : filtered) if (r.getId().equals(selected)) return r;
        return null;
    }

    private float maxScroll() {
        int rows = (filtered.size() + 1) / 2;
        return Math.max(0, rows * ROW_H - LIST_H);
    }

    void renderBg(GuiGraphics g, long now) {
        if (dirty) refresh();
        int L = s.L(), T = s.T();
        float a = s.anim;
        Font f = s.font();
        PoseStack ps = g.pose();
        int mx = s.rmx, my = s.rmy;
        boolean inter = s.interactive();
        Level lvl = s.mc().level;

        // --- поиск ---
        Gfx.inset(g, L + 10, T + 10, 150, 18, a);
        Gfx.magnifier(g, L + 15, T + 14, a);
        s.craftBox.render(g, mx, my, 0f);

        // --- категории ---
        for (int i = 0; i < 7; i++) {
            int x = L + 10 + i * 21, y = T + 32;
            boolean over = inter && HubScreen.hit(mx, my, x, y, 20, 20);
            catHover[i] += ((over ? 1f : 0f) - catHover[i]) * Math.min(1f, s.dt * 16f);
            boolean act = i == category;
            float h = Math.max(catHover[i], act ? 1f : 0f);
            if (act) g.fill(x - 2, y - 2, x + 22, y + 22, Gfx.argb(0.14f * a, Gfx.CYAN));
            g.fill(x - 1, y - 1, x + 21, y + 21, Gfx.mul(Gfx.lerp(0xFF19335D, 0xFF5CE1FF, h), a));
            g.fill(x, y, x + 20, y + 20, Gfx.mul(0xFF070E20, a));
            g.renderItem(catIcons[i], x + 2, y + 2);
        }

        // --- список рецептов ---
        Gfx.inset(g, L + 10, T + 56, 150, 118, a);
        float dt = s.dt;
        scrollTarget = Mth.clamp(scrollTarget, 0f, maxScroll());
        scroll += (scrollTarget - scroll) * Math.min(1f, dt * 14f);

        if (filtered.isEmpty()) {
            ps.pushPose();
            ps.translate(L + 85 - 12, T + 84, 0);
            ps.scale(1.5f, 1.5f, 1f);
            g.renderItem(anvil, 0, 0);
            ps.popPose();
            int ty = T + 116;
            String l1 = s.craftQuery.isBlank() && category == 0 ? "Рецептов пока нет" : "Ничего не найдено";
            g.drawString(f, l1, L + 85 - f.width(l1) / 2, ty, Gfx.argb(a, Gfx.TEXT), false);
            if (s.craftQuery.isBlank() && category == 0) {
                for (FormattedCharSequence seq : f.split(Component.literal("Они появятся по ходу истории"), 130)) {
                    ty += 12;
                    g.drawString(f, seq, L + 85 - f.width(seq) / 2, ty, Gfx.argb(a, Gfx.MUTED), false);
                }
            }
        } else {
            g.enableScissor(L + LIST_X, T + LIST_Y, L + LIST_X + LIST_W + 3, T + LIST_Y + LIST_H);
            for (int i = 0; i < filtered.size(); i++) {
                HubRecipe r = filtered.get(i);
                int cx = L + LIST_X + (i % 2) * (CARD_W + 3);
                int cy = T + LIST_Y + (i / 2) * ROW_H - Math.round(scroll);
                if (cy + CARD_H < T + LIST_Y || cy > T + LIST_Y + LIST_H) continue;
                drawCard(g, r, cx, cy, a, inter && HubScreen.hit(mx, my, cx, cy, CARD_W, CARD_H)
                        && HubScreen.hit(mx, my, L + LIST_X, T + LIST_Y, LIST_W, LIST_H), lvl);
            }
            g.disableScissor();
            float ms = maxScroll();
            if (ms > 0) {
                int tx = L + 155, ty = T + LIST_Y, th = LIST_H;
                g.fill(tx, ty, tx + 3, ty + th, Gfx.argb(0.5f * a, 0x152D55));
                int thumb = Math.max(14, Math.round(th * (th / (th + ms))));
                int off = Math.round((th - thumb) * (scroll / ms));
                g.fill(tx, ty + off, tx + 3, ty + off + thumb, Gfx.argb(a, Gfx.CYAN));
            }
        }

        drawDetail(g, now, a, lvl);
    }

    private void drawCard(GuiGraphics g, HubRecipe r, int x, int y, float a, boolean over, Level lvl) {
        Font f = s.font();
        PoseStack ps = g.pose();
        float h = cardHover.getOrDefault(r.getId(), 0f);
        h += ((over ? 1f : 0f) - h) * Math.min(1f, s.dt * 16f);
        cardHover.put(r.getId(), h);
        boolean sel = r.getId().equals(selected);
        float k = Math.max(h, sel ? 1f : 0f);
        if (sel) g.fill(x - 2, y - 2, x + CARD_W + 2, y + CARD_H + 2, Gfx.argb(0.14f * a, Gfx.CYAN));
        g.fill(x - 1, y - 1, x + CARD_W + 1, y + CARD_H + 1, Gfx.mul(Gfx.lerp(0xFF19335D, 0xFF5CE1FF, k), a));
        g.fillGradient(x, y, x + CARD_W, y + CARD_H, Gfx.mul(Gfx.lerp(0xFF081124, 0xFF0F2345, k), a),
                Gfx.mul(Gfx.lerp(0xFF050B19, 0xFF08152B, k), a));

        ItemStack out = r.getResultItem(lvl.registryAccess());
        ps.pushPose();
        ps.translate(x + (CARD_W - 24) / 2f, y + 5 - 2 * h, 0);
        ps.scale(1.5f, 1.5f, 1f);
        g.renderItem(out, 0, 0);
        ps.popPose();

        int ty = y + 33;
        List<FormattedCharSequence> nl = f.split(out.getHoverName(), CARD_W - 6);
        for (int i = 0; i < Math.min(2, nl.size()); i++) {
            FormattedCharSequence seq = nl.get(i);
            g.drawString(f, seq, x + CARD_W / 2 - f.width(seq) / 2, ty, Gfx.argb(a, Gfx.TEXT), false);
            ty += 9;
        }
        if (s.getMenu().canCraft(r)) g.fill(x + CARD_W - 7, y + 3, x + CARD_W - 3, y + 7, Gfx.argb(a, Gfx.GREEN));
    }

    private void drawDetail(GuiGraphics g, long now, float a, Level lvl) {
        int L = s.L(), T = s.T();
        Font f = s.font();
        PoseStack ps = g.pose();
        int x = L + 168, y = T + 10;
        Gfx.inset(g, x, y, 124, 164, a);
        HubRecipe r = selectedRecipe();
        if (r == null) {
            int ty = y + 70;
            for (FormattedCharSequence seq : f.split(Component.literal("Выбери рецепт слева"), 100)) {
                g.drawString(f, seq, x + 62 - f.width(seq) / 2, ty, Gfx.argb(a, Gfx.MUTED), false);
                ty += 11;
            }
            return;
        }
        ItemStack out = r.getResultItem(lvl.registryAccess());
        Gfx.inset(g, x + 8, y + 8, 24, 24, a);
        g.renderItem(out, x + 12, y + 12);
        int ty = y + 10;
        for (FormattedCharSequence seq : f.split(out.getHoverName(), 82)) {
            g.drawString(f, seq, x + 38, ty, Gfx.argb(a, Gfx.TEXT), false);
            ty += 10;
        }
        ty = y + 38;
        int shown = 0;
        for (FormattedCharSequence seq : f.split(Component.literal(r.description), 108)) {
            if (shown++ >= 4) break;
            g.drawString(f, seq, x + 8, ty, Gfx.argb(a, Gfx.MUTED), false);
            ty += 9;
        }

        // "Нужно"
        int hy = y + 78;
        ps.pushPose();
        ps.translate(x + 8, hy - 3, 0);
        ps.scale(0.7f, 0.7f, 1f);
        g.renderItem(anvil, 0, 0);
        ps.popPose();
        g.drawString(f, "Нужно", x + 22, hy, Gfx.argb(a, Gfx.TEXT), false);
        g.fill(x + 8, hy + 10, x + 116, hy + 11, Gfx.argb(0.4f * a, Gfx.BLUE));

        int ry = hy + 15;
        int idx = 0;
        for (HubRecipe.Req req : r.reqs) {
            if (ry + 17 > y + 138) {
                int left = r.reqs.size() - idx;
                g.drawString(f, "+ ещё " + left, x + 8, ry + 4, Gfx.argb(a, Gfx.MUTED), false);
                break;
            }
            ItemStack[] items = req.ingredient().getItems();
            ItemStack disp = items.length == 0 ? ItemStack.EMPTY : items[(int) (now / 1000 % items.length)];
            g.fill(x + 8, ry, x + 116, ry + 17, Gfx.argb(0.55f * a, 0x0A172E));
            if (!disp.isEmpty()) g.renderItem(disp, x + 10, ry + 1);
            int have = s.getMenu().countItems(req.ingredient());
            boolean ok = have >= req.count();
            String cnt = Math.min(have, 999) + " / " + req.count();
            int cw = f.width(cnt);
            g.drawString(f, cnt, x + 113 - cw, ry + 5, Gfx.argb(a, ok ? Gfx.GREEN : Gfx.RED), false);
            String nm = disp.isEmpty() ? "?" : f.plainSubstrByWidth(disp.getHoverName().getString(), 112 - cw - 32);
            g.drawString(f, nm, x + 29, ry + 5, Gfx.argb(a, Gfx.SOFT), false);
            ry += 19;
            idx++;
        }

        // кнопка
        boolean can = s.getMenu().canCraft(r);
        int bx = x + 8, by = y + 142;
        boolean over = can && s.interactive() && HubScreen.hit(s.rmx, s.rmy, bx, by, 108, 18);
        craftHover += ((over ? 1f : 0f) - craftHover) * Math.min(1f, s.dt * 16f);
        float flash = Mth.clamp(1f - (now - flashAt) / 350f, 0f, 1f);
        Gfx.button(g, bx, by, 108, 18, Math.max(craftHover, flash), can, a);
        String label = "Создать ×1";
        g.drawString(f, label, bx + 54 - f.width(label) / 2, by + 5, Gfx.argb(a, can ? 0xFFFFFF : 0x495872), false);
    }

    boolean scroll(double mx, double my, double delta) {
        int L = s.L(), T = s.T();
        if (HubScreen.hit(mx, my, L + 10, T + 56, 155, 118)) {
            scrollTarget = Mth.clamp(scrollTarget - (float) delta * 28f, 0f, maxScroll());
            return true;
        }
        return false;
    }

    boolean click(double mx, double my, int button) {
        if (button != 0) return false;
        int L = s.L(), T = s.T();

        for (int i = 0; i < 7; i++) {
            if (HubScreen.hit(mx, my, L + 10 + i * 21, T + 32, 20, 20)) {
                if (category != i) {
                    category = i;
                    scrollTarget = 0;
                    dirty = true;
                    UiSound.play(ModRegistry.UI_TAB, 1.2f);
                }
                return true;
            }
        }
        if (HubScreen.hit(mx, my, L + LIST_X, T + LIST_Y, LIST_W, LIST_H)) {
            for (int i = 0; i < filtered.size(); i++) {
                int cx = L + LIST_X + (i % 2) * (CARD_W + 3);
                int cy = T + LIST_Y + (i / 2) * ROW_H - Math.round(scroll);
                if (HubScreen.hit(mx, my, cx, cy, CARD_W, CARD_H)) {
                    selected = filtered.get(i).getId();
                    UiSound.play(ModRegistry.UI_TAB, 1.1f);
                    return true;
                }
            }
        }
        HubRecipe r = selectedRecipe();
        if (r != null && HubScreen.hit(mx, my, L + 168 + 8, T + 10 + 142, 108, 18) && s.getMenu().canCraft(r)) {
            NetworkHandler.CHANNEL.sendToServer(new CraftPacket(r.getId()));
            flashAt = net.minecraft.Util.getMillis();
            UiSound.play(ModRegistry.UI_CRAFT, 1f);
            return true;
        }
        return false;
    }
}
