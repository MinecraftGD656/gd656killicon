package org.mods.gd656killicon.client.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * format 默认文本的单一语言来源(jar 资源 assets/gd656killicon/formats/&lt;lang&gt;.json)。
 * <p>
 * 所有带 format 的元素(加分项/击杀类型/连杀/突袭占领/分数/伤害/honor)的默认文本都从这里按当前客户端语言取,
 * json 按元素分组(顶层 = 元素 id, 组内 = 该元素的 format 键), 键名在不同元素间允许重叠。
 * 当前语言 json 缺失某键 → 回落 zh_cn → 再缺失返回空串。
 * 官方预设 json 中的 format 键优先级更高(渲染/重置时 json 有值即用 json 值, 不经过此处)。
 * </p>
 */
public final class FormatDefaultsManager {
    private static final String DEFAULT_LANGUAGE = "zh_cn";
    private static final String FORMATS_PATH_PREFIX = "formats/";

    private static Map<String, Map<String, String>> cache = Map.of();
    private static String cachedLanguage = null;

    private FormatDefaultsManager() {
    }

    /** 取当前语言某元素下 format 键的默认文本; 缺失回落 zh_cn; 再无返回空串。 */
    public static String getDefault(String elementId, String key) {
        if (elementId == null || key == null || key.isEmpty()) {
            return "";
        }
        Map<String, Map<String, String>> current = currentLanguageDefaults();
        Map<String, String> element = current.get(elementId);
        if (element != null) {
            String value = element.get(key);
            if (value != null) {
                return value;
            }
        }
        if (!DEFAULT_LANGUAGE.equals(cachedLanguage)) {
            Map<String, String> zhElement = load(DEFAULT_LANGUAGE).get(elementId);
            if (zhElement != null) {
                String value = zhElement.get(key);
                if (value != null) {
                    return value;
                }
            }
        }
        return "";
    }

    /** 当前客户端语言 code(如 "zh_cn"/"en_us"); 客户端不可用返回 null。 */
    private static String currentLanguage() {
        try {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            return minecraft != null && minecraft.options != null ? minecraft.options.languageCode : null;
        } catch (Throwable throwable) {
            return null;
        }
    }

    /** 当前语言默认文本表(按语言 code 缓存, 语言变化时重载)。 */
    private static Map<String, Map<String, String>> currentLanguageDefaults() {
        String language = currentLanguage();
        if (language == null) {
            return load(DEFAULT_LANGUAGE);
        }
        if (!language.equals(cachedLanguage)) {
            cache = load(language);
            cachedLanguage = language;
        }
        return cache;
    }

    /** 从 jar 资源加载某语言 format json(元素分组); 加载失败返回空表。 */
    private static Map<String, Map<String, String>> load(String language) {
        try {
            ResourceLocation location = new ResourceLocation("gd656killicon", FORMATS_PATH_PREFIX + language + ".json");
            var resourceOptional = net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(location);
            if (resourceOptional.isEmpty()) {
                return Map.of();
            }
            try (InputStreamReader reader = new InputStreamReader(resourceOptional.get().open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                Map<String, Map<String, String>> result = new HashMap<>(root.size());
                for (String elementId : root.keySet()) {
                    JsonObject elementObject = root.getAsJsonObject(elementId);
                    Map<String, String> element = new HashMap<>(elementObject.size());
                    for (String key : elementObject.keySet()) {
                        element.put(key, elementObject.get(key).getAsString());
                    }
                    result.put(elementId, element);
                }
                return result;
            }
        } catch (Exception ignored) {
            return Map.of();
        }
    }
}
