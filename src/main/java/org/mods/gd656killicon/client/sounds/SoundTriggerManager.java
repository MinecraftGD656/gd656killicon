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
                    ExternalSoundManager.playSound("cardkillsound_armorheadshot_cs");
                } else {
                    ExternalSoundManager.playSound("cardkillsound_headshot_cs");
                }
            } else if (killType == KillType.EXPLOSION) {
                ExternalSoundManager.playSound("cardkillsound_explosion_cs");
            } else if (killType == KillType.CRIT) {
                ExternalSoundManager.playSound("cardkillsound_crit_cs");
            } else {
                ExternalSoundManager.playSound("cardkillsound_default_cs");
            }
        } else if ("kill_icon".equals(category) && ("scrolling".equals(name) || "combo".equals(name))) {
            if ("combo".equals(name)) {
                int count = Mth.clamp(comboCount, 1, 6);
                ExternalSoundManager.playSound("combokillsound_" + count + "_cf");
            } else {
                if ("00007".equals(ConfigManager.getCurrentPresetId())) {
                    if (killType == KillType.HEADSHOT) {
                        ExternalSoundManager.playSound("headshotkillsound_bf5");
                    } else if (killType == KillType.DESTROY_VEHICLE) {
                        ExternalSoundManager.playSound("vehiclekillsound_bf5");
                    } else if (killType == KillType.EXPLOSION) {
                        ExternalSoundManager.playSound("headshotkillsound_bf5");
                    } else if (killType == KillType.CRIT) {
                        ExternalSoundManager.playSound("killsound_bf5");
                    } else if (killType == KillType.ASSIST) {
                        ExternalSoundManager.playSound("defaulticonsound_df");
                    } else {
                        ExternalSoundManager.playSound("killsound_bf5");
                    }
                } else {
                    if (killType == KillType.HEADSHOT) {
                        ExternalSoundManager.playSound("headshotkillsound_df");
                    } else if (killType == KillType.EXPLOSION || killType == KillType.DESTROY_VEHICLE) {
                        ExternalSoundManager.playSound("explosionkillsound_df");
                    } else if (killType == KillType.CRIT) {
                        ExternalSoundManager.playSound("critkillsound_df");
                    } else if (killType == KillType.ASSIST) {
                        ExternalSoundManager.playSound("defaulticonsound_df");
                    } else {
                        ExternalSoundManager.playSound("killsound_df");
                    }
                }
            }
        }
    }

    public static void playHitSound() {
        ExternalSoundManager.playSound("hitsound_df");
    }

    public static void playScoreSound() {
        ExternalSoundManager.playSound("addscore_df", true);
    }
}
