package org.mods.gd656killicon.client.textures;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.mods.gd656killicon.Gd656killicon;
import org.mods.gd656killicon.client.config.ConfigManager;
import org.mods.gd656killicon.client.util.ClientMessageLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ExternalTextureManager {
    // New structure: config/gd656killicon/assets/<preset_id>/textures/
    private static final Path CONFIG_ASSETS_DIR = FMLPaths.CONFIGDIR.get().resolve("gd656killicon/assets");
    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new HashMap<>();
    private static final ExecutorService TEXTURE_THREAD_POOL = Executors.newCachedThreadPool();
    
    // 需要复制的默认纹理列表
    private static final String[] DEFAULT_TEXTURES = {
        "killicon_default.png",
        "killicon_headshot.png",
        "killicon_explosion.png",
        "killicon_crit.png",
        "killicon_assist.png",
        "killicon_scrolling_default.png",
        "killicon_scrolling_headshot.png",
        "killicon_scrolling_explosion.png",
        "killicon_scrolling_crit.png",
        "killicon_scrolling_assist.png",
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
        "killicon_battlefield1_crit.png"
    };

    public static void init() {
        ensureAllPresetsTextureFiles(false);
        reload();
    }

    public static void reload() {
        ensureAllPresetsTextureFiles(false);
        clearCache();
        ClientMessageLogger.chatInfo("gd656killicon.client.texture.reloading");
        
        // Reload textures for the current preset
        String currentPresetId = ConfigManager.getCurrentPresetId();
        int loadedCount = loadTexturesForPreset(currentPresetId);
        
        ClientMessageLogger.chatSuccess("gd656killicon.client.texture.reload_success", currentPresetId, loadedCount);
    }

    public static void reloadAsync() {
        TEXTURE_THREAD_POOL.submit(() -> {
            try {
                // Send start message on main thread
                Minecraft.getInstance().execute(() -> {
                    ClientMessageLogger.chatInfo("gd656killicon.client.texture.reload_start");
                });

                // Ensure texture files exist for all presets
                ensureAllPresetsTextureFiles(false);

                String currentPresetId = ConfigManager.getCurrentPresetId();
                Path presetDir = CONFIG_ASSETS_DIR.resolve(currentPresetId).resolve("textures");
                final int[] totalTextures = { DEFAULT_TEXTURES.length };
                
                // Load images into memory asynchronously
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
                        Minecraft.getInstance().execute(() -> {
                            ClientMessageLogger.chatInfo("gd656killicon.client.texture.reload_progress", current, totalTextures[0]);
                        });
                    }
                }

                // Register textures on main thread
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
                             // If registration fails, we should close the image manually to avoid leak?
                             // DynamicTexture takes ownership, but if new DynamicTexture fails...
                             image.close();
                        }
                    }
                    ClientMessageLogger.chatSuccess("gd656killicon.client.texture.reload_success", currentPresetId, successCount);
                });

            } catch (Exception e) {
                ClientMessageLogger.error("gd656killicon.client.texture.reload_error", e.getMessage());
            }
        });
    }

    public static void resetTextures(String presetId) {
        ensureTextureFilesForPreset(presetId, true);
        ClientMessageLogger.chatSuccess("gd656killicon.client.texture.reset_success", presetId);
        
        // If resetting current preset, reload cache
        if (presetId.equals(ConfigManager.getCurrentPresetId())) {
            reload();
        }
    }
    
    public static void resetTexturesAsync(String presetId) {
        TEXTURE_THREAD_POOL.submit(() -> {
            try {
                Minecraft.getInstance().execute(() -> {
                    ClientMessageLogger.chatInfo("gd656killicon.client.texture.reset_start");
                });

                ensureTextureFilesForPreset(presetId, true);
                
                Minecraft.getInstance().execute(() -> {
                    ClientMessageLogger.chatSuccess("gd656killicon.client.texture.reset_success", presetId);
                });

                if (presetId.equals(ConfigManager.getCurrentPresetId())) {
                    reloadAsync();
                }
            } catch (Exception e) {
                ClientMessageLogger.error("gd656killicon.client.texture.reset_error", e.getMessage());
            }
        });
    }
    
    public static void resetAllTextures() {
        Set<String> presets = ConfigManager.getPresetIds();
        for (String presetId : presets) {
            resetTextures(presetId);
        }
    }

    public static void resetAllTexturesAsync() {
        TEXTURE_THREAD_POOL.submit(() -> {
            try {
                Minecraft.getInstance().execute(() -> {
                    ClientMessageLogger.chatInfo("gd656killicon.client.texture.reset_start");
                });

                Set<String> presets = ConfigManager.getPresetIds();
                int total = presets.size();
                int current = 0;

                for (String presetId : presets) {
                    ensureTextureFilesForPreset(presetId, true);
                    current++;
                    final int progress = current;
                    Minecraft.getInstance().execute(() -> {
                        ClientMessageLogger.chatInfo("gd656killicon.client.texture.reset_progress", progress, total);
                    });
                }
                
                Minecraft.getInstance().execute(() -> {
                    ClientMessageLogger.chatSuccess("gd656killicon.client.texture.reset_success", "all presets");
                });
                
                reloadAsync();
            } catch (Exception e) {
                ClientMessageLogger.error("gd656killicon.client.texture.reset_error", e.getMessage());
            }
        });
    }

    public static ResourceLocation getTexture(String path) {
        String presetId = ConfigManager.getCurrentPresetId();
        String resolvedPath = IconTextureAnimationManager.resolveTexturePath(path);
        String cacheKey = presetId + ":" + resolvedPath;
        
        // 如果缓存中有，直接返回
        if (TEXTURE_CACHE.containsKey(cacheKey)) {
            return TEXTURE_CACHE.get(cacheKey);
        }

        // 如果缓存中没有，尝试加载一次
        if (loadExternalTexture(presetId, resolvedPath)) {
            return TEXTURE_CACHE.get(cacheKey);
        }

        // 如果外部加载失败，回退到默认的 ResourceLocation (从 JAR 读取)
        return ResourceLocation.fromNamespaceAndPath(Gd656killicon.MODID, "textures/" + resolvedPath);
    }

    public static java.util.List<String> getDefaultTexturePaths() {
        return java.util.Arrays.asList(DEFAULT_TEXTURES);
    }

    private static void ensureAllPresetsTextureFiles(boolean forceReset) {
        Set<String> presets = ConfigManager.getPresetIds();
        // Always ensure default presets exist
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
                // 确保父目录存在
                if (!Files.exists(targetPath.getParent())) {
                    Files.createDirectories(targetPath.getParent());
                }

                // 如果强制重置 或 文件不存在，则从 JAR 复制
                if (forceReset || !Files.exists(targetPath)) {
                    ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(Gd656killicon.MODID, "textures/" + texturePath);
                    try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(resourceLocation).get().open()) {
                        Files.copy(stream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception e) {
                         // Fallback mechanism
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
        Path file = CONFIG_ASSETS_DIR.resolve(presetId).resolve("textures").resolve(path);
        if (!Files.exists(file)) {
            return false;
        }

        try (InputStream stream = new FileInputStream(file.toFile())) {
            NativeImage image = NativeImage.read(stream);
            DynamicTexture texture = new DynamicTexture(image);
            // 生成唯一的 ResourceLocation 名称，包含预设ID
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
        // 释放旧纹理
        for (ResourceLocation loc : TEXTURE_CACHE.values()) {
            Minecraft.getInstance().getTextureManager().release(loc);
        }
        TEXTURE_CACHE.clear();
    }
}
