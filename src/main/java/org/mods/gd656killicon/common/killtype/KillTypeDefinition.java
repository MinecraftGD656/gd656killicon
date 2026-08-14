package org.mods.gd656killicon.common.killtype;

/**
 * 击杀类型定义：一处注册，全自动接线。
 *
 * <p>字段即各消费点的配置键/标识符映射（SubtitleRenderer 格式/颜色/开关、
 * 滚动图标纹理、Battlefield1 纹理、滚动声音槽位、ring 效果开关），
 * 均为实际数据，无默认兜底语义（未注册类型即无定义，调用方快速失败）。</p>
 */
public final class KillTypeDefinition {
    private final String id;
    private final int type;
    /** 显示名 lang key（gd656killicon.killtype.<ID>.name）。 */
    private final String displayName;
    private final String formatKey;
    private final String placeholderColorKey;
    private final String emphasisColorKey;
    /** 启用开关配置键；null = 该类型无开关（总是启用）。 */
    private final String enableKey;
    /** scrolling 元素纹理键（InfiniteGridWidget/ScrollingIconRenderer 预览）。 */
    private final String textureKey;
    /** battlefield1 元素纹理键。 */
    private final String bf1TextureKey;
    /** 滚动声音槽位 ID（ExternalSoundManager.SLOT_SCROLLING_* 的值）。 */
    private final String scrollingSoundSlotId;
    /** ring 效果开关配置键；null = 无 ring 开关。 */
    private final String ringEnableKey;

    private KillTypeDefinition(Builder b) {
        this.id = b.id;
        this.type = b.type;
        this.displayName = b.displayName;
        this.formatKey = b.formatKey;
        this.placeholderColorKey = b.placeholderColorKey;
        this.emphasisColorKey = b.emphasisColorKey;
        this.enableKey = b.enableKey;
        this.textureKey = b.textureKey;
        this.bf1TextureKey = b.bf1TextureKey;
        this.scrollingSoundSlotId = b.scrollingSoundSlotId;
        this.ringEnableKey = b.ringEnableKey;
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

    public String displayName() {
        return displayName;
    }

    public String formatKey() {
        return formatKey;
    }

    public String placeholderColorKey() {
        return placeholderColorKey;
    }

    public String emphasisColorKey() {
        return emphasisColorKey;
    }

    public String enableKey() {
        return enableKey;
    }

    public String textureKey() {
        return textureKey;
    }

    public String bf1TextureKey() {
        return bf1TextureKey;
    }

    public String scrollingSoundSlotId() {
        return scrollingSoundSlotId;
    }

    public String ringEnableKey() {
        return ringEnableKey;
    }

    public static final class Builder {
        private String id;
        private int type = -1;
        private String displayName;
        private String formatKey;
        private String placeholderColorKey;
        private String emphasisColorKey;
        private String enableKey;
        private String textureKey;
        private String bf1TextureKey;
        private String scrollingSoundSlotId;
        private String ringEnableKey;

        public Builder id(String v) {
            this.id = v;
            return this;
        }

        public Builder type(int v) {
            this.type = v;
            return this;
        }

        public Builder displayName(String v) {
            this.displayName = v;
            return this;
        }

        public Builder formatKey(String v) {
            this.formatKey = v;
            return this;
        }

        public Builder placeholderColorKey(String v) {
            this.placeholderColorKey = v;
            return this;
        }

        public Builder emphasisColorKey(String v) {
            this.emphasisColorKey = v;
            return this;
        }

        public Builder enableKey(String v) {
            this.enableKey = v;
            return this;
        }

        public Builder textureKey(String v) {
            this.textureKey = v;
            return this;
        }

        public Builder bf1TextureKey(String v) {
            this.bf1TextureKey = v;
            return this;
        }

        public Builder scrollingSoundSlotId(String v) {
            this.scrollingSoundSlotId = v;
            return this;
        }

        public Builder ringEnableKey(String v) {
            this.ringEnableKey = v;
            return this;
        }

        public KillTypeDefinition build() {
            if (id == null || id.isBlank() || !id.equals(id.toUpperCase())) {
                throw new IllegalStateException("KillTypeDefinition: id must be non-blank uppercase, got: " + id);
            }
            if (type < 0) {
                throw new IllegalStateException("KillTypeDefinition[" + id + "]: type is required");
            }
            if (displayName == null || displayName.isBlank() || formatKey == null || placeholderColorKey == null || emphasisColorKey == null
                    || textureKey == null || bf1TextureKey == null || scrollingSoundSlotId == null) {
                throw new IllegalStateException("KillTypeDefinition[" + id + "]: displayName/formatKey/colorKeys/textureKeys/soundSlotId are required");
            }
            return new KillTypeDefinition(this);
        }
    }
}
