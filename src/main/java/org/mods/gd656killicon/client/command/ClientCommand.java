package org.mods.gd656killicon.client.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.config.ClientConfigManager;
import org.mods.gd656killicon.client.config.ElementConfigManager;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.sounds.ExternalSoundManager;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;
import org.mods.gd656killicon.client.util.ClientMessageLogger;

public class ClientCommand {

    // Suggestions
    public static final SuggestionProvider<CommandSourceStack> PRESET_SUGGESTIONS = (context, builder) -> {
        ConfigManager.loadConfig();
        return SharedSuggestionProvider.suggest(ConfigManager.getPresetIds(), builder);
    };

    public static final SuggestionProvider<CommandSourceStack> ELEMENT_SUGGESTIONS = (context, builder) -> {
        String presetIdStr;
        try {
            presetIdStr = normalizePresetIdForLookup(StringArgumentType.getString(context, "presetId"));
        } catch (IllegalArgumentException e) {
            // Fallback to current preset if presetId argument is not available
            presetIdStr = ConfigManager.getCurrentPresetId();
        }
        return SharedSuggestionProvider.suggest(
            ConfigManager.getElementIds(presetIdStr).stream()
                .map(id -> "\"" + id + "\""), 
            builder
        );
    };

    public static final SuggestionProvider<CommandSourceStack> ADD_ELEMENT_SUGGESTIONS = (context, builder) -> {
        String presetIdStr;
        try {
            presetIdStr = normalizePresetIdForLookup(StringArgumentType.getString(context, "presetId"));
        } catch (IllegalArgumentException e) {
            presetIdStr = ConfigManager.getCurrentPresetId();
        }
        return SharedSuggestionProvider.suggest(
            ConfigManager.getAvailableElementTypes(presetIdStr).stream()
                .map(id -> "\"" + id + "\""), 
            builder
        );
    };

    public static final SuggestionProvider<CommandSourceStack> KEY_SUGGESTIONS = (context, builder) -> {
        String presetId = normalizePresetIdForLookup(StringArgumentType.getString(context, "presetId"));
        String elementId = StringArgumentType.getString(context, "elementId");
        if (elementId.startsWith("\"") && elementId.endsWith("\"")) {
            elementId = elementId.substring(1, elementId.length() - 1);
        }
        return SharedSuggestionProvider.suggest(ConfigManager.getConfigKeys(presetId, elementId), builder);
    };

    // Command Logic
    public static int reload(CommandContext<CommandSourceStack> context) {
        ConfigManager.loadConfig();
        ExternalTextureManager.reloadAsync();
        ExternalSoundManager.reloadAsync();
        // Feedback is handled by async managers
        return 1;
    }

    public static int reset(CommandContext<CommandSourceStack> context) {
        ConfigManager.resetFull();
        return 1;
    }

    public static int info(CommandContext<CommandSourceStack> context) {
        ClientMessageLogger.chatInfo("gd656killicon.client.command.info" + GuiConstants.MOD_VERSION);
        return 1;
    }

    public static int resetPresetConfig(CommandContext<CommandSourceStack> context) {
        String presetId = StringArgumentType.getString(context, "presetId");
        try {
            int idVal = Integer.parseInt(presetId);
            if (idVal < 0 || idVal > 99999) throw new NumberFormatException();
            presetId = String.format("%05d", idVal);
        } catch (NumberFormatException e) {
            ClientMessageLogger.chatError("gd656killicon.client.command.invalid_id_format");
            return 0;
        }
        
        // 重置预设配置：获取当前元素列表并从官方预设还原
        ConfigManager.resetPresetConfig(presetId);
        ClientMessageLogger.chatSuccess("gd656killicon.client.command.preset_reset_success", presetId);
        return 1;
    }

    public static int resetPresetTextures(CommandContext<CommandSourceStack> context) {
        String presetId = StringArgumentType.getString(context, "presetId");
        try {
            int idVal = Integer.parseInt(presetId);
            if (idVal < 0 || idVal > 99999) throw new NumberFormatException();
            presetId = String.format("%05d", idVal);
        } catch (NumberFormatException e) {
            ClientMessageLogger.chatError("gd656killicon.client.command.invalid_id_format");
            return 0;
        }
        ExternalTextureManager.resetTexturesAsync(presetId);
        return 1;
    }

    public static int resetPresetSounds(CommandContext<CommandSourceStack> context) {
        String presetId = StringArgumentType.getString(context, "presetId");
        try {
            int idVal = Integer.parseInt(presetId);
            if (idVal < 0 || idVal > 99999) throw new NumberFormatException();
            presetId = String.format("%05d", idVal);
        } catch (NumberFormatException e) {
            ClientMessageLogger.chatError("gd656killicon.client.command.invalid_id_format");
            return 0;
        }
        ExternalSoundManager.resetSoundsAsync(presetId);
        return 1;
    }

    public static int setPreset(CommandContext<CommandSourceStack> context) {
        String presetId = StringArgumentType.getString(context, "id");
        try {
            int idVal = Integer.parseInt(presetId);
            if (idVal < 0 || idVal > 99999) throw new NumberFormatException();
            presetId = String.format("%05d", idVal);
        } catch (NumberFormatException e) {
            ClientMessageLogger.chatError("gd656killicon.client.command.invalid_id_format");
            return 0;
        }
        ConfigManager.setCurrentPresetId(presetId);
        ClientMessageLogger.chatSuccess("gd656killicon.client.command.switch_success", presetId);
        return 1;
    }

    public static int createPreset(CommandContext<CommandSourceStack> context) {
        String presetId = StringArgumentType.getString(context, "id");
        try {
            int idVal = Integer.parseInt(presetId);
            if (idVal < 0 || idVal > 99999) throw new NumberFormatException();
            presetId = String.format("%05d", idVal);
        } catch (NumberFormatException e) {
            ClientMessageLogger.chatError("gd656killicon.client.command.invalid_id_format");
            return 0;
        }
        ConfigManager.createPreset(presetId);
        ExternalTextureManager.ensureTextureFilesForPreset(presetId);
        ClientMessageLogger.chatSuccess("gd656killicon.client.command.create_success", presetId);
        return 1;
    }

    public static int addElement(CommandContext<CommandSourceStack> context) {
        String elementId = StringArgumentType.getString(context, "elementId");
        if (elementId.startsWith("\"") && elementId.endsWith("\"")) {
            elementId = elementId.substring(1, elementId.length() - 1);
        }
        String presetId = StringArgumentType.getString(context, "presetId");
        try {
            int idVal = Integer.parseInt(presetId);
            if (idVal < 0 || idVal > 99999) throw new NumberFormatException();
            presetId = String.format("%05d", idVal);
        } catch (NumberFormatException e) {
            ClientMessageLogger.chatError("gd656killicon.client.command.invalid_preset_id_simple");
            return 0;
        }
        ConfigManager.addElementToPreset(presetId, elementId);
        return 1;
    }

    public static int delElement(CommandContext<CommandSourceStack> context) {
        String elementId = StringArgumentType.getString(context, "elementId");
        if (elementId.startsWith("\"") && elementId.endsWith("\"")) {
            elementId = elementId.substring(1, elementId.length() - 1);
        }
        String presetId = StringArgumentType.getString(context, "presetId");
        try {
            int idVal = Integer.parseInt(presetId);
            if (idVal < 0 || idVal > 99999) throw new NumberFormatException();
            presetId = String.format("%05d", idVal);
        } catch (NumberFormatException e) {
            ClientMessageLogger.chatError("gd656killicon.client.command.invalid_preset_id_simple");
            return 0;
        }
        ConfigManager.removeElementFromPreset(presetId, elementId);
        return 1;
    }

    public static int editConfig(CommandContext<CommandSourceStack> context) {
        String presetId = StringArgumentType.getString(context, "presetId");
        try {
            int idVal = Integer.parseInt(presetId);
            if (idVal < 0 || idVal > 99999) throw new NumberFormatException();
            presetId = String.format("%05d", idVal);
        } catch (NumberFormatException e) {
            ClientMessageLogger.chatError("gd656killicon.client.command.invalid_preset_id");
            return 0;
        }
        String elementId = StringArgumentType.getString(context, "elementId");
        if (elementId.startsWith("\"") && elementId.endsWith("\"")) {
            elementId = elementId.substring(1, elementId.length() - 1);
        }
        String key = StringArgumentType.getString(context, "key");
        String value = StringArgumentType.getString(context, "value");
        ConfigManager.updateConfigValue(presetId, elementId, key, value);
        return 1;
    }

    public static int setGlobalConfig(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key");
        String value = StringArgumentType.getString(context, "value");

        switch (key) {
            case "current_preset":
                try {
                    int idVal = Integer.parseInt(value);
                    if (idVal < 0 || idVal > 99999) throw new NumberFormatException();
                    value = String.format("%05d", idVal);
                } catch (NumberFormatException e) {
                     ClientMessageLogger.chatError("gd656killicon.client.command.invalid_id_format");
                     return 0;
                }
                ConfigManager.setCurrentPresetId(value);
                ClientMessageLogger.chatSuccess("gd656killicon.client.command.switch_success", value);
                break;
                
            case "enable_sound":
                boolean sound = Boolean.parseBoolean(value);
                ConfigManager.setEnableSound(sound);
                ClientMessageLogger.chatSuccess("gd656killicon.client.command.global_config_updated", key, value);
                break;
                
            case "show_bonus_message":
                boolean bonus = Boolean.parseBoolean(value);
                ConfigManager.setShowBonusMessage(bonus);
                ClientMessageLogger.chatSuccess("gd656killicon.client.command.global_config_updated", key, value);
                break;

            case "sound_volume":
                try {
                    int volume = Integer.parseInt(value);
                    if (volume < 0 || volume > 200) {
                        ClientMessageLogger.chatError("gd656killicon.client.command.global_config_invalid_value", value);
                        return 0;
                    }
                    ConfigManager.setSoundVolume(volume);
                    ClientMessageLogger.chatSuccess("gd656killicon.client.command.global_config_updated", key, value);
                } catch (NumberFormatException e) {
                    ClientMessageLogger.chatError("gd656killicon.client.command.global_config_invalid_value", value);
                    return 0;
                }
                break;

            default:
                ClientMessageLogger.chatError("gd656killicon.client.command.global_config_invalid_key", key);
                return 0;
        }
        return 1;
    }

    public static int setPresetDisplayName(CommandContext<CommandSourceStack> context) {
        String presetId = StringArgumentType.getString(context, "id");
        try {
            int idVal = Integer.parseInt(presetId);
            if (idVal < 0 || idVal > 99999) throw new NumberFormatException();
            presetId = String.format("%05d", idVal);
        } catch (NumberFormatException e) {
            ClientMessageLogger.chatError("gd656killicon.client.command.invalid_id_format");
            return 0;
        }
        String displayName = StringArgumentType.getString(context, "displayName");
        ElementConfigManager.setPresetDisplayName(presetId, displayName);
        return 1;
    }

    private static String normalizePresetIdForLookup(String presetId) {
        try {
            int idVal = Integer.parseInt(presetId);
            if (idVal < 0 || idVal > 99999) return presetId;
            return String.format("%05d", idVal);
        } catch (NumberFormatException e) {
            return presetId;
        }
    }

    public static void register(RegisterClientCommandsEvent event) {
        // GD656Killicon指令系统注册入口点
        event.getDispatcher().register(Commands.literal("gd656killicon")
            .then(Commands.literal("client")                                                                                                                  // 客户端
                .then(Commands.literal("info").executes(ClientCommand::info))                                                                                 //  客户端信息
                .then(Commands.literal("config")                                                                                                              //  配置文件设置
                    .then(Commands.literal("reload").executes(ClientCommand::reload))                                                                         //   重新加载配置文件
                    .then(Commands.literal("global")
                        .then(Commands.argument("key", StringArgumentType.word())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"current_preset", "enable_sound", "sound_volume", "show_bonus_message"}, builder))
                            .then(Commands.argument("value", StringArgumentType.string())
                                .executes(ClientCommand::setGlobalConfig)
                            )
                        )
                    )
                    .then(Commands.literal("reset").executes(ClientCommand::reset)                                                                           //   重置配置文件
                        .then(Commands.literal("element")
                            .then(Commands.argument("presetId", StringArgumentType.word())
                                .suggests(PRESET_SUGGESTIONS)
                                .then(Commands.literal("config").executes(ClientCommand::resetPresetConfig))
                                .then(Commands.literal("textures").executes(ClientCommand::resetPresetTextures))
                                .then(Commands.literal("sounds").executes(ClientCommand::resetPresetSounds))
                            )
                        )
                    )
                    .then(Commands.literal("preset")                                                                                                          //   预设
                        .then(Commands.literal("choose")                                                                                                         //     设置预设
                            .then(Commands.argument("id", StringArgumentType.word())                                                                          //       设置的预设ID
                                .suggests(PRESET_SUGGESTIONS)
                                .executes(ClientCommand::setPreset)
                            )
                        )
                        .then(Commands.literal("create")                                                                                                      //     创建预设
                            .then(Commands.argument("id", StringArgumentType.word())                                                                          //       创建的预设ID
                                .executes(ClientCommand::createPreset)
                            )
                        )
                        .then(Commands.literal("displayname")                                                                                                 //     设置预设名称
                            .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(PRESET_SUGGESTIONS)
                                .then(Commands.argument("displayName", StringArgumentType.string())
                                    .executes(ClientCommand::setPresetDisplayName)
                                )
                            )
                        )
                        .then(Commands.literal("element")                                                                                                     //     元素
                                .then(Commands.literal("add")                                                                                                     //       添加元素
                                    .then(Commands.argument("presetId", StringArgumentType.word())                                                            //         添加到的预设ID
                                        .suggests(PRESET_SUGGESTIONS)
                                        .then(Commands.argument("elementId", StringArgumentType.string())                                                             //         添加的元素ID
                                            .suggests(ADD_ELEMENT_SUGGESTIONS)
                                            .executes(ClientCommand::addElement)
                                        )
                                    )
                                )
                            .then(Commands.literal("del")                                                                                                      //       删除元素
                                .then(Commands.argument("presetId", StringArgumentType.word())                                                              //         删除从的预设ID
                                    .suggests(PRESET_SUGGESTIONS)
                                    .then(Commands.argument("elementId", StringArgumentType.string())                                                              //         删除的元素ID
                                        .suggests(ELEMENT_SUGGESTIONS)
                                        .executes(ClientCommand::delElement)
                                    )
                                )
                            )
                            .then(Commands.literal("edit")                                                                                                      //     编辑配置
                                .then(Commands.argument("presetId", StringArgumentType.word())                                                                  //       编辑的预设ID
                                    .suggests(PRESET_SUGGESTIONS)
                                    .then(Commands.argument("elementId", StringArgumentType.string())                                                           //       编辑的元素ID
                                        .suggests(ELEMENT_SUGGESTIONS)
                                        .then(Commands.argument("key", StringArgumentType.word())                                                               //         编辑的键
                                            .suggests(KEY_SUGGESTIONS)
                                            .then(Commands.argument("value", StringArgumentType.string())                                                       //         编辑的值
                                                .executes(ClientCommand::editConfig)
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        );
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(ClientCommand.class);
    }
    
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        register(event);
    }
}
