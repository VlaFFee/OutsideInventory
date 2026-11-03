package com.example.hubui.client;

import com.example.hubui.HubConfig;
import com.example.hubui.HubUiMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HubUiMod.MODID, value = Dist.CLIENT)
public final class TooltipEvents {

    /** Все подсказки предметов (в том числе в чужих окнах) рисуются нашей оцифрованной подсказкой. */
    @SubscribeEvent
    public static void onTooltip(RenderTooltipEvent.Pre e) {
        if (!HubConfig.fancyTooltips()) return;
        ItemStack st = e.getItemStack();
        if (st.isEmpty() || st.getTooltipImage().isPresent()) return;   // с картинкой (рюкзак-мешок и т.п.) - ванильная
        e.setCanceled(true);
        Minecraft mc = Minecraft.getInstance();
        double scale = (double) mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
        int mx = (int) (mc.mouseHandler.xpos() * scale);
        int my = (int) (mc.mouseHandler.ypos() * scale);
        FancyTooltip.render(e.getGraphics(), e.getFont(), st, mx, my);
    }

    /** Клавиша чтения над предметом в любом окне инвентаря открывает крупное окно описания. */
    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre e) {
        if (!KeyBindings.READ.matches(e.getKeyCode(), e.getScanCode())) return;
        Screen s = e.getScreen();
        if (s instanceof HubScreen hs && hs.isTextFocused()) return;
        if (!(s instanceof AbstractContainerScreen<?> cs)) return;
        Slot sl = cs.getSlotUnderMouse();
        if (sl == null || !sl.hasItem()) return;
        e.setCanceled(true);
        Minecraft.getInstance().setScreen(new ItemReadScreen(s, sl.getItem().copy()));
    }

    private TooltipEvents() {}
}
