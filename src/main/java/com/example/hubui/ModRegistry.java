package com.example.hubui;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, HubUiMod.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, HubUiMod.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, HubUiMod.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, HubUiMod.MODID);

    public static final RegistryObject<MenuType<HubMenu>> HUB_MENU =
            MENUS.register("hub", () -> IForgeMenuType.create(HubMenu::clientCreate));

    public static final RegistryObject<RecipeType<HubRecipe>> HUB_RECIPE_TYPE =
            RECIPE_TYPES.register("hub_crafting", () -> new RecipeType<HubRecipe>() {
                @Override
                public String toString() {
                    return HubUiMod.MODID + ":hub_crafting";
                }
            });
    public static final RegistryObject<RecipeSerializer<HubRecipe>> HUB_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("hub_crafting", HubRecipe.Serializer::new);

    public static final RegistryObject<SoundEvent> UI_OPEN = sound("ui_open");
    public static final RegistryObject<SoundEvent> UI_CLOSE = sound("ui_close");
    public static final RegistryObject<SoundEvent> UI_TAB = sound("ui_tab");
    public static final RegistryObject<SoundEvent> UI_COIN = sound("ui_coin");
    public static final RegistryObject<SoundEvent> UI_CRAFT = sound("ui_craft");
    public static final RegistryObject<SoundEvent> UI_NOTE = sound("ui_note");
    public static final RegistryObject<SoundEvent> UI_SCAN = sound("ui_scan");
    public static final RegistryObject<SoundEvent> UI_HOVER = sound("ui_hover");
    public static final RegistryObject<SoundEvent> UI_READ_OPEN = sound("ui_read_open");
    public static final RegistryObject<SoundEvent> UI_READ_CLOSE = sound("ui_read_close");
    public static final RegistryObject<SoundEvent> UI_SELECT = sound("ui_select");
    public static final RegistryObject<SoundEvent> UI_ERROR = sound("ui_error");
    public static final RegistryObject<SoundEvent> UI_GLITCH = sound("ui_glitch");
    public static final RegistryObject<SoundEvent> UI_DATA = sound("ui_data");
    public static final RegistryObject<SoundEvent> UI_DECODE = sound("ui_decode");
    public static final RegistryObject<SoundEvent> UI_HUM = sound("ui_hum");

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HubUiMod.MODID, name)));
    }

    public static void register(IEventBus bus) {
        MENUS.register(bus);
        SOUNDS.register(bus);
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }

    private ModRegistry() {}
}
