package com.example.hubui.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class KeyBindings {
    /** Открыть окно крупного чтения описания предмета под курсором (работает в окнах инвентаря). */
    public static final KeyMapping READ = new KeyMapping("key.hubui.read", KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F, "key.categories.hubui");

    private KeyBindings() {}
}
