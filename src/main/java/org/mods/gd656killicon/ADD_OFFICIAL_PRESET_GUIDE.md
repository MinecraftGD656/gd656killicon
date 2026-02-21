# 新增官方预设指南 (Adding Official Preset Guide)

本文档详细介绍了如何在 `GD656Killicon` 模组中添加新的官方预设（Official Preset）。官方预设是内置在模组中的配置集合，玩家可以通过重置命令恢复这些配置。

## 核心步骤概览

1.  **注册预设 ID**：在代码中声明新的预设 ID，确保系统将其识别为“官方预设”。
2.  **准备资源文件**：在模组的 `assets` 目录下创建对应的默认配置文件。
3.  **验证与测试**：在游戏中测试重置功能，确保预设能被正确加载。

---

## 步骤 1：注册预设 ID

为了防止玩家误删官方预设，以及支持 `reset` 命令的正确逻辑，必须在代码中注册预设 ID。

**修改文件**：`src/main/java/org/mods/gd656killicon/client/config/ElementConfigManager.java`

找到 `OFFICIAL_PRESET_IDS` 集合的初始化位置（通常在静态代码块或 `loadConfig` 方法的 fallback 逻辑中），添加你的新预设 ID。

```java
// 在 ElementConfigManager.java 中
public static boolean isOfficialPresetId(String presetId) {
    // 确保你的 ID 在这个列表中
    // 如果资源加载失败，系统会使用这个硬编码列表作为后备
    // Collections.addAll(OFFICIAL_PRESET_IDS, "00001", "00002", "00003", "00004", "00005", "NEW_ID");
    return OFFICIAL_PRESET_IDS.contains(presetId);
}
```

> **注意**：虽然系统会尝试从资源包中动态加载预设列表，但为了稳健性（Robustness），**必须**在 `OFFICIAL_PRESET_IDS` 的 fallback 列表中添加硬编码 ID。这能防止在某些环境下资源加载失败导致官方预设被误判为自定义预设而被删除。

---

## 步骤 2：准备资源文件

你需要将预设的 JSON 配置文件放置在模组的资源目录下。

**文件路径格式**：
`src/main/resources/assets/gd656killicon/presets/<preset_id>/<element_name>.json`

假设你要添加预设 `00006`，并且该预设包含 `kill_icon` 元素的配置：

1.  创建目录：`src/main/resources/assets/gd656killicon/presets/00006/`
2.  创建文件：`kill_icon.json`
3.  编写 JSON 内容：

```json
{
  "visible": true,
  "scale": 1.0,
  "x_offset": 0,
  "y_offset": 50,
  "animation_duration": 1000,
  "other_config_fields": "value"
}
```

**提示**：你可以先在游戏中调整好配置，然后从 `.minecraft/config/gd656killicon/presets/custom/<your_preset>/` 目录中复制生成的 JSON 文件到源代码的 `assets` 目录中。

---

## 步骤 3：验证与测试

完成代码和文件添加后，重新构建并启动游戏进行测试。

1.  **进入游戏**。
2.  **切换到新预设**（如果已实现 UI 切换）：
    或者使用命令加载：`/gd656killicon client config load preset 00006`
3.  **测试重置保护**：
    尝试重置该预设的某个元素配置：
    `/gd656killicon client config reset element 00006 kill_icon`
    
    *   **预期行为**：配置应恢复为你放在 `assets` 目录下的默认值，而不是被删除。
    *   **错误行为**：如果控制台显示“Deleted preset element...”，说明系统没识别出这是官方预设。请检查 **步骤 1** 中的 ID 是否注册正确，以及 `ConfigManager.isOfficialPreset("00006")` 是否返回 `true`。

---

## 常见问题

### Q: 为什么重置后文件被删除了？
A: 这通常是因为 `ConfigManager.isOfficialPreset(id)` 返回了 `false`。请检查 `ElementConfigManager.java` 中的 fallback 列表是否包含了你的 ID。系统逻辑是：如果是官方预设 -> 覆盖为默认值；如果是自定义预设 -> 删除文件。

### Q: 我添加了 ID，但加载时提示找不到文件？
A: 请检查 `src/main/resources/assets/...` 下的路径是否完全正确，区分大小写。确保构建工具（Gradle/Maven）正确将资源文件打包进了 JAR。
