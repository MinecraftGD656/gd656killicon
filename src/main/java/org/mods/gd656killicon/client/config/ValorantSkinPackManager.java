package org.mods.gd656killicon.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.loading.FMLPaths;
import org.mods.gd656killicon.client.sounds.ExternalSoundManager;
import org.mods.gd656killicon.client.textures.ExternalTextureManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class ValorantSkinPackManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path EXPORT_DIR = FMLPaths.CONFIGDIR.get().resolve("gd656killicon").resolve("export");
    private static final String PACK_TYPE = "valorant_skin_pack";
    private static final int PACK_VERSION = 1;
    private static final String MANIFEST_FILE = "valorant_skin.json";
    private static final List<String> ROOT_SETTING_KEYS = List.of(
        "visible",
        "scale",
        "x_offset",
        "y_offset",
        "display_duration"
    );

    private ValorantSkinPackManager() {
    }

    public static Path getExportDir() {
        try {
            Files.createDirectories(EXPORT_DIR);
        } catch (IOException ignored) {
        }
        return EXPORT_DIR;
    }

    public static boolean exportCurrentSkinPack(String presetId, Path targetFile) {
        if (presetId == null || targetFile == null) {
            return false;
        }

        JsonObject config = ElementConfigManager.getElementConfig(presetId, "kill_icon/valorant");
        if (config == null) {
            return false;
        }

        ValorantSkinProfileManager.syncActiveSkinProfile(config);
        String activeStyle = ValorantSkinProfileManager.resolveActiveSkinStyle(config);
        String label = ValorantSkinProfileManager.resolveSkinLabel(config, activeStyle);
        String baseSkin = ValorantSkinProfileManager.resolveEffectiveBaseSkin(config);
        JsonObject profile = ValorantSkinProfileManager.getSkinProfile(config, activeStyle);
        if (profile == null) {
            return false;
        }

        JsonObject manifest = new JsonObject();
        manifest.addProperty("type", PACK_TYPE);
        manifest.addProperty("format_version", PACK_VERSION);
        manifest.addProperty("label", label);
        manifest.addProperty("base_skin", baseSkin);
        manifest.add("profile", profile.deepCopy());
        manifest.add("element_settings", snapshotElementSettings(config));

        JsonObject textureAssets = new JsonObject();
        Map<String, String> textureLabels = ExternalTextureManager.getCustomTextureLabels(presetId);
        appendTextureAsset(presetId, profile, "icon", textureAssets, textureLabels);
        appendTextureAsset(presetId, profile, "bar", textureAssets, textureLabels);
        if (!textureAssets.entrySet().isEmpty()) {
            manifest.add("texture_assets", textureAssets);
        }

        JsonObject soundAssets = new JsonObject();
        JsonObject soundSelections = profile.has(ValorantSkinProfileManager.PROFILE_SOUND_SELECTIONS_KEY)
            && profile.get(ValorantSkinProfileManager.PROFILE_SOUND_SELECTIONS_KEY).isJsonObject()
            ? profile.getAsJsonObject(ValorantSkinProfileManager.PROFILE_SOUND_SELECTIONS_KEY)
            : null;
        Map<String, String> soundLabels = ExternalSoundManager.getCustomSoundLabels(presetId);
        if (soundSelections != null) {
            for (String slotId : soundSelections.keySet()) {
                String baseName = soundSelections.get(slotId).getAsString();
                if (baseName == null || !baseName.startsWith("custom_")) {
                    continue;
                }
                try {
                    String extension = ExternalSoundManager.getSoundExtensionForPreset(presetId, baseName).toLowerCase();
                    JsonObject entry = new JsonObject();
                    entry.addProperty("file", "sounds/" + slotId + "." + extension);
                    entry.addProperty("label", soundLabels.getOrDefault(baseName, baseName));
                    soundAssets.add(slotId, entry);
                } catch (Exception ignored) {
                }
            }
        }
        if (!soundAssets.entrySet().isEmpty()) {
            manifest.add("sound_assets", soundAssets);
        }

        try {
            if (targetFile.getParent() != null) {
                Files.createDirectories(targetFile.getParent());
            }
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(targetFile))) {
                writeJsonEntry(zos, MANIFEST_FILE, manifest);

                for (Map.Entry<String, JsonElement> entry : textureAssets.entrySet()) {
                    JsonObject asset = entry.getValue().getAsJsonObject();
                    String fileName = profile.get("custom_texture_" + entry.getKey()).getAsString();
                    writeBinaryEntry(zos, asset.get("file").getAsString(), ExternalTextureManager.readTextureBytes(presetId, fileName));
                }

                for (Map.Entry<String, JsonElement> entry : soundAssets.entrySet()) {
                    String slotId = entry.getKey();
                    JsonObject asset = entry.getValue().getAsJsonObject();
                    String baseName = soundSelections.get(slotId).getAsString();
                    writeBinaryEntry(zos, asset.get("file").getAsString(), ExternalSoundManager.readSoundBytes(presetId, baseName));
                }
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static ImportResult importSkinPack(String presetId, Path packFile) {
        if (presetId == null || packFile == null || !Files.exists(packFile)) {
            return null;
        }

        try (ZipFile zipFile = new ZipFile(packFile.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry manifestEntry = zipFile.getEntry(MANIFEST_FILE);
            if (manifestEntry == null) {
                return null;
            }

            JsonObject manifest;
            try (InputStreamReader reader = new InputStreamReader(zipFile.getInputStream(manifestEntry), StandardCharsets.UTF_8)) {
                manifest = GSON.fromJson(reader, JsonObject.class);
            }
            if (manifest == null || !manifest.has("type") || !PACK_TYPE.equals(manifest.get("type").getAsString())) {
                return null;
            }

            JsonObject config = ElementConfigManager.getElementConfig(presetId, "kill_icon/valorant");
            if (config == null) {
                config = ElementConfigManager.getDefaultElementConfig("kill_icon/valorant");
            } else {
                config = config.deepCopy();
            }

            String label = manifest.has("label") ? manifest.get("label").getAsString() : "Imported Skin";
            String baseSkin = manifest.has("base_skin") ? manifest.get("base_skin").getAsString() : ElementTextureDefinition.VALORANT_SKIN_PRIME;
            JsonObject profile = manifest.has("profile") && manifest.get("profile").isJsonObject()
                ? manifest.getAsJsonObject("profile").deepCopy()
                : new JsonObject();
            JsonObject elementSettings = manifest.has("element_settings") && manifest.get("element_settings").isJsonObject()
                ? manifest.getAsJsonObject("element_settings").deepCopy()
                : new JsonObject();

            JsonObject textureAssets = manifest.has("texture_assets") && manifest.get("texture_assets").isJsonObject()
                ? manifest.getAsJsonObject("texture_assets")
                : new JsonObject();
            for (String textureKey : List.of("icon", "bar")) {
                importTextureAsset(zipFile, textureAssets, profile, presetId, textureKey);
            }

            JsonObject soundSelections = profile.has(ValorantSkinProfileManager.PROFILE_SOUND_SELECTIONS_KEY)
                && profile.get(ValorantSkinProfileManager.PROFILE_SOUND_SELECTIONS_KEY).isJsonObject()
                ? profile.getAsJsonObject(ValorantSkinProfileManager.PROFILE_SOUND_SELECTIONS_KEY)
                : new JsonObject();
            JsonObject soundAssets = manifest.has("sound_assets") && manifest.get("sound_assets").isJsonObject()
                ? manifest.getAsJsonObject("sound_assets")
                : new JsonObject();
            for (String slotId : List.of(
                ExternalSoundManager.SLOT_COMBO_1,
                ExternalSoundManager.SLOT_COMBO_2,
                ExternalSoundManager.SLOT_COMBO_3,
                ExternalSoundManager.SLOT_COMBO_4,
                ExternalSoundManager.SLOT_COMBO_5,
                ExternalSoundManager.SLOT_COMBO_6
            )) {
                importSoundAsset(zipFile, soundAssets, soundSelections, presetId, slotId);
            }
            if (!soundSelections.entrySet().isEmpty()) {
                profile.add(ValorantSkinProfileManager.PROFILE_SOUND_SELECTIONS_KEY, soundSelections);
            } else {
                profile.remove(ValorantSkinProfileManager.PROFILE_SOUND_SELECTIONS_KEY);
            }

            String newStyle = ValorantSkinProfileManager.importSkinProfile(config, label, baseSkin, profile, true);
            if (newStyle == null) {
                return null;
            }
            applyElementSettings(config, elementSettings);
            ElementConfigManager.setElementConfig(presetId, "kill_icon/valorant", config);
            return new ImportResult(newStyle, ValorantSkinProfileManager.resolveSkinLabel(config, newStyle));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void appendTextureAsset(
        String presetId,
        JsonObject profile,
        String textureKey,
        JsonObject textureAssets,
        Map<String, String> textureLabels
    ) {
        String modeKey = ElementTextureDefinition.getTextureModeKey(textureKey);
        String customKey = ElementTextureDefinition.getCustomTextureKey(textureKey);
        if (!profile.has(modeKey) || !"custom".equalsIgnoreCase(profile.get(modeKey).getAsString()) || !profile.has(customKey)) {
            return;
        }

        String fileName = profile.get(customKey).getAsString();
        if (!ExternalTextureManager.isCustomTextureName(fileName)) {
            return;
        }

        JsonObject entry = new JsonObject();
        entry.addProperty("file", "textures/" + textureKey + ".png");
        entry.addProperty("label", textureLabels.getOrDefault(fileName, fileName));
        JsonObject meta = ExternalTextureManager.getCustomMeta(presetId, fileName);
        if (meta != null) {
            entry.add("meta", meta.deepCopy());
        }
        textureAssets.add(textureKey, entry);
    }

    private static void importTextureAsset(ZipFile zipFile, JsonObject textureAssets, JsonObject profile, String presetId, String textureKey) throws IOException {
        String modeKey = ElementTextureDefinition.getTextureModeKey(textureKey);
        String customKey = ElementTextureDefinition.getCustomTextureKey(textureKey);
        if (!textureAssets.has(textureKey) || !textureAssets.get(textureKey).isJsonObject()) {
            if (profile.has(modeKey) && "custom".equalsIgnoreCase(profile.get(modeKey).getAsString())) {
                profile.addProperty(modeKey, "official");
                profile.remove(customKey);
            }
            return;
        }

        JsonObject asset = textureAssets.getAsJsonObject(textureKey);
        String zipPath = asset.has("file") ? asset.get("file").getAsString() : null;
        if (zipPath == null) {
            return;
        }

        ZipEntry fileEntry = zipFile.getEntry(zipPath);
        if (fileEntry == null) {
            return;
        }

        Path tempFile = Files.createTempFile("gd656_valorant_texture_", ".png");
        try (InputStream input = zipFile.getInputStream(fileEntry); OutputStream output = Files.newOutputStream(tempFile)) {
            input.transferTo(output);
        }

        JsonObject meta = asset.has("meta") && asset.get("meta").isJsonObject() ? asset.getAsJsonObject("meta") : null;
        Integer frames = meta != null && meta.has("frames") ? meta.get("frames").getAsInt() : null;
        Integer interval = meta != null && meta.has("interval") ? meta.get("interval").getAsInt() : null;
        String orientation = meta != null && meta.has("orientation") ? meta.get("orientation").getAsString() : null;
        boolean gif = meta != null && meta.has("gif") && meta.get("gif").getAsBoolean();
        String label = asset.has("label") ? asset.get("label").getAsString() : textureKey + ".png";

        String importedName = ExternalTextureManager.createCustomTextureFromFile(presetId, tempFile, label, gif, frames, interval, orientation);
        Files.deleteIfExists(tempFile);
        if (importedName != null) {
            profile.addProperty(modeKey, "custom");
            profile.addProperty(customKey, importedName);
        }
    }

    private static void importSoundAsset(ZipFile zipFile, JsonObject soundAssets, JsonObject soundSelections, String presetId, String slotId) throws IOException {
        if (!soundSelections.has(slotId)) {
            return;
        }

        String currentBaseName = soundSelections.get(slotId).getAsString();
        boolean customSelection = currentBaseName != null && currentBaseName.startsWith("custom_");
        if (!soundAssets.has(slotId) || !soundAssets.get(slotId).isJsonObject()) {
            if (customSelection) {
                soundSelections.remove(slotId);
            }
            return;
        }

        JsonObject asset = soundAssets.getAsJsonObject(slotId);
        String zipPath = asset.has("file") ? asset.get("file").getAsString() : null;
        if (zipPath == null) {
            return;
        }
        ZipEntry fileEntry = zipFile.getEntry(zipPath);
        if (fileEntry == null) {
            if (customSelection) {
                soundSelections.remove(slotId);
            }
            return;
        }

        String suffix = zipPath.toLowerCase().endsWith(".wav") ? ".wav" : ".ogg";
        Path tempFile = Files.createTempFile("gd656_valorant_sound_", suffix);
        try (InputStream input = zipFile.getInputStream(fileEntry); OutputStream output = Files.newOutputStream(tempFile)) {
            input.transferTo(output);
        }

        String label = asset.has("label") ? asset.get("label").getAsString() : slotId + suffix;
        String importedBaseName = ExternalSoundManager.createCustomSoundFromFile(presetId, tempFile, label);
        Files.deleteIfExists(tempFile);
        if (importedBaseName != null) {
            soundSelections.addProperty(slotId, importedBaseName);
        } else if (customSelection) {
            soundSelections.remove(slotId);
        }
    }

    private static void writeJsonEntry(ZipOutputStream zos, String name, JsonObject json) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        zos.write(GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private static void writeBinaryEntry(ZipOutputStream zos, String name, byte[] bytes) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        zos.write(bytes);
        zos.closeEntry();
    }

    private static JsonObject snapshotElementSettings(JsonObject config) {
        JsonObject defaults = ElementConfigManager.getDefaultElementConfig("kill_icon/valorant");
        JsonObject settings = new JsonObject();
        for (String key : ROOT_SETTING_KEYS) {
            if (config.has(key)) {
                settings.add(key, config.get(key).deepCopy());
            } else if (defaults.has(key)) {
                settings.add(key, defaults.get(key).deepCopy());
            }
        }
        return settings;
    }

    private static void applyElementSettings(JsonObject config, JsonObject settings) {
        JsonObject defaults = ElementConfigManager.getDefaultElementConfig("kill_icon/valorant");
        for (String key : ROOT_SETTING_KEYS) {
            if (settings.has(key)) {
                config.add(key, settings.get(key).deepCopy());
            } else if (defaults.has(key)) {
                config.add(key, defaults.get(key).deepCopy());
            }
        }
    }

    public record ImportResult(String style, String label) {
    }
}
