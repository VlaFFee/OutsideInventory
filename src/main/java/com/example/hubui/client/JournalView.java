package com.example.hubui.client;

import com.example.hubui.JournalEntry;
import com.example.hubui.ModRegistry;
import com.example.hubui.network.NetworkHandler;
import com.example.hubui.network.ReadEntryPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Вкладка "Журнал": записи, которые в сюжете оставляет персонаж. Игрок их только читает. */
@OnlyIn(Dist.CLIENT)
final class JournalView {
    private static final int ROW_H = 30, LIST_Y = 12, LIST_H = 176;
    private final HubScreen s;
    private final ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
    private final Map<Integer, Float> rowHover = new HashMap<>();
    private int selectedId = -1;
    private float listScroll, listTarget, bodyScroll, bodyTarget;

    JournalView(HubScreen s) {
        this.s = s;
    }

    private List<JournalEntry> newestFirst() {
        List<JournalEntry> l = new ArrayList<>(ClientHub.entries);
        Collections.reverse(l);
        return l;
    }

    void onShow() {
        List<JournalEntry> l = newestFirst();
        boolean exists = false;
        for (JournalEntry e : l) if (e.id() == selectedId) exists = true;
        if (!exists) selectedId = l.isEmpty() ? -1 : l.get(0).id();
        listScroll = listTarget = bodyScroll = bodyTarget = 0;
        markRead(selectedId);
    }

    private void markRead(int id) {
        for (JournalEntry e : ClientHub.entries) {
            if (e.id() == id && e.unread()) {
                NetworkHandler.CHANNEL.sendToServer(new ReadEntryPacket(id));
                ClientHub.markReadLocal(id);
            }
        }
    }

    void renderBg(GuiGraphics g, long now) {
        int L = s.L(), T = s.T();
        float a = s.anim;
        Font f = s.font();
        PoseStack ps = g.pose();
        int mx = s.rmx, my = s.rmy;
        List<JournalEntry> list = newestFirst();

        Gfx.inset(g, L + 10, T + 10, 100, 180, a);
        Gfx.inset(g, L + 118, T + 10, 174, 180, a);

        if (list.isEmpty()) {
            ps.pushPose();
            ps.translate(L + 205 - 16, T + 70, 0);
            ps.scale(2f, 2f, 1f);
            g.renderItem(book, 0, 0);
            ps.popPose();
            String l1 = "Журнал пока пуст";
            g.drawString(f, l1, L + 205 - f.width(l1) / 2, T + 112, Gfx.argb(a, Gfx.TEXT), false);
            int ty = T + 125;
            for (FormattedCharSequence seq : f.split(Component.literal("Здесь будут появляться записи по ходу истории"), 150)) {
                g.drawString(f, seq, L + 205 - f.width(seq) / 2, ty, Gfx.argb(a, Gfx.MUTED), false);
                ty += 11;
            }
            return;
        }

        // ---- список ----
        float maxList = Math.max(0, list.size() * ROW_H - LIST_H);
        listTarget = Mth.clamp(listTarget, 0f, maxList);
        listScroll += (listTarget - listScroll) * Math.min(1f, s.dt * 14f);
        g.enableScissor(L + 11, T + LIST_Y, L + 109, T + LIST_Y + LIST_H);
        for (int i = 0; i < list.size(); i++) {
            JournalEntry e = list.get(i);
            int y = T + LIST_Y + i * ROW_H - Math.round(listScroll);
            if (y + ROW_H < T + LIST_Y || y > T + LIST_Y + LIST_H) continue;
            boolean over = s.interactive() && HubScreen.hit(mx, my, L + 12, y, 96, ROW_H - 2)
                    && HubScreen.hit(mx, my, L + 11, T + LIST_Y, 98, LIST_H);
            float h = rowHover.getOrDefault(e.id(), 0f);
            h += ((over ? 1f : 0f) - h) * Math.min(1f, s.dt * 16f);
            rowHover.put(e.id(), h);
            boolean sel = e.id() == selectedId;
            float k = Math.max(h, sel ? 1f : 0f);
            g.fill(L + 12, y, L + 108, y + ROW_H - 2, Gfx.mul(Gfx.lerp(0xFF070E20, 0xFF12274B, k), a));
            if (sel) g.fill(L + 12, y, L + 14, y + ROW_H - 2, Gfx.argb(a, Gfx.CYAN));
            String title = f.plainSubstrByWidth(e.title(), 80);
            g.drawString(f, title, L + 19, y + 5, Gfx.argb(a, Gfx.TEXT), false);
            g.drawString(f, f.plainSubstrByWidth(e.author() + " · день " + e.day(), 80), L + 19, y + 16,
                    Gfx.argb(a, Gfx.MUTED), false);
            if (e.unread()) {
                float pu = 0.5f + 0.5f * (float) Math.sin(now / 1000f * 5f);
                g.fill(L + 100, y + 4, L + 106, y + 10, Gfx.argb(0.25f * a * pu, Gfx.CYAN));
                g.fill(L + 101, y + 5, L + 105, y + 9, Gfx.argb(a, 0xFFFFFF));
            }
        }
        g.disableScissor();

        // ---- запись ----
        JournalEntry cur = null;
        for (JournalEntry e : list) if (e.id() == selectedId) cur = e;
        if (cur == null) return;

        int bx = L + 128, by = T + 18, bw = 154;
        ps.pushPose();
        ps.translate(bx, by, 0);
        ps.scale(1.3f, 1.3f, 1f);
        int ty = 0;
        for (FormattedCharSequence seq : f.split(Component.literal(cur.title()), (int) (bw / 1.3f))) {
            g.drawString(f, seq, 0, ty, Gfx.argb(a, Gfx.TEXT), false);
            ty += 10;
        }
        ps.popPose();
        int sy = by + Math.round(ty * 1.3f) + 3;
        g.drawString(f, cur.author(), bx, sy, Gfx.argb(a, Gfx.CYAN), false);
        String day = "день " + cur.day();
        g.drawString(f, day, bx + bw - f.width(day), sy, Gfx.argb(a, Gfx.MUTED), false);
        g.fill(bx, sy + 12, bx + bw, sy + 13, Gfx.argb(0.4f * a, Gfx.BLUE));

        int textTop = sy + 18, textBottom = T + 186;
        List<FormattedCharSequence> lines = f.split(Component.literal(cur.text()), bw);
        float contentH = lines.size() * 10f;
        float maxBody = Math.max(0, contentH - (textBottom - textTop));
        bodyTarget = Mth.clamp(bodyTarget, 0f, maxBody);
        bodyScroll += (bodyTarget - bodyScroll) * Math.min(1f, s.dt * 14f);
        g.enableScissor(bx - 2, textTop, bx + bw + 6, textBottom);
        for (int i = 0; i < lines.size(); i++) {
            int y = textTop + i * 10 - Math.round(bodyScroll);
            if (y + 10 < textTop || y > textBottom) continue;
            g.drawString(f, lines.get(i), bx, y, Gfx.argb(a, Gfx.SOFT), false);
        }
        g.disableScissor();
        if (bodyScroll > 1) g.fillGradient(bx - 2, textTop, bx + bw + 6, textTop + 10, Gfx.argb(a, 0x040814), 0x00040814);
        if (bodyScroll < maxBody - 1) g.fillGradient(bx - 2, textBottom - 10, bx + bw + 6, textBottom, 0x00040814, Gfx.argb(a, 0x040814));
    }

    boolean scroll(double mx, double my, double delta) {
        int L = s.L(), T = s.T();
        if (HubScreen.hit(mx, my, L + 10, T + 10, 100, 180)) {
            listTarget -= (float) delta * 28f;
            return true;
        }
        if (HubScreen.hit(mx, my, L + 118, T + 10, 174, 180)) {
            bodyTarget -= (float) delta * 24f;
            return true;
        }
        return false;
    }

    boolean click(double mx, double my, int button) {
        if (button != 0) return false;
        int L = s.L(), T = s.T();
        if (!HubScreen.hit(mx, my, L + 11, T + LIST_Y, 98, LIST_H)) return false;
        List<JournalEntry> list = newestFirst();
        for (int i = 0; i < list.size(); i++) {
            int y = T + LIST_Y + i * ROW_H - Math.round(listScroll);
            if (HubScreen.hit(mx, my, L + 12, y, 96, ROW_H - 2)) {
                int id = list.get(i).id();
                if (id != selectedId) {
                    selectedId = id;
                    bodyScroll = bodyTarget = 0;
                    markRead(id);
                    UiSound.play(ModRegistry.UI_TAB, 1.1f);
                }
                return true;
            }
        }
        return false;
    }
}
