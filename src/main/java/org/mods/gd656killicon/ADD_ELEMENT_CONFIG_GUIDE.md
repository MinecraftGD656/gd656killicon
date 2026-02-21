# 新增元素配置项指南 (Adding Element Config Guide)

本文档介绍了如何在 `GD656Killicon` 模组中为现有的 UI 元素（如击杀图标、计分板等）添加新的配置项（Config Entry）。

## 核心流程

1.  **定义配置字段**：在渲染器的配置类中添加 Java 字段。
2.  **设置安全默认值**：在 `ElementConfigManager` 中注册默认值，防止空指针或加载错误。
3.  **应用配置逻辑**：在渲染逻辑中使用新字段。
4.  **持久化支持**：确保 JSON 序列化/反序列化能处理新字段（通常由 GSON 自动处理）。

---

## 步骤 1：定义配置字段

找到对应元素的渲染器类（例如 `BonusListRenderer.java` 或 `ScoreSubtitleRenderer.java`），在其内部定义的 `Config` 类中添加新字段。

```java
// 示例：在 BonusListRenderer.java 中
public static class Config {
    // 现有字段
    public boolean visible = true;
    public double scale = 1.0;
    
    // [新增] 新的配置项
    @SerializedName("new_feature_enabled") // 可选：指定 JSON 中的键名
    public boolean newFeatureEnabled = true;
    
    @SerializedName("text_color")
    public String textColor = "#FFFFFF";
}
```

> **注意**：建议使用 `@SerializedName` 注解来保持 JSON 键名的风格统一（通常是 snake_case），而 Java 字段使用 camelCase。

---

## 步骤 2：设置安全默认值

为了防止用户配置文件缺失该字段导致程序崩溃，需要在 `ElementConfigManager` 中定义“安全默认值”。当从 JSON 加载失败或字段缺失时，将使用此默认值。

**修改文件**：`src/main/java/org/mods/gd656killicon/client/config/ElementConfigManager.java`

找到 `ELEMENT_SAFE_DEFAULTS` 映射表，在对应元素的默认 JSON 字符串中添加新字段。

```java
static {
    // ... 其他元素
    
    // 假设这是 BonusList 的默认配置
    Map<String, Object> bonusListDefaults = new HashMap<>();
    bonusListDefaults.put("visible", true);
    bonusListDefaults.put("scale", 1.0);
    
    // [新增] 添加新字段的默认值
    bonusListDefaults.put("new_feature_enabled", true);
    bonusListDefaults.put("text_color", "#FFFFFF");
    
    ELEMENT_SAFE_DEFAULTS.put("bonus_list", bonusListDefaults);
}
```

**作用**：
当模组首次运行或配置文件损坏重置时，`ElementConfigManager` 会利用这些 Map 生成初始的默认配置对象。如果这一步被忽略，新字段可能会被初始化为 Java 类型的默认值（如 `false` 或 `null`），这可能不是预期的行为。

---

## 步骤 3：应用配置逻辑

回到渲染器类，在 `render` 方法或其他逻辑中使用该配置。通常需要通过 `ElementConfigManager.getConfig("element_name")` 获取配置对象。

```java
// 在 BonusListRenderer.java 的 render 方法中
public void render(PoseStack poseStack) {
    Config config = (Config) ElementConfigManager.getConfig("bonus_list");
    
    if (config == null || !config.visible) return;
    
    // [新增] 使用新配置
    if (config.newFeatureEnabled) {
        int color = Color.parse(config.textColor); // 假设有颜色解析工具
        // 执行相关渲染逻辑...
    }
}
```

---

## 步骤 4：验证配置读写

1.  **启动游戏**。
2.  **检查生成的文件**：查看 `.minecraft/config/gd656killicon/presets/<current_preset>/<element>.json`。
3.  **预期结果**：文件中应包含你新增的字段 `"new_feature_enabled": true`。
4.  **热重载测试**：在游戏中修改该文件并保存，执行 `/gd656killicon client config reload`，观察游戏内效果是否实时变化。

---

## 进阶：添加到配置 UI (Config UI)

如果模组有图形化配置界面（GUI），你还需要在构建 GUI 的代码中添加对应的控件。

1.  找到配置 GUI 构建类（如 `GlobalConfigTab` 或对应的 Widget 构建器）。
2.  使用 `ConfigEntryListWidget` 添加对应的 `BooleanEntry`、`StringEntry` 或 `SliderEntry`。
3.  绑定回调函数以更新 `ElementConfigManager` 中的值并保存。

```java
// 伪代码示例
listWidget.addEntry(new BooleanEntry(
    Component.translatable("config.gd656killicon.new_feature"),
    config.newFeatureEnabled,
    (newValue) -> {
        config.newFeatureEnabled = newValue;
        ElementConfigManager.saveConfig("bonus_list"); // 保存更改
    }
));
```
