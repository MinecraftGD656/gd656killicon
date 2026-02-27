package org.mods.gd656killicon.client.textures;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ExternalTextureManager {
    private static final Path CONFIG_ASSETS_DIR = FMLPaths.CONFIGDIR.get().resolve("gd656killicon/assets");
    private static final Path COMMON_TEXTURES_DIR = CONFIG_ASSETS_DIR.resolve("common").resolve("textures");
    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new HashMap<>();
    private static final ExecutorService TEXTURE_THREAD_POOL = Executors.newCachedThreadPool();
    
    
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
    private static final Map<String, TextureState> TEXTURE_STATE_CACHE = new HashMap<>();

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
        return textureName != null && DEFAULT_TEXTURE_SET.contains(textureName);
    }

    public static ResourceLocation getTexture(String path) {
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
            refreshTextureCache(presetId, textureName);
            invalidateTextureState(presetId, textureName);
            return true;
        } catch (IOException e) {
            ClientMessageLogger.error("gd656killicon.client.texture.reset_error", presetId, textureName, e.getMessage());
            return false;
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
        Path targetPath = CONFIG_ASSETS_DIR.resolve(presetId).resolve("textures").resolve(textureName);
        if (!Files.exists(targetPath)) {
            return false;
        }
        byte[] defaultBytes = getDefaultTextureBytes(textureName);
        if (defaultBytes == null) {
            return false;
        }
        try {
            long lastModified = Files.getLastModifiedTime(targetPath).toMillis();
            long size = Files.size(targetPath);
            String key = presetId + ":" + textureName;
            TextureState cachedState = TEXTURE_STATE_CACHE.get(key);
            if (cachedState != null && cachedState.lastModified == lastModified && cachedState.size == size) {
                return cachedState.modified;
            }
            byte[] currentBytes = Files.readAllBytes(targetPath);
            boolean modified = !Arrays.equals(currentBytes, defaultBytes);
            TEXTURE_STATE_CACHE.put(key, new TextureState(lastModified, size, modified));
            return modified;
        } catch (IOException e) {
            return false;
        }
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
