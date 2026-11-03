package com.example.hubui;

import com.example.hubui.network.NetworkHandler;
import com.example.hubui.network.SyncPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;

/**
 * Публичный API мода: им пользуются команды, а позже - сюжетные скрипты и другие моды.
 * Все изменения сразу синхронизируются с клиентом игрока.
 */
public final class HubApi {

    public static String currencyWord(long n) {
        return n == 1 ? "Влаффта" : "Влаффты";
    }

    public static void sync(ServerPlayer p) {
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new SyncPacket(HubData.getBalance(p), HubData.getRows(p), HubConfig.limited(), HubData.getEntries(p)));
    }

    public static long getBalance(ServerPlayer p) {
        return HubData.getBalance(p);
    }

    public static void setBalance(ServerPlayer p, long v) {
        HubData.setBalance(p, v);
        sync(p);
    }

    public static void addBalance(ServerPlayer p, long delta) {
        HubData.setBalance(p, HubData.getBalance(p) + delta);
        sync(p);
    }

    public static void setStorageRows(ServerPlayer p, int rows) {
        HubData.setRows(p, rows);
        sync(p);
    }

    /** Добавляет запись в журнал игрока от имени персонажа author. */
    public static void addJournalEntry(ServerPlayer p, String author, String title, String text) {
        int day = (int) (p.level().getDayTime() / 24000L) + 1;
        HubData.addEntry(p, author, title, text, day);
        sync(p);
        p.displayClientMessage(Component.literal("§b✎ Новая запись в журнале"), true);
    }

    public static void clearJournal(ServerPlayer p) {
        HubData.clearJournal(p);
        sync(p);
    }

    /** Открывает хаб (вместо ванильного инвентаря). */
    public static void openHub(ServerPlayer p) {
        if (p.isSpectator()) return;
        int rows = HubData.getRows(p);
        SimpleContainer storage = new SimpleContainer(HubMenu.STORAGE_SIZE);
        HubData.loadStorage(p, storage);
        storage.addListener(c -> HubData.saveStorage(p, storage));   // каждое изменение сразу в данные игрока
        NetworkHooks.openScreen(p,
                new SimpleMenuProvider((id, inv, pl) -> new HubMenu(id, inv, storage, rows), Component.literal("Хаб")),
                buf -> buf.writeInt(rows));
    }

    private HubApi() {}
}
