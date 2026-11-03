package com.example.hubui.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Рисование в тёмно-голубо-синей теме: панели, рамки слотов, свечение, молнии, частицы. */
@OnlyIn(Dist.CLIENT)
public final class Gfx {
    public static final int CYAN = 0x5CE1FF, BLUE = 0x3A9BFF, TEXT = 0xE6F2FF, MUTED = 0x7F9CC4,
            SOFT = 0xB8C7E0, RED = 0xFF7A7A, GREEN = 0x7CFFB2;

    // ---------- цвета ----------
    public static int argb(float a, int rgb) {
        return (int) (Mth.clamp(a, 0f, 1f) * 255f) << 24 | (rgb & 0xFFFFFF);
    }

    public static int mul(int argb, float f) {
        int a = (int) ((argb >>> 24) * Mth.clamp(f, 0f, 1f));
        return a << 24 | (argb & 0xFFFFFF);
    }

    private static int ch(int c, int shift) {
        return c >>> shift & 255;
    }

    public static int lerp(int c1, int c2, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int a = (int) Mth.lerp(t, ch(c1, 24), ch(c2, 24));
        int r = (int) Mth.lerp(t, ch(c1, 16), ch(c2, 16));
        int g = (int) Mth.lerp(t, ch(c1, 8), ch(c2, 8));
        int b = (int) Mth.lerp(t, ch(c1, 0), ch(c2, 0));
        return a << 24 | r << 16 | g << 8 | b;
    }

    // ---------- плавность ----------
    public static float easeOutCubic(float t) {
        float u = 1f - Mth.clamp(t, 0f, 1f);
        return 1f - u * u * u;
    }

    public static float easeOutBack(float t) {
        t = Mth.clamp(t, 0f, 1f);
        float c1 = 1.70158f, c3 = c1 + 1f, u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    public static float hash01(int n) {
        n = n << 13 ^ n;
        return ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 2147483647f;
    }

    // ---------- панели ----------
    public static void panel(GuiGraphics g, int x, int y, int w, int h, float pulse, float a) {
        int glow = argb(0.07f * a * (0.6f + 0.4f * pulse), BLUE);
        for (int i = 4; i >= 1; i--) g.fill(x - i * 2, y - i * 2, x + w + i * 2, y + h + i * 2, glow);
        int edge = lerp(0xFF224D96, 0xFF58D6FF, pulse * 0.6f);
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, mul(0xFF02060E, a));
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, mul(edge, a));
        g.fillGradient(x, y, x + w, y + h, mul(0xFF091328, a), mul(0xFF040A17, a));
        g.fill(x + 1, y + 1, x + w - 1, y + 2, argb(0.16f * a, 0x8FD0FF));
        int c = argb(0.95f * a, CYAN), l = 9;
        g.fill(x - 2, y - 2, x - 2 + l, y - 1, c);
        g.fill(x - 2, y - 2, x - 1, y - 2 + l, c);
        g.fill(x + w + 2 - l, y - 2, x + w + 2, y - 1, c);
        g.fill(x + w + 1, y - 2, x + w + 2, y - 2 + l, c);
        g.fill(x - 2, y + h + 1, x - 2 + l, y + h + 2, c);
        g.fill(x - 2, y + h + 2 - l, x - 1, y + h + 2, c);
        g.fill(x + w + 2 - l, y + h + 1, x + w + 2, y + h + 2, c);
        g.fill(x + w + 1, y + h + 2 - l, x + w + 2, y + h + 2, c);
    }

    public static void inset(GuiGraphics g, int x, int y, int w, int h, float a) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, mul(0xFF152D55, a));
        g.fill(x, y, x + w, y + h, mul(0xFF040814, a));
    }

    public static void tabBg(GuiGraphics g, int x, int y, int w, int h, boolean active, float hover, float a, float pulse) {
        if (active) {
            g.fill(x - 3, y - 3, x + w + 3, y + h + 3, argb(0.10f * a * (0.7f + 0.3f * pulse), BLUE));
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, mul(lerp(0xFF3F9BFF, 0xFF8FF0FF, pulse * 0.7f), a));
            g.fillGradient(x, y, x + w, y + h, mul(0xFF163E7E, a), mul(0xFF0C224B, a));
            g.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, argb(0.9f * a, CYAN));
        } else {
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, mul(lerp(0xFF172F57, 0xFF4F9BE8, hover), a));
            g.fillGradient(x, y, x + w, y + h, mul(lerp(0xFF0A1327, 0xFF12274B, hover), a),
                    mul(lerp(0xFF060C1B, 0xFF0A172E, hover), a));
        }
    }

    public static void pill(GuiGraphics g, int x, int y, int w, int h, float a) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, mul(0xFF224D96, a));
        g.fillGradient(x, y, x + w, y + h, mul(0xFF0A172E, a), mul(0xFF050C1B, a));
    }

    public static void button(GuiGraphics g, int x, int y, int w, int h, float hover, boolean enabled, float a) {
        int border = enabled ? lerp(0xFF2F8FE0, 0xFF8FF0FF, hover) : 0xFF121C30;
        int top = enabled ? lerp(0xFF194D94, 0xFF2D86E0, hover) : 0xFF0A1120;
        int bot = enabled ? lerp(0xFF0E2F64, 0xFF164D90, hover) : 0xFF070C19;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, mul(border, a));
        g.fillGradient(x, y, x + w, y + h, mul(top, a), mul(bot, a));
    }

    /** Рамка слота: (x, y) - левый верхний угол предмета 16x16. */
    public static void slotFrame(GuiGraphics g, int x, int y, float hover, boolean locked) {
        int bx = x - 1, by = y - 1;
        if (hover > 0.02f) g.fill(bx - 2, by - 2, bx + 20, by + 20, argb(0.16f * hover, CYAN));
        g.fill(bx, by, bx + 18, by + 18, lerp(locked ? 0xFF0E172A : 0xFF19335D, 0xFF5CE1FF, hover));
        g.fill(bx + 1, by + 1, bx + 17, by + 17, locked ? 0xFF060B17 : 0xFF070E20);
        g.fill(bx + 1, by + 1, bx + 17, by + 2, argb(0.05f, 0xFFFFFF));
    }

    public static void lock(GuiGraphics g, int x, int y) {
        int c = 0xFF2F4362;
        g.fill(x + 2, y, x + 3, y + 3, c);
        g.fill(x + 7, y, x + 8, y + 3, c);
        g.fill(x + 2, y, x + 8, y + 1, c);
        g.fill(x, y + 3, x + 10, y + 9, c);
        g.fill(x + 4, y + 5, x + 6, y + 7, 0xFF060B17);
    }

    public static void magnifier(GuiGraphics g, int x, int y, float a) {
        int c = mul(0xFF7FA8D8, a);
        ellipseRing(g, x + 4, y + 4, 4, 4, 1, c);
        g.fill(x + 7, y + 7, x + 9, y + 9, c);
        g.fill(x + 9, y + 9, x + 11, y + 11, c);
    }

    // ---------- эллипсы ----------
    public static void ellipseFill(GuiGraphics g, int cx, int cy, int rx, int ry, int color) {
        for (int dy = -ry; dy <= ry; dy++) {
            float oy = 1f - (float) (dy * dy) / (ry * ry);
            if (oy < 0) continue;
            int w = Math.round(rx * (float) Math.sqrt(oy));
            g.fill(cx - w, cy + dy, cx + w, cy + dy + 1, color);
        }
    }

    public static void ellipseRing(GuiGraphics g, int cx, int cy, int rx, int ry, int th, int color) {
        int irx = rx - th, iry = ry - th;
        for (int dy = -ry; dy <= ry; dy++) {
            float oy = 1f - (float) (dy * dy) / (ry * ry);
            if (oy < 0) continue;
            int oh = Math.round(rx * (float) Math.sqrt(oy));
            int ih = 0;
            if (iry > 0 && Math.abs(dy) < iry) {
                float iy = 1f - (float) (dy * dy) / (iry * iry);
                ih = Math.round(irx * (float) Math.sqrt(Math.max(0f, iy)));
            }
            if (ih <= 0) {
                g.fill(cx - oh, cy + dy, cx + oh, cy + dy + 1, color);
            } else {
                g.fill(cx - oh, cy + dy, cx - ih, cy + dy + 1, color);
                g.fill(cx + ih, cy + dy, cx + oh, cy + dy + 1, color);
            }
        }
    }

    /** Вращающееся пунктирное кольцо. */
    public static void dashedRing(GuiGraphics g, int cx, int cy, int rx, int ry, float t, float a) {
        int n = 28;
        for (int i = 0; i < n; i += 2) {
            double ang = i * Math.PI * 2 / n + t * 0.5;
            int x = cx + (int) Math.round(Math.cos(ang) * rx);
            int y = cy + (int) Math.round(Math.sin(ang) * ry);
            g.fill(x - 1, y, x + 2, y + 1, argb(0.75f * a, 0xCFEFFF));
        }
    }

    // ---------- фон экрана ----------
    public static void lightning(GuiGraphics g, int x, int h, float t, float a) {
        float flick = 0.75f + 0.25f * (float) Math.sin(t * 9.0) * (float) Math.sin(t * 3.1);
        for (int y = 0; y < h; y += 5) {
            float off = (float) (Math.sin(y * 0.045 + t * 1.1) * 5.0 + Math.sin(y * 0.13 - t * 2.3) * 2.5)
                    + (hash01(y * 131 + (int) (t * 9f)) - 0.5f) * 2.2f;
            int xx = x + Math.round(off);
            g.fill(xx - 4, y, xx + 6, y + 5, argb(0.04f * a * flick, 0x4FA8FF));
            g.fill(xx - 2, y, xx + 4, y + 5, argb(0.10f * a * flick, 0x6FC4FF));
            g.fill(xx, y, xx + 2, y + 5, argb(0.62f * a * flick, 0xE8F6FF));
        }
    }

    public static void particles(GuiGraphics g, int w, int h, float t, float a) {
        int span = h + 40;
        for (int i = 0; i < 46; i++) {
            float x = hash01(i * 31 + 7) * w;
            float base = hash01(i * 17 + 3) * span;
            float speed = 6f + hash01(i * 13 + 1) * 22f;
            float ph = hash01(i * 5 + 9) * 6.28f;
            int size = 2 + (int) (hash01(i * 3 + 2) * 3f);
            float y = ((base - t * speed) % span + span) % span - 20f;
            float tw = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(t * 1.7f + ph));
            int col = i % 3 == 0 ? 0xCFEFFF : i % 3 == 1 ? 0x4FE3FF : 0x3A9BFF;
            g.fill((int) x, (int) y, (int) x + size, (int) y + size, argb(0.38f * tw * a, col));
        }
    }

    public static String fmt(long v) {
        String s = Long.toString(Math.abs(v));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) sb.append(' ');
            sb.append(s.charAt(i));
        }
        return (v < 0 ? "-" : "") + sb;
    }

    // ---------- "оцифровка" ----------
    private static final String[] DIG = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

    /** Прозрачный цифровой дождь: колонки падающих цифр с затухающим шлейфом. */
    public static void digitRain(GuiGraphics g, Font font, int w, int h, float t, float a) {
        if (a < 0.03f) return;
        int colW = 13, rowH = 11, cols = w / colW + 1, rows = h / rowH + 2;
        float cycle = (rows + 12) * rowH;
        for (int c = 0; c < cols; c++) {
            float speed = 14f + hash01(c * 57 + 3) * 40f;
            float headY = (hash01(c * 91 + 11) * cycle + t * speed) % cycle - 4 * rowH;
            int trail = 6 + (int) (hash01(c * 13 + 5) * 8f);
            for (int k = 0; k < trail; k++) {
                int ry = Math.round((headY - k * rowH) / rowH);
                int y = ry * rowH;
                if (y < -rowH || y > h) continue;
                float fade = 1f - k / (float) trail;
                float al = (k == 0 ? 0.22f : 0.11f * fade) * a;
                if (al < 0.03f) continue;
                int step = (int) (t * 2.5f);
                float r = hash01(c * 977 + ry * 131 + step);
                String s = r < 0.55f ? (hash01(c * 31 + ry * 7 + step) < 0.5f ? "0" : "1") : DIG[(int) (r * 10f) % 10];
                g.drawString(font, s, c * colW + 2, y, argb(al, k == 0 ? 0xCFF4FF : CYAN), false);
            }
        }
    }

    /** Цифровое растворение блоками: p = 0 - всё закрыто, p = 1 - всё открыто. */
    public static void dissolve(GuiGraphics g, int x, int y, int w, int h, float p, int rgb) {
        int bs = 10;
        for (int by = 0; by < h; by += bs) {
            for (int bx = 0; bx < w; bx += bs) {
                float r = hash01((bx / bs) * 73 + (by / bs) * 151 + 17);
                float local = p * 1.4f - r * 0.4f;
                if (local >= 1f) continue;
                float al = 1f - Mth.clamp(local, 0f, 1f);
                int col = local > 0.45f && local < 0.8f ? argb(0.35f + 0.6f * al, CYAN) : argb(0.96f * al + 0.02f, rgb);
                g.fill(x + bx, y + by, Math.min(x + bx + bs - 1, x + w), Math.min(y + by + bs - 1, y + h), col);
            }
        }
    }

    public static void hudPanel(GuiGraphics g, int x, int y, int w, int h, float a) {
        g.fill(x - 3, y - 3, x + w + 3, y + h + 3, argb(0.07f * a, BLUE));
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, mul(0xFF224D96, a));
        g.fillGradient(x, y, x + w, y + h, mul(0xF0091328, a), mul(0xF0040A17, a));
        g.fill(x + 1, y + 1, x + w - 1, y + 2, argb(0.16f * a, 0x8FD0FF));
    }

    // ---------- атмосфера ----------
    /** Редкое мерцание "лампы": обычно около 1, изредка короткие провалы яркости. */
    public static float flicker(float t) {
        float k = (float) (Math.sin(t * 0.9) * Math.sin(t * 2.3 + 1.0));
        float dip = k > 0.82f ? (k - 0.82f) * 3.2f : 0f;
        float fast = (hash01((int) (t * 24f)) - 0.5f) * 0.05f;
        return Mth.clamp(1f - dip * (0.55f + 0.45f * hash01((int) (t * 18f))) + fast, 0.35f, 1f);
    }

    /** Тёмная виньетка по краям экрана. */
    public static void vignette(GuiGraphics g, int w, int h, float a) {
        int band = Math.max(40, h / 4), sideBand = Math.max(40, w / 6);
        int c = argb(0.62f * a, 0x000000);
        g.fillGradient(0, 0, w, band, c, 0x00000000);
        g.fillGradient(0, h - band, w, h, 0x00000000, c);
        g.fillGradient(0, 0, sideBand, h, c, 0x00000000);
        g.fillGradient(w - sideBand, 0, w, h, 0x00000000, c);
    }

    private Gfx() {}
}
