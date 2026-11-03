package com.example.hubui;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = HubUiMod.MODID)
public final class HubEvents {

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        HubCommands.register(e.getDispatcher());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) HubApi.sync(sp);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) HubApi.sync(sp);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) HubApi.sync(sp);
    }

    /** Смерть/переход из Края: валюта, хранилище и журнал не теряются. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        CompoundTag old = e.getOriginal().getPersistentData();
        if (old.contains(HubData.ROOT)) {
            e.getEntity().getPersistentData().put(HubData.ROOT, old.getCompound(HubData.ROOT).copy());
        }
    }

    // ---------------- ограниченный инвентарь ----------------
    private static final Map<UUID, Long> LAST_FULL_MSG = new HashMap<>();

    private static boolean limitApplies(Player p) {
        return HubConfig.limited() && !p.isCreative() && !p.isSpectator();
    }

    /** Подбор с земли: берём только то, что помещается в 9 ячеек, остальное остаётся лежать. */
    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent e) {
        Player p = e.getEntity();
        if (!(p instanceof ServerPlayer sp) || !limitApplies(p)) return;
        ItemEntity ent = e.getItem();
        ItemStack st = ent.getItem();
        int room = AllowedInv.room(p.getInventory(), st);
        if (room <= 0) {
            e.setCanceled(true);
            long now = System.currentTimeMillis();
            Long last = LAST_FULL_MSG.get(p.getUUID());
            if (last == null || now - last > 2000L) {
                LAST_FULL_MSG.put(p.getUUID(), now);
                sp.displayClientMessage(Component.literal("§cИнвентарь полон"), true);
                sp.playNotifySound(ModRegistry.UI_ERROR.get(), SoundSource.MASTER, 0.6f, 1f);
            }
        } else if (room < st.getCount()) {
            ItemStack part = st.copy();
            part.setCount(room);
            ItemStack rest = st.copy();
            rest.shrink(room);
            AllowedInv.insert(p.getInventory(), part);
            ent.setItem(rest);
            p.take(ent, room);
            e.setCanceled(true);
        }
    }

    /** Всё, что оказалось в скрытых ячейках 9..35 (сундуки, /give, крафт...), переносим или выбрасываем. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 10 != 0 || !limitApplies(sp)) return;
        Inventory inv = sp.getInventory();
        boolean moved = false;
        for (int i = AllowedInv.SLOTS; i < 36; i++) {
            ItemStack s = inv.items.get(i);
            if (s.isEmpty()) continue;
            inv.items.set(i, ItemStack.EMPTY);
            ItemStack rest = AllowedInv.insert(inv, s);
            if (!rest.isEmpty()) {
                sp.drop(rest, false);
                sp.displayClientMessage(Component.literal("§cНет места в инвентаре"), true);
            }
            moved = true;
        }
        if (moved) {
            inv.setChanged();
            sp.containerMenu.broadcastChanges();
        }
    }

    private HubEvents() {}
}
