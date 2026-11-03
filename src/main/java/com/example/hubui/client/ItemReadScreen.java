package com.example.hubui.client;

import com.example.hubui.HubConfig;
import com.example.hubui.ModRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Отдельное окно "крупным планом": большая 3D-модель (можно вращать мышью) и полный текст описания
 * с прокруткой. Открывается клавишей чтения над предметом или кнопкой "Читать полностью".
 */
@OnlyIn(Dist.CLIENT)
public class ItemReadScreen extends Screen {
    private static final float S = 1.2f, LINE_H = 12f;

    private final Screen parent;
    private final ItemStack stack;
    private final List<Component> lines;
    private List<FormattedCharSequence> rows = new ArrayList<>();

    private final long openStart = Util.getMillis();
    private boolean closing;
    private long closeStart;
    private long lastFrame = openStart;
    private long lastScrollSound;

    private float anim, animPop;
    private float yaw = 30f, pitch = -16f;
    private long lastDragAt = -100000L;
    private boolean dragging;
    private float scroll, scrollTarget, closeHover;

    private int wx, wy, ww, wh;          // окно
    private int stX, stY, stW, stH;      // сцена с моделью
    private int txX, txY, txW, txH;      // область текста

    public ItemReadScreen(Screen parent, ItemStack stack) {
        super(Component.literal("Описание"));
        this.parent = parent;
        this.stack = stack;
        this.lines = FancyTooltip.lines(stack);
        UiSound.play(ModRegistry.UI_READ_OPEN, 1f);
        UiSound.ensureHum();
    }

    @Override
    protected void init() {
        ww = Math.min(width - 24, 420);
        wh = Math.min(height - 24, 250);
        wx = (width - ww) / 2;
        wy = (height - wh) / 2;
        stX = wx + 10;
        stY = wy + 34;
        stW = 134;
        stH = wh - 34 - 28;
        txX = wx + 152;
        txY = wy + 34;
        txW = ww - 152 - 12;
        txH = wh - 34 - 28;
        rows = new ArrayList<>();
        int wrap = (int) ((txW - 22) / S);
        for (int i = 1; i < lines.size(); i++) {
            Component c = lines.get(i);
            if (c.getString().isEmpty()) rows.add(FormattedCharSequence.EMPTY);
            else rows.addAll(font.split(c, wrap));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean interactive() {
        return !closing && anim > 0.85f;
    }

    private float maxScroll() {
        return Math.max(0f, rows.size() * LINE_H - (txH - 8));
    }

    @Override
    public void onClose() {
        if (closing) return;
        closing = true;
        closeStart = Util.getMillis();
        UiSound.play(ModRegistry.UI_READ_CLOSE, 1f);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = Util.getMillis();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        if (closing && now - closeStart >= 220L) {
            minecraft.setScreen(parent);
            return;
        }
        if (closing) {
            float t = Mth.clamp((now - closeStart) / 220f, 0f, 1f);
            anim = 1f - t * t;
            animPop = anim;
        } else {
            float t = Mth.clamp((now - openStart) / 380f, 0f, 1f);
            anim = Gfx.easeOutCubic(t);
            animPop = Gfx.easeOutBack(t);
        }
        float time = now / 1000f;
        int acc = FancyTooltip.accent(stack);

        scrollTarget = Mth.clamp(scrollTarget, 0f, maxScroll());
        scroll += (scrollTarget - scroll) * Math.min(1f, dt * 14f);
        if (!dragging && now - lastDragAt > 1600L) yaw += dt * 32f;

        // ---- фон ----
        g.fillGradient(0, 0, width, height, Gfx.argb(0.80f * anim, 0x02050C), Gfx.argb(0.93f * anim, 0x000206));
        if (HubConfig.digitalRain()) Gfx.digitRain(g, font, width, height, time, anim);
        Gfx.vignette(g, width, height, anim);

        PoseStack ps = g.pose();
        ps.pushPose();
        float sc = 0.90f + 0.10f * animPop;
        ps.translate(width / 2f, height / 2f + (1f - anim) * 16f, 0);
        ps.scale(sc, sc, 1f);
        ps.translate(-width / 2f, -height / 2f, 0);

        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 1.6f);
        Gfx.panel(g, wx, wy, ww, wh, pulse, anim);

        // ---- заголовок ----
        float dp = Mth.clamp((now - openStart - 80f) / 520f, 0f, 1f);
        String title = lines.isEmpty() ? stack.getHoverName().getString() : lines.get(0).getString();
        String shownTitle = FancyTooltip.scramble(title, dp, now);
        ps.pushPose();
        ps.translate(wx + 14, wy + 10, 0);
        ps.scale(1.4f, 1.4f, 1f);
        g.drawString(font, font.plainSubstrByWidth(shownTitle, (int) ((ww - 70) / 1.4f)), 0, 0,
                Gfx.argb(anim, dp < 1f ? acc : 0xFFFFFF), false);
        ps.popPose();
        g.fill(wx + 10, wy + 28, wx + ww - 10, wy + 29, Gfx.argb(0.5f * anim, acc));

        // кнопка закрытия
        int cbx = wx + ww - 30, cby = wy + 8;
        boolean over = interactive() && HubScreen.hit(mx, my, cbx, cby, 20, 16);
        closeHover += ((over ? 1f : 0f) - closeHover) * Math.min(1f, dt * 16f);
        Gfx.button(g, cbx, cby, 20, 16, closeHover, true, anim);
        g.drawString(font, "X", cbx + 10 - font.width("X") / 2, cby + 4, Gfx.argb(anim, 0xFFFFFF), false);

        // ---- сцена с моделью ----
        Gfx.inset(g, stX, stY, stW, stH, anim);
        for (int i = 1; i < stW / 12; i++) g.fill(stX + i * 12, stY, stX + i * 12 + 1, stY + stH, Gfx.argb(0.05f * anim, Gfx.CYAN));
        for (int i = 1; i < stH / 12; i++) g.fill(stX, stY + i * 12, stX + stW, stY + i * 12 + 1, Gfx.argb(0.05f * anim, Gfx.CYAN));
        int mcx = stX + stW / 2, floorY = stY + stH - 22;
        g.fillGradient(mcx - 34, stY + 8, mcx + 34, floorY, Gfx.argb(0f, Gfx.CYAN), Gfx.argb(0.13f * anim, Gfx.CYAN));
        Gfx.ellipseFill(g, mcx, floorY, 42, 9, Gfx.argb(0.2f * anim, Gfx.BLUE));
        Gfx.ellipseRing(g, mcx, floorY, 42, 9, 1, Gfx.argb((0.55f + 0.35f * pulse) * anim, acc));
        Gfx.dashedRing(g, mcx, floorY, 52, 12, time, anim);
        float scanY = stY + (time * 0.5f % 1f) * stH;
        g.fill(stX, (int) scanY, stX + stW, (int) scanY + 1, Gfx.argb(0.3f * anim, Gfx.CYAN));
        float size = Math.min(stW - 20, stH - 40) * 0.62f;
        float bob = (float) Math.sin(time * 1.8f) * 2f;
        ItemModel3D.render(g, stack, mcx, (stY + floorY) / 2f - 4f + bob, size, yaw, pitch, 80f);
        g.drawCenteredString(font, "тяни мышью — вращать", mcx, stY + stH - 11, Gfx.argb(0.7f * anim, Gfx.MUTED));

        // ---- текст ----
        Gfx.inset(g, txX, txY, txW, txH, anim);
        float head = (now - openStart - 200f) / 1000f * 30f;
        float top = txY + 4, bottom = txY + txH - 4;
        for (int i = 0; i < rows.size(); i++) {
            float y = top + i * LINE_H - scroll;
            if (y < top - LINE_H || y > bottom) continue;
            FormattedCharSequence row = rows.get(i);
            if (row == FormattedCharSequence.EMPTY) continue;
            float ra = Mth.clamp(head - i, 0f, 1f);
            float fa = Mth.clamp((y - top + 6f) / 12f, 0f, 1f) * Mth.clamp((bottom - y - LINE_H + 6f) / 12f, 0f, 1f);
            float al = ra * fa * anim;
            if (al < 0.03f) continue;
            ps.pushPose();
            ps.translate(txX + 8 + (1f - ra) * 8f, y, 0);
            ps.scale(S, S, 1f);
            g.drawString(font, row, 0, 0, Gfx.argb(al, 0xC4D2EA), false);
            ps.popPose();
            if (ra > 0f && ra < 1f) g.fill(txX + 6, (int) y, txX + 8, (int) y + 9, Gfx.argb(0.8f * anim, Gfx.CYAN));
        }
        float ms = maxScroll();
        if (ms > 0f) {
            int tx = txX + txW - 8, ty0 = txY + 4, th = txH - 8;
            g.fill(tx, ty0, tx + 3, ty0 + th, Gfx.argb(0.5f * anim, 0x152D55));
            int thumb = Math.max(16, Math.round(th * (th / (th + ms))));
            int off = Math.round((th - thumb) * (scroll / ms));
            g.fill(tx, ty0 + off, tx + 3, ty0 + off + thumb, Gfx.argb(anim, Gfx.CYAN));
        }

        // ---- подвал ----
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String info = id + (stack.getCount() > 1 ? "  ×" + stack.getCount() : "");
        g.drawString(font, info, wx + 12, wy + wh - 17, Gfx.argb(0.8f * anim, Gfx.MUTED), false);
        String hint = "Колесо — прокрутка   ·   Esc — закрыть";
        g.drawString(font, hint, wx + ww - 12 - font.width(hint), wy + wh - 17, Gfx.argb(0.8f * anim, Gfx.MUTED), false);

        ps.popPose();
    }

    // ---------- ввод ----------
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!interactive()) return true;
        if (button == 0) {
            if (HubScreen.hit(mx, my, wx + ww - 30, wy + 8, 20, 16)) {
                onClose();
                return true;
            }
            if (HubScreen.hit(mx, my, stX, stY, stW, stH)) {
                dragging = true;
                lastDragAt = Util.getMillis();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            yaw += (float) dx * 1.2f;
            pitch = Mth.clamp(pitch + (float) dy * 0.6f, -65f, 65f);
            lastDragAt = Util.getMillis();
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scrollBy((float) -delta * 30f);
        return true;
    }

    private void scrollBy(float d) {
        float before = scrollTarget;
        scrollTarget = Mth.clamp(scrollTarget + d, 0f, maxScroll());
        long now = Util.getMillis();
        if (scrollTarget != before && now - lastScrollSound > 70L) {
            lastScrollSound = now;
            UiSound.play(ModRegistry.UI_DECODE, 1f, 0.4f);
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (minecraft.options.keyInventory.matches(key, scan) || KeyBindings.READ.matches(key, scan)) {
            onClose();
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_DOWN -> scrollBy(LINE_H * 2);
            case GLFW.GLFW_KEY_UP -> scrollBy(-LINE_H * 2);
            case GLFW.GLFW_KEY_PAGE_DOWN -> scrollBy(txH * 0.8f);
            case GLFW.GLFW_KEY_PAGE_UP -> scrollBy(-txH * 0.8f);
            default -> {
                return super.keyPressed(key, scan, mods);
            }
        }
        return true;
    }
}
