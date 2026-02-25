# GD656Killicon 加分项开发指南

本文档旨在指导开发人员如何在 GD656Killicon 模组中添加一个标准的、高质量的“加分项”（Bonus）。为了保持代码的一致性与可维护性，请务必遵循以下流程与规范。

---

## 1. 核心定义 (Common)

### 1.1 注册 BonusType ID
在 `org.mods.gd656killicon.common.BonusType` 中添加唯一的整数 ID。
- **规则**：按顺序递增，不可重复。
- **重要**：记得在 `static` 代码块中调用 `register` 方法注册名称映射。
- **示例**：
  ```java
  public static final int NEW_BONUS_ITEM = 12;
  // static block:
  register("NEW_BONUS_ITEM", NEW_BONUS_ITEM);
  ```

### 1.2 (可选) 注册 KillType ID
如果该加分项对应一种新的击杀方式（如：助攻、爆头、摧毁载具），需在 `org.mods.gd656killicon.common.KillType` 中注册。
- **示例**：
  ```java
  public static final int NEW_KILL_TYPE = 5;
  ```

---

## 2. 服务端逻辑 (Server)

### 2.1 定义默认加分表达式 (重要)
在 `org.mods.gd656killicon.server.ServerData.initDefaults` 中注册该加分项的默认分值表达式。
- **如果不注册，加分项将默认得分为0**。
- **配置落盘**：`world/gd656killicon/server_config.json`，字段 `disabled_bonuses` 和 `bonus_expressions` 会覆盖默认值。
- **示例**：
  ```java
  // 固定分数
  addDefault(BonusType.NEW_BONUS_ITEM, "100");
  // 动态分数 (如基于伤害)
  addDefault(BonusType.HIT_VEHICLE_ARMOR, "1");
  ```

### 2.2 触发加分奖励
在 `org.mods.gd656killicon.server.event.ServerEventHandler` 或特定的事件处理器（如 `YwzjVehicleEventHandler`）中编写触发逻辑。

- **核心方法**：使用 `ServerCore.BONUS.add(player, bonusType, score, extraData)`。
- **方法分离规范 (重要)**：
    - 通用击杀逻辑应分类放入 `ServerEventHandler` 的 `award...` 方法中。
    - 模组联动逻辑应放入独立的 Handler 类中（如 `YwzjVehicleEventHandler`）。
- **状态捕捉 (重要)**：
    - 对于跨Tick的判断（如“无伤击杀”），使用 `activeCombats` 等状态追踪机制。
    - 对于瞬间事件，直接在事件监听器中处理。

### 2.3 触发击杀播报 (Kill Feed/Icon)
如果该加分项应被视为一次击杀（如摧毁载具），需要手动触发击杀播报。
- **调用方法**：`ServerCore.BONUS.sendKillEffects(player, killType, comboCount, targetName)`。
- **注意**：普通的击杀（玩家/生物）通常由 `processKill` 自动处理。对于非实体击杀（如摧毁载具），需要手动构造参数调用。

---

## 3. 客户端配置 (Client Config)

### 3.1 定义默认配置
在 `org.mods.gd656killicon.client.config.DefaultConfigRegistry` 中为 `subtitle/bonus_list` 添加该加分项的格式项。
`ElementConfigManager` 在创建预设时会读取此默认值。
- **键名规范**：
    - 字幕格式：`format_<bonus_name_lower>`
- **注意事项**：加分项不应拥有独立的占位符颜色配置，除非有特殊需求（如摧毁载具的特殊颜色）。
- **示例**：
  ```java
  bonusList.addProperty("format_new_type", "gd656killicon.client.format.bonus_new_type");
  ```

---

## 4. 客户端处理与渲染 (Client Handling & Rendering)

### 4.1 网络包处理流程
`BonusScorePacket` 会统一触发 HUD 加分列表与分数字幕，不需要额外注册处理器。
新增加分项只需保证 `BonusListRenderer` 的格式映射与本地化键可用。

### 4.2 HUD 渲染适配 (BonusListRenderer)
在 `org.mods.gd656killicon.client.render.impl.BonusListRenderer` 中完成配置映射与占位符处理：
1. **注册映射**：在 `static` 代码块中调用 `registerConfig`。
2. **处理新占位符**：如果引入了新的动态数字占位符，需在 `BonusItem` 内部类中使用 `AnimatedStat` 字段并对接 `DigitalScrollEffect` 动画。

### 4.3 击杀字幕适配 (SubtitleRenderer)
如果加分项触发了 `SubtitleRenderer`（通常通过 `KillType`），需要在 `SubtitleRenderer.onKillIconPacket` 中处理：
- **格式化**：定义字幕格式（如“你 摧毁了 <target>”）。
- **特殊逻辑**：如延迟显示、特殊颜色配置。

### 4.4 战地1风格图标适配 (Battlefield1Renderer)
如果支持 `Battlefield1Renderer`：
- **贴图**：添加 `killicon_battlefield1_<type>.png`。
- **逻辑**：在 `onKillIconPacket` 中根据 `KillType` 选择贴图和文本逻辑。
- **特殊需求**：如隐藏受害者名字、使用固定文本等。

### 4.5 滚动图标适配 (ScrollingIconRenderer)
- **图标**：在 `ScrollingIconRenderer.getTexturePath` 中添加映射（`killicon_scrolling_<type>.png`）。
- **音效**：在 `SoundTriggerManager.onKillIconPacket` 中添加逻辑。

### 4.6 帮助页与更新日志 (HelpTab & Update Log)
1. 在 `HelpTab` 中为对应加分项标题添加灰色 `[bonustype] ` 前缀，前缀与标题之间保留一个空格。
2. 在 `HelpTab` 的 `1.1.0.006Alpha` 更新日志中补充该加分项与相关修复/配置变更内容。

---

## 5. 语言文件 (Localization)

在 `src/main/resources/assets/gd656killicon/lang/` 下的 `zh_cn.json`、`zh_tw.json` 和 `en_us.json` 中添加对应的键值对。
- **HUD 格式**：使用 `<score>` 占位符。
- **字幕格式**：支持 `%s` (武器/目标) 等标准格式化符。
- **名称与说明**：用于帮助页和配置展示。

---

## 6. 代码规则与最佳实践 (Best Practices)

1.  **逻辑分离**：严禁在 `processKill` 中直接编写长段 `if-else`。必须遵循方法分类原则，将逻辑抽离。
2.  **代码复用**：尽量复用现有的工具类（如 `ServerCore.BONUS`, `DigitalScrollEffect`）。
3.  **配置驱动**：所有文本、颜色、格式都应尽可能通过配置文件或语言文件管理，避免硬编码。
