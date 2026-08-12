package org.mods.gd656killicon.common.bonus;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 加分项注册表：全部加分项定义的唯一数据源。
 *
 * <p>新增一个加分项 = 在 {@link BonusDefinitionsArea#all()} 中加一段定义，
 * 常量表、默认倍率、格式配置、默认键注入、Help 名称/描述、特殊行为全部自动接线。</p>
 *
 * <p>设计原则：唯一路径，零兜底。查询不到即视为未注册（调用方不处理）；
 * 定义数据非法（重复 id/type、缺显示名/格式）在类初始化时直接抛错，失败快。</p>
 */
public final class BonusRegistry {
    private static final Map<String, BonusDefinition> BY_ID = new LinkedHashMap<>();
    private static final Map<Integer, BonusDefinition> BY_TYPE = new HashMap<>();
    private static final Map<String, Integer> FORMAT_KEY_TO_TYPE = new HashMap<>();
    private static final Set<String> FORMAT_KEYS = new HashSet<>();

    static {
        for (BonusDefinition def : BonusDefinitionsArea.all()) {
            register(def);
        }
        validate();
    }

    private BonusRegistry() {
    }

    /** 注册一条定义（查重失败直接抛错，阻止模组启动）。 */
    public static void register(BonusDefinition def) {
        if (BY_ID.containsKey(def.id())) {
            throw new IllegalStateException("BonusRegistry: duplicate bonus id: " + def.id());
        }
        if (BY_TYPE.containsKey(def.type())) {
            throw new IllegalStateException("BonusRegistry: duplicate bonus type " + def.type() + " for id " + def.id());
        }
        BY_ID.put(def.id(), def);
        BY_TYPE.put(def.type(), def);
        // 声明式配置注册表: 顺带注册该加分项的 format 配置键(默认值空, 语言默认由 formats json 提供)
        org.mods.gd656killicon.common.config.ElementConfigRegistry.registerFormatKey(
                "subtitle/bonus_list", def.formatConfigKey(), "");
        FORMAT_KEY_TO_TYPE.put(def.formatConfigKey(), def.type());
        FORMAT_KEYS.add(def.formatConfigKey());
    }

    /** 定义数据完整性校验：显示名必填。 */
    private static void validate() {
        for (BonusDefinition def : BY_ID.values()) {
            if (isBlank(def.displayName())) {
                throw new IllegalStateException("BonusRegistry[" + def.id() + "]: displayName is required");
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static BonusDefinition get(String id) {
        return BY_ID.get(id);
    }

    public static BonusDefinition get(int type) {
        return BY_TYPE.get(type);
    }

    public static boolean isRegistered(int type) {
        return BY_TYPE.containsKey(type);
    }

    /** 全部定义，按注册顺序（= type 升序）。 */
    public static Collection<BonusDefinition> getAll() {
        return BY_ID.values();
    }

    /** 该键名是否为加分项 format 配置键（用于区分其它 format_* 键）。 */
    public static boolean isFormatKey(String key) {
        return FORMAT_KEYS.contains(key);
    }

    /** 由 format 配置键反查加分项 type，非加分项键返回 -1。 */
    public static int getTypeByFormatKey(String formatKey) {
        Integer type = FORMAT_KEY_TO_TYPE.get(formatKey);
        return type != null ? type : -1;
    }

    /** 加分项显示名 lang key（gd656killicon.bonus.&lt;ID&gt;.name），未注册返回空串。 */
    public static String nameKey(int type) {
        BonusDefinition def = get(type);
        return def != null ? def.displayName() : "";
    }

    /** 加分项描述 lang key（gd656killicon.bonus.&lt;ID&gt;.desc），未注册返回空串。 */
    public static String descKey(int type) {
        BonusDefinition def = get(type);
        return def != null ? def.description() : "";
    }
}
