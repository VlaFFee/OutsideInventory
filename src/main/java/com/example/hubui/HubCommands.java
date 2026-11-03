package com.example.hubui;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * /vlaffta get|give|take|set     - валюта
 * /journal add|clear              - журнал (запись от имени персонажа)
 * /hubstorage rows <игроки> <n>   - сколько рядов хранилища открыто (0..7)
 * Подходят и для командных блоков.
 */
public final class HubCommands {
    private static final long MAX = 1_000_000_000L;

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("vlaffta").requires(s -> s.hasPermission(2))
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.player()).executes(HubCommands::get)))
                .then(Commands.literal("give")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", LongArgumentType.longArg(0, MAX))
                                        .executes(c -> change(c, 1)))))
                .then(Commands.literal("take")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", LongArgumentType.longArg(0, MAX))
                                        .executes(c -> change(c, -1)))))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", LongArgumentType.longArg(0, MAX))
                                        .executes(c -> change(c, 0))))));

        d.register(Commands.literal("journal").requires(s -> s.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("author", StringArgumentType.string())
                                        .then(Commands.argument("title", StringArgumentType.string())
                                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                                        .executes(HubCommands::journalAdd))))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityArgument.players()).executes(HubCommands::journalClear))));

        d.register(Commands.literal("hubstorage").requires(s -> s.hasPermission(2))
                .then(Commands.literal("rows")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("rows", IntegerArgumentType.integer(0, HubData.MAX_ROWS))
                                        .executes(HubCommands::rows)))));
    }

    private static int get(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "target");
        long b = HubData.getBalance(p);
        c.getSource().sendSuccess(() -> Component.literal(p.getName().getString() + ": " + b + " " + HubApi.currencyWord(b)), false);
        return (int) Math.min(b, Integer.MAX_VALUE);
    }

    /** mode: 1 = выдать, -1 = забрать, 0 = установить */
    private static int change(CommandContext<CommandSourceStack> c, int mode) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "targets");
        long amount = LongArgumentType.getLong(c, "amount");
        for (ServerPlayer p : targets) {
            if (mode == 0) HubApi.setBalance(p, amount);
            else HubApi.addBalance(p, mode * amount);
        }
        String verb = mode == 1 ? "Выдано" : mode == -1 ? "Забрано" : "Установлено";
        c.getSource().sendSuccess(() -> Component.literal(verb + ": " + amount + " " + HubApi.currencyWord(amount)
                + " (" + targets.size() + " игр.)"), true);
        return targets.size();
    }

    private static int journalAdd(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "targets");
        String author = StringArgumentType.getString(c, "author");
        String title = StringArgumentType.getString(c, "title");
        String text = StringArgumentType.getString(c, "text").replace("\\n", "\n");
        for (ServerPlayer p : targets) HubApi.addJournalEntry(p, author, title, text);
        c.getSource().sendSuccess(() -> Component.literal("Запись добавлена (" + targets.size() + " игр.)"), true);
        return targets.size();
    }

    private static int journalClear(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "targets");
        for (ServerPlayer p : targets) HubApi.clearJournal(p);
        c.getSource().sendSuccess(() -> Component.literal("Журнал очищен (" + targets.size() + " игр.)"), true);
        return targets.size();
    }

    private static int rows(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "targets");
        int rows = IntegerArgumentType.getInteger(c, "rows");
        for (ServerPlayer p : targets) HubApi.setStorageRows(p, rows);
        c.getSource().sendSuccess(() -> Component.literal("Открыто рядов хранилища: " + rows + " (" + targets.size() + " игр.)"), true);
        return targets.size();
    }

    private HubCommands() {}
}
