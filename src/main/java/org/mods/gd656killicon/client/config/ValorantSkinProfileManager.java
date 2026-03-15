package org.mods.gd656killicon.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.mods.gd656killicon.client.sounds.ExternalSoundManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ValorantSkinProfileManager {
    public static final String PROFILES_KEY = "valorant_skin_profiles";
    public static final String PROFILE_LABEL_KEY = "label";
    public static final String PROFILE_BASE_SKIN_KEY = "base_skin";
    public static final String PROFILE_SOUND_SELECTIONS_KEY = "sound_selections";
    public static final String CREATE_CUSTOM_SKIN_STYLE = "__create_custom_skin__";
    private static final Pattern ILLEGAL_SKIN_NAME_PATTERN = Pattern.compile("[\\\\/:*?\"<>|]");

    private static final List<String> PROFILE_KEYS = List.of(
        "enable_accent_tint",
        "color_accent",
        "brightness",
        "contrast",
        "particle_intensity",
        "particle_direction",
        "enable_custom_particle_color",
        "color_particle",
        "sound_volume",
        "icon_scale",
        "bar_scale",
        "bar_x_offset",
        "bar_y_offset",
        "bar_radius_offset",
        "texture_mode_icon",
        "texture_style_icon",
        "custom_texture_icon",
        "vanilla_texture_icon",
        "texture_mode_bar",
        "texture_style_bar",
        "custom_texture_bar",
        "vanilla_texture_bar",
        "anim_icon_enable_texture_animation",
        "anim_icon_texture_animation_total_frames",
        "anim_icon_texture_animation_interval_ms",
        "anim_icon_texture_animation_orientation",
        "anim_icon_texture_animation_loop",
        "anim_icon_texture_animation_play_style",
        "anim_icon_texture_frame_width_ratio",
        "anim_icon_texture_frame_height_ratio",
        "anim_bar_enable_texture_animation",
        "anim_bar_texture_animation_total_frames",
        "anim_bar_texture_animation_interval_ms",
        "anim_bar_texture_animation_orientation",
        "anim_bar_texture_animation_loop",
        "anim_bar_texture_animation_play_style",
        "anim_bar_texture_frame_width_ratio",
        "anim_bar_texture_frame_height_ratio"
    );

    private ValorantSkinProfileManager() {
    }

    public static String resolveActiveSkinStyle(JsonObject config) {
        if (config == null) {
            return ElementTextureDefinition.VALORANT_SKIN_PRIME;
        }

        if (config.has("skin_style")) {
            String stored = ElementTextureDefinition.normalizeValorantSkinStyle(config.get("skin_style").getAsString());
            if (ElementTextureDefinition.isValorantCustomSkinStyle(stored)) {
                return hasSkinProfile(config, stored) ? stored : ElementTextureDefinition.VALORANT_SKIN_PRIME;
            }
            if (ElementTextureDefinition.isBuiltInValorantSkinStyle(stored)) {
                return stored;
            }
        }

        String iconFile = config.has("texture_style_icon") ? config.get("texture_style_icon").getAsString() : "";
        String barFile = config.has("texture_style_bar") ? config.get("texture_style_bar").getAsString() : "";
        boolean gaiaIcon = iconFile.contains("valorant_gaia");
        boolean gaiaBar = barFile.contains("valorant_gaia");
        return gaiaIcon && gaiaBar
            ? ElementTextureDefinition.VALORANT_SKIN_GAIA
            : ElementTextureDefinition.VALORANT_SKIN_PRIME;
    }

    public static String resolveEffectiveBaseSkin(JsonObject config) {
        String style = resolveActiveSkinStyle(config);
        if (ElementTextureDefinition.VALORANT_SKIN_GAIA.equals(style)) {
            return ElementTextureDefinition.VALORANT_SKIN_GAIA;
        }
        if (!ElementTextureDefinition.isValorantCustomSkinStyle(style)) {
            return ElementTextureDefinition.VALORANT_SKIN_PRIME;
        }

        JsonObject profile = getSkinProfile(config, style);
        if (profile != null && profile.has(PROFILE_BASE_SKIN_KEY)) {
            return ElementTextureDefinition.normalizeBuiltInValorantSkinStyle(profile.get(PROFILE_BASE_SKIN_KEY).getAsString());
        }
        return ElementTextureDefinition.VALORANT_SKIN_PRIME;
    }

    public static boolean hasSkinProfile(JsonObject config, String skinStyle) {
        JsonObject profiles = getProfiles(config, false);
        return profiles != null && profiles.has(skinStyle) && profiles.get(skinStyle).isJsonObject();
    }

    public static JsonObject getSkinProfile(JsonObject config, String skinStyle) {
        JsonObject profiles = getProfiles(config, false);
        if (profiles == null || !profiles.has(skinStyle) || !profiles.get(skinStyle).isJsonObject()) {
            return null;
        }
        return profiles.getAsJsonObject(skinStyle);
    }

    public static List<SkinChoice> listSkinChoices(JsonObject config) {
        List<SkinChoice> choices = new ArrayList<>();
        choices.add(new SkinChoice(ElementTextureDefinition.VALORANT_SKIN_PRIME, "Prime", true));
        choices.add(new SkinChoice(ElementTextureDefinition.VALORANT_SKIN_GAIA, "Gaia", true));

        JsonObject profiles = getProfiles(config, false);
        if (profiles != null) {
            List<SkinChoice> customChoices = new ArrayList<>();
            for (String key : profiles.keySet()) {
                if (!ElementTextureDefinition.isValorantCustomSkinStyle(key)) {
                    continue;
                }
                JsonObject profile = profiles.getAsJsonObject(key);
                String label = profile.has(PROFILE_LABEL_KEY) ? profile.get(PROFILE_LABEL_KEY).getAsString() : ElementTextureDefinition.extractValorantCustomSkinId(key);
                customChoices.add(new SkinChoice(key, label, false));
            }
            customChoices.sort(Comparator.comparing(choice -> choice.label().toLowerCase(Locale.ROOT)));
            choices.addAll(customChoices);
        }

        return choices;
    }

    public static String resolveSkinLabel(JsonObject config, String skinStyle) {
        String normalized = ElementTextureDefinition.normalizeValorantSkinStyle(skinStyle);
        if (ElementTextureDefinition.isValorantCustomSkinStyle(normalized)) {
            JsonObject profile = getSkinProfile(config, normalized);
            if (profile != null && profile.has(PROFILE_LABEL_KEY)) {
                return profile.get(PROFILE_LABEL_KEY).getAsString();
            }
            return ElementTextureDefinition.extractValorantCustomSkinId(normalized);
        }
        return ElementTextureDefinition.VALORANT_SKIN_GAIA.equals(normalized) ? "Gaia" : "Prime";
    }

    public static String createCustomSkin(String presetId, JsonObject config, String displayName) {
        if (config == null) {
            return null;
        }

        String trimmed = displayName == null ? "" : displayName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        syncActiveSkinProfile(config);

        JsonObject profiles = getProfiles(config, true);
        String baseId = sanitizeSkinId(trimmed);
        String candidateId = baseId;
        int suffix = 2;
        while (profiles.has(ElementTextureDefinition.buildValorantCustomSkinStyle(candidateId))) {
            candidateId = baseId + "_" + suffix++;
        }

        String newStyle = ElementTextureDefinition.buildValorantCustomSkinStyle(candidateId);
        JsonObject snapshot = snapshotCurrentRootToProfile(config);
        copyActiveSkinSoundSelections(presetId, config, snapshot);
        snapshot.addProperty(PROFILE_LABEL_KEY, trimmed);
        snapshot.addProperty(PROFILE_BASE_SKIN_KEY, resolveEffectiveBaseSkin(config));
        profiles.add(newStyle, snapshot);
        config.addProperty("skin_style", newStyle);
        return newStyle;
    }

    public static boolean isValidCustomSkinDisplayName(String displayName) {
        String trimmed = displayName == null ? "" : displayName.trim();
        return !trimmed.isEmpty() && !ILLEGAL_SKIN_NAME_PATTERN.matcher(trimmed).find();
    }

    public static boolean hasSkinLabelConflict(JsonObject config, String displayName) {
        if (config == null) {
            return false;
        }
        String trimmed = displayName == null ? "" : displayName.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        String normalizedTarget = trimmed.toLowerCase(Locale.ROOT);
        Set<String> existing = new HashSet<>();
        existing.add("prime");
        existing.add("gaia");
        for (SkinChoice choice : listSkinChoices(config)) {
            String label = choice.label();
            if (label == null || label.isEmpty()) {
                continue;
            }
            existing.add(label.trim().toLowerCase(Locale.ROOT));
        }
        return existing.contains(normalizedTarget);
    }

    public static String createCustomSkin(JsonObject config, String displayName) {
        return createCustomSkin(null, config, displayName);
    }

    public static void applySkinStyle(JsonObject config, String targetStyle) {
        if (config == null) {
            return;
        }

        syncActiveSkinProfile(config);

        String normalized = ElementTextureDefinition.normalizeValorantSkinStyle(targetStyle);
        JsonObject profile = ensureSkinProfile(config, normalized, false);
        if (profile == null) {
            return;
        }

        applyProfileToRoot(config, profile);
        config.addProperty("skin_style", normalized);
    }

    public static void syncActiveSkinProfile(JsonObject config) {
        if (config == null) {
            return;
        }

        String style = resolveActiveSkinStyle(config);
        JsonObject profile = ensureSkinProfile(config, style, true);
        if (profile == null) {
            return;
        }

        JsonObject snapshot = snapshotCurrentRootToProfile(config);
        for (String key : snapshot.keySet()) {
            profile.add(key, snapshot.get(key));
        }

        if (ElementTextureDefinition.isValorantCustomSkinStyle(style)) {
            if (!profile.has(PROFILE_LABEL_KEY)) {
                profile.addProperty(PROFILE_LABEL_KEY, ElementTextureDefinition.extractValorantCustomSkinId(style));
            }
            if (!profile.has(PROFILE_BASE_SKIN_KEY)) {
                profile.addProperty(PROFILE_BASE_SKIN_KEY, resolveEffectiveBaseSkin(config));
            }
        } else {
            profile.addProperty(PROFILE_BASE_SKIN_KEY, ElementTextureDefinition.normalizeBuiltInValorantSkinStyle(style));
        }
    }

    public static String getActiveSkinSoundSelection(JsonObject config, String slotId) {
        return getSkinSoundSelection(config, resolveActiveSkinStyle(config), slotId);
    }

    public static String getSkinSoundSelection(JsonObject config, String skinStyle, String slotId) {
        JsonObject profile = getSkinProfile(config, skinStyle);
        if (profile == null || slotId == null) {
            return null;
        }

        JsonObject soundSelections = getSoundSelections(profile, false);
        if (soundSelections == null || !soundSelections.has(slotId)) {
            return null;
        }

        String baseName = soundSelections.get(slotId).getAsString();
        return baseName == null || baseName.isEmpty() ? null : baseName;
    }

    public static void setActiveSkinSoundSelection(JsonObject config, String slotId, String baseName) {
        if (config == null || slotId == null) {
            return;
        }

        JsonObject profile = ensureSkinProfile(config, resolveActiveSkinStyle(config), true);
        if (profile == null) {
            return;
        }

        JsonObject soundSelections = getSoundSelections(profile, true);
        if (baseName == null || baseName.isEmpty()) {
            soundSelections.remove(slotId);
        } else {
            soundSelections.addProperty(slotId, baseName);
        }
        cleanupSoundSelections(profile);
    }

    public static void clearActiveSkinSoundSelection(JsonObject config, String slotId) {
        if (config == null || slotId == null) {
            return;
        }

        JsonObject profile = getSkinProfile(config, resolveActiveSkinStyle(config));
        if (profile == null) {
            return;
        }

        JsonObject soundSelections = getSoundSelections(profile, false);
        if (soundSelections == null) {
            return;
        }

        soundSelections.remove(slotId);
        cleanupSoundSelections(profile);
    }

    public static void clearAllSkinSoundSelections(JsonObject config) {
        JsonObject profiles = getProfiles(config, false);
        if (profiles == null) {
            return;
        }

        for (String style : new ArrayList<>(profiles.keySet())) {
            JsonElement element = profiles.get(style);
            if (!element.isJsonObject()) {
                continue;
            }
            element.getAsJsonObject().remove(PROFILE_SOUND_SELECTIONS_KEY);
        }
    }

    public static void removeSoundFromAllSkinProfiles(JsonObject config, String baseName) {
        if (config == null || baseName == null || baseName.isEmpty()) {
            return;
        }

        JsonObject profiles = getProfiles(config, false);
        if (profiles == null) {
            return;
        }

        for (String style : new ArrayList<>(profiles.keySet())) {
            JsonElement element = profiles.get(style);
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject profile = element.getAsJsonObject();
            JsonObject soundSelections = getSoundSelections(profile, false);
            if (soundSelections == null) {
                continue;
            }

            List<String> toRemove = new ArrayList<>();
            for (String slotId : soundSelections.keySet()) {
                if (baseName.equals(soundSelections.get(slotId).getAsString())) {
                    toRemove.add(slotId);
                }
            }
            for (String slotId : toRemove) {
                soundSelections.remove(slotId);
            }
            cleanupSoundSelections(profile);
        }
    }

    public static String deleteSkinProfile(JsonObject config, String skinStyle) {
        if (config == null) {
            return ElementTextureDefinition.VALORANT_SKIN_PRIME;
        }

        String normalized = ElementTextureDefinition.normalizeValorantSkinStyle(skinStyle);
        if (!ElementTextureDefinition.isValorantCustomSkinStyle(normalized)) {
            return resolveActiveSkinStyle(config);
        }

        JsonObject profiles = getProfiles(config, false);
        JsonObject profile = getSkinProfile(config, normalized);
        if (profiles == null || profile == null) {
            return resolveActiveSkinStyle(config);
        }

        String fallbackStyle = ElementTextureDefinition.VALORANT_SKIN_PRIME;
        if (profile.has(PROFILE_BASE_SKIN_KEY)) {
            fallbackStyle = ElementTextureDefinition.normalizeBuiltInValorantSkinStyle(profile.get(PROFILE_BASE_SKIN_KEY).getAsString());
        }

        boolean deletingActiveSkin = normalized.equals(resolveActiveSkinStyle(config));
        profiles.remove(normalized);

        if (!deletingActiveSkin) {
            return resolveActiveSkinStyle(config);
        }

        JsonObject fallbackProfile = ensureSkinProfile(config, fallbackStyle, false);
        if (fallbackProfile != null) {
            applyProfileToRoot(config, fallbackProfile);
        }
        config.addProperty("skin_style", fallbackStyle);
        return fallbackStyle;
    }

    public static String importSkinProfile(JsonObject config, String displayName, String baseSkin, JsonObject importedProfile, boolean activate) {
        if (config == null || importedProfile == null) {
            return null;
        }

        syncActiveSkinProfile(config);

        JsonObject profiles = getProfiles(config, true);
        String trimmed = displayName == null ? "" : displayName.trim();
        if (trimmed.isEmpty()) {
            trimmed = "Imported Skin";
        }

        String baseId = sanitizeSkinId(trimmed);
        String candidateId = baseId;
        int suffix = 2;
        while (profiles.has(ElementTextureDefinition.buildValorantCustomSkinStyle(candidateId))) {
            candidateId = baseId + "_" + suffix++;
        }

        String normalizedBaseSkin = ElementTextureDefinition.normalizeBuiltInValorantSkinStyle(baseSkin);
        String newStyle = ElementTextureDefinition.buildValorantCustomSkinStyle(candidateId);
        JsonObject profile = createDefaultProfile(normalizedBaseSkin);
        for (String key : PROFILE_KEYS) {
            if (importedProfile.has(key)) {
                profile.add(key, importedProfile.get(key).deepCopy());
            }
        }
        if (importedProfile.has(PROFILE_SOUND_SELECTIONS_KEY) && importedProfile.get(PROFILE_SOUND_SELECTIONS_KEY).isJsonObject()) {
            profile.add(PROFILE_SOUND_SELECTIONS_KEY, importedProfile.get(PROFILE_SOUND_SELECTIONS_KEY).deepCopy());
        }
        profile.addProperty(PROFILE_LABEL_KEY, trimmed);
        profile.addProperty(PROFILE_BASE_SKIN_KEY, normalizedBaseSkin);
        profiles.add(newStyle, profile);

        if (activate) {
            applyProfileToRoot(config, profile);
            config.addProperty("skin_style", newStyle);
        }
        return newStyle;
    }

    private static JsonObject ensureSkinProfile(JsonObject config, String skinStyle, boolean snapshotCurrentIfMissing) {
        if (config == null) {
            return null;
        }

        JsonObject profiles = getProfiles(config, true);
        if (profiles.has(skinStyle) && profiles.get(skinStyle).isJsonObject()) {
            return profiles.getAsJsonObject(skinStyle);
        }

        JsonObject profile = snapshotCurrentIfMissing
            ? snapshotCurrentRootToProfile(config)
            : createDefaultProfile(skinStyle);

        if (profile == null) {
            return null;
        }

        if (ElementTextureDefinition.isValorantCustomSkinStyle(skinStyle)) {
            profile.addProperty(PROFILE_LABEL_KEY, ElementTextureDefinition.extractValorantCustomSkinId(skinStyle));
            profile.addProperty(PROFILE_BASE_SKIN_KEY, resolveEffectiveBaseSkin(config));
        } else {
            profile.addProperty(PROFILE_BASE_SKIN_KEY, ElementTextureDefinition.normalizeBuiltInValorantSkinStyle(skinStyle));
        }

        profiles.add(skinStyle, profile);
        return profile;
    }

    private static JsonObject createDefaultProfile(String skinStyle) {
        JsonObject defaults = ElementConfigManager.getDefaultElementConfig("kill_icon/valorant");
        JsonObject profile = new JsonObject();
        for (String key : PROFILE_KEYS) {
            if (defaults.has(key)) {
                profile.add(key, defaults.get(key).deepCopy());
            }
        }

        String builtIn = ElementTextureDefinition.normalizeBuiltInValorantSkinStyle(skinStyle);
        profile.addProperty("texture_mode_icon", "official");
        profile.addProperty("texture_mode_bar", "official");
        profile.addProperty("texture_style_icon", ElementTextureDefinition.getValorantSkinTextureFileName(builtIn, "icon"));
        profile.addProperty("texture_style_bar", ElementTextureDefinition.getValorantSkinTextureFileName(builtIn, "bar"));
        return profile;
    }

    private static JsonObject snapshotCurrentRootToProfile(JsonObject config) {
        JsonObject defaults = ElementConfigManager.getDefaultElementConfig("kill_icon/valorant");
        JsonObject snapshot = new JsonObject();
        for (String key : PROFILE_KEYS) {
            if (config.has(key)) {
                snapshot.add(key, config.get(key).deepCopy());
            } else if (defaults.has(key)) {
                snapshot.add(key, defaults.get(key).deepCopy());
            }
        }
        return snapshot;
    }

    private static void applyProfileToRoot(JsonObject config, JsonObject profile) {
        JsonObject defaults = ElementConfigManager.getDefaultElementConfig("kill_icon/valorant");
        for (String key : PROFILE_KEYS) {
            if (profile.has(key)) {
                config.add(key, profile.get(key).deepCopy());
            } else if (defaults.has(key)) {
                config.add(key, defaults.get(key).deepCopy());
            }
        }
    }

    private static JsonObject getProfiles(JsonObject config, boolean create) {
        if (config == null) {
            return null;
        }
        if (config.has(PROFILES_KEY) && config.get(PROFILES_KEY).isJsonObject()) {
            return config.getAsJsonObject(PROFILES_KEY);
        }
        if (!create) {
            return null;
        }
        JsonObject profiles = new JsonObject();
        config.add(PROFILES_KEY, profiles);
        return profiles;
    }

    private static JsonObject getSoundSelections(JsonObject profile, boolean create) {
        if (profile == null) {
            return null;
        }
        if (profile.has(PROFILE_SOUND_SELECTIONS_KEY) && profile.get(PROFILE_SOUND_SELECTIONS_KEY).isJsonObject()) {
            return profile.getAsJsonObject(PROFILE_SOUND_SELECTIONS_KEY);
        }
        if (!create) {
            return null;
        }
        JsonObject soundSelections = new JsonObject();
        profile.add(PROFILE_SOUND_SELECTIONS_KEY, soundSelections);
        return soundSelections;
    }

    private static void cleanupSoundSelections(JsonObject profile) {
        JsonObject soundSelections = getSoundSelections(profile, false);
        if (soundSelections != null && soundSelections.entrySet().isEmpty()) {
            profile.remove(PROFILE_SOUND_SELECTIONS_KEY);
        }
    }

    private static void copyActiveSkinSoundSelections(String presetId, JsonObject config, JsonObject targetProfile) {
        if (presetId == null || config == null || targetProfile == null) {
            return;
        }

        JsonObject soundSelections = new JsonObject();
        for (String slotId : List.of(
            ExternalSoundManager.SLOT_COMBO_1,
            ExternalSoundManager.SLOT_COMBO_2,
            ExternalSoundManager.SLOT_COMBO_3,
            ExternalSoundManager.SLOT_COMBO_4,
            ExternalSoundManager.SLOT_COMBO_5,
            ExternalSoundManager.SLOT_COMBO_6
        )) {
            String baseName = ExternalSoundManager.getSelectedSoundBaseName(presetId, slotId);
            if (baseName != null && !baseName.isEmpty()) {
                soundSelections.addProperty(slotId, baseName);
            }
        }

        if (!soundSelections.entrySet().isEmpty()) {
            targetProfile.add(PROFILE_SOUND_SELECTIONS_KEY, soundSelections);
        }
    }

    private static String sanitizeSkinId(String raw) {
        String normalized = raw.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+", "")
            .replaceAll("_+$", "");
        return normalized.isEmpty() ? "imported_skin" : normalized;
    }

    public record SkinChoice(String style, String label, boolean builtIn) {
    }
}
