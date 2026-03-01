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

    public static final SuggestionProvider<CommandSourceStack> PRESET_SUGGESTIONS = (context, builder) -> {
        ConfigManager.loadConfig();
        return SharedSuggestionProvider.suggest(ConfigManager.getPresetIds(), builder);
    };

    public static final SuggestionProvider<CommandSourceStack> ELEMENT_SUGGESTIONS = (context, builder) -> {
        String presetIdStr;
        try {
            presetIdStr = normalizePresetIdForLookup(StringArgumentType.getString(context, "presetId"));
        } catch (IllegalArgumentException e) {
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

    public static int reload(CommandContext<CommandSourceStack> context) {
        ConfigManager.loadConfig();
        ExternalTextureManager.reloadAsync();
        ExternalSoundManager.reloadAsync();
        return 1;
    }

    public static int reset(CommandContext<CommandSourceStack> context) {
        ConfigManager.resetFull();
        return 1;
    }

    public static int info(CommandContext<CommandSourceStack> context) {
        ClientMessageLogger.chatInfo("gd656killicon.client.command.info", GuiConstants.MOD_VERSION);
        return 1;
    }

    public static int iamanew(CommandContext<CommandSourceStack> context) {
        ClientConfigManager.resetIntroPrompts();
        ClientMessageLogger.chatSuccess("gd656killicon.client.command.iamanew");
        return 1;
    }

    public static int versionSet(CommandContext<CommandSourceStack> context) {
        String value = StringArgumentType.getString(context, "value");
        ClientConfigManager.setRecordedModVersion(value);
        ClientMessageLogger.chatSuccess("gd656killicon.client.command.versionset", value);
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
        event.getDispatcher().register(Commands.literal("gd656killicon")
            .then(Commands.literal("client")
                .then(Commands.literal("info").executes(ClientCommand::info))
                .then(Commands.literal("debug")
                    .then(Commands.literal("iamanew").executes(ClientCommand::iamanew))
                    .then(Commands.literal("versionset")
                        .then(Commands.argument("value", StringArgumentType.string())
                            .executes(ClientCommand::versionSet)
                        )
                    )
                )
                .then(Commands.literal("config")
                    .then(Commands.literal("reload").executes(ClientCommand::reload))
                    .then(Commands.literal("global")
                        .then(Commands.argument("key", StringArgumentType.word())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"current_preset", "enable_sound", "sound_volume", "show_bonus_message"}, builder))
                            .then(Commands.argument("value", StringArgumentType.string())
                                .executes(ClientCommand::setGlobalConfig)
                            )
                        )
                    )
                )
                .then(Commands.literal("reset").executes(ClientCommand::reset)
                    .then(Commands.literal("element")
                        .then(Commands.argument("presetId", StringArgumentType.word())
                            .suggests(PRESET_SUGGESTIONS)
                            .then(Commands.literal("config").executes(ClientCommand::resetPresetConfig))
                            .then(Commands.literal("textures").executes(ClientCommand::resetPresetTextures))
                            .then(Commands.literal("sounds").executes(ClientCommand::resetPresetSounds))
                        )
                    )
                )
                .then(Commands.literal("preset")
                    .then(Commands.literal("choose")
                        .then(Commands.argument("id", StringArgumentType.word())
                            .suggests(PRESET_SUGGESTIONS)
                            .executes(ClientCommand::setPreset)
                        )
                    )
                    .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                            .executes(ClientCommand::createPreset)
                        )
                    )
                    .then(Commands.literal("displayname")
                        .then(Commands.argument("id", StringArgumentType.word())
                            .suggests(PRESET_SUGGESTIONS)
                            .then(Commands.argument("displayName", StringArgumentType.string())
                                .executes(ClientCommand::setPresetDisplayName)
                            )
                        )
                    )
                    .then(Commands.literal("element")
                        .then(Commands.literal("add")
                            .then(Commands.argument("presetId", StringArgumentType.word())
                                .suggests(PRESET_SUGGESTIONS)
                                .then(Commands.argument("elementId", StringArgumentType.string())
                                    .suggests(ADD_ELEMENT_SUGGESTIONS)
                                    .executes(ClientCommand::addElement)
                                )
                            )
                        )
                        .then(Commands.literal("del")
                            .then(Commands.argument("presetId", StringArgumentType.word())
                                .suggests(PRESET_SUGGESTIONS)
                                .then(Commands.argument("elementId", StringArgumentType.string())
                                    .suggests(ELEMENT_SUGGESTIONS)
                                    .executes(ClientCommand::delElement)
                                )
                            )
                        )
                        .then(Commands.literal("edit")
                            .then(Commands.argument("presetId", StringArgumentType.word())
                                .suggests(PRESET_SUGGESTIONS)
                                .then(Commands.argument("elementId", StringArgumentType.string())
                                    .suggests(ELEMENT_SUGGESTIONS)
                                    .then(Commands.argument("key", StringArgumentType.word())
                                        .suggests(KEY_SUGGESTIONS)
                                        .then(Commands.argument("value", StringArgumentType.string())
                                            .executes(ClientCommand::editConfig)
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
