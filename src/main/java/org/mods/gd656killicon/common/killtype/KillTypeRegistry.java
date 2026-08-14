package org.mods.gd656killicon.common.killtype;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 击杀类型注册表：全部 KillType 映射的唯一数据源。
 *
 * <p>各消费点（SubtitleRenderer/ScrollingIconRenderer/InfiniteGridWidget/
 * Battlefield1Renderer/SoundTriggerManager/ElementConfigContent）的
 * killType → 配置键/纹理键/声音槽位映射全部查表，删除散落 switch。</p>
 *
 * <p>未注册的类型即无定义：查询返回 null，调用方直接解引用（失败快）。</p>
 */
public final class KillTypeRegistry {
    private static final Map<String, KillTypeDefinition> BY_ID = new LinkedHashMap<>();
    private static final Map<Integer, KillTypeDefinition> BY_TYPE = new HashMap<>();

    static {
        for (KillTypeDefinition def : KillTypeDefinitionsArea.all()) {
            register(def);
        }
        validate();
    }

    private KillTypeRegistry() {
    }

    public static void register(KillTypeDefinition def) {
        if (BY_ID.containsKey(def.id())) {
            throw new IllegalStateException("KillTypeRegistry: duplicate id: " + def.id());
        }
        if (BY_TYPE.containsKey(def.type())) {
            throw new IllegalStateException("KillTypeRegistry: duplicate type " + def.type() + " for id " + def.id());
        }
        BY_ID.put(def.id(), def);
        BY_TYPE.put(def.type(), def);
        // 声明式配置注册表: 顺带注册该击杀类型的 format 配置键(默认值空, 语言默认由 formats json 提供)
        org.mods.gd656killicon.common.config.ElementConfigRegistry.registerFormatKey(
                "subtitle/kill_feed", def.formatKey(), "");
    }

    private static void validate() {
        for (KillTypeDefinition def : BY_ID.values()) {
            if (def.displayName() == null || def.displayName().isBlank()) {
                throw new IllegalStateException("KillTypeRegistry[" + def.id() + "]: displayName is required");
            }
        }
    }

    public static KillTypeDefinition get(int type) {
        return BY_TYPE.get(type);
    }

    public static KillTypeDefinition get(String id) {
        return BY_ID.get(id);
    }

    public static Collection<KillTypeDefinition> getAll() {
        return BY_ID.values();
    }

    /** 按 scrolling 纹理键反查类型（第一个匹配，NORMAL 的 "default" 在前），无匹配返回 -1。 */
    public static int getKillTypeByTextureKey(String textureKey) {
        if (textureKey == null) {
            return -1;
        }
        for (KillTypeDefinition def : BY_ID.values()) {
            if (textureKey.equals(def.textureKey())) {
                return def.type();
            }
        }
        return -1;
    }

    /** 按 format 键名反查类型（共享 format_normal 时返回第一个 NORMAL），无匹配返回 -1。 */
    public static int getKillTypeByFormatKey(String formatKey) {
        if (formatKey == null) {
            return -1;
        }
        for (KillTypeDefinition def : BY_ID.values()) {
            if (formatKey.equals(def.formatKey())) {
                return def.type();
            }
        }
        return -1;
    }
}
