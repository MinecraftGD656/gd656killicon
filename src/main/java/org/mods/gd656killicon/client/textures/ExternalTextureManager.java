package org.mods.gd656killicon.client.textures;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.mods.gd656killicon.Gd656killicon;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.config.ElementTextureDefinition;
import org.mods.gd656killicon.client.util.ClientMessageLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ExternalTextureManager {
    private static final Path CONFIG_ASSETS_DIR = FMLPaths.CONFIGDIR.get().resolve("gd656killicon/assets");
    private static final Path COMMON_TEXTURES_DIR = CONFIG_ASSETS_DIR.resolve("common").resolve("textures");
    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new HashMap<>();
    private static final ExecutorService TEXTURE_THREAD_POOL = Executors.newCachedThreadPool();
    private static final Gson GSON = new Gson();
    private static final Pattern CUSTOM_TEXTURE_PATTERN = Pattern.compile("^custom_(\\d+)\\.png$");
    private static final String CUSTOM_LABELS_FILE = "custom_labels.json";
    private static final String CUSTOM_META_FILE = "custom_meta.json";
    
    private static final String[] DEFAULT_TEXTURES = {
        "killicon_scrolling_default.png",
        "killicon_scrolling_headshot.png",
        "killicon_scrolling_explosion.png",
        "killicon_scrolling_crit.png",
        "killicon_scrolling_assist.png",
        "killicon_scrolling_destroyvehicle.png",
        "killicon_combo_1.png",
        "killicon_combo_2.png",
        "killicon_combo_3.png",
        "killicon_combo_4.png",
        "killicon_combo_5.png",
        "killicon_combo_6.png",
        "killicon_card_bar_ct.png",
        "killicon_card_bar_t.png",
        "killicon_card_default_t.png",
        "killicon_card_default_ct.png",
        "killicon_card_headshot_t.png",
        "killicon_card_headshot_ct.png",
        "killicon_card_explosion_t.png",
        "killicon_card_explosion_ct.png",
        "killicon_card_crit_t.png",
        "killicon_card_crit_ct.png",
        "killicon_card_light_t.png",
        "killicon_card_light_ct.png",
        "killicon_battlefield1_default.png",
        "killicon_battlefield1_headshot.png",
        "killicon_battlefield1_explosion.png",
        "killicon_battlefield1_crit.png",
        "killicon_battlefield1_destroyvehicle.png",
        "killicon_battlefield5_default.png",
        "killicon_battlefield5_headshot.png",
        "killicon_battlefield5_assist.png",
        "killicon_battlefield5_destroyvehicle.png",
        "killicon_df_default.png",
        "killicon_df_headshot.png",
        "killicon_df_destroyvehicle.png"
    };
    private static final Set<String> DEFAULT_TEXTURE_SET = new HashSet<>(Arrays.asList(DEFAULT_TEXTURES));
    private static final List<String> DEFAULT_TEXTURE_LIST = Collections.unmodifiableList(Arrays.asList(DEFAULT_TEXTURES));
    private static final Map<String, Map<String, TextureBackup>> PENDING_TEXTURE_BACKUPS = new HashMap<>();
    private static final Map<String, byte[]> DEFAULT_TEXTURE_BYTES = new HashMap<>();
    private static final Map<String, TextureState> TEXTURE_STATE_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> TEXTURE_STATE_LOADING = ConcurrentHashMap.newKeySet();

    public static void init() {
        ensureAllPresetsTextureFiles(false);
        reload();
    }

    public static void reload() {
        ensureAllPresetsTextureFiles(false);
        clearCache();
        ClientMessageLogger.chatInfo("gd656killicon.client.texture.reloading");
        
        String currentPresetId = ConfigManager.getCurrentPresetId();
        int loadedCount = loadTexturesForPreset(currentPresetId);
        
        ClientMessageLogger.chatSuccess("gd656killicon.client.texture.reload_success", currentPresetId, loadedCount);
    }

    public static void reloadAsync() {
        TEXTURE_THREAD_POOL.submit(() -> {
            try {
                ClientMessageLogger.info("Async texture reload started.");

                ensureAllPresetsTextureFiles(false);

                String currentPresetId = ConfigManager.getCurrentPresetId();
                Path presetDir = CONFIG_ASSETS_DIR.resolve(currentPresetId).resolve("textures");
                final int[] totalTextures = { DEFAULT_TEXTURES.length };
                
                Map<String, NativeImage> loadedImages = new HashMap<>();
                AtomicInteger processedCount = new AtomicInteger(0);

                for (String path : DEFAULT_TEXTURES) {
                    Path file = presetDir.resolve(path);
                    if (Files.exists(file)) {
                        try (InputStream stream = new FileInputStream(file.toFile())) {
                            NativeImage image = NativeImage.read(stream);
                            loadedImages.put(path, image);
                        } catch (IOException e) {
                            ClientMessageLogger.error("gd656killicon.client.texture.load_fail", currentPresetId, path, e.getMessage());
                        }
                    }
                    
                    int current = processedCount.incrementAndGet();
                    if (current % 2 == 0 || current == totalTextures[0]) {
                        ClientMessageLogger.info("Async texture reload progress: %d/%d.", current, totalTextures[0]);
                    }
                }

                Minecraft.getInstance().execute(() -> {
                    clearCache();
                    int successCount = 0;
                    for (Map.Entry<String, NativeImage> entry : loadedImages.entrySet()) {
                        String path = entry.getKey();
                        NativeImage image = entry.getValue();
                        try {
                            DynamicTexture texture = new DynamicTexture(image);
                            String dynamicName = "gd656killicon_external_" + currentPresetId + "_" + path.replace("/", "_").replace(".", "_");
                            ResourceLocation dynamicLoc = Minecraft.getInstance().getTextureManager().register(dynamicName, texture);
                            TEXTURE_CACHE.put(currentPresetId + ":" + path, dynamicLoc);
                            successCount++;
                        } catch (Exception e) {
                             image.close();
                        }
                    }
                    ClientMessageLogger.info("Async texture reload complete for preset %s: %d loaded.", currentPresetId, successCount);
                });

            } catch (Exception e) {
                ClientMessageLogger.error("Async texture reload failed: %s", e.getMessage());
            }
        });
    }

    public static void resetTextures(String presetId) {
        ensureCommonTextureFiles(false);
        ensureTextureFilesForPreset(presetId, true);
        ClientMessageLogger.chatSuccess("gd656killicon.client.texture.reset_success", presetId);
        
        if (presetId.equals(ConfigManager.getCurrentPresetId())) {
            reload();
        }
    }
    
    public static void resetTexturesAsync(String presetId) {
        TEXTURE_THREAD_POOL.submit(() -> {
            try {
                ensureCommonTextureFiles(false);
                ensureTextureFilesForPreset(presetId, true);
                
                ClientMessageLogger.info("Async texture reset complete for preset %s.", presetId);

                if (presetId.equals(ConfigManager.getCurrentPresetId())) {
                    reloadAsync();
                }
            } catch (Exception e) {
                ClientMessageLogger.error("Async texture reset failed for preset %s: %s", presetId, e.getMessage());
            }
        });
    }

    public static void markPendingTextureReset(String presetId) {
        if (presetId == null) {
            return;
        }
        ensureTextureFilesForPreset(presetId, false);
        Path presetDir = CONFIG_ASSETS_DIR.resolve(presetId).resolve("textures");
        Map<String, TextureBackup> presetBackups = PENDING_TEXTURE_BACKUPS.computeIfAbsent(presetId, k -> new HashMap<>());
        if (Files.exists(presetDir)) {
            try (java.util.stream.Stream<Path> paths = Files.list(presetDir)) {
                paths.filter(Files::isRegularFile).forEach(path -> {
                    try {
                        backupTextureFileIfNeeded(presetBackups, path);
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
            } catch (IOException ignored) {
            }
        }
        if (presetId.equals(ConfigManager.getCurrentPresetId())) {
            reloadAsync();
        }
    }
    
    public static void resetAllTextures() {
        ensureCommonTextureFiles(true);
        Set<String> presets = ConfigManager.getPresetIds();
        for (String presetId : presets) {
            resetTextures(presetId);
        }
    }

    public static void resetAllTexturesAsync() {
        TEXTURE_THREAD_POOL.submit(() -> {
            try {
                ensureCommonTextureFiles(true);
                Set<String> presets = ConfigManager.getPresetIds();

                for (String presetId : presets) {
                    ensureTextureFilesForPreset(presetId, true);
                }
                
                ClientMessageLogger.info("Async texture reset complete for all presets.");
                
                reloadAsync();
            } catch (Exception e) {
                ClientMessageLogger.error("Async texture reset failed: %s", e.getMessage());
            }
        });
    }

    public static List<String> getAllTextureFileNames() {
        return DEFAULT_TEXTURE_LIST;
    }

    public static boolean isValidTextureName(String textureName) {
        if (textureName == null) {
            return false;
        }
        if (DEFAULT_TEXTURE_SET.contains(textureName)) {
            return true;
        }
        return isCustomTextureName(textureName);
    }

    public static boolean isOfficialTextureName(String textureName) {
        return textureName != null && DEFAULT_TEXTURE_SET.contains(textureName);
    }

    public static boolean isCustomTextureName(String textureName) {
        if (textureName == null) {
            return false;
        }
        return CUSTOM_TEXTURE_PATTERN.matcher(textureName).matches();
    }

    public static boolean isVanillaTexturePath(String path) {
        return path != null && path.startsWith("minecraft:");
    }

    public static boolean isVanillaTextureAvailable(String path) {
        if (!isVanillaTexturePath(path)) {
            return false;
        }
        ResourceLocation vanilla = getVanillaTextureLocation(path);
        if (vanilla == null) {
            return false;
        }
        return Minecraft.getInstance().getResourceManager().getResource(vanilla).isPresent();
    }

    public static ResourceLocation getVanillaTextureLocation(String path) {
        if (!isVanillaTexturePath(path)) {
            return null;
        }
        String raw = path.substring("minecraft:".length());
        String normalized = normalizeVanillaTexturePath(raw);
        return ResourceLocation.fromNamespaceAndPath("minecraft", normalized);
    }

    public static ResourceLocation getTexture(String path) {
        if (isVanillaTexturePath(path)) {
            ResourceLocation vanilla = getVanillaTextureLocation(path);
            if (vanilla != null) {
                return vanilla;
            }
        }
        String presetId = ConfigManager.getCurrentPresetId();
        String resolvedPath = IconTextureAnimationManager.resolveTexturePath(path);
        String cacheKey = presetId + ":" + resolvedPath;
        
        if (TEXTURE_CACHE.containsKey(cacheKey)) {
            return TEXTURE_CACHE.get(cacheKey);
        }

        if (loadExternalTexture(presetId, resolvedPath)) {
            return TEXTURE_CACHE.get(cacheKey);
        }

        return ResourceLocation.fromNamespaceAndPath(Gd656killicon.MODID, "textures/" + resolvedPath);
    }

    public static java.util.List<String> getDefaultTexturePaths() {
        return java.util.Arrays.asList(DEFAULT_TEXTURES);
    }

    public static boolean replaceTextureWithBackup(String presetId, String textureName, Path sourcePath) {
        if (presetId == null || textureName == null || sourcePath == null) {
            return false;
        }
        if (!DEFAULT_TEXTURE_SET.contains(textureName)) {
            return false;
        }
        if (!Files.exists(sourcePath)) {
            return false;
        }
        ensureTextureFilesForPreset(presetId, false);
        Path targetPath = CONFIG_ASSETS_DIR.resolve(presetId).resolve("textures").resolve(textureName);
        try {
            if (!Files.exists(targetPath.getParent())) {
                Files.createDirectories(targetPath.getParent());
            }
            Map<String, TextureBackup> presetBackups = PENDING_TEXTURE_BACKUPS.computeIfAbsent(presetId, k -> new HashMap<>());
            if (!presetBackups.containsKey(textureName)) {
                if (Files.exists(targetPath)) {
                    byte[] original = Files.readAllBytes(targetPath);
                    presetBackups.put(textureName, new TextureBackup(true, original));
                } else {
                    presetBackups.put(textureName, new TextureBackup(false, null));
                }
            }
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            refreshTextureCache(presetId, textureName);
            invalidateTextureState(presetId, textureName);
            return true;
        } catch (IOException e) {
            ClientMessageLogger.error("gd656killicon.client.texture.replace_fail", presetId, textureName, e.getMessage());
            return false;
        }
    }

    public static boolean resetTextureWithBackup(String presetId, String textureName) {
        return resetTextureWithBackupInternal(presetId, textureName, true);
    }

    public static boolean resetTextureWithBackupSilent(String presetId, String textureName) {
        return resetTextureWithBackupInternal(presetId, textureName, false);
    }

    private static boolean resetTextureWithBackupInternal(String presetId, String textureName, boolean logErrors) {
        if (presetId == null || textureName == null) {
            return false;
        }
        if (!DEFAULT_TEXTURE_SET.contains(textureName)) {
            return false;
        }
        ensureTextureFilesForPreset(presetId, false);
        Path targetPath = CONFIG_ASSETS_DIR.resolve(presetId).resolve("textures").resolve(textureName);
        try {
            if (!Files.exists(targetPath.getParent())) {
                Files.createDirectories(targetPath.getParent());
            }
            Map<String, TextureBackup> presetBackups = PENDING_TEXTURE_BACKUPS.computeIfAbsent(presetId, k -> new HashMap<>());
            if (!presetBackups.containsKey(textureName)) {
                if (Files.exists(targetPath)) {
                    byte[] original = Files.readAllBytes(targetPath);
                    presetBackups.put(textureName, new TextureBackup(true, original));
                } else {
                    presetBackups.put(textureName, new TextureBackup(false, null));
                }
            }
            Files.deleteIfExists(targetPath);
            if (logErrors) {
                refreshTextureCache(presetId, textureName);
            } else {
                refreshTextureCacheAsync(presetId, textureName);
            }
            invalidateTextureState(presetId, textureName);
            return true;
        } catch (IOException e) {
            if (logErrors) {
                ClientMessageLogger.error("gd656killicon.client.texture.reset_error", presetId, textureName, e.getMessage());
            }
            return false;
        }
    }

    private static void backupTextureFileIfNeeded(Map<String, TextureBackup> presetBackups, Path path) throws IOException {
        String fileName = path.getFileName().toString();
        if (presetBackups.containsKey(fileName)) {
            return;
        }
        if (Files.exists(path)) {
            byte[] original = Files.readAllBytes(path);
            presetBackups.put(fileName, new TextureBackup(true, original));
        } else {
            presetBackups.put(fileName, new TextureBackup(false, null));
        }
    }

    public static boolean resetTexturesForElement(String presetId, String elementId) {
        if (presetId == null || elementId == null) {
            return false;
        }
        if (!ElementTextureDefinition.hasTextures(elementId)) {
            return false;
        }
        boolean any = false;
        for (String texture : ElementTextureDefinition.getTextures(elementId)) {
            String fileName = ElementTextureDefinition.getTextureFileName(presetId, elementId, texture);
            if (fileName != null) {
                any = resetTextureWithBackup(presetId, fileName) || any;
            }
        }
        return any;
    }

    public static void confirmPendingTextureReplacements() {
        PENDING_TEXTURE_BACKUPS.clear();
    }

    public static void revertPendingTextureReplacements() {
        if (PENDING_TEXTURE_BACKUPS.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Map<String, TextureBackup>> presetEntry : PENDING_TEXTURE_BACKUPS.entrySet()) {
            String presetId = presetEntry.getKey();
            for (Map.Entry<String, TextureBackup> entry : presetEntry.getValue().entrySet()) {
                String textureName = entry.getKey();
                TextureBackup backup = entry.getValue();
                Path targetPath = CONFIG_ASSETS_DIR.resolve(presetId).resolve("textures").resolve(textureName);
                try {
                    if (backup.existed) {
                        Files.write(targetPath, backup.data);
                    } else {
                        Files.deleteIfExists(targetPath);
                    }
                    refreshTextureCache(presetId, textureName);
                    invalidateTextureState(presetId, textureName);
                } catch (IOException e) {
                    ClientMessageLogger.error("gd656killicon.client.texture.revert_fail", presetId, textureName, e.getMessage());
                }
            }
        }
        PENDING_TEXTURE_BACKUPS.clear();
    }

    public static void revertPendingTextureReplacementsForElement(String presetId, String elementId) {
        if (presetId == null || elementId == null) {
            return;
        }
        Map<String, TextureBackup> presetBackups = PENDING_TEXTURE_BACKUPS.get(presetId);
        if (presetBackups == null || presetBackups.isEmpty()) {
            return;
        }
        if (!ElementTextureDefinition.hasTextures(elementId)) {
            return;
        }
        for (String texture : ElementTextureDefinition.getTextures(elementId)) {
            String fileName = ElementTextureDefinition.getTextureFileName(presetId, elementId, texture);
            if (fileName == null) {
                continue;
            }
            TextureBackup backup = presetBackups.remove(fileName);
            if (backup == null) {
                continue;
            }
            Path targetPath = CONFIG_ASSETS_DIR.resolve(presetId).resolve("textures").resolve(fileName);
            try {
                if (backup.existed) {
                    Files.write(targetPath, backup.data);
                } else {
                    Files.deleteIfExists(targetPath);
                }
                refreshTextureCache(presetId, fileName);
                invalidateTextureState(presetId, fileName);
            } catch (IOException e) {
                ClientMessageLogger.error("gd656killicon.client.texture.revert_fail", presetId, fileName, e.getMessage());
            }
        }
        if (presetBackups.isEmpty()) {
            PENDING_TEXTURE_BACKUPS.remove(presetId);
        }
    }

    public static boolean hasPendingTextureChanges() {
        for (Map<String, TextureBackup> presetBackups : PENDING_TEXTURE_BACKUPS.values()) {
            if (presetBackups != null && !presetBackups.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasPendingTextureChangesForElement(String presetId, String elementId) {
        if (presetId == null || elementId == null) {
            return false;
        }
        Map<String, TextureBackup> presetBackups = PENDING_TEXTURE_BACKUPS.get(presetId);
        if (presetBackups == null || presetBackups.isEmpty()) {
            return false;
        }
        if (!ElementTextureDefinition.hasTextures(elementId)) {
            return false;
        }
        for (String texture : ElementTextureDefinition.getTextures(elementId)) {
            String fileName = ElementTextureDefinition.getTextureFileName(presetId, elementId, texture);
            if (fileName != null && presetBackups.containsKey(fileName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTextureModified(String presetId, String textureName) {
        if (presetId == null || textureName == null) {
            return false;
        }
        if (!DEFAULT_TEXTURE_SET.contains(textureName)) {
            return false;
        }
        String key = presetId + ":" + textureName;
        TextureState cachedState = TEXTURE_STATE_CACHE.get(key);
        if (cachedState != null) {
            return cachedState.modified;
        }
        if (TEXTURE_STATE_LOADING.contains(key)) {
            return false;
        }
        TEXTURE_STATE_LOADING.add(key);
        TEXTURE_THREAD_POOL.submit(() -> {
            try {
                Path targetPath = CONFIG_ASSETS_DIR.resolve(presetId).resolve("textures").resolve(textureName);
                if (!Files.exists(targetPath)) {
                    return;
                }
                byte[] defaultBytes = getDefaultTextureBytes(textureName);
                if (defaultBytes == null) {
                    return;
                }
                long lastModified = Files.getLastModifiedTime(targetPath).toMillis();
                long size = Files.size(targetPath);
                byte[] currentBytes = Files.readAllBytes(targetPath);
                boolean modified = !Arrays.equals(currentBytes, defaultBytes);
                TEXTURE_STATE_CACHE.put(key, new TextureState(lastModified, size, modified));
            } catch (IOException ignored) {
            } finally {
                TEXTURE_STATE_LOADING.remove(key);
            }
        });
        return false;
    }

    private static void ensureAllPresetsTextureFiles(boolean forceReset) {
        ensureCommonTextureFiles(forceReset);
        Set<String> presets = ConfigManager.getPresetIds();
        ensureTextureFilesForPreset("00001", forceReset);
        ensureTextureFilesForPreset("00002", forceReset);
        
        for (String presetId : presets) {
            ensureTextureFilesForPreset(presetId, forceReset);
        }
    }

    public static void ensureTextureFilesForPreset(String presetId) {
        ensureTextureFilesForPreset(presetId, false);
    }

    public static void ensureTextureFilesForPreset(String presetId, boolean forceReset) {
        try {
            Path presetDir = CONFIG_ASSETS_DIR.resolve(presetId).resolve("textures");
            if (!Files.exists(presetDir)) {
                Files.createDirectories(presetDir);
            }

            for (String texturePath : DEFAULT_TEXTURES) {
                Path targetPath = presetDir.resolve(texturePath);
                if (!Files.exists(targetPath.getParent())) {
                    Files.createDirectories(targetPath.getParent());
                }

                if (forceReset) {
                    Files.deleteIfExists(targetPath);
                }
            }
        } catch (IOException e) {
            ClientMessageLogger.error("gd656killicon.client.texture.init_fail", presetId, e.getMessage());
        }
    }
    
    private static int loadTexturesForPreset(String presetId) {
        int loadedCount = 0;
        for (String path : DEFAULT_TEXTURES) {
            if (loadExternalTexture(presetId, path)) {
                loadedCount++;
            }
        }
        return loadedCount;
    }

    private static boolean loadExternalTexture(String presetId, String path) {
        Path file = resolveExternalTexturePath(presetId, path);
        if (!Files.exists(file)) {
            return false;
        }

        try (InputStream stream = new FileInputStream(file.toFile())) {
            NativeImage image = NativeImage.read(stream);
            DynamicTexture texture = new DynamicTexture(image);
            String dynamicName = "gd656killicon_external_" + presetId + "_" + path.replace("/", "_").replace(".", "_");
            ResourceLocation dynamicLoc = Minecraft.getInstance().getTextureManager().register(dynamicName, texture);
            
            TEXTURE_CACHE.put(presetId + ":" + path, dynamicLoc);
            return true;
        } catch (IOException e) {
            ClientMessageLogger.chatError("gd656killicon.client.texture.load_fail", presetId, path, e.getMessage());
            return false;
        }
    }

    private static void clearCache() {
        for (ResourceLocation loc : TEXTURE_CACHE.values()) {
            Minecraft.getInstance().getTextureManager().release(loc);
        }
        TEXTURE_CACHE.clear();
    }

    private static void refreshTextureCache(String presetId, String textureName) {
        String cacheKey = presetId + ":" + textureName;
        ResourceLocation cached = TEXTURE_CACHE.remove(cacheKey);
        if (cached != null) {
            Minecraft.getInstance().getTextureManager().release(cached);
        }
        loadExternalTexture(presetId, textureName);
    }

    private static void refreshTextureCacheAsync(String presetId, String textureName) {
        String cacheKey = presetId + ":" + textureName;
        Minecraft.getInstance().execute(() -> {
            ResourceLocation cached = TEXTURE_CACHE.remove(cacheKey);
            if (cached != null) {
                Minecraft.getInstance().getTextureManager().release(cached);
            }
        });
        TEXTURE_THREAD_POOL.submit(() -> {
            Path file = resolveExternalTexturePath(presetId, textureName);
            if (!Files.exists(file)) {
                return;
            }
            try (InputStream stream = new FileInputStream(file.toFile())) {
                NativeImage image = NativeImage.read(stream);
                Minecraft.getInstance().execute(() -> {
                    try {
                        DynamicTexture texture = new DynamicTexture(image);
                        String dynamicName = "gd656killicon_external_" + presetId + "_" + textureName.replace("/", "_").replace(".", "_");
                        ResourceLocation dynamicLoc = Minecraft.getInstance().getTextureManager().register(dynamicName, texture);
                        TEXTURE_CACHE.put(cacheKey, dynamicLoc);
                    } catch (Exception e) {
                        image.close();
                    }
                });
            } catch (IOException ignored) {
            }
        });
    }

    private static byte[] getDefaultTextureBytes(String textureName) {
        byte[] cached = DEFAULT_TEXTURE_BYTES.get(textureName);
        if (cached != null) {
            return cached;
        }
        ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(Gd656killicon.MODID, "textures/" + textureName);
        try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(resourceLocation).get().open()) {
            byte[] data = stream.readAllBytes();
            DEFAULT_TEXTURE_BYTES.put(textureName, data);
            return data;
        } catch (Exception e) {
            try (InputStream stream = ExternalTextureManager.class.getResourceAsStream("/assets/gd656killicon/textures/" + textureName)) {
                if (stream != null) {
                    byte[] data = stream.readAllBytes();
                    DEFAULT_TEXTURE_BYTES.put(textureName, data);
                    return data;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static final Map<String, TextureDimensions> TEXTURE_DIMENSIONS = new HashMap<>();

    public static class TextureDimensions {
        public final int width;
        public final int height;
        public TextureDimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    public static TextureDimensions getTextureDimensions(String presetId, String path) {
        String key = presetId + ":" + path;
        if (TEXTURE_DIMENSIONS.containsKey(key)) {
            return TEXTURE_DIMENSIONS.get(key);
        }

        if (isVanillaTexturePath(path)) {
            ResourceLocation vanilla = getVanillaTextureLocation(path);
            if (vanilla != null) {
                try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(vanilla).get().open();
                     NativeImage image = NativeImage.read(stream)) {
                    TextureDimensions dims = new TextureDimensions(image.getWidth(), image.getHeight());
                    TEXTURE_DIMENSIONS.put(key, dims);
                    return dims;
                } catch (Exception ignored) {
                }
            }
            TextureDimensions dims = new TextureDimensions(16, 16);
            TEXTURE_DIMENSIONS.put(key, dims);
            return dims;
        }
        
        Path file = resolveExternalTexturePath(presetId, path);
        if (Files.exists(file)) {
             try (InputStream stream = new FileInputStream(file.toFile());
                  NativeImage image = NativeImage.read(stream)) {
                 TextureDimensions dims = new TextureDimensions(image.getWidth(), image.getHeight());
                 TEXTURE_DIMENSIONS.put(key, dims);
                 return dims;
             } catch (IOException ignored) {}
        }
        
        ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(Gd656killicon.MODID, "textures/" + path);
        try {
             try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(resourceLocation).get().open();
                  NativeImage image = NativeImage.read(stream)) {
                 TextureDimensions dims = new TextureDimensions(image.getWidth(), image.getHeight());
                 TEXTURE_DIMENSIONS.put(key, dims);
                 return dims;
             }
        } catch (Exception e) {
             try (InputStream stream = ExternalTextureManager.class.getResourceAsStream("/assets/gd656killicon/textures/" + path)) {
                if (stream != null) {
                     try (NativeImage image = NativeImage.read(stream)) {
                        TextureDimensions dims = new TextureDimensions(image.getWidth(), image.getHeight());
                        TEXTURE_DIMENSIONS.put(key, dims);
                        return dims;
                     }
                }
            } catch (Exception ignored) {}
        }
        
        return new TextureDimensions(0, 0);
    }

    private static String normalizeVanillaTexturePath(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "textures/misc/missing_texture.png";
        }
        String normalized = raw;
        if (normalized.startsWith("textures/")) {
            return normalized;
        }
        if (normalized.startsWith("item/") || normalized.startsWith("block/") || normalized.startsWith("gui/")) {
            normalized = "textures/" + normalized;
        } else {
            normalized = "textures/item/" + normalized;
        }
        if (!normalized.endsWith(".png")) {
            normalized += ".png";
        }
        return normalized;
    }

    public static List<String> getCustomTextureFileNames(String presetId) {
        if (presetId == null) {
            return Collections.emptyList();
        }
        Path presetDir = CONFIG_ASSETS_DIR.resolve(presetId).resolve("textures");
        if (!Files.exists(presetDir)) {
            return Collections.emptyList();
        }
        try {
            return Files.list(presetDir)
                .filter(path -> Files.isRegularFile(path))
                .map(path -> path.getFileName().toString())
                .filter(ExternalTextureManager::isCustomTextureName)
                .sorted((a, b) -> {
                    Matcher ma = CUSTOM_TEXTURE_PATTERN.matcher(a);
                    Matcher mb = CUSTOM_TEXTURE_PATTERN.matcher(b);
                    if (ma.matches() && mb.matches()) {
                        int ia = Integer.parseInt(ma.group(1));
                        int ib = Integer.parseInt(mb.group(1));
                        return Integer.compare(ia, ib);
                    }
                    return a.compareToIgnoreCase(b);
                })
                .toList();
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public static Map<String, String> getCustomTextureLabels(String presetId) {
        if (presetId == null) {
            return Collections.emptyMap();
        }
        Path labelsPath = CONFIG_ASSETS_DIR.resolve(presetId).resolve("textures").resolve(CUSTOM_LABELS_FILE);
        if (!Files.exists(labelsPath)) {
            return Collections.emptyMap();
        }
        try {
            String content = Files.readString(labelsPath);
            JsonObject obj = JsonParser.parseString(content).getAsJsonObject();
            Map<String, String> map = new HashMap<>();
            for (String key : obj.keySet()) {
                map.put(key, obj.get(key).getAsString());
            }
            return map;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    public static String createCustomTextureFromFile(String presetId, Path sourcePath, String originalName, boolean gifDerived, Integer frameCount, Integer intervalMs, String orientation) {
        if (presetId == null || sourcePath == null || !Files.exists(sourcePath)) {
            return null;
        }
        Path presetDir = CONFIG_ASSETS_DIR.resolve(presetId).resolve("textures");
        try {
            if (!Files.exists(presetDir)) {
                Files.createDirectories(presetDir);
            }
            int nextIndex = 1;
            for (String name : getCustomTextureFileNames(presetId)) {
                Matcher matcher = CUSTOM_TEXTURE_PATTERN.matcher(name);
                if (matcher.matches()) {
                    int index = Integer.parseInt(matcher.group(1));
                    if (index >= nextIndex) {
                        nextIndex = index + 1;
                    }
                }
            }
            String fileName = "custom_" + nextIndex + ".png";
            Path targetPath = presetDir.resolve(fileName);
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            refreshTextureCache(presetId, fileName);
            invalidateTextureState(presetId, fileName);
            updateCustomLabel(presetId, fileName, originalName);
            updateCustomMeta(presetId, fileName, gifDerived, frameCount, intervalMs, orientation);
            return fileName;
        } catch (IOException e) {
            ClientMessageLogger.error("gd656killicon.client.texture.replace_fail", presetId, sourcePath.toString(), e.getMessage());
            return null;
        }
    }

    public static String createCustomTextureFromFile(String presetId, Path sourcePath, String originalName) {
        return createCustomTextureFromFile(presetId, sourcePath, originalName, false, null, null, null);
    }

    private static void updateCustomLabel(String presetId, String fileName, String originalName) {
        if (presetId == null || fileName == null) {
            return;
        }
        String label = originalName == null ? fileName : originalName;
        Path labelsPath = CONFIG_ASSETS_DIR.resolve(presetId).resolve("textures").resolve(CUSTOM_LABELS_FILE);
        JsonObject obj = new JsonObject();
        if (Files.exists(labelsPath)) {
            try {
                String content = Files.readString(labelsPath);
                obj = JsonParser.parseString(content).getAsJsonObject();
            } catch (Exception ignored) {
            }
        }
        obj.addProperty(fileName, label);
        try {
            Files.writeString(labelsPath, GSON.toJson(obj));
        } catch (IOException ignored) {
        }
    }

    private static void updateCustomMeta(String presetId, String fileName, boolean gifDerived, Integer frameCount, Integer intervalMs, String orientation) {
        if (presetId == null || fileName == null) {
            return;
        }
        Path metaPath = CONFIG_ASSETS_DIR.resolve(presetId).resolve("textures").resolve(CUSTOM_META_FILE);
        JsonObject obj = new JsonObject();
        if (Files.exists(metaPath)) {
            try {
                String content = Files.readString(metaPath);
                obj = JsonParser.parseString(content).getAsJsonObject();
            } catch (Exception ignored) {
            }
        }
        JsonObject meta = new JsonObject();
        meta.addProperty("gif", gifDerived);
        if (frameCount != null) {
            meta.addProperty("frames", frameCount);
        }
        if (intervalMs != null) {
            meta.addProperty("interval", intervalMs);
        }
        if (orientation != null) {
            meta.addProperty("orientation", orientation);
        }
        obj.add(fileName, meta);
        try {
            Files.writeString(metaPath, GSON.toJson(obj));
        } catch (IOException ignored) {
        }
    }

    public static boolean isGifDerivedCustomTexture(String presetId, String fileName) {
        JsonObject meta = getCustomMeta(presetId, fileName);
        return meta != null && meta.has("gif") && meta.get("gif").getAsBoolean();
    }

    public static JsonObject getCustomMeta(String presetId, String fileName) {
        if (presetId == null || fileName == null) {
            return null;
        }
        Path metaPath = CONFIG_ASSETS_DIR.resolve(presetId).resolve("textures").resolve(CUSTOM_META_FILE);
        if (!Files.exists(metaPath)) {
            return null;
        }
        try {
            String content = Files.readString(metaPath);
            JsonObject obj = JsonParser.parseString(content).getAsJsonObject();
            if (obj.has(fileName) && obj.get(fileName).isJsonObject()) {
                return obj.getAsJsonObject(fileName);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static void invalidateTextureState(String presetId, String textureName) {
        TEXTURE_STATE_CACHE.remove(presetId + ":" + textureName);
        TEXTURE_DIMENSIONS.remove(presetId + ":" + textureName);
    }

    private static void ensureCommonTextureFiles(boolean forceReset) {
        try {
            if (!Files.exists(COMMON_TEXTURES_DIR)) {
                Files.createDirectories(COMMON_TEXTURES_DIR);
            }
            for (String texturePath : DEFAULT_TEXTURES) {
                Path targetPath = COMMON_TEXTURES_DIR.resolve(texturePath);
                if (!Files.exists(targetPath.getParent())) {
                    Files.createDirectories(targetPath.getParent());
                }
                if (forceReset || !Files.exists(targetPath)) {
                    ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(Gd656killicon.MODID, "textures/" + texturePath);
                    try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(resourceLocation).get().open()) {
                        Files.copy(stream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception e) {
                        try (InputStream stream = ExternalTextureManager.class.getResourceAsStream("/assets/gd656killicon/textures/" + texturePath)) {
                            if (stream != null) {
                                Files.copy(stream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            } else {
                                ClientMessageLogger.error("gd656killicon.client.texture.extract_fail", texturePath, e.getMessage());
                            }
                        } catch (Exception ex) {
                            ClientMessageLogger.error("gd656killicon.client.texture.extract_fail", texturePath, ex.getMessage());
                        }
                    }
                }
            }
        } catch (IOException e) {
            ClientMessageLogger.error("gd656killicon.client.texture.init_fail", "common", e.getMessage());
        }
    }

    private static Path resolveExternalTexturePath(String presetId, String path) {
        Path presetPath = CONFIG_ASSETS_DIR.resolve(presetId).resolve("textures").resolve(path);
        if (Files.exists(presetPath)) {
            return presetPath;
        }
        return COMMON_TEXTURES_DIR.resolve(path);
    }

    private static final class TextureBackup {
        private final boolean existed;
        private final byte[] data;

        private TextureBackup(boolean existed, byte[] data) {
            this.existed = existed;
            this.data = data;
        }
    }

    private static final class TextureState {
        private final long lastModified;
        private final long size;
        private final boolean modified;

        private TextureState(long lastModified, long size, boolean modified) {
            this.lastModified = lastModified;
            this.size = size;
            this.modified = modified;
        }
    }
}
