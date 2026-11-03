package com.example.hubui.client;

import com.example.hubui.HubConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.RegistryObject;

@OnlyIn(Dist.CLIENT)
public final class UiSound {
    private static HumLoop hum;

    public static void play(RegistryObject<SoundEvent> ev, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev.get(), pitch));
    }

    public static void play(RegistryObject<SoundEvent> ev, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev.get(), pitch, volume));
    }

    /** Включает (или не даёт затухнуть) тихому гулу: он живёт, пока открыт хаб или окно чтения. */
    public static void ensureHum() {
        if (!HubConfig.hum()) return;
        if (hum != null && !hum.isStopped()) {
            hum.revive();
            return;
        }
        hum = new HumLoop();
        Minecraft.getInstance().getSoundManager().play(hum);
    }

    private UiSound() {}
}
