package org.mods.gd656killicon.client.sounds;

import com.google.gson.JsonObject;
import net.minecraft.util.Mth;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.common.KillType;

public class SoundTriggerManager {

    public static void tryPlaySound(String category, String name, int killType, int comboCount, boolean hasHelmet) {
        JsonObject config = ConfigManager.getElementConfig(category, name);
        if (config == null) {
            return;
        }

        boolean visible = !config.has("visible") || config.get("visible").getAsBoolean();
        if (!visible) {
            return;
        }

        if ("kill_icon".equals(category) && "card".equals(name)) {
            if (killType == KillType.HEADSHOT) {
                if (hasHelmet) {
                    ExternalSoundManager.playConfiguredSound(ConfigManager.getCurrentPresetId(), ExternalSoundManager.SLOT_CARD_ARMOR_HEADSHOT);
                } else {
                    ExternalSoundManager.playConfiguredSound(ConfigManager.getCurrentPresetId(), ExternalSoundManager.SLOT_CARD_HEADSHOT);
                }
            } else if (killType == KillType.EXPLOSION) {
                ExternalSoundManager.playConfiguredSound(ConfigManager.getCurrentPresetId(), ExternalSoundManager.SLOT_CARD_EXPLOSION);
            } else if (killType == KillType.CRIT) {
                ExternalSoundManager.playConfiguredSound(ConfigManager.getCurrentPresetId(), ExternalSoundManager.SLOT_CARD_CRIT);
            } else {
                ExternalSoundManager.playConfiguredSound(ConfigManager.getCurrentPresetId(), ExternalSoundManager.SLOT_CARD_DEFAULT);
            }
        } else if ("kill_icon".equals(category) && ("scrolling".equals(name) || "combo".equals(name))) {
            if ("combo".equals(name)) {
                int count = Mth.clamp(comboCount, 1, 6);
                String slotId = switch (count) {
                    case 1 -> ExternalSoundManager.SLOT_COMBO_1;
                    case 2 -> ExternalSoundManager.SLOT_COMBO_2;
                    case 3 -> ExternalSoundManager.SLOT_COMBO_3;
                    case 4 -> ExternalSoundManager.SLOT_COMBO_4;
                    case 5 -> ExternalSoundManager.SLOT_COMBO_5;
                    default -> ExternalSoundManager.SLOT_COMBO_6;
                };
                ExternalSoundManager.playConfiguredSound(ConfigManager.getCurrentPresetId(), slotId);
            } else {
                if (killType == KillType.HEADSHOT) {
                    ExternalSoundManager.playConfiguredSound(ConfigManager.getCurrentPresetId(), ExternalSoundManager.SLOT_SCROLLING_HEADSHOT);
                } else if (killType == KillType.DESTROY_VEHICLE) {
                    ExternalSoundManager.playConfiguredSound(ConfigManager.getCurrentPresetId(), ExternalSoundManager.SLOT_SCROLLING_VEHICLE);
                } else if (killType == KillType.EXPLOSION) {
                    ExternalSoundManager.playConfiguredSound(ConfigManager.getCurrentPresetId(), ExternalSoundManager.SLOT_SCROLLING_EXPLOSION);
                } else if (killType == KillType.CRIT) {
                    ExternalSoundManager.playConfiguredSound(ConfigManager.getCurrentPresetId(), ExternalSoundManager.SLOT_SCROLLING_CRIT);
                } else if (killType == KillType.ASSIST) {
                    ExternalSoundManager.playConfiguredSound(ConfigManager.getCurrentPresetId(), ExternalSoundManager.SLOT_SCROLLING_ASSIST);
                } else {
                    ExternalSoundManager.playConfiguredSound(ConfigManager.getCurrentPresetId(), ExternalSoundManager.SLOT_SCROLLING_DEFAULT);
                }
            }
        }
    }

    public static void playHitSound() {
        ExternalSoundManager.playConfiguredSound(ConfigManager.getCurrentPresetId(), ExternalSoundManager.SLOT_COMMON_HIT);
    }

    public static void playScoreSound() {
        ExternalSoundManager.playConfiguredSound(ConfigManager.getCurrentPresetId(), ExternalSoundManager.SLOT_COMMON_SCORE, true);
    }
}
