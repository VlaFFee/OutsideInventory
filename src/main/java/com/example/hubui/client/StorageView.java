package com.example.hubui.client;

import com.example.hubui.HubMenu;
import com.example.hubui.ModRegistry;
import com.example.hubui.network.NetworkHandler;
import com.example.hubui.network.SortStoragePacket;
import net.minecraft.Util;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Locale;

/** Вкладка "Хранилище": поиск, сортировка, сетка слотов и панель сведений о предмете. */
@OnlyIn(Dist.CLIENT)
final class StorageView {
    private static final int DX = 132, DY = 10, DW = 160, DH = 162;
    private final HubScreen s;
    private final ItemStack chest = new ItemStack(Items.CHEST);
    private float sortHover, readHover;
    private ItemStack lastStack = ItemStack.EMPTY;   // "липкая" панель: предмет остаётся, пока мышь едет к кнопке
    private long lastAt;
    private ItemStack readStack = ItemStack.EMPTY;

    StorageView(HubScreen s) {
        this.s = s;
    }

    void renderBg(GuiGraphics g, long now) {
        int L = s.L(), T = s.T();
        float a = s.anim;
        Font f = s.font();
        int mx = s.rmx, my = s.rmy;

        Gfx.inset(g, L + 10, T + 10, 92, 18, a);
        Gfx.magnifier(g, L + 15, T + 14, a);
        s.storageBox.render(g, mx, my, 0f);

        boolean over = s.interactive() && HubScreen.hit(mx, my, L + 106, T + 10, 18, 18);
        sortHover += ((over ? 1f : 0f) - sortHover) * Math.min(1f, s.dt * 16f);
        Gfx.button(g, L + 106, T + 10, 18, 18, sortHover, true, a);
        int bc = Gfx.argb(a, Gfx.TEXT);
        g.fill(L + 110, T + 15, L + 120, T + 16, bc);
        g.fill(L + 110, T + 18, L + 117, T + 19, bc);
        g.fill(L + 110, T + 21, L + 114, T + 22, bc);

        Gfx.inset(g, L + 10, T + 31, 114, 131, a);
        if (s.getMenu().unlockedRows < 7) {
            Gfx.lock(g, L + 12, T + 165);
            g.drawString(f, "Ещё ряд откроется позже", L + 26, T + 166, Gfx.argb(a, Gfx.MUTED), false);
        }
        drawDetail(g, a);
    }

    private void drawDetail(GuiGraphics g, float a) {
        int L = s.L(), T = s.T();
        Font f = s.font();
        PoseStack ps = g.pose();
        int x = L + DX, y = T + DY;
        Gfx.inset(g, x, y, DW, DH, a);

        Slot hs = s.hover;
        long nowMs = Util.getMillis();
        ItemStack st = hs != null && hs.hasItem() ? hs.getItem() : ItemStack.EMPTY;
        if (!st.isEmpty()) {
            lastStack = st;
            lastAt = nowMs;
        } else if (!lastStack.isEmpty()
                && (nowMs - lastAt < 450L || HubScreen.hit(s.rmx, s.rmy, x, y, DW, DH))) {
            st = lastStack;
        } else {
            lastStack = ItemStack.EMPTY;
        }
        readStack = ItemStack.EMPTY;
        if (st.isEmpty()) {
            ps.pushPose();
            ps.translate(x + DW / 2f - 16, y + 46, 0);
            ps.scale(2f, 2f, 1f);
            g.renderItem(chest, 0, 0);
            ps.popPose();
            int ty = y + 92;
            for (FormattedCharSequence seq : f.split(Component.literal("Наведи на предмет — здесь появится всё о нём"), DW - 36)) {
                g.drawString(f, seq, x + DW / 2 - f.width(seq) / 2, ty, Gfx.argb(a, Gfx.MUTED), false);
                ty += 11;
            }
            return;
        }

        Gfx.inset(g, x + 9, y + 9, 54, 54, a);
        ps.pushPose();
        ps.translate(x + 12, y + 12, 0);
        ps.scale(3f, 3f, 1f);
        g.renderItem(st, 0, 0);
        ps.popPose();

        List<Component> lines = st.getTooltipLines(s.mc().player, TooltipFlag.NORMAL);
        int ty = y + 13;
        for (FormattedCharSequence seq : f.split(lines.get(0), DW - 80)) {
            g.drawString(f, seq, x + 72, ty, Gfx.argb(a, Gfx.TEXT), false);
            ty += 10;
        }
        g.drawString(f, "Количество: " + st.getCount(), x + 72, ty + 3, Gfx.argb(a, Gfx.MUTED), false);

        boolean isLong = FancyTooltip.isLong(st);
        int limit = y + DH - (isLong ? 30 : 12);
        ty = y + 70;
        boolean cut = false;
        for (int i = 1; i < lines.size() && !cut; i++) {
            for (FormattedCharSequence seq : f.split(lines.get(i), DW - 20)) {
                if (ty >= limit) {
                    cut = true;
                    break;
                }
                g.drawString(f, seq, x + 10, ty, Gfx.argb(a, Gfx.SOFT), false);
                ty += 10;
            }
        }
        if (isLong) {
            readStack = st;
            int bx = x + 10, by = y + DH - 24, bw = DW - 20;
            boolean over = s.interactive() && HubScreen.hit(s.rmx, s.rmy, bx, by, bw, 16);
            readHover += ((over ? 1f : 0f) - readHover) * Math.min(1f, s.dt * 16f);
            Gfx.button(g, bx, by, bw, 16, readHover, true, a);
            String label = "Читать полностью  [" + KeyBindings.READ.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT) + "]";
            g.drawString(f, label, bx + bw / 2 - f.width(label) / 2, by + 4, Gfx.argb(a, 0xFFFFFF), false);
        }
    }

    /** Подсветка найденных предметов и затемнение остальных (рисуется поверх предметов). */
    void overlay(GuiGraphics g) {
        String q = s.storageQuery.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return;
        int L = s.L(), T = s.T();
        for (Slot sl : s.getMenu().slots) {
            if (!(sl instanceof HubMenu.StorageSlot) || !sl.isActive() || !sl.hasItem()) continue;
            int x = L + sl.x - 1, y = T + sl.y - 1;
            boolean match = sl.getItem().getHoverName().getString().toLowerCase(Locale.ROOT).contains(q);
            if (match) g.renderOutline(x, y, 18, 18, Gfx.argb(0.9f, Gfx.CYAN));
            else g.fill(x + 1, y + 1, x + 17, y + 17, 0xB8030710);
        }
    }

    boolean click(double mx, double my, int button) {
        if (button != 0) return false;
        if (HubScreen.hit(mx, my, s.L() + 106, s.T() + 10, 18, 18)) {
            NetworkHandler.CHANNEL.sendToServer(new SortStoragePacket());
            UiSound.play(ModRegistry.UI_DATA, 1f);
            return true;
        }
        if (!readStack.isEmpty() && HubScreen.hit(mx, my, s.L() + DX + 10, s.T() + DY + DH - 24, DW - 20, 16)) {
            s.mc().setScreen(new ItemReadScreen(s, readStack.copy()));
            return true;
        }
        return false;
    }
}
