package org.mods.gd656killicon.common.honor;

/**
 * 荣誉定义(声明式, 仿 BonusDefinition)。
 * <p>
 * 一个荣誉 = 一条声明: 唯一 ID + 条件类型 + 判定参数 + 显示资源。
 * 判定逻辑不在此处, 由 {@link ConditionType} 对应的分型判定器执行。
 * </p>
 *
 * @param id             荣誉唯一 ID(小写下划线, 同时用于纹理键 honor_&lt;id&gt;.png 与 lang 键)
 * @param conditionType  条件类型(决定判定器分派)
 * @param conditionParams 判定参数(格式由对应判定器解析, 见 {@link ConditionType} 注释)
 * @param nameKey        显示名 lang 键(gd656killicon.honor.&lt;id&gt;.name)
 * @param descKey        描述 lang 键(gd656killicon.honor.&lt;id&gt;.desc)
 * @param textureKey     纹理键(honor_&lt;id&gt;, 对应 textures/honor/honor_&lt;id&gt;.png)
 * @param unlockRequired 解锁所需次数(获得次数 ≥ 该值才判定已解锁; 默认 1)
 */
public record HonorDefinition(
        String id,
        ConditionType conditionType,
        String conditionParams,
        String nameKey,
        String descKey,
        String textureKey,
        int unlockRequired
) {
    public HonorDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("honor id must not be blank");
        }
        if (conditionType == null) {
            throw new IllegalArgumentException("conditionType must not be null");
        }
        if (unlockRequired <= 0) {
            unlockRequired = 1;
        }
    }

    /** 便捷构造: 名称/描述/纹理键按默认约定生成; 解锁所需次数 1。 */
    public HonorDefinition(String id, ConditionType conditionType, String conditionParams) {
        this(id, conditionType, conditionParams,
                "gd656killicon.honor." + id + ".name",
                "gd656killicon.honor." + id + ".desc",
                "honor_" + id, 1);
    }

    /** 便捷构造: 带解锁所需次数。 */
    public HonorDefinition(String id, ConditionType conditionType, String conditionParams, int unlockRequired) {
        this(id, conditionType, conditionParams,
                "gd656killicon.honor." + id + ".name",
                "gd656killicon.honor." + id + ".desc",
                "honor_" + id, unlockRequired);
    }

    public String displayNameKey() {
        return nameKey != null ? nameKey : "gd656killicon.honor." + id + ".name";
    }

    public String descriptionKey() {
        return descKey != null ? descKey : "gd656killicon.honor." + id + ".desc";
    }

    public String textureKey() {
        return textureKey != null ? textureKey : "honor_" + id;
    }
}
