package org.mods.gd656killicon.client.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent.ClientCommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.config.BonusFormatMigrator;
import org.mods.gd656killicon.client.config.ClientConfigManager;
import org.mods.gd656killicon.client.config.ElementConfigManager;
import org.mods.gd656killicon.client.gui.GuiConstants;
import org.mods.gd656killicon.client.sounds.ExternalSoundManager;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;
import org.mods.gd656killicon.client.util.ClientMessageLogger;

public class ClientCommand {

    public static final SuggestionProvider<ClientCommandSourceStack> PRESET_SUGGESTIONS = (context, builder) -> {
        ConfigManager.loadConfig();
        return SharedSuggestionProvider.suggest(ConfigManager.getPresetIds(), builder);
    };

    public static final SuggestionProvider<ClientCommandSourceStack> ELEMENT_SUGGESTIONS = (context, builder) -> {
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

    public static final SuggestionProvider<ClientCommandSourceStack> ADD_ELEMENT_SUGGESTIONS = (context, builder) -> {
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

    public static final SuggestionProvider<ClientCommandSourceStack> KEY_SUGGESTIONS = (context, builder) -> {
        String presetId = normalizePresetIdForLookup(StringArgumentType.getString(context, "presetId"));
        String elementId = StringArgumentType.getString(context, "elementId");
        if (elementId.startsWith("\"") && elementId.endsWith("\"")) {
            elementId = elementId.substring(1, elementId.length() - 1);
        }
        return SharedSuggestionProvider.suggest(ConfigManager.getConfigKeys(presetId, elementId), builder);
    };

    public static int reload(CommandContext<ClientCommandSourceStack> context) {
        ConfigManager.loadConfig();
        ExternalTextureManager.reloadAsync();
        ExternalSoundManager.reloadAsync();
        return 1;
    }

    /** 手动触发加分项系统大清洗升级器（通常由 loadConfig 自动执行一次）。 */
    public static int migrateBonusFormat(CommandContext<ClientCommandSourceStack> context) {
        boolean changed = BonusFormatMigrator.migrate();
        ClientConfigManager.setBonusFormatMigrated(BonusFormatMigrator.TARGET_VERSION);
        if (changed) {
            ClientMessageLogger.chatSuccess("gd656killicon.client.command.bonus_migrate_success");
        } else {
            ClientMessageLogger.chatInfo("gd656killicon.client.command.bonus_migrate_noop");
        }
        return 1;
    }

    public static int reset(CommandContext<ClientCommandSourceStack> context) {
        ConfigManager.resetFull();
        return 1;
    }

    public static int info(CommandContext<ClientCommandSourceStack> context) {
        ClientMessageLogger.chatInfo("gd656killicon.client.command.info", GuiConstants.MOD_VERSION);
        return 1;
    }

    public static int iamanew(CommandContext<ClientCommandSourceStack> context) {
        ClientConfigManager.resetIntroPrompts();
        ClientMessageLogger.chatSuccess("gd656killicon.client.command.iamanew");
        return 1;
    }

    public static int versionSet(CommandContext<ClientCommandSourceStack> context) {
        String value = StringArgumentType.getString(context, "value");
        ClientConfigManager.setRecordedModVersion(value);
        ClientMessageLogger.chatSuccess("gd656killicon.client.command.versionset", value);
        return 1;
    }

    public static int resetPresetConfig(CommandContext<ClientCommandSourceStack> context) {
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

    public static int resetPresetTextures(CommandContext<ClientCommandSourceStack> context) {
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

    public static int resetPresetSounds(CommandContext<ClientCommandSourceStack> context) {
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

    public static int setPreset(CommandContext<ClientCommandSourceStack> context) {
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

    public static int createPreset(CommandContext<ClientCommandSourceStack> context) {
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

    public static int addElement(CommandContext<ClientCommandSourceStack> context) {
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

    public static int delElement(CommandContext<ClientCommandSourceStack> context) {
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

    public static int editConfig(CommandContext<ClientCommandSourceStack> context) {
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

    public static int setGlobalConfig(CommandContext<ClientCommandSourceStack> context) {
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

    public static int setPresetDisplayName(CommandContext<ClientCommandSourceStack> context) {
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

    public static void register(CommandDispatcher<ClientCommandSourceStack> dispatcher) {
        dispatcher.register(ClientCommandRegistrationEvent.literal("gd656killicon")
            .then(ClientCommandRegistrationEvent.literal("client")
                .then(ClientCommandRegistrationEvent.literal("info").executes(ClientCommand::info))
                .then(ClientCommandRegistrationEvent.literal("debug")
                    .then(ClientCommandRegistrationEvent.literal("iamanew").executes(ClientCommand::iamanew))
                    .then(ClientCommandRegistrationEvent.literal("versionset")
                        .then(ClientCommandRegistrationEvent.argument("value", StringArgumentType.string())
                            .executes(ClientCommand::versionSet)
                        )
                    )
                )
                .then(ClientCommandRegistrationEvent.literal("config")
                    .then(ClientCommandRegistrationEvent.literal("reload").executes(ClientCommand::reload))
                    .then(ClientCommandRegistrationEvent.literal("migrate").executes(ClientCommand::migrateBonusFormat))
                    .then(ClientCommandRegistrationEvent.literal("global")
                        .then(ClientCommandRegistrationEvent.argument("key", StringArgumentType.word())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"current_preset", "enable_sound", "sound_volume", "show_bonus_message"}, builder))
                            .then(ClientCommandRegistrationEvent.argument("value", StringArgumentType.string())
                                .executes(ClientCommand::setGlobalConfig)
                            )
                        )
                    )
                )
                .then(ClientCommandRegistrationEvent.literal("reset").executes(ClientCommand::reset)
                    .then(ClientCommandRegistrationEvent.literal("element")
                        .then(ClientCommandRegistrationEvent.argument("presetId", StringArgumentType.word())
                            .suggests(PRESET_SUGGESTIONS)
                            .then(ClientCommandRegistrationEvent.literal("config").executes(ClientCommand::resetPresetConfig))
                            .then(ClientCommandRegistrationEvent.literal("textures").executes(ClientCommand::resetPresetTextures))
                            .then(ClientCommandRegistrationEvent.literal("sounds").executes(ClientCommand::resetPresetSounds))
                        )
                    )
                )
                .then(ClientCommandRegistrationEvent.literal("preset")
                    .then(ClientCommandRegistrationEvent.literal("choose")
                        .then(ClientCommandRegistrationEvent.argument("id", StringArgumentType.word())
                            .suggests(PRESET_SUGGESTIONS)
                            .executes(ClientCommand::setPreset)
                        )
                    )
                    .then(ClientCommandRegistrationEvent.literal("create")
                        .then(ClientCommandRegistrationEvent.argument("id", StringArgumentType.word())
                            .executes(ClientCommand::createPreset)
                        )
                    )
                    .then(ClientCommandRegistrationEvent.literal("displayname")
                        .then(ClientCommandRegistrationEvent.argument("id", StringArgumentType.word())
                            .suggests(PRESET_SUGGESTIONS)
                            .then(ClientCommandRegistrationEvent.argument("displayName", StringArgumentType.string())
                                .executes(ClientCommand::setPresetDisplayName)
                            )
                        )
                    )
                    .then(ClientCommandRegistrationEvent.literal("element")
                        .then(ClientCommandRegistrationEvent.literal("add")
                            .then(ClientCommandRegistrationEvent.argument("presetId", StringArgumentType.word())
                                .suggests(PRESET_SUGGESTIONS)
                                .then(ClientCommandRegistrationEvent.argument("elementId", StringArgumentType.string())
                                    .suggests(ADD_ELEMENT_SUGGESTIONS)
                                    .executes(ClientCommand::addElement)
                                )
                            )
                        )
                        .then(ClientCommandRegistrationEvent.literal("del")
                            .then(ClientCommandRegistrationEvent.argument("presetId", StringArgumentType.word())
                                .suggests(PRESET_SUGGESTIONS)
                                .then(ClientCommandRegistrationEvent.argument("elementId", StringArgumentType.string())
                                    .suggests(ELEMENT_SUGGESTIONS)
                                    .executes(ClientCommand::delElement)
                                )
                            )
                        )
                        .then(ClientCommandRegistrationEvent.literal("edit")
                            .then(ClientCommandRegistrationEvent.argument("presetId", StringArgumentType.word())
                                .suggests(PRESET_SUGGESTIONS)
                                .then(ClientCommandRegistrationEvent.argument("elementId", StringArgumentType.string())
                                    .suggests(ELEMENT_SUGGESTIONS)
                                    .then(ClientCommandRegistrationEvent.argument("key", StringArgumentType.word())
                                        .suggests(KEY_SUGGESTIONS)
                                        .then(ClientCommandRegistrationEvent.argument("value", StringArgumentType.string())
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
        ClientCommandRegistrationEvent.EVENT.register((dispatcher, buildContext) -> register(dispatcher));
    }
}
