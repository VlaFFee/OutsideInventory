package com.example.hubui;

import com.example.hubui.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(HubUiMod.MODID)
public class HubUiMod {
    public static final String MODID = "hubui";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HubUiMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, HubConfig.CLIENT);
        bus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent e) {
        e.enqueueWork(NetworkHandler::register);
    }
}
