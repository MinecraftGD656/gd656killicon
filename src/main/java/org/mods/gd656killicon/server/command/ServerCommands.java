package org.mods.gd656killicon.server.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import org.mods.gd656killicon.common.BonusType;
import org.mods.gd656killicon.server.data.ServerData;
import org.mods.gd656killicon.server.util.ServerLog;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServerCommands {
    private static final String[] SCOREBOARD_DEBUG_PREFIXES = {"Pro", "Noob", "God", "Master", "Legend", "Ghost", "Shadow", "Flame", "Ice", "Storm"};
    private static final String[] SCOREBOARD_DEBUG_SUFFIXES = {"Hunter", "Killer", "Player", "Warrior", "Seeker", "X", "Alpha", "Omega", "King", "Lord"};

    /** honor 参数 Tab 补全: all + 全部已注册 honor id。 */
    private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> HONOR_SUGGESTIONS =
            (c, b) -> {
                b.suggest("all");
                for (String id : org.mods.gd656killicon.common.honor.HonorRegistry.getIds()) {
                    b.suggest(id);
                }
                return b.buildFuture();
            };
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
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
                        .then(Commands.literal("NeutralVehicleSkip").then(Commands.argument("val", BoolArgumentType.bool())
                            .executes(c -> setNeutralVehicleSkip(c, BoolArgumentType.getBool(c, "val")))))
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
                .then(Commands.literal("honor").requires(s -> s.hasPermission(2))
                    .then(Commands.literal("list")
                        .executes(ServerCommands::honorList))
                    .then(Commands.literal("set")
                        .then(Commands.argument("honor", StringArgumentType.word())
                            .suggests(HONOR_SUGGESTIONS)
                            .then(Commands.argument("value", IntegerArgumentType.integer())
                                .executes(ServerCommands::honorSet))))
                    .then(Commands.literal("add")
                        .then(Commands.argument("honor", StringArgumentType.word())
                            .suggests(HONOR_SUGGESTIONS)
                            .then(Commands.argument("value", IntegerArgumentType.integer())
                                .executes(ServerCommands::honorAdd))))
                    .then(Commands.literal("player")
                        .then(Commands.literal("set")
                            .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("honor", StringArgumentType.word())
                                    .suggests(HONOR_SUGGESTIONS)
                                    .then(Commands.argument("value", IntegerArgumentType.integer())
                                        .executes(ServerCommands::honorPlayerSet)))))
                        .then(Commands.literal("get")
                            .then(Commands.argument("player", EntityArgument.player())
                                .executes(ServerCommands::honorPlayerGet))))
                )
                .then(Commands.literal("debug").requires(s -> s.hasPermission(2))
                    .then(Commands.literal("scoreboarddebug")
                        .then(Commands.argument("count", IntegerArgumentType.integer(-1))
                            .executes(ServerCommands::scoreboardDebug)))
                    .then(Commands.literal("honor")
                        .then(Commands.argument("honorId", StringArgumentType.word())
                            .executes(ServerCommands::debugHonor)))
                    .then(Commands.literal("bonus")
                        .then(Commands.argument("type", IntegerArgumentType.integer())
                            .executes(ServerCommands::debugBonus)))
                    .then(Commands.literal("revive")
                        .executes(ServerCommands::debugRevive))))
        );
    }

    private static int honorList(CommandContext<CommandSourceStack> c) {
        java.util.Map<String, Integer> bests = org.mods.gd656killicon.server.data.PlayerDataManager.get().getAllGlobalBest();
        StringBuilder sb = new StringBuilder();
        bests.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                .forEach(e -> sb.append(e.getKey()).append(": ").append(e.getValue()).append('\n'));
        if (sb.length() == 0) {
            sb.append("(none)");
        }
        String text = sb.toString().stripTrailing();
        c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(text), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int honorSet(CommandContext<CommandSourceStack> c) {
        String honor = StringArgumentType.getString(c, "honor");
        int value = IntegerArgumentType.getInteger(c, "value");
        if (!applyHonorAllOrSingle(c, honor, h -> org.mods.gd656killicon.server.data.PlayerDataManager.get().setGlobalBest(h, value))) {
            return 0;
        }
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.honor_set", honor, value);
        return Command.SINGLE_SUCCESS;
    }

    private static int honorAdd(CommandContext<CommandSourceStack> c) {
        String honor = StringArgumentType.getString(c, "honor");
        int value = IntegerArgumentType.getInteger(c, "value");
        if (!applyHonorAllOrSingle(c, honor, h -> org.mods.gd656killicon.server.data.PlayerDataManager.get().addGlobalBest(h, value))) {
            return 0;
        }
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.honor_add", honor, value);
        return Command.SINGLE_SUCCESS;
    }

    private static int honorPlayerSet(CommandContext<CommandSourceStack> c) {
        String honor = StringArgumentType.getString(c, "honor");
        int value = IntegerArgumentType.getInteger(c, "value");
        java.util.Collection<ServerPlayer> players;
        try {
            players = EntityArgument.getPlayers(c, "players");
        } catch (Exception e) {
            ServerLog.sendError(c.getSource(), "gd656killicon.server.command.honor_player_requires_player");
            return 0;
        }
        if (!"all".equals(honor) && !org.mods.gd656killicon.common.honor.HonorRegistry.isRegistered(honor)) {
            ServerLog.sendError(c.getSource(), "gd656killicon.server.command.debug_honor_not_registered", honor);
            return 0;
        }
        org.mods.gd656killicon.server.data.PlayerDataManager data = org.mods.gd656killicon.server.data.PlayerDataManager.get();
        for (ServerPlayer p : players) {
            if ("all".equals(honor)) {
                for (String id : org.mods.gd656killicon.common.honor.HonorRegistry.getIds()) {
                    data.setHonorCount(p.getUUID(), id, value);
                }
            } else {
                data.setHonorCount(p.getUUID(), honor, value);
            }
        }
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.honor_player_set", players.size(), honor, value);
        return Command.SINGLE_SUCCESS;
    }

    private static int honorPlayerGet(CommandContext<CommandSourceStack> c) {
        ServerPlayer player;
        try {
            player = EntityArgument.getPlayer(c, "player");
        } catch (Exception e) {
            ServerLog.sendError(c.getSource(), "gd656killicon.server.command.honor_player_requires_player");
            return 0;
        }
        java.util.Map<String, Integer> counts = org.mods.gd656killicon.server.data.PlayerDataManager.get()
                .getPlayerData(player.getUUID()).getAllHonorCounts();
        StringBuilder sb = new StringBuilder();
        counts.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                .forEach(e -> sb.append(e.getKey()).append(": ").append(e.getValue()).append('\n'));
        if (sb.length() == 0) {
            sb.append("(none)");
        }
        String text = sb.toString().stripTrailing();
        c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(player.getDisplayName().getString() + "\n" + text), false);
        return Command.SINGLE_SUCCESS;
    }

    /** honor 参数校验并执行: "all" 遍历全部注册 honor, 否则单个 honor(未注册报错)。 */
    private static boolean applyHonorAllOrSingle(CommandContext<CommandSourceStack> c, String honor, java.util.function.Consumer<String> action) {
        if ("all".equals(honor)) {
            for (String id : org.mods.gd656killicon.common.honor.HonorRegistry.getIds()) {
                action.accept(id);
            }
            return true;
        }
        if (!org.mods.gd656killicon.common.honor.HonorRegistry.isRegistered(honor)) {
            ServerLog.sendError(c.getSource(), "gd656killicon.server.command.debug_honor_not_registered", honor);
            return false;
        }
        action.accept(honor);
        return true;
    }

    private static int debugHonor(CommandContext<CommandSourceStack> c) {
        String honorId = StringArgumentType.getString(c, "honorId");
        if (!org.mods.gd656killicon.common.honor.HonorRegistry.isRegistered(honorId)) {
            ServerLog.sendError(c.getSource(), "gd656killicon.server.command.debug_honor_not_registered", honorId);
            return 0;
        }
        ServerPlayer player;
        try {
            player = c.getSource().getPlayerOrException();
        } catch (Exception e) {
            ServerLog.sendError(c.getSource(), "gd656killicon.server.command.debug_honor_requires_player");
            return 0;
        }
        // 服务端下发目标荣誉显示包到客户端
        org.mods.gd656killicon.network.NetworkHandler.sendToPlayer(new org.mods.gd656killicon.network.packet.HonorPacket(honorId), player);
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.debug_honor_sent", honorId, player.getDisplayName().getString());
        return Command.SINGLE_SUCCESS;
    }

    private static int debugBonus(CommandContext<CommandSourceStack> c) {
        int bonusType = IntegerArgumentType.getInteger(c, "type");
        if (!org.mods.gd656killicon.common.bonus.BonusRegistry.isRegistered(bonusType)) {
            ServerLog.sendError(c.getSource(), "gd656killicon.server.command.debug_bonus_not_registered", bonusType);
            return 0;
        }
        ServerPlayer player;
        try {
            player = c.getSource().getPlayerOrException();
        } catch (Exception e) {
            ServerLog.sendError(c.getSource(), "gd656killicon.server.command.debug_bonus_requires_player");
            return 0;
        }
        // 下发加分项显示包, 附加数据默认均为 1
        org.mods.gd656killicon.network.NetworkHandler.sendToPlayer(
                new org.mods.gd656killicon.network.packet.BonusScorePacket(bonusType, 1.0f, "1", -1, null), player);
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.debug_bonus_sent", bonusType, player.getDisplayName().getString());
        return Command.SINGLE_SUCCESS;
    }

    private static int debugRevive(CommandContext<CommandSourceStack> c) {
        ServerPlayer player;
        try {
            player = c.getSource().getPlayerOrException();
        } catch (Exception e) {
            ServerLog.sendError(c.getSource(), "gd656killicon.server.command.debug_revive_requires_player");
            return 0;
        }
        // 模拟一次真实救援: 走服务端 onRevive 链路(急救使者/烟幕计数)
        org.mods.gd656killicon.server.ServerCore.HONOR.onRevive(player);
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.debug_revive_sent", player.getDisplayName().getString());
        return Command.SINGLE_SUCCESS;
    }

    private static int scoreboardDebug(CommandContext<CommandSourceStack> c) {
        int count = IntegerArgumentType.getInteger(c, "count");
        if (count == -1) {
            return clearScoreboardDebugData(c);
        }
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < count; i++) {
            java.util.UUID uuid = java.util.UUID.randomUUID();
            String name = SCOREBOARD_DEBUG_PREFIXES[random.nextInt(SCOREBOARD_DEBUG_PREFIXES.length)]
                + SCOREBOARD_DEBUG_SUFFIXES[random.nextInt(SCOREBOARD_DEBUG_SUFFIXES.length)]
                + random.nextInt(999);
            
            org.mods.gd656killicon.server.data.PlayerData data = org.mods.gd656killicon.server.data.PlayerDataManager.get().getOrCreatePlayerData(uuid);
            data.setLastLoginName(name);
            data.setScore(random.nextInt(5000));
            data.setKill(random.nextInt(100));
            data.setDeath(random.nextInt(100));
            data.setAssist(random.nextInt(100));
            data.setMetadata("scoreboard_debug", true);
            
            org.mods.gd656killicon.server.data.PlayerDataManager.get().forceSave(uuid);
        }

        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.debug_generated", count);
        return Command.SINGLE_SUCCESS;
    }

    private static int clearScoreboardDebugData(CommandContext<CommandSourceStack> c) {
        int removed = 0;
        var manager = org.mods.gd656killicon.server.data.PlayerDataManager.get();
        for (var entry : manager.getAllPlayerData().entrySet()) {
            org.mods.gd656killicon.server.data.PlayerData data = entry.getValue();
            Boolean marked = data.getMetadata("scoreboard_debug", Boolean.class);
            boolean nameMatch = isScoreboardDebugName(data.getLastLoginName());
            if (Boolean.TRUE.equals(marked) || nameMatch) {
                manager.removePlayerData(entry.getKey());
                removed++;
            }
        }
        ServerData.get().refreshScoreboard(c.getSource().getServer());
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.debug_removed", removed);
        return Command.SINGLE_SUCCESS;
    }

    private static boolean isScoreboardDebugName(String name) {
        if (name == null || name.isEmpty()) return false;
        for (String prefix : SCOREBOARD_DEBUG_PREFIXES) {
            if (!name.startsWith(prefix)) continue;
            String rest = name.substring(prefix.length());
            for (String suffix : SCOREBOARD_DEBUG_SUFFIXES) {
                if (!rest.startsWith(suffix)) continue;
                String digits = rest.substring(suffix.length());
                if (digits.isEmpty()) continue;
                boolean allDigits = true;
                for (int i = 0; i < digits.length(); i++) {
                    if (!Character.isDigit(digits.charAt(i))) {
                        allDigits = false;
                        break;
                    }
                }
                if (allDigits) return true;
            }
        }
        return false;
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

    private static int setNeutralVehicleSkip(CommandContext<CommandSourceStack> c, boolean val) {
        ServerData.get().setNeutralVehicleSkip(val);
        ServerLog.sendSuccess(c.getSource(), "gd656killicon.server.command.neutral_vehicle_skip_set", val);
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
            Double.parseDouble(expr);         } catch (NumberFormatException e) {
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
