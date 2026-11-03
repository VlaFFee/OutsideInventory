package com.example.hubui;

import net.minecraftforge.common.ForgeConfigSpec;

/** CLIENT: config/hubui-client.toml (только оформление). */
public final class HubConfig {
    public static final ForgeConfigSpec CLIENT;
    private static final ForgeConfigSpec.BooleanValue FANCY_TOOLTIPS, TOOLTIP_SOUNDS, DIGITAL_RAIN, CUSTOM_HOTBAR, HUM;
    private static final ForgeConfigSpec.DoubleValue HUM_VOLUME;

    static {
        ForgeConfigSpec.Builder k = new ForgeConfigSpec.Builder();
        FANCY_TOOLTIPS = k.comment("Оцифрованные подсказки предметов с 3D-моделью (заменяют ванильные для всех предметов).")
                .define("fancyTooltips", true);
        TOOLTIP_SOUNDS = k.comment("Тихий цифровой звук при появлении подсказки.")
                .define("tooltipSounds", true);
        DIGITAL_RAIN = k.comment("Прозрачный цифровой дождь на фоне хаба и окна чтения.")
                .define("digitalRain", true);
        CUSTOM_HOTBAR = k.comment("Свой 5-слотовый хотбар на экране (вместо ванильного 9-слотового).")
                .define("customHotbar", true);
        HUM = k.comment("Тихий цифровой гул, пока открыт хаб или окно чтения.")
                .define("hum", true);
        HUM_VOLUME = k.comment("Громкость гула (0..1). Дополнительно регулируется ползунком звука \"Окружение\".")
                .defineInRange("humVolume", 0.28, 0.0, 1.0);
        CLIENT = k.build();
    }

    /** Ограниченный инвентарь (5 ячеек хотбара + 4 кармана) включён всегда; не действует в творческом режиме и у наблюдателей. */
    public static boolean limited() { return true; }
    public static boolean fancyTooltips() { try { return FANCY_TOOLTIPS.get(); } catch (Throwable t) { return true; } }
    public static boolean tooltipSounds() { try { return TOOLTIP_SOUNDS.get(); } catch (Throwable t) { return true; } }
    public static boolean digitalRain() { try { return DIGITAL_RAIN.get(); } catch (Throwable t) { return true; } }
    public static boolean hum() { try { return HUM.get(); } catch (Throwable t) { return true; } }
    public static double humVolume() { try { return HUM_VOLUME.get(); } catch (Throwable t) { return 0.28; } }
    public static boolean customHotbar() { try { return CUSTOM_HOTBAR.get(); } catch (Throwable t) { return true; } }

    private HubConfig() {}
}
