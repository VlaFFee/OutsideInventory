package com.example.hubui.client;

import com.example.hubui.HubMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Вкладка "Инвентарь": модель игрока, показатели, броня, левая рука и нижний ряд (карманы + хотбар). */
@OnlyIn(Dist.CLIENT)
final class InventoryView {
    private final HubScreen s;
    private float hp, armor, food;
    private boolean init;

    InventoryView(HubScreen s) {
        this.s = s;
    }

    void renderBg(GuiGraphics g, long now) {
        int L = s.L(), T = s.T();
        float a = s.anim, t = now / 1000f;
        Font f = s.font();
        Player p = s.mc().player;
        PoseStack ps = g.pose();

        // колонка брони и левая рука
        Gfx.inset(g, L + HubMenu.ARMOR_X - 5, T + HubMenu.ARMOR_Y - 5, 26, 3 * HubMenu.ARMOR_STEP + 26, a);
        Gfx.inset(g, L + HubMenu.OFF_X - 5, T + HubMenu.OFF_Y - 5, 26, 26, a);

        // пьедестал и модель игрока
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 1.4f);
        int cx = L + 150, fy = T + 148;
        Gfx.ellipseFill(g, cx, fy, 38, 9, Gfx.argb((0.10f + 0.06f * pulse) * a, Gfx.BLUE));
        Gfx.ellipseRing(g, cx, fy, 38, 9, 1, Gfx.argb((0.45f + 0.35f * pulse) * a, Gfx.CYAN));
        Gfx.dashedRing(g, cx, fy, 46, 11, t, a);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, cx, fy - 2, 58,
                (float) (cx - s.rmx), (float) (fy - 2 - 90 - s.rmy), p);
        g.drawCenteredString(f, p.getName().getString(), cx, T + 10, Gfx.argb(a, Gfx.TEXT));

        // показатели (оцифрованные полосы)
        if (!init) {
            hp = frac(p.getHealth(), p.getMaxHealth());
            armor = frac(p.getArmorValue(), 20f);
            food = frac(p.getFoodData().getFoodLevel(), 20f);
            init = true;
        }
        float k = Math.min(1f, s.dt * 6f);
        hp += (frac(p.getHealth(), p.getMaxHealth()) - hp) * k;
        armor += (frac(p.getArmorValue(), 20f) - armor) * k;
        food += (frac(p.getFoodData().getFoodLevel(), 20f) - food) * k;
        int sx = L + 196, sy = T + 34;
        stat(g, f, sx, sy, "ЗДОРОВЬЕ", Math.round(p.getHealth()) + "/" + Math.round(p.getMaxHealth()), hp, 0xFF6A8A, a);
        stat(g, f, sx, sy + 26, "БРОНЯ", String.valueOf(p.getArmorValue()), armor, 0x6FC4FF, a);
        stat(g, f, sx, sy + 52, "СЫТОСТЬ", String.valueOf(p.getFoodData().getFoodLevel()), food, 0xFFC857, a);

        // подписи нижнего ряда
        label(g, f, "КАРМАНЫ", L + 78, T + 167, a);
        label(g, f, "ХОТБАР", L + 150, T + 167, a);
        label(g, f, "КАРМАНЫ", L + 222, T + 167, a);
        label(g, f, "ЛЕВАЯ РУКА", L + HubMenu.OFF_X + 8, T + HubMenu.OFF_Y + 24, a);
    }

    private static float frac(float v, float max) {
        return max <= 0f ? 0f : Math.max(0f, Math.min(1f, v / max));
    }

    private void stat(GuiGraphics g, Font f, int x, int y, String label, String value, float frac, int rgb, float a) {
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x, y, 0);
        ps.scale(0.75f, 0.75f, 1f);
        g.drawString(f, label, 0, 0, Gfx.argb(a, Gfx.MUTED), false);
        ps.popPose();
        g.drawString(f, value, x + 80 - f.width(value), y, Gfx.argb(a, rgb), false);
        g.fill(x, y + 11, x + 80, y + 15, Gfx.argb(0.9f * a, 0x0A172E));
        g.fill(x, y + 11, x + Math.round(80 * frac), y + 15, Gfx.argb(a, rgb));
        for (int i = 1; i < 10; i++) g.fill(x + i * 8, y + 11, x + i * 8 + 1, y + 15, 0x66040814);
    }

    private void label(GuiGraphics g, Font f, String text, int cx, int y, float a) {
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx - f.width(text) * 0.35f, y, 0);
        ps.scale(0.7f, 0.7f, 1f);
        g.drawString(f, text, 0, 0, Gfx.argb(0.8f * a, Gfx.MUTED), false);
        ps.popPose();
    }
}
