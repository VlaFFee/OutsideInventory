package com.example.hubui.client;

import com.example.hubui.HubUiMod;
import com.example.hubui.network.NetworkHandler;
import com.example.hubui.network.OpenHubPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HubUiMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    /** Ванильный инвентарь (выживание) подменяется хабом: просим сервер открыть меню. */
    @SubscribeEvent
    public static void onOpening(ScreenEvent.Opening e) {
        if (!(e.getNewScreen() instanceof InventoryScreen)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) return;
        e.setNewScreen(null);
        NetworkHandler.CHANNEL.sendToServer(new OpenHubPacket());
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        ClientHub.reset();
    }

    private ClientEvents() {}
}
