package org.mods.gd656killicon.common.bonus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 加分项定义：一处注册，全自动接线。
 *
 * <p>必填：id / type / scoreExpression / displayName / description / format。
 * 其余字段可选，均有默认值（对应现状各消费点的硬编码特判）。</p>
 */
public final class BonusDefinition {
    private final String id;
    private final int type;
    private final String scoreExpression;
    private final FactorType factor;
    /** 显示名 lang key（gd656killicon.bonus.<ID>.name）。 */
    private final String displayName;
    /** 描述 lang key（gd656killicon.bonus.<ID>.desc）。 */
    private final String description;
    private final float scoreCap;
    private final MergeBehavior mergeBehavior;
    private final boolean priorityKill;
    private final int iconKillType;
    private final String killFeedPayload;
    private final StatCategory statCategory;
    private final boolean disabledByDefault;
    /** 连杀档位 → 专用字幕 lang key（KILL_COMBO 的 2~8 连杀）。 */
    private final Map<Integer, String> streakSubtitles;

    private BonusDefinition(Builder b) {
        this.id = b.id;
        this.type = b.type;
        this.scoreExpression = b.scoreExpression;
        this.factor = b.factor;
        this.displayName = b.displayName;
        this.description = b.description;
        this.scoreCap = b.scoreCap;
        this.mergeBehavior = b.mergeBehavior;
        this.priorityKill = b.priorityKill;
        this.iconKillType = b.iconKillType;
        this.killFeedPayload = b.killFeedPayload;
        this.statCategory = b.statCategory;
        this.disabledByDefault = b.disabledByDefault;
        this.streakSubtitles = b.streakSubtitles;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String id() {
        return id;
    }

    public int type() {
        return type;
    }

    public String scoreExpression() {
        return scoreExpression;
    }

    public FactorType factor() {
        return factor;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public String formatConfigKey() {
        return "format_" + id.toLowerCase();
    }

    /** 分数上限；&lt;= 0 表示使用全局上限。 */
    public float scoreCap() {
        return scoreCap;
    }

    public MergeBehavior mergeBehavior() {
        return mergeBehavior;
    }

    public boolean priorityKill() {
        return priorityKill;
    }

    /** 触发的 HUD 图标类型（KillType 值），-1 表示无。 */
    public int iconKillType() {
        return iconKillType;
    }

    /** 触发 capture killfeed 的 payload，null 表示不触发。 */
    public String killFeedPayload() {
        return killFeedPayload;
    }

    public StatCategory statCategory() {
        return statCategory;
    }

    public boolean disabledByDefault() {
        return disabledByDefault;
    }

    /** 指定连杀档位的专用字幕 lang key，未注册该档返回 null。 */
    public String streakSubtitle(int combo) {
        return streakSubtitles.get(combo);
    }

    public static final class Builder {
        private String id;
        private int type = -1;
        private String scoreExpression = "0";
        private FactorType factor = FactorType.NONE;
        private String displayName;
        private String description;
        private float scoreCap = -1f;
        private MergeBehavior mergeBehavior = MergeBehavior.BY_TYPE_EXTRA;
        private boolean priorityKill = false;
        private int iconKillType = -1;
        private String killFeedPayload;
        private StatCategory statCategory = StatCategory.NONE;
        private boolean disabledByDefault = false;
        private final Map<Integer, String> streakSubtitles = new LinkedHashMap<>();

        public Builder id(String v) {
            this.id = v;
            return this;
        }

        public Builder type(int v) {
            this.type = v;
            return this;
        }

        public Builder scoreExpression(String v) {
            this.scoreExpression = v;
            return this;
        }

        public Builder factor(FactorType v) {
            this.factor = v;
            return this;
        }

        public Builder displayName(String v) {
            this.displayName = v;
            return this;
        }

        public Builder description(String v) {
            this.description = v;
            return this;
        }

        public Builder scoreCap(float v) {
            this.scoreCap = v;
            return this;
        }

        public Builder mergeBehavior(MergeBehavior v) {
            this.mergeBehavior = v;
            return this;
        }

        public Builder priorityKill(boolean v) {
            this.priorityKill = v;
            return this;
        }

        public Builder iconKillType(int v) {
            this.iconKillType = v;
            return this;
        }

        public Builder killFeedPayload(String v) {
            this.killFeedPayload = v;
            return this;
        }

        public Builder statCategory(StatCategory v) {
            this.statCategory = v;
            return this;
        }

        public Builder disabledByDefault(boolean v) {
            this.disabledByDefault = v;
            return this;
        }

        /** 注册一个连杀档位的专用字幕 lang key（如 2~8 连杀）。 */
        public Builder streakSubtitle(int combo, String langKey) {
            this.streakSubtitles.put(combo, langKey);
            return this;
        }

        public BonusDefinition build() {
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("BonusDefinition: id is required");
            }
            if (!id.equals(id.toUpperCase())) {
                throw new IllegalStateException("BonusDefinition: id must be uppercase, got: " + id);
            }
            if (type < 0) {
                throw new IllegalStateException("BonusDefinition[" + id + "]: type is required (>= 0)");
            }
            if (displayName == null || displayName.isBlank()) {
                throw new IllegalStateException("BonusDefinition[" + id + "]: displayName is required");
            }
            return new BonusDefinition(this);
        }
    }
}
