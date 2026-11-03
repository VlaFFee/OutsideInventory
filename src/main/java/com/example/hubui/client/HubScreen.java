package com.example.hubui.client;

import com.example.hubui.HubConfig;
import com.example.hubui.HubMenu;
import com.example.hubui.ModRegistry;
import com.example.hubui.network.NetworkHandler;
import com.example.hubui.network.SetTabPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Хаб: Инвентарь / Хранилище / Создание / Журнал.
 * Слоты и клики обрабатывает ванильный AbstractContainerScreen, а вся графика и анимации - свои.
 * В ванильный render() передаются "далёкие" координаты мыши, чтобы он не рисовал белую подсветку;
 * наведение, свечение и подсказки считаются здесь по настоящей мыши.
 */
@OnlyIn(Dist.CLIENT)
public class HubScreen extends AbstractContainerScreen<HubMenu> {
    public static final ResourceLocation COIN = new ResourceLocation("hubui", "textures/gui/coin.png");

    static final int PW = 300, PH = 200, TAB_AREA = 28, TAB_H = 22, FAR = -10000;
    static final float OPEN_MS = 420f, CLOSE_MS = 240f;
    static final String[] TAB_NAMES = {"Инвентарь", "Хранилище", "Создание", "Журнал"};

    private final long openStart = Util.getMillis();
    private boolean closing;
    private long closeStart;
    private long lastFrame = Util.getMillis();

    int tab;
    private long tabChangedAt = Util.getMillis() + 120L;   // содержимое "оцифровывается" сразу после появления панели
    private final float[] tabW = {26, 26, 26, 26};
    private final float[] tabHover = new float[4];
    final int[] tabHitX = new int[4];
    final int[] tabHitW = new int[4];
    int tabHitY;
    private int pillX, pillW;
    private final ItemStack[] tabIcons = {
            new ItemStack(Items.LEATHER_CHESTPLATE), new ItemStack(Items.CHEST),
            new ItemStack(Items.ANVIL), new ItemStack(Items.WRITABLE_BOOK)};

    float anim, animPop, dt;
    int rmx, rmy;
    Slot hover;
    private Slot lastHover;
    private final Map<Integer, Float> slotHover = new HashMap<>();
    private float shownBalance = -1f;

    String storageQuery = "", craftQuery = "";
    EditBox storageBox, craftBox;
    final InventoryView invView = new InventoryView(this);
    final StorageView storageView = new StorageView(this);
    final CraftView craftView = new CraftView(this);
    final JournalView journalView = new JournalView(this);

    public HubScreen(HubMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PW;
        this.imageHeight = PH;
        UiSound.play(ModRegistry.UI_OPEN, 1f);
        UiSound.ensureHum();
    }

    // ---------- доступ для вкладок ----------
    int L() { return leftPos; }
    int T() { return topPos; }
    Font font() { return font; }
    Minecraft mc() { return minecraft; }
    boolean interactive() { return !closing && anim > 0.85f; }

    boolean isTextFocused() {
        return (tab == HubMenu.TAB_STORAGE && storageBox != null && storageBox.isFocused())
                || (tab == HubMenu.TAB_CRAFT && craftBox != null && craftBox.isFocused());
    }

    static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ---------- инициализация ----------
    @Override
    protected void init() {
        super.init();
        leftPos = (width - PW) / 2;
        topPos = (height - (TAB_AREA + PH)) / 2 + TAB_AREA;
        storageBox = makeBox(storageQuery, "Поиск...", leftPos + 32, topPos + 15, 66, v -> storageQuery = v);
        craftBox = makeBox(craftQuery, "Найти рецепт...", leftPos + 32, topPos + 15, 122, v -> {
            craftQuery = v;
            craftView.dirty = true;
        });
        getMenu().tab = tab;
        applyTabVisibility();
    }

    private EditBox makeBox(String value, String hint, int x, int y, int w, Consumer<String> onChange) {
        EditBox b = new EditBox(font, x, y, w, 10, Component.empty());
        b.setBordered(false);
        b.setTextColor(0xE6F2FF);
        b.setMaxLength(40);
        b.setResponder(v -> {
            b.setSuggestion(v.isEmpty() ? hint : null);
            onChange.accept(v);
        });
        b.setValue(value);
        b.setSuggestion(value.isEmpty() ? hint : null);
        addWidget(b);
        return b;
    }

    private void applyTabVisibility() {
        storageBox.visible = tab == HubMenu.TAB_STORAGE;
        craftBox.visible = tab == HubMenu.TAB_CRAFT;
        if (!storageBox.visible) storageBox.setFocused(false);
        if (!craftBox.visible) craftBox.setFocused(false);
    }

    void setTab(int t, boolean sound) {
        if (t == tab) return;
        tab = t;
        tabChangedAt = Util.getMillis();
        getMenu().tab = t;
        NetworkHandler.CHANNEL.sendToServer(new SetTabPacket(t));
        setFocused(null);
        applyTabVisibility();
        if (t == HubMenu.TAB_CRAFT) craftView.dirty = true;
        if (t == HubMenu.TAB_JOURNAL) journalView.onShow();
        if (sound) UiSound.play(ModRegistry.UI_GLITCH, 1f);
    }

    // ---------- закрытие ----------
    @Override
    public void onClose() {
        if (closing) return;
        closing = true;
        closeStart = Util.getMillis();
        UiSound.play(ModRegistry.UI_CLOSE, 1f);
    }

    // ---------- главный цикл отрисовки ----------
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = Util.getMillis();
        dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        if (closing && now - closeStart >= CLOSE_MS) {
            super.onClose();
            return;
        }
        if (closing) {
            float t = Mth.clamp((now - closeStart) / CLOSE_MS, 0f, 1f);
            anim = 1f - t * t;
            animPop = anim;
        } else {
            float t = Mth.clamp((now - openStart) / OPEN_MS, 0f, 1f);
            anim = Gfx.easeOutCubic(t);
            animPop = Gfx.easeOutBack(t);
        }
        rmx = mx;
        rmy = my;
        hover = interactive() ? slotAt(mx, my) : null;
        if (hover != lastHover) {
            if (hover != null && hover.hasItem()) UiSound.play(ModRegistry.UI_HOVER, 1f, 0.35f);
            lastHover = hover;
        }
        stepAnimations(now);
        drawBackdrop(g, now);

        PoseStack ps = g.pose();
        ps.pushPose();
        float sc = 0.90f + 0.10f * animPop;
        float cx = leftPos + PW / 2f, cy = topPos + PH / 2f;
        ps.translate(cx, cy + (1f - anim) * 18f, 0);
        ps.scale(sc, sc, 1f);
        ps.translate(-cx, -cy, 0);

        super.render(g, FAR, FAR, pt);   // renderBg + предметы в слотах
        this.hoveredSlot = hover;

        // оверлеи поверх предметов
        ps.pushPose();
        ps.translate(0, 0, 300);
        if (tab == HubMenu.TAB_STORAGE) storageView.overlay(g);
        float sw = Mth.clamp((now - tabChangedAt) / 300f, 0f, 1f);
        if (sw < 1f) Gfx.dissolve(g, leftPos, topPos, PW, PH, sw, 0x060E1D);   // "оцифровка" смены вкладки
        ps.popPose();
        ps.popPose();

        renderTopLayer(g, mx, my);
    }

    private void renderTopLayer(GuiGraphics g, int mx, int my) {
        ItemStack carried = getMenu().getCarried();
        PoseStack ps = g.pose();
        if (!carried.isEmpty()) {
            ps.pushPose();
            ps.translate(0, 0, 232);
            g.renderItem(carried, mx - 8, my - 8);
            g.renderItemDecorations(font, carried, mx - 8, my - 8, null);
            ps.popPose();
        } else if (interactive()) {
            if (hover != null && hover.hasItem() && tab != HubMenu.TAB_STORAGE) {
                g.renderTooltip(font, hover.getItem(), mx, my);   // рисуется оцифрованной подсказкой (TooltipEvents)
            } else if (hit(mx, my, pillX, tabHitY, pillW, TAB_H)) {
                g.renderTooltip(font, Component.literal("Влаффты: " + Gfx.fmt(ClientHub.balance)), mx, my);
            }
        }
    }

    private void stepAnimations(long now) {
        float k = Math.min(1f, dt * 14f);
        for (int i = 0; i < 4; i++) {
            float target = i == tab ? 34f + font.width(TAB_NAMES[i]) + 8f : 26f;
            tabW[i] += (target - tabW[i]) * k;
            boolean over = interactive() && hit(rmx, rmy, tabHitX[i], tabHitY, tabHitW[i], TAB_H);
            tabHover[i] += ((over ? 1f : 0f) - tabHover[i]) * Math.min(1f, dt * 16f);
        }
        int x = leftPos;
        tabHitY = topPos - TAB_AREA;
        for (int i = 0; i < 4; i++) {
            tabHitX[i] = x;
            tabHitW[i] = Math.round(tabW[i]);
            x += tabHitW[i] + 4;
        }
        for (Slot sl : getMenu().slots) {
            float cur = slotHover.getOrDefault(sl.index, 0f);
            float target = sl == hover ? 1f : 0f;
            cur += (target - cur) * Math.min(1f, dt * 16f);
            if (cur < 0.01f && target == 0f) slotHover.remove(sl.index);
            else slotHover.put(sl.index, cur);
        }
        if (shownBalance < 0f) shownBalance = ClientHub.balance;
        shownBalance += (ClientHub.balance - shownBalance) * Math.min(1f, dt * 7f);
        if (Math.abs(ClientHub.balance - shownBalance) < 0.5f) shownBalance = ClientHub.balance;
    }

    private Slot slotAt(double mx, double my) {
        for (Slot sl : getMenu().slots) {
            if (sl.isActive() && hit(mx, my, leftPos + sl.x - 1, topPos + sl.y - 1, 18, 18)) return sl;
        }
        return null;
    }

    // ---------- фон ----------
    private void drawBackdrop(GuiGraphics g, long now) {
        float t = now / 1000f;
        float fl = Gfx.flicker(t);
        g.fillGradient(0, 0, width, height, Gfx.argb(0.88f * anim, 0x02060F), Gfx.argb(0.97f * anim, 0x010308));
        for (int i = 6; i >= 1; i--) {
            g.fill(leftPos - i * 16, topPos - TAB_AREA - i * 10, leftPos + PW + i * 16, topPos + PH + i * 10,
                    Gfx.argb(0.016f * anim * fl, 0x2A7BFF));
        }
        if (HubConfig.digitalRain()) Gfx.digitRain(g, font, width, height, t, anim);   // прозрачные цифры
        int lx = Math.max(10, Math.min(26, leftPos / 3));
        Gfx.lightning(g, lx, height, t, anim * fl);
        Gfx.lightning(g, width - lx, height, t + 3.7f, anim * fl);
        Gfx.particles(g, width, height, t, anim);
        Gfx.vignette(g, width, height, anim);
    }

    // ---------- панель ----------
    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        long now = Util.getMillis();
        float pulse = (0.5f + 0.5f * (float) Math.sin(now / 1000f * 1.6f)) * Gfx.flicker(now / 1000f);
        Gfx.panel(g, leftPos, topPos, PW, PH, pulse, anim);
        switch (tab) {
            case HubMenu.TAB_INV -> invView.renderBg(g, now);
            case HubMenu.TAB_STORAGE -> storageView.renderBg(g, now);
            case HubMenu.TAB_CRAFT -> craftView.renderBg(g, now);
            default -> journalView.renderBg(g, now);
        }
        drawSlotFrames(g, now);
        drawTabs(g, now, pulse);
        drawCurrency(g, now);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        // ванильные подписи не нужны
    }

    private void drawSlotFrames(GuiGraphics g, long now) {
        Inventory inv = minecraft.player.getInventory();
        float pulse = 0.5f + 0.5f * (float) Math.sin(now / 1000f * 3f);
        for (Slot sl : getMenu().slots) {
            if (!sl.isActive()) continue;
            int x = leftPos + sl.x, y = topPos + sl.y;
            float h = slotHover.getOrDefault(sl.index, 0f);
            boolean selected = sl.index < HubMenu.HOTBAR_COUNT && inv.selected == sl.index;
            boolean side = sl.index >= HubMenu.SIDE_START && sl.index < HubMenu.INV_END;
            boolean locked = sl instanceof HubMenu.StorageSlot ss && !ss.isUnlocked();
            Gfx.slotFrame(g, x, y, Math.max(h, selected ? 0.35f + 0.25f * pulse : 0f), locked);
            if (locked) Gfx.lock(g, x + 3, y + 3);
            if (side) {   // боковые карманы: цифровые уголки
                int c = Gfx.argb(0.9f, Gfx.CYAN);
                g.fill(x - 1, y - 1, x + 3, y, c);
                g.fill(x - 1, y - 1, x, y + 3, c);
                g.fill(x + 13, y + 16, x + 17, y + 17, c);
                g.fill(x + 16, y + 13, x + 17, y + 17, c);
            }
        }
    }

    private void drawTabs(GuiGraphics g, long now, float pulse) {
        PoseStack ps = g.pose();
        for (int i = 0; i < 4; i++) {
            float p = Mth.clamp((now - openStart - i * 70L) / 300f, 0f, 1f);
            float e = Gfx.easeOutCubic(p);
            float alpha = e * anim;
            int x = tabHitX[i], y = tabHitY + Math.round(-(1f - e) * 14f), w = tabHitW[i];
            boolean act = i == tab;
            Gfx.tabBg(g, x, y, w, TAB_H, act, tabHover[i], alpha, pulse);

            float sc = 1f + 0.18f * tabHover[i];
            float icx = act ? x + 13 : x + w / 2f;
            ps.pushPose();
            ps.translate(icx, y + 11, 0);
            ps.scale(sc, sc, 1f);
            ps.translate(-8, -8, 0);
            g.renderItem(tabIcons[i], 0, 0);
            ps.popPose();

            if (act) {
                float target = 34f + font.width(TAB_NAMES[i]) + 8f;
                float la = Mth.clamp((w - 30f) / (target - 30f), 0f, 1f);
                if (la > 0.2f) g.drawString(font, TAB_NAMES[i], x + 27, y + 7, Gfx.argb(alpha * la, Gfx.TEXT), false);
            }
            if (i == HubMenu.TAB_JOURNAL && ClientHub.hasUnread() && !act) {
                float pu = 0.5f + 0.5f * (float) Math.sin(now / 1000f * 5f);
                g.fill(x + w - 8, y + 2, x + w - 3, y + 7, Gfx.argb(0.25f * alpha * pu, Gfx.CYAN));
                g.fill(x + w - 7, y + 3, x + w - 4, y + 6, Gfx.argb(alpha, 0xFFFFFF));
            }
        }
    }

    private void drawCurrency(GuiGraphics g, long now) {
        String txt = Gfx.fmt(Math.round(shownBalance));
        pillW = Math.max(72, 32 + font.width(txt) + 8);
        pillX = leftPos + PW - pillW;
        float p = Mth.clamp((now - openStart - 280L) / 300f, 0f, 1f);
        float e = Gfx.easeOutCubic(p);
        float alpha = e * anim;
        int y = tabHitY + Math.round(-(1f - e) * 14f);
        PoseStack ps = g.pose();

        Gfx.pill(g, pillX, y, pillW, TAB_H, alpha);

        float st = (now - ClientHub.changedAt) / 800f;
        float glow = 0.10f + 0.06f * (float) Math.sin(now / 1000f * 2f);
        Gfx.ellipseFill(g, pillX + 14, y + 11, 11, 11, Gfx.argb(glow * alpha, Gfx.CYAN));
        float sx = st < 1f ? Math.abs((float) Math.cos(st * Math.PI * 4)) : 1f;
        ps.pushPose();
        ps.translate(pillX + 14, y + 11, 0);
        ps.scale(Math.max(0.12f, sx), 1f, 1f);
        ps.translate(-8, -8, 0);
        g.blit(COIN, 0, 0, 16, 16, 0, 0, 16, 16, 16, 16);
        ps.popPose();

        float bump = st < 0.5f ? (1f - st * 2f) * 0.25f : 0f;
        int tcol = Gfx.lerp(0xFF000000 | Gfx.TEXT, 0xFF000000 | (ClientHub.lastDelta >= 0 ? Gfx.GREEN : Gfx.RED), st < 1f ? 1f - st : 0f);
        ps.pushPose();
        ps.translate(pillX + 28, y + 11, 0);
        ps.scale(1f + bump, 1f + bump, 1f);
        ps.translate(0, -4, 0);
        g.drawString(font, txt, 0, 0, Gfx.mul(tcol, alpha), false);
        ps.popPose();

        if (st < 1.5f) {
            String d = (ClientHub.lastDelta >= 0 ? "+" : "") + Gfx.fmt(ClientHub.lastDelta);
            float fa = 1f - st / 1.5f;
            g.drawString(font, d, pillX + pillW / 2 - font.width(d) / 2, y - 6 - Math.round(st * 10f),
                    Gfx.argb(fa * alpha, ClientHub.lastDelta >= 0 ? Gfx.GREEN : Gfx.RED), false);
        }
    }

    // ---------- ввод ----------
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!interactive()) return true;
        if (button == 0) {
            for (int i = 0; i < 4; i++) {
                if (hit(mx, my, tabHitX[i], tabHitY, tabHitW[i], TAB_H)) {
                    setTab(i, true);
                    return true;
                }
            }
        }
        boolean handled = switch (tab) {
            case HubMenu.TAB_STORAGE -> storageView.click(mx, my, button);
            case HubMenu.TAB_CRAFT -> craftView.click(mx, my, button);
            case HubMenu.TAB_JOURNAL -> journalView.click(mx, my, button);
            default -> false;
        };
        return handled || super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (interactive()) {
            if (tab == HubMenu.TAB_CRAFT && craftView.scroll(mx, my, delta)) return true;
            if (tab == HubMenu.TAB_JOURNAL && journalView.scroll(mx, my, delta)) return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        EditBox active = tab == HubMenu.TAB_STORAGE ? storageBox : tab == HubMenu.TAB_CRAFT ? craftBox : null;
        if (active != null && active.isFocused() && key != GLFW.GLFW_KEY_ESCAPE) {
            active.keyPressed(key, scan, mods);   // печатаем в поиск, а не закрываем окно клавишей инвентаря
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
