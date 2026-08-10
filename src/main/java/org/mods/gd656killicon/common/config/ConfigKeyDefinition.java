package org.mods.gd656killicon.common.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * 单个配置项定义(仿 BonusDefinition 的声明式 builder)。
 * 一个定义 = 键名 + 归属元素 + 值类型 + 一级分类 + 一级开关键依赖 + 默认值。
 * 显示名/描述 lang 键由 key 自动派生: generic.&lt;key&gt; / config.desc.&lt;key&gt;。
 */
public final class ConfigKeyDefinition {
    private final String key;
    private final String elementId;   // 配置归属: 属于哪个元素(如 "subtitle/hit_info")
    private final ConfigType type;
    private final Category category;
    private final String dependsOn;   // 一级开关键名(null = 无依赖, 永不灰)
    private final JsonElement defaultValue;
    private final String[] choices;   // 仅 CHOICE 用
    private final String textureTab;  // 仅 TEXTURE 类键: 所属纹理名(如 "default"); 其它为 null

    private ConfigKeyDefinition(Builder b) {
        this.key = b.key;
        this.elementId = b.elementId;
        this.type = b.type;
        this.category = b.category;
        this.dependsOn = b.dependsOn;
        this.defaultValue = b.defaultValue;
        this.choices = b.choices;
        this.textureTab = b.textureTab;
    }

    public String key() { return key; }
    public String elementId() { return elementId; }
    public ConfigType type() { return type; }
    public Category category() { return category; }
    public String dependsOn() { return dependsOn; }
    public JsonElement defaultValue() { return defaultValue; }
    public String[] choices() { return choices; }
    public String textureTab() { return textureTab; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String key;
        private String elementId;
        private ConfigType type = ConfigType.STRING;
        private Category category = Category.EFFECT;
        private String dependsOn;
        private JsonElement defaultValue;
        private String[] choices;
        private String textureTab;

        public Builder key(String key) { this.key = key; return this; }
        public Builder element(String elementId) { this.elementId = elementId; return this; }
        public Builder type(ConfigType type) { this.type = type; return this; }
        public Builder category(Category category) { this.category = category; return this; }
        public Builder dependsOn(String dependsOn) { this.dependsOn = dependsOn; return this; }
        public Builder textureTab(String textureTab) { this.textureTab = textureTab; return this; }

        public Builder defaultValue(boolean v) { this.defaultValue = new JsonPrimitive(v); return this; }
        public Builder defaultValue(int v) { this.defaultValue = new JsonPrimitive(v); return this; }
        public Builder defaultValue(float v) { this.defaultValue = new JsonPrimitive(v); return this; }
        public Builder defaultValue(String v) { this.defaultValue = new JsonPrimitive(v); return this; }
        public Builder defaultValue(JsonElement v) { this.defaultValue = v; return this; }

        public Builder choices(String... choices) { this.choices = choices; return this; }

        public ConfigKeyDefinition build() {
            if (key == null || elementId == null) {
                throw new IllegalStateException("ConfigKeyDefinition 必须提供 key 与 element: " + key + "@" + elementId);
            }
            if (defaultValue == null) {
                throw new IllegalStateException("ConfigKeyDefinition 必须提供默认值: " + elementId + "/" + key);
            }
            return new ConfigKeyDefinition(this);
        }
    }
}
