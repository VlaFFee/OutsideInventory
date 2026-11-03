package com.example.hubui.client;

import com.example.hubui.HubConfig;
import com.example.hubui.ModRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Оцифрованная подсказка предмета: голографическая рамка с вращающейся 3D-моделью, "расшифровка" названия,
 * построчное появление, сканирующая линия. Длинное описание обрезается и предлагает окно крупного чтения.
 */
@OnlyIn(Dist.CLIENT)
public final class FancyTooltip {
    static final int MAX_ROWS = 9, WRAP = 196;
    private static int lastKey;
    private static long appearAt, lastSeen;

    public static List<Component> lines(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        TooltipFlag flag = mc.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;
        return stack.getTooltipLines(mc.player, flag);
    }

    static List<FormattedCharSequence> bodyRows(Font f, List<Component> lines, int wrap) {
        List<FormattedCharSequence> out = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            Component c = lines.get(i);
            if (c.getString().isEmpty()) out.add(FormattedCharSequence.EMPTY);
            else out.addAll(f.split(c, wrap));
        }
        return out;
    }

    public static boolean isLong(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return bodyRows(Minecraft.getInstance().font, lines(stack), WRAP).size() > MAX_ROWS;
    }

    static int accent(ItemStack stack) {
        Rarity r = stack.getRarity();
        if (r != Rarity.COMMON && r.color.getColor() != null) return r.color.getColor();
        return Gfx.CYAN;
    }

    /** Строка с "расшифровкой": левая часть уже настоящая, правая ещё мелькает цифрами. */
    public static String scramble(String s, float prog, long now) {
        if (prog >= 1f) return s;
        int shown = (int) (prog * s.length());
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i < shown || c == ' ') sb.append(c);
            else sb.append((char) ('0' + (int) (Gfx.hash01(i * 31 + (int) (now / 45)) * 10f)));
        }
        return sb.toString();
    }

    public static void render(GuiGraphics g, Font f, ItemStack stack, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        int sw = g.guiWidth(), sh = g.guiHeight();
        long now = Util.getMillis();
        float t = now / 1000f;

        int key = Objects.hash(stack.getItem(), stack.getCount(), stack.getTag());
        if (key != lastKey || now - lastSeen > 160) {
            lastKey = key;
            appearAt = now;
            if (HubConfig.tooltipSounds()) {
                UiSound.play(ModRegistry.UI_SCAN, 0.9f + stack.getRarity().ordinal() * 0.12f, 0.35f);
            }
        }
        lastSeen = now;
        float el = now - appearAt;
        float e = Gfx.easeOutCubic(el / 240f);
        float a = e;
        int acc = accent(stack);

        // ---- содержимое ----
        List<Component> lines = lines(stack);
        Component name = lines.isEmpty() ? stack.getHoverName() : lines.get(0);
        List<FormattedCharSequence> nameRows = f.split(name, 150);
        if (nameRows.size() > 2) nameRows = nameRows.subList(0, 2);
        List<FormattedCharSequence> body = bodyRows(f, lines, WRAP);
        boolean isLong = body.size() > MAX_ROWS;
        int shown = isLong ? MAX_ROWS - 1 : body.size();
        int extraRows = isLong ? 1 : 0;

        String sub = stack.isDamageableItem()
                ? "Прочность " + (stack.getMaxDamage() - stack.getDamageValue()) + "/" + stack.getMaxDamage()
                : stack.getCount() > 1 ? "Количество: " + stack.getCount() : "";

        int maxBodyW = 0;
        for (int i = 0; i < shown; i++) maxBodyW = Math.max(maxBodyW, f.width(body.get(i)));
        int nameW = 0;
        for (FormattedCharSequence r : nameRows) nameW = Math.max(nameW, f.width(r));
        int boxW = Mth.clamp(Math.max(maxBodyW + 20, 56 + Math.max(nameW, f.width(sub)) + 12), 176, 236);
        int headerH = 54;
        int boxH = headerH + 4 + (shown + extraRows) * 10 + (isLong ? 22 : 6);
        if (shown + extraRows == 0) boxH = headerH + 4;

        // ---- позиция ----
        int x = mx + 14, y = my - 14;
        if (x + boxW > sw - 4) x = mx - boxW - 14;
        if (x < 4) x = 4;
        if (y + boxH > sh - 4) y = sh - 4 - boxH;
        if (y < 4) y = 4;

        PoseStack ps = g.pose();
        ps.pushPose();
        float sc = 0.92f + 0.08f * e;
        ps.translate(x + boxW / 2f, y + boxH / 2f, 400f);
        ps.scale(sc, sc, 1f);
        ps.translate(-boxW / 2f, -boxH / 2f, 0f);

        // ---- фон и рамка ----
        g.fill(-4, -4, boxW + 4, boxH + 4, Gfx.argb(0.06f * a, acc));
        g.fill(-2, -2, boxW + 2, boxH + 2, Gfx.argb(0.10f * a, acc));
        g.fill(-1, -1, boxW + 1, boxH + 1, Gfx.argb(0.85f * a, Gfx.lerp(0xFF000000 | acc, 0xFF19407E, 0.35f) & 0xFFFFFF));
        g.fillGradient(0, 0, boxW, boxH, Gfx.argb(0.96f * a, 0x070E20), Gfx.argb(0.96f * a, 0x030814));

        // фоновые цифры (очень прозрачные)
        for (int gy = 4; gy < boxH - 8; gy += 12) {
            for (int gx = 4; gx < boxW - 8; gx += 14) {
                float r = Gfx.hash01(gx * 31 + gy * 17 + (int) (t * 1.5f + gy * 0.05f));
                if (r > 0.45f) continue;
                g.drawString(f, r < 0.22f ? "0" : "1", gx, gy, Gfx.argb(0.07f * a, Gfx.CYAN), false);
            }
        }

        // сканирующая линия
        float sy = (t * 0.7f % 1f) * boxH;
        g.fillGradient(0, (int) sy - 4, boxW, (int) sy, 0x00000000 | (acc & 0xFFFFFF), Gfx.argb(0.10f * a, acc));
        g.fill(0, (int) sy, boxW, (int) sy + 1, Gfx.argb(0.22f * a, acc));

        // верхняя полоса с бегущим бликом
        g.fill(0, 0, boxW, 1, Gfx.argb(0.9f * a, acc));
        int hx = (int) ((t * 0.6f % 1f) * boxW);
        g.fill(Math.max(0, hx - 14), 0, Math.min(boxW, hx + 14), 2, Gfx.argb(a, 0xFFFFFF));

        // уголки
        int cc = Gfx.argb(a, acc), L = 7;
        g.fill(-1, -1, L, 0, cc); g.fill(-1, -1, 0, L, cc);
        g.fill(boxW - L, -1, boxW + 1, 0, cc); g.fill(boxW, -1, boxW + 1, L, cc);
        g.fill(-1, boxH, L, boxH + 1, cc); g.fill(-1, boxH - L, 0, boxH + 1, cc);
        g.fill(boxW - L, boxH, boxW + 1, boxH + 1, cc); g.fill(boxW, boxH - L, boxW + 1, boxH + 1, cc);

        // ---- голограмма предмета ----
        g.fill(7, 7, 49, 49, Gfx.argb(0.9f * a, 0x194072));
        g.fillGradient(8, 8, 48, 48, Gfx.argb(a, 0x061226), Gfx.argb(a, 0x030915));
        for (int i = 1; i < 5; i++) {
            g.fill(8 + i * 8, 8, 9 + i * 8, 48, Gfx.argb(0.07f * a, Gfx.CYAN));
            g.fill(8, 8 + i * 8, 48, 9 + i * 8, Gfx.argb(0.07f * a, Gfx.CYAN));
        }
        g.fillGradient(18, 14, 38, 42, Gfx.argb(0f, Gfx.CYAN), Gfx.argb(0.16f * a, Gfx.CYAN));
        Gfx.ellipseFill(g, 28, 42, 14, 4, Gfx.argb(0.22f * a, Gfx.BLUE));
        Gfx.ellipseRing(g, 28, 42, 14, 4, 1, Gfx.argb(0.85f * a, acc));
        float hy = 8 + (t * 0.9f % 1f) * 40f;
        g.fill(8, (int) hy, 48, (int) hy + 1, Gfx.argb(0.35f * a, Gfx.CYAN));
        float bob = (float) Math.sin(t * 2.0) * 1.5f;
        ItemModel3D.render(g, stack, 28f, 26f + bob, 26f, t * 50f % 360f, -16f, 60f);

        // ---- название ----
        float dp = Mth.clamp((el - 40f) / 420f, 0f, 1f);
        int ny = 11;
        if (dp < 1f) {
            String sc2 = scramble(name.getString(), dp, now);
            g.drawString(f, f.plainSubstrByWidth(sc2, 150), 56, ny, Gfx.argb(a, acc), false);
        } else {
            for (FormattedCharSequence r : nameRows) {
                g.drawString(f, r, 56, ny, Gfx.argb(a, 0xFFFFFF), false);
                ny += 10;
            }
        }
        if (!sub.isEmpty()) g.drawString(f, sub, 56, 36, Gfx.argb(a, Gfx.MUTED), false);
        g.fill(8, headerH - 2, boxW - 8, headerH - 1, Gfx.argb(0.35f * a, acc));

        // ---- описание ----
        int by = headerH + 4;
        for (int i = 0; i < shown; i++) {
            float ra = Mth.clamp((el - 70f - i * 28f) / 160f, 0f, 1f);
            FormattedCharSequence row = body.get(i);
            if (ra >= 0.03f && row != FormattedCharSequence.EMPTY) {
                g.drawString(f, row, 10 + Math.round((1f - ra) * 6f), by, Gfx.argb(ra * a, 0xC4D2EA), false);
            }
            by += 10;
        }
        if (isLong) {
            g.drawString(f, "…", 10, by, Gfx.argb(a, Gfx.MUTED), false);
            by += 12;
            String keyName = KeyBindings.READ.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT);
            String label = "[" + keyName + "]  Читать полностью";
            int cw = f.width(label) + 22;
            float pulse = 0.5f + 0.5f * (float) Math.sin(t * 4f);
            g.fill(9, by - 1, 9 + cw + 2, by + 13, Gfx.argb((0.45f + 0.4f * pulse) * a, acc));
            g.fill(10, by, 10 + cw, by + 12, Gfx.argb(0.95f * a, 0x0C2245));
            // значок "развернуть"
            g.fill(14, by + 3, 18, by + 4, Gfx.argb(a, 0xFFFFFF));
            g.fill(14, by + 3, 15, by + 7, Gfx.argb(a, 0xFFFFFF));
            g.fill(20, by + 8, 24, by + 9, Gfx.argb(a, 0xFFFFFF));
            g.fill(23, by + 5, 24, by + 9, Gfx.argb(a, 0xFFFFFF));
            g.drawString(f, label, 29, by + 2, Gfx.argb(a, 0xFFFFFF), false);
        }
        ps.popPose();
    }

    private FancyTooltip() {}
}
