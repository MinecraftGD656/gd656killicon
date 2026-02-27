package org.mods.gd656killicon.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.loading.FMLPaths;

import org.mods.gd656killicon.client.config.ElementConfigManager.ElementPreset;
import org.mods.gd656killicon.client.util.ClientMessageLogger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.Random;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class PresetPackManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("gd656killicon");
    private static final Path EXPORT_DIR = CONFIG_DIR.resolve("export");
    private static final Path ASSETS_DIR = CONFIG_DIR.resolve("assets");

    public static Path getExportDir() {
        return EXPORT_DIR;
    }

    public static boolean exportPreset(String presetId, String fileName) {
        try {
            if (!Files.exists(EXPORT_DIR)) {
                Files.createDirectories(EXPORT_DIR);
            }

            if (!fileName.toLowerCase().endsWith(".gdpack")) {
                fileName += ".gdpack";
            }

            Path exportPath = EXPORT_DIR.resolve(fileName);
            
            ElementPreset preset = ElementConfigManager.getActivePresets().get(presetId);
            if (preset == null) {
                ClientMessageLogger.chatError("gd656killicon.client.config.export.preset_not_found", presetId);
                return false;
            }

            JsonObject presetJson = new JsonObject();
            presetJson.addProperty("original_id", presetId);
            presetJson.addProperty("display_name", preset.getDisplayName());
            JsonObject elements = new JsonObject();
            for (java.util.Map.Entry<String, JsonObject> entry : preset.getElementConfigs().entrySet()) {
                elements.add(entry.getKey(), entry.getValue());
            }
            presetJson.add("elements", elements);

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(exportPath))) {
                ZipEntry configEntry = new ZipEntry("preset.json");
                zos.putNextEntry(configEntry);
                zos.write(GSON.toJson(presetJson).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zos.closeEntry();

                Path presetAssetsDir = ASSETS_DIR.resolve(presetId);
                if (Files.exists(presetAssetsDir)) {
                    Files.walkFileTree(presetAssetsDir, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            String relativePath = presetAssetsDir.relativize(file).toString().replace("\\", "/");
                            ZipEntry assetEntry = new ZipEntry("assets/" + relativePath);
                            zos.putNextEntry(assetEntry);
                            Files.copy(file, zos);
                            zos.closeEntry();
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            }

            ClientMessageLogger.chatSuccess("gd656killicon.client.config.export.success", fileName);
            return true;
        } catch (Exception e) {
            ClientMessageLogger.chatError("gd656killicon.client.config.export.fail", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static String checkImport(File file) {
        try (ZipFile zipFile = new ZipFile(file, StandardCharsets.UTF_8)) {
            ZipEntry configEntry = zipFile.getEntry("preset.json");
            if (configEntry == null) {
                return "invalid_format";
            }
            
            
            
            return "valid";
        } catch (Exception e) {
            return "error";
        }
    }
    
    public static String getPresetIdFromPack(File file) {
        try (ZipFile zipFile = new ZipFile(file, StandardCharsets.UTF_8)) {
            ZipEntry configEntry = zipFile.getEntry("preset.json");
            if (configEntry != null) {
                try (InputStreamReader reader = new InputStreamReader(zipFile.getInputStream(configEntry), StandardCharsets.UTF_8)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (json.has("original_id")) {
                        return json.get("original_id").getAsString();
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static boolean importPreset(File file, String targetId) {
        try (ZipFile zipFile = new ZipFile(file, StandardCharsets.UTF_8)) {
            ZipEntry configEntry = zipFile.getEntry("preset.json");
            if (configEntry == null) throw new IOException("Missing preset.json");

            JsonObject presetJson;
            try (InputStreamReader reader = new InputStreamReader(zipFile.getInputStream(configEntry), StandardCharsets.UTF_8)) {
                presetJson = GSON.fromJson(reader, JsonObject.class);
            }
            
            ElementPreset preset = new ElementPreset();
            if (presetJson.has("display_name")) {
                preset.setDisplayName(presetJson.get("display_name").getAsString());
            }
            if (presetJson.has("elements")) {
                JsonObject elements = presetJson.getAsJsonObject("elements");
                for (String key : elements.keySet()) {
                    preset.addElementConfig(key, elements.getAsJsonObject(key));
                }
            }
            
            ElementConfigManager.getActivePresets().put(targetId, preset);
            ElementConfigManager.saveConfig();             ElementConfigManager.ensurePresetAssets(targetId);
            
            Path targetAssetsDir = ASSETS_DIR.resolve(targetId);
            if (!Files.exists(targetAssetsDir)) {
                Files.createDirectories(targetAssetsDir);
            }
            
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().startsWith("assets/") && !entry.isDirectory()) {
                    String relativePath = entry.getName().substring("assets/".length());
                    Path targetPath = targetAssetsDir.resolve(relativePath);
                    
                    if (!Files.exists(targetPath.getParent())) {
                        Files.createDirectories(targetPath.getParent());
                    }
                    
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            
            ClientMessageLogger.chatSuccess("gd656killicon.client.config.import.success", targetId);
            
            
            return true;
        } catch (Exception e) {
            ClientMessageLogger.chatError("gd656killicon.client.config.import.fail", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public static String generateRandomId() {
        Random rand = new Random();
        String id;
        do {
             id = String.format("%05d", rand.nextInt(100000));
        } while (ElementConfigManager.getActivePresets().containsKey(id));
        return id;
    }
}
