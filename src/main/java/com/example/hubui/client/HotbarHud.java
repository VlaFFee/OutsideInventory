package com.example.hubui.client;

import com.example.hubui.HubConfig;
import com.example.hubui.HubUiMod;
import com.example.hubui.ModRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Свой хотбар на 5 ячеек: оцифрованная панель, плавное скольжение выделения, цифры-подсказки. */
@Mod.EventBusSubscriber(modid = HubUiMod.MODID, value = Dist.CLIENT)
public final class HotbarHud {
    private static final int SLOTS = 5, STEP = 22;
    private static float selX = -1f;
    private static int lastSel = -1;
    private static long lastFrame = Util.getMillis();

    static boolean active(Minecraft mc) {
        return HubConfig.customHotbar() && ClientHub.limited && mc.player != null
                && !mc.player.isCreative() && !mc.player.isSpectator();
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre e) {
        if (e.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        Minecraft mc = Minecraft.getInstance();
        if (!active(mc)) return;
        e.setCanceled(true);
        draw(e.getGuiGraphics(), e.getPartialTick(), mc, mc.player);
    }

    private static void draw(GuiGraphics g, float pt, Minecraft mc, Player p) {
        long now = Util.getMillis();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float t = now / 1000f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 3f);

        int sw = g.guiWidth(), sh = g.guiHeight();
        int bw = SLOTS * STEP + 8, bh = 26;
        int x0 = sw / 2 - bw / 2, y0 = sh - bh - 2;
        Inventory inv = p.getInventory();
        PoseStack ps = g.pose();

        Gfx.hudPanel(g, x0, y0, bw, bh, 1f);

        float target = x0 + 4 + inv.selected * STEP;
        if (selX < 0f) selX = target;
        selX += (target - selX) * Math.min(1f, dt * 18f);

        for (int i = 0; i < SLOTS; i++) {
            int sx = x0 + 4 + i * STEP;
            g.fill(sx, y0 + 3, sx + 20, y0 + 23, 0xFF152D55);
            g.fill(sx + 1, y0 + 4, sx + 19, y0 + 22, 0xFF070E20);
        }
        // выделение
        int hx = Math.round(selX);
        g.fill(hx - 2, y0 + 1, hx + 22, y0 + 25, Gfx.argb(0.12f + 0.08f * pulse, Gfx.CYAN));
        g.renderOutline(hx, y0 + 3, 20, 20, Gfx.argb(1f, Gfx.CYAN));
        g.fill(hx + 2, y0 + 24, hx + 18, y0 + 25, Gfx.argb(0.8f, 0xFFFFFF));

        for (int i = 0; i < SLOTS; i++) {
            int sx = x0 + 4 + i * STEP;
            ItemStack st = inv.items.get(i);
            ps.pushPose();
            ps.translate(sx + 2, y0 + 4, 0);
            ps.scale(0.6f, 0.6f, 1f);
            g.drawString(mc.font, String.valueOf(i + 1), 0, 0, Gfx.argb(0.55f, Gfx.SOFT), false);
            ps.popPose();
            if (st.isEmpty()) continue;
            float pop = st.getPopTime() - pt;
            int ix = sx + 2, iy = y0 + 5;
            if (pop > 0f) {
                float f1 = 1f + pop / 5f;
                ps.pushPose();
                ps.translate(ix + 8, iy + 12, 0);
                ps.scale(1f / f1, (f1 + 1f) / 2f, 1f);
                ps.translate(-(ix + 8), -(iy + 12), 0);
            }
            g.renderItem(p, st, ix, iy, i + 1);
            if (pop > 0f) ps.popPose();
            g.renderItemDecorations(mc.font, st, ix, iy);
        }

        ItemStack off = p.getOffhandItem();
        if (!off.isEmpty()) {
            int ox = x0 - 28;
            Gfx.hudPanel(g, ox, y0, 24, bh, 1f);
            g.fill(ox + 2, y0 + 3, ox + 22, y0 + 23, 0xFF152D55);
            g.fill(ox + 3, y0 + 4, ox + 21, y0 + 22, 0xFF070E20);
            g.renderItem(p, off, ox + 4, y0 + 5, 9);
            g.renderItemDecorations(mc.font, off, ox + 4, y0 + 5);
        }
    }

    /** Колесо мыши крутит только 5 ячеек. */
    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (!active(mc) || mc.screen != null) return;
        double d = e.getScrollDelta();
        if (d == 0) return;
        Inventory inv = mc.player.getInventory();
        inv.selected = Math.floorMod(inv.selected - (int) Math.signum(d), SLOTS);
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (!active(mc)) {
            lastSel = -1;
            return;
        }
        Inventory inv = mc.player.getInventory();
        if (inv.selected >= SLOTS) inv.selected = lastSel >= 0 ? lastSel : 0;   // цифры 6-9 не работают
        if (mc.screen == null && lastSel != -1 && inv.selected != lastSel) {
            UiSound.play(ModRegistry.UI_SELECT, 1f + inv.selected * 0.06f, 0.5f);
        }
        lastSel = inv.selected;
    }

    private HotbarHud() {}
}
