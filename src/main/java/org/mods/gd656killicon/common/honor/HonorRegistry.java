package org.mods.gd656killicon.common.honor;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Set;

/**
 * 荣誉注册表(声明式, 仿 BonusRegistry)。
 * <p>
 * 荣誉定义在 {@link HonorDefinitionsArea} 中集中声明, 引擎/判定器/后续的配置
 * 与客户端显示均从此注册表取数。
 * </p>
 */
public final class HonorRegistry {

    private static final Map<String, HonorDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<String, String> FORMAT_KEY_TO_ID = new HashMap<>();
    private static final Set<String> FORMAT_KEYS = new HashSet<>();

    private HonorRegistry() {
    }

    static {
        HonorDefinitionsArea.register();
    }

    // ==================== 注册 ====================

    public static void register(HonorDefinition def) {
        if (def == null || DEFINITIONS.containsKey(def.id())) {
            return;
        }
        DEFINITIONS.put(def.id(), def);
        // 声明式配置注册表: 顺带注册该荣誉的字幕配置键 format_<id>(默认值 = 注册表 format, 单一来源)
        String formatKey = "format_" + def.id();
        org.mods.gd656killicon.common.config.ElementConfigRegistry.registerFormatKey(
                "kill_icon/honor", formatKey, def.format() != null ? def.format() : "");
        FORMAT_KEY_TO_ID.put(formatKey, def.id());
        FORMAT_KEYS.add(formatKey);
    }

    // ==================== 查询 ====================

    public static HonorDefinition get(String id) {
        return DEFINITIONS.get(id);
    }

    public static boolean isRegistered(String id) {
        return DEFINITIONS.containsKey(id);
    }

    public static Set<String> getIds() {
        return Collections.unmodifiableSet(DEFINITIONS.keySet());
    }

    public static Collection<HonorDefinition> getAll() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }

    public static int size() {
        return DEFINITIONS.size();
    }

    // ==================== format 键(字幕配置) ====================

    /** 该键名是否为荣誉字幕配置键(format_<honor_id>)。 */
    public static boolean isFormatKey(String key) {
        return FORMAT_KEYS.contains(key);
    }

    /** 由字幕配置键反查荣誉 id, 非荣誉字幕键返回 null。 */
    public static String getHonorIdByFormatKey(String formatKey) {
        return FORMAT_KEY_TO_ID.get(formatKey);
    }

    /** 语言驱动默认字幕的 lang 键(gd656killicon.honor.&lt;id&gt;.format), 未注册返回空串。 */
    public static String formatLangKey(String honorId) {
        HonorDefinition def = get(honorId);
        return def != null ? def.formatLangKey() : "";
    }

    /** 注册表默认字幕(单文本), 未注册返回空串。 */
    public static String registryFormat(String honorId) {
        HonorDefinition def = get(honorId);
        return def != null && def.format() != null ? def.format() : "";
    }

    /** 荣誉显示名 lang 键(gd656killicon.honor.&lt;id&gt;.name), 未注册返回空串。 */
    public static String displayNameKey(String honorId) {
        HonorDefinition def = get(honorId);
        return def != null ? def.displayNameKey() : "";
    }

    /**
     * 字幕文本唯一解析路径(与 BonusRegistry.resolveFormat 同款):
     * <pre>
     * 配置值缺失 / 为空 → 语言默认字幕(由调用方按语言解析 lang 键)→ 注册表 format(单文本) → 显示名
     * 其他               → 玩家自定义文本, 原样使用
     * </pre>
     * 未注册的荣誉返回空串(调用方不显示)。
     * 语言解析不在此处(I18n 为客户端类, common 层不可依赖), 由客户端调用方传入 resolvedLangDefault。
     */
    public static String resolveFormat(String honorId, String configValue, String resolvedLangDefault) {
        HonorDefinition def = get(honorId);
        if (def == null) {
            return "";
        }
        if (configValue != null && !configValue.isEmpty()) {
            return configValue;
        }
        if (resolvedLangDefault != null && !resolvedLangDefault.isEmpty()) {
            return resolvedLangDefault;
        }
        if (def.format() != null && !def.format().isEmpty()) {
            return def.format();
        }
        return resolvedLangDefault != null ? resolvedLangDefault : "";
    }
}
