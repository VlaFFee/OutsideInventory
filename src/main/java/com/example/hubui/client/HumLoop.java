package com.example.hubui.client;

import com.example.hubui.HubConfig;
import com.example.hubui.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Тихий цифровой гул: плавно нарастает при открытии хаба и затухает при закрытии. */
@OnlyIn(Dist.CLIENT)
public class HumLoop extends AbstractTickableSoundInstance {
    private int age;
    private boolean revived;

    public HumLoop() {
        super(ModRegistry.UI_HUM.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.001f;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    public void revive() {
        revived = true;
    }

    @Override
    public void tick() {
        Screen s = Minecraft.getInstance().screen;
        boolean ours = s instanceof HubScreen || s instanceof ItemReadScreen;
        age++;
        float swell = Math.min(1f, age / 80f);   // за ~4 секунды набирает полную силу
        float target = ours ? (float) HubConfig.humVolume() * (0.55f + 0.45f * swell) : 0f;
        volume += (target - volume) * 0.08f;
        if (target == 0f && volume < 0.01f) stop();
    }
}
