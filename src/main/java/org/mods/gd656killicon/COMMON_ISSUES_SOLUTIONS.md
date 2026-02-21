# 常见问题与解决方案 (Common Issues and Solutions)

本文档汇总了 `GD656Killicon` 开发和使用过程中可能遇到的常见问题及其技术解决方案。

## 1. 官方预设被重置命令误删

**现象**：
执行 `/gd656killicon client config reset element <preset_id> <element>` 后，控制台提示 "Deleted preset element..."，且配置文件被物理删除，而不是恢复默认值。这通常发生在 `00004` 等预设上。

**原因**：
系统未能正确识别该 ID 为“官方预设”。`ConfigManager` 在重置时会检查 ID：
*   **是官方预设** -> 从 JAR 包 `assets` 中读取默认文件并覆盖。
*   **是自定义预设** -> 直接删除文件（认为用户想清空配置）。

如果 ID 未在 `ElementConfigManager.OFFICIAL_PRESET_IDS` 列表中注册，系统会将其视为自定义预设。

**解决方案**：
1.  检查 `ElementConfigManager.java` 中的 `isOfficialPresetId` 方法。
2.  确保 `OFFICIAL_PRESET_IDS` 包含该 ID。
3.  **关键修复**：在静态代码块或初始化逻辑中，显式添加硬编码 ID 作为 fallback，防止资源加载失败导致的误判。

```java
// ElementConfigManager.java
// Fallback: Hardcode known official presets
Collections.addAll(OFFICIAL_PRESET_IDS, "00001", "00002", "00003", "00004", "00005");
```

---

## 2. 击杀音效不触发

**现象**：
击杀图标显示正常，但没有任何音效播放。

**原因**：
1.  `ClientConfigManager` 中的全局音效开关默认值可能被设为了 `false`。
2.  `SoundTriggerManager` 逻辑被旁路或未被调用。
3.  资源包中缺少声音文件或 `sounds.json` 映射错误。

**解决方案**：
1.  检查 `ClientConfigManager.java`，确保 `DEFAULT_ENABLE_SOUND = true`。
2.  检查 `ClientEventHandler` 或 `KillIconRenderer` 中是否调用了 `SoundTriggerManager.playSound(...)`。
3.  使用 `/playsound` 命令测试声音 ID 是否存在，例如 `/playsound gd656killicon:killsound_bf1 master @p`。

---

## 3. 服务端爆头检测不准确 (TACZ 联动)

**现象**：
当玩家对生物造成的伤害中有一半来自爆头，即使最后一击不是爆头，KillType 依然显示为爆头。

**原因**：
`TaczIntegration` 类中的临时状态集合（如 `headshotVictims`）没有在 Server Tick 结束时及时清理。这导致上一 Tick 或之前的爆头记录“泄漏”到了当前的击杀判断中。

**解决方案**：
1.  在 `TaczIntegration` 中实现 `tick()` 方法，清空所有临时 Set。
2.  在 `ServerEventHandler.onTick` (Phase.END) 中调用 `ServerCore.TACZ.tick()`。

```java
// TaczIntegration.java
public void tick() {
    headshotVictims.clear();
    headshotDamageVictims.clear(); // 必须清理，否则状态会残留
    // ...
}
```

---

## 4. 配置文件修改后不生效

**现象**：
修改了 `.json` 文件，但游戏内 UI 没变化。

**原因**：
模组通常只在启动或特定事件时加载配置。手动修改文件后，内存中的配置对象未更新。

**解决方案**：
1.  使用重载命令：`/gd656killicon client config reload`。
2.  确保修改的是**当前激活预设**（Current Preset）下的文件。检查 `global.json` 确认当前使用的 `preset_id`。

---

## 5. UI 元素位置错乱或重叠

**现象**：
不同分辨率下 UI 元素位置偏移。

**解决方案**：
1.  检查渲染代码中是否正确使用了 `Screen.width` / `Screen.height` 进行相对定位。
2.  检查 `ElementConfigManager` 中的 `x_offset` / `y_offset` 是否被错误地作为绝对坐标处理，而没有结合锚点（Anchor）。
3.  建议使用 `PoseStack` 进行缩放和平移，保持坐标系一致。
