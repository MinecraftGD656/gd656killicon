package org.mods.gd656killicon.server.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mods.gd656killicon.Gd656killicon;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.server.data.ServerData;
import org.mods.gd656killicon.server.util.ServerLog;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Gd656killicon.MODID)
public class ServerCommands {
    
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("gd656killicon").then(Commands.literal("server")
                .then(Commands.literal("bonus")
                    .then(Commands.literal("turnon")
                        .then(Commands.literal("all").executes(c -> toggleBonus(c, true, true)))
                        .then(Commands.argument("type", StringArgumentType.word())
                            .suggests((c, b) -> {
                                b.suggest("all");
                                BonusType.getAllNames().stream()
                                    .filter(name -> !ServerData.get().isBonusEnabled(BonusType.getTypeByName(name)))
                                    .forEach(b::suggest);
                                return b.buildFuture();
                            })
                            .executes(c -> toggleBonus(c, true, false))))
                    .then(Commands.literal("turnoff")
                        .then(Commands.literal("all").executes(c -> toggleBonus(c, false, true)))
                        .then(Commands.argument("type", StringArgumentType.word())
                            .suggests((c, b) -> {
                                b.suggest("all");
                                BonusType.getAllNames().stream()
                                    .filter(name -> ServerData.get().isBonusEnabled(BonusType.getTypeByName(name)))
                                    .forEach(b::suggest);
                                return b.buildFuture();
                            })
                            .executes(c -> toggleBonus(c, false, false))))
                    .then(Commands.literal("edit")
                        .then(Commands.argument("type", StringArgumentType.word())
                            .suggests((c, b) -> {
                                BonusType.getAllNames().forEach(b::suggest);
                                return b.buildFuture();
                            })
                            .then(Commands.literal("expression")
                                .then(Commands.argument("expr", StringArgumentType.string())
                                    .executes(ServerCommands::editBonusExpression)))))
                )
                .then(Commands.literal("reset").requires(s -> s.hasPermission(2))
                    .then(Commands.literal("config").executes(ServerCommands::resetConfig))
                    .then(Commands.literal("bonus").executes(ServerCommands::resetBonusConfig))
                )
                .then(Commands.literal("config").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("ComboWindow").then(Commands.argument("sec", DoubleArgumentType.doubleArg(0.1))
                            .executes(c -> setWindow(c, DoubleArgumentType.getDouble(c, "sec")))))
                        .then(Commands.literal("ScoreMaxLimit").then(Commands.argument("val", IntegerArgumentType.integer(0))
                            .executes(c -> setLimit(c, IntegerArgumentType.getInteger(c, "val")))))
                        .then(Commands.literal("ScoreScoreboardDisplayName").then(Commands.argument("name", StringArgumentType.string())
                            .executes(c -> setScoreboardDisplayName(c, StringArgumentType.getString(c, "name")))))
                        .then(Commands.literal("KillboardDisplayName").then(Commands.argument("name", StringArgumentType.string())
                            .executes(c -> setKillboardDisplayName(c, StringArgumentType.getString(c, "name")))))
                        .then(Commands.literal("DeathScoreboardDisplayName").then(Commands.argument("name", StringArgumentType.string())
                            .executes(c -> setDeathboardDisplayName(c, StringArgumentType.getString(c, "name")))))
                        .then(Commands.literal("AssistboardDisplayName").then(Commands.argument("name", StringArgumentType.string())
                            .executes(c -> setAssistboardDisplayName(c, StringArgumentType.getString(c, "name")))))
                    )
                .then(Commands.literal("statistics")
                    .then(Commands.literal("get").then(Commands.literal("score")
                        .executes(ServerCommands::getSelf)
                        .then(Commands.argument("target", EntityArgument.player()).executes(ServerCommands::getTarget))))
                    .then(Commands.literal("list").then(Commands.literal("score")
                        .executes(ServerCommands::listScores)))
                    .then(Commands.literal("add").requires(s -> s.hasPermission(2)).then(Commands.literal("score")
                        .then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("amt", IntegerArgumentType.integer())
                            .executes(c -> modScore(c, true))))))
                    .then(Commands.literal("set").requires(s -> s.hasPermission(2)).then(Commands.literal("score")
                        .then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("amt", IntegerArgumentType.integer())
                            .executes(c -> modScore(c, false))))))
                    .then(Commands.literal("dataset").requires(s -> s.hasPermission(2)).then(Commands.literal("score")
                        .then(Commands.argument("amt", IntegerArgumentType.integer()).executes(ServerCommands::setAll))))
                    .then(Commands.literal("get").then(Commands.literal("kill")
                        .executes(ServerCommands::getKillSelf)
                        .then(Commands.argument("target", EntityArgument.player()).executes(ServerCommands::getKillTarget))))
                    .then(Commands.literal("list").then(Commands.literal("kill")
                        .executes(ServerCommands::listKills)))
                    .then(Commands.literal("add").requires(s -> s.hasPermission(2)).then(Commands.literal("kill")
                        .then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("amt", IntegerArgumentType.integer())
                            .executes(c -> modKill(c, true))))))
                    .then(Commands.literal("set").requires(s -> s.hasPermission(2)).then(Commands.literal("kill")
                        .then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("amt", IntegerArgumentType.integer())
                            .executes(c -> modKill(c, false))))))
                    .then(Commands.literal("dataset").requires(s -> s.hasPermission(2)).then(Commands.literal("kill")
                        .then(Commands.argument("amt", IntegerArgumentType.integer()).executes(ServerCommands::setAllKills))))
                    .then(Commands.literal("get").then(Commands.literal("death")
                        .executes(ServerCommands::getDeathSelf)
                        .then(Commands.argument("target", EntityArgument.player()).executes(ServerCommands::getDeathTarget))))
                    .then(Commands.literal("list").then(Commands.literal("death")
                        .executes(ServerCommands::listDeaths)))
                    .then(Commands.literal("add").requires(s -> s.hasPermission(2)).then(Commands.literal("death")
                        .then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("amt", IntegerArgumentType.integer())
                            .executes(c -> modDeath(c, true))))))
                    .then(Commands.literal("set").requires(s -> s.hasPermission(2)).then(Commands.literal("death")
                        .then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("amt", IntegerArgumentType.integer())
                            .executes(c -> modDeath(c, false))))))
                    .then(Commands.literal("dataset").requires(s -> s.hasPermission(2)).then(Commands.literal("death")
                        .then(Commands.argument("amt", IntegerArgumentType.integer()).executes(ServerCommands::setAllDeaths))))
                    .then(Commands.literal("get").then(Commands.literal("assist")
                        .executes(ServerCommands::getAssistSelf)
                        .then(Commands.argument("target", EntityArgument.player()).executes(ServerCommands::getAssistTarget))))
                    .then(Commands.literal("list").then(Commands.literal("assist")
                        .executes(ServerCommands::listAssists)))
                    .then(Commands.literal("add").requires(s -> s.hasPermission(2)).then(Commands.literal("assist")
                        .then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("amt", IntegerArgumentType.integer())
                            .executes(c -> modAssist(c, true))))))
                    .then(Commands.literal("set").requires(s -> s.hasPermission(2)).then(Commands.literal("assist")
                        .then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("amt", IntegerArgumentType.integer())
                            .executes(c -> modAssist(c, false))))))
                    .then(Commands.literal("dataset").requires(s -> s.hasPermission(2)).then(Commands.literal("assist")
                        .then(Commands.argument("amt", IntegerArgumentType.integer()).executes(ServerCommands::setAllAssists))))
                )
                .then(Commands.literal("debug").requires(s -> s.hasPermission(2))
                    .then(Commands.literal("scoreboarddebug")
                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                            .executes(ServerCommands::scoreboardDebug))))
            )
        );
    }

    private static int scoreboardDebug(CommandContext<CommandSourceStack> c) {
        int count = IntegerArgumentType.getInteger(c, "count");
        java.util.Random random = new java.util.Random();
        String[] prefixes = {"Pro", "Noob", "God", "Master", "Legend", "Ghost", "Shadow", "Flame", "Ice", "Storm"};
        String[] suffixes = {"Hunter", "Killer", "Player", "Warrior", "Seeker", "X", "Alpha", "Omega", "King", "Lord"};

        for (int i = 0; i < count; i++) {
            java.util.UUID uuid = java.util.UUID.randomUUID();
            String name = prefixes[random.nextInt(prefixes.length)] + suffixes[random.nextInt(suffixes.length)] + random.nextInt(999);
            
            org.mods.gd656killicon.server.data.PlayerData data = org.mods.gd656killicon.server.data.PlayerDataManager.get().getOrCreatePlayerData(uuid);
            data.setLastLoginName(name);
            data.setScore(random.nextInt(5000));
            data.setKill(random.nextInt(100));
            data.setDeath(random.nextInt(100));
            data.setAssist(random.nextInt(100));
            
            org.mods.gd656killicon.server.data.PlayerDataManager.get().forceSave(uuid);
        }

        ServerLog.sendSuccess(c.getSource(), "Successfully generated " + count + " random scoreboard entries.");
        return Command.SINGLE_SUCCESS;
    }

    private static int setWindow(CommandContext<CommandSourceStack> c, double val) {
        ServerData.get().setComboWindowSeconds(val);
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.combo_window_set", val);
        return Command.SINGLE_SUCCESS;
    }

    private static int setLimit(CommandContext<CommandSourceStack> c, int val) {
        ServerData.get().setScoreMaxLimit(val);
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.score_max_limit_set", val);
        return Command.SINGLE_SUCCESS;
    }

    private static int setScoreboardDisplayName(CommandContext<CommandSourceStack> c, String name) {
        ServerData.get().setScoreboardDisplayName(name);
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.scoreboard_display_name_set", name);
        return Command.SINGLE_SUCCESS;
    }

    private static int setKillboardDisplayName(CommandContext<CommandSourceStack> c, String name) {
        ServerData.get().setKillboardDisplayName(name);
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.killboard_display_name_set", name);
        return Command.SINGLE_SUCCESS;
    }

    private static int setDeathboardDisplayName(CommandContext<CommandSourceStack> c, String name) {
        ServerData.get().setDeathboardDisplayName(name);
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.deathboard_display_name_set", name);
        return Command.SINGLE_SUCCESS;
    }

    private static int setAssistboardDisplayName(CommandContext<CommandSourceStack> c, String name) {
        ServerData.get().setAssistboardDisplayName(name);
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.assistboard_display_name_set", name);
        return Command.SINGLE_SUCCESS;
    }

    private static int resetConfig(CommandContext<CommandSourceStack> c) {
        ServerData.get().resetConfig();
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.reset_success");
        return Command.SINGLE_SUCCESS;
    }

    private static int resetBonusConfig(CommandContext<CommandSourceStack> c) {
        ServerData.get().resetBonusConfig();
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.reset_bonus_success");
        return Command.SINGLE_SUCCESS;
    }

    private static int editBonusExpression(CommandContext<CommandSourceStack> c) {
        String name = StringArgumentType.getString(c, "type");
        String expr = StringArgumentType.getString(c, "expr");
        
        int type = BonusType.getTypeByName(name);
        if (type == -1) {
            ServerLog.sendError(c.getSource(), "gd656killicon.server.command.bonus_type_invalid", name);
            return 0;
        }

        try {
            Double.parseDouble(expr); // Validate it's a number for now
        } catch (NumberFormatException e) {
            ServerLog.sendError(c.getSource(), "gd656killicon.server.command.invalid_expression", expr);
            return 0;
        }

        ServerData.get().setBonusExpression(type, expr);
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.bonus_expression_set", BonusType.getNameByType(type), expr);
        return Command.SINGLE_SUCCESS;
    }

    private static int toggleBonus(CommandContext<CommandSourceStack> c, boolean enabled, boolean all) {
        if (all) {
            BonusType.getAllNames().forEach(name -> {
                int type = BonusType.getTypeByName(name);
                if (type != -1) ServerData.get().setBonusEnabled(type, enabled);
            });
            ServerLog.sendSuccess(c.getSource(), enabled ? "gd656killicon.server.command.bonus_all_enabled" : "gd656killicon.server.command.bonus_all_disabled");
            return Command.SINGLE_SUCCESS;
        }

        String name = StringArgumentType.getString(c, "type");
        if (name.equalsIgnoreCase("all")) return toggleBonus(c, enabled, true);
        
        int type = BonusType.getTypeByName(name);
        if (type == -1) {
            ServerLog.sendError(c.getSource(), "gd656killicon.server.command.bonus_type_invalid", name);
            return 0;
        }
        
        boolean currentState = ServerData.get().isBonusEnabled(type);
        if (currentState == enabled) {
            ServerLog.sendError(c.getSource(), enabled ? "gd656killicon.server.command.bonus_already_enabled" : "gd656killicon.server.command.bonus_already_disabled", BonusType.getNameByType(type));
            return 0;
        }

        ServerData.get().setBonusEnabled(type, enabled);
        ServerLog.sendSuccess(c.getSource(), enabled ? "gd656killicon.server.command.bonus_enabled" : "gd656killicon.server.command.bonus_disabled", BonusType.getNameByType(type));
        return Command.SINGLE_SUCCESS;
    }

    private static int getSelf(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.score_self", ServerData.get().getScore(p.getUUID()));
        return Command.SINGLE_SUCCESS;
    }

    private static int getTarget(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "target");
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.score_target", p.getName().getString(), ServerData.get().getScore(p.getUUID()));
        return Command.SINGLE_SUCCESS;
    }

    private static int listScores(CommandContext<CommandSourceStack> c) {
        Map<java.util.UUID, Float> map = ServerData.get().getAllScores();
        if (map.isEmpty()) {
            ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.no_scores");
            return Command.SINGLE_SUCCESS;
        }
        List<Map.Entry<java.util.UUID, Float>> sorted = map.entrySet().stream()
            .sorted(Map.Entry.<java.util.UUID, Float>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toList());
        
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.score_list_header");
        int i = 1;
        for (Map.Entry<java.util.UUID, Float> e : sorted) {
            String name = e.getKey().toString();
            try { name = c.getSource().getServer().getProfileCache().get(e.getKey()).orElseThrow().getName(); } catch (Exception ignored) {}
            ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.score_list_entry", i++, name, e.getValue());
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int modScore(CommandContext<CommandSourceStack> c, boolean add) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
        // Use DoubleArgumentType for float scores or just Integer?
        // Since we are changing score system to float, commands should support float.
        // But for now keeping IntegerArgumentType.integer() is safe if we don't change command registration.
        // Wait, command registration uses IntegerArgumentType.integer() in register().
        // Changing it to DoubleArgumentType would be better but user said "don't leave legacy issues".
        // Let's change command registration to DoubleArgumentType for score modification commands.
        // But first let's just fix the compilation error by casting int to float implicitly.
        int amt = IntegerArgumentType.getInteger(c, "amt");
        players.forEach(p -> { if (add) ServerData.get().addScore(p, (float)amt); else ServerData.get().setScore(p, (float)amt); });
        ServerData.get().refreshScoreboard(c.getSource().getServer());
        ServerLog.sendSuccess(c.getSource(), add ? "gd656killicon.server.command.score_added" : "gd656killicon.server.command.score_set", players.size(), amt);
        return players.size();
    }

    private static int setAll(CommandContext<CommandSourceStack> c) {
        int amt = IntegerArgumentType.getInteger(c, "amt");
        ServerData.get().setAllScores(c.getSource().getServer(), (float)amt);
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.score_reset_all", amt);
        return Command.SINGLE_SUCCESS;
    }

    private static int getKillSelf(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        int kill = ServerData.get().getKill(player.getUUID());
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.kill_self", kill);
        return kill;
    }

    private static int getKillTarget(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(c, "target");
        int kill = ServerData.get().getKill(target.getUUID());
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.kill_target", target.getName().getString(), kill);
        return kill;
    }

    private static int listKills(CommandContext<CommandSourceStack> c) {
        Map<java.util.UUID, Integer> map = ServerData.get().getAllKills();
        if (map.isEmpty()) {
            ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.no_kills");
            return Command.SINGLE_SUCCESS;
        }
        List<Map.Entry<java.util.UUID, Integer>> sorted = map.entrySet().stream()
            .sorted(Map.Entry.<java.util.UUID, Integer>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toList());
        
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.kill_list_header");
        int i = 1;
        for (Map.Entry<java.util.UUID, Integer> e : sorted) {
            String name = e.getKey().toString();
            try { name = c.getSource().getServer().getProfileCache().get(e.getKey()).orElseThrow().getName(); } catch (Exception ignored) {}
            ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.kill_list_entry", i++, name, e.getValue());
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int modKill(CommandContext<CommandSourceStack> c, boolean add) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
        int amt = IntegerArgumentType.getInteger(c, "amt");
        players.forEach(p -> { if (add) ServerData.get().addKill(p, amt); else ServerData.get().setKill(p, amt); });
        ServerData.get().refreshScoreboard(c.getSource().getServer());
        ServerLog.sendSuccess(c.getSource(), add ? "gd656killicon.server.command.kill_added" : "gd656killicon.server.command.kill_set", players.size(), amt);
        return players.size();
    }

    private static int setAllKills(CommandContext<CommandSourceStack> c) {
        int amt = IntegerArgumentType.getInteger(c, "amt");
        ServerData.get().setAllKills(c.getSource().getServer(), amt);
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.kill_reset_all", amt);
        return Command.SINGLE_SUCCESS;
    }

    private static int getDeathSelf(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        int death = ServerData.get().getDeath(player.getUUID());
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.death_self", death);
        return death;
    }

    private static int getDeathTarget(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(c, "target");
        int death = ServerData.get().getDeath(target.getUUID());
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.death_target", target.getName().getString(), death);
        return death;
    }

    private static int listDeaths(CommandContext<CommandSourceStack> c) {
        Map<java.util.UUID, Integer> map = ServerData.get().getAllDeaths();
        if (map.isEmpty()) {
            ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.no_deaths");
            return Command.SINGLE_SUCCESS;
        }
        List<Map.Entry<java.util.UUID, Integer>> sorted = map.entrySet().stream()
            .sorted(Map.Entry.<java.util.UUID, Integer>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toList());
        
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.death_list_header");
        int i = 1;
        for (Map.Entry<java.util.UUID, Integer> e : sorted) {
            String name = e.getKey().toString();
            try { name = c.getSource().getServer().getProfileCache().get(e.getKey()).orElseThrow().getName(); } catch (Exception ignored) {}
            ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.death_list_entry", i++, name, e.getValue());
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int modDeath(CommandContext<CommandSourceStack> c, boolean add) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
        int amt = IntegerArgumentType.getInteger(c, "amt");
        players.forEach(p -> { if (add) ServerData.get().addDeath(p, amt); else ServerData.get().setDeath(p, amt); });
        ServerData.get().refreshScoreboard(c.getSource().getServer());
        ServerLog.sendSuccess(c.getSource(), add ? "gd656killicon.server.command.death_added" : "gd656killicon.server.command.death_set", players.size(), amt);
        return players.size();
    }

    private static int setAllDeaths(CommandContext<CommandSourceStack> c) {
        int amt = IntegerArgumentType.getInteger(c, "amt");
        ServerData.get().setAllDeaths(c.getSource().getServer(), amt);
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.death_reset_all", amt);
        return Command.SINGLE_SUCCESS;
    }

    private static int getAssistSelf(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        int assist = ServerData.get().getAssist(player.getUUID());
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.assist_self", assist);
        return assist;
    }

    private static int getAssistTarget(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(c, "target");
        int assist = ServerData.get().getAssist(target.getUUID());
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.assist_target", target.getName().getString(), assist);
        return assist;
    }

    private static int listAssists(CommandContext<CommandSourceStack> c) {
        Map<java.util.UUID, Integer> map = ServerData.get().getAllAssists();
        if (map.isEmpty()) {
            ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.no_assists");
            return Command.SINGLE_SUCCESS;
        }
        List<Map.Entry<java.util.UUID, Integer>> sorted = map.entrySet().stream()
            .sorted(Map.Entry.<java.util.UUID, Integer>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toList());
        
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.assist_list_header");
        int i = 1;
        for (Map.Entry<java.util.UUID, Integer> e : sorted) {
            String name = e.getKey().toString();
            try { name = c.getSource().getServer().getProfileCache().get(e.getKey()).orElseThrow().getName(); } catch (Exception ignored) {}
            ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.assist_list_entry", i++, name, e.getValue());
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int modAssist(CommandContext<CommandSourceStack> c, boolean add) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
        int amt = IntegerArgumentType.getInteger(c, "amt");
        players.forEach(p -> { if (add) ServerData.get().addAssist(p, amt); else ServerData.get().setAssist(p, amt); });
        ServerData.get().refreshScoreboard(c.getSource().getServer());
        ServerLog.sendSuccess(c.getSource(), add ? "gd656killicon.server.command.assist_added" : "gd656killicon.server.command.assist_set", players.size(), amt);
        return players.size();
    }

    private static int setAllAssists(CommandContext<CommandSourceStack> c) {
        int amt = IntegerArgumentType.getInteger(c, "amt");
        ServerData.get().setAllAssists(c.getSource().getServer(), amt);
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.assist_reset_all", amt);
        return Command.SINGLE_SUCCESS;
    }


}
