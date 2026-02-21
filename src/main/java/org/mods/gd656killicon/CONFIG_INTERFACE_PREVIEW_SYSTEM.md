# 配置界面元素预览系统实现文档

本文档描述配置界面中的元素预览系统结构、渲染流程与扩展方式，覆盖元素列表预览与独立元素配置页预览两条路径。

---

## 架构总览

预览系统由三层协作完成：

1. **配置界面入口与布局层**  
   - 入口：`PresetConfigTab` 负责全局预览绘制、元素列表与拖动交互。  
   - 独立配置页：`ElementConfigContent` 负责单个元素的网格预览与局部触发。

2. **预览模型层**  
   - `ElementPreview` 负责读取配置、计算尺寸、确定位置与响应拖动交互。

3. **渲染器层**  
   - `IHudRenderer` 定义触发与渲染规范。  
   - 各元素渲染器实现 `render` 与 `trigger`，并在配置界面中通过 `renderAt` 实现定点预览。

---

## 主要入口与职责

### 1) 全局预览入口

- **文件**：`client/gui/tabs/PresetConfigTab.java`  
- **职责**：
  - 管理当前预设的所有 `ElementPreview` 实例。
  - 在 `render` 中排序、更新并绘制所有元素预览。
  - 处理拖动、悬停、Z 轴排序与点击命中。

### 2) 独立配置页预览入口

- **文件**：`client/gui/tabs/ElementConfigContent.java`  
- **职责**：
  - 计算网格区域并创建 `InfiniteGridWidget`。
  - 在网格内触发与绘制预览（如 scrolling、combo）。
  - 通过裁剪确保预览仅显示在网格范围内。

### 3) 网格视图

- **文件**：`client/gui/elements/InfiniteGridWidget.java`  
- **职责**：
  - 提供滚动网格背景与原点坐标。
  - 负责网格区域裁剪与可视区域计算。

---

## 预览渲染流程

### A. 元素列表预览（PresetConfigTab）

1. `PresetConfigTab.updatePreviews()` 从 `ElementConfigManager` 读取当前预设配置。  
2. `ElementPreview.updateConfig()` 解析可见性、缩放、偏移等信息，并据此计算尺寸。  
3. `PresetConfigTab.render()` 中更新位置、排序并调用 `ElementPreview.render()` 绘制。  
4. 鼠标拖动时，`ElementPreview.mouseClicked()` 与拖动逻辑协作更新配置坐标。

### B. 独立配置页预览（ElementConfigContent）

1. `updateLayout()` 计算网格区域并维护 `InfiniteGridWidget`。  
2. `render()` 中先绘制网格背景，再执行预览渲染逻辑。  
3. 预览逻辑通过 `renderAt` 进行定点渲染，并使用裁剪保证不会超出网格区域。

---

## 预览触发与渲染约定

### IHudRenderer 触发规范

- **接口**：`client/render/IHudRenderer.java`  
- **触发调用**：

```java
IHudRenderer.TriggerContext context = IHudRenderer.TriggerContext.of(killType, entityId, comboCount);
renderer.trigger(context);
```

### 预览渲染约定

配置界面内的预览需要渲染到自定义坐标，因此渲染器应提供 `renderAt` 方法：

```java
public void renderAt(GuiGraphics guiGraphics, float partialTick, float centerX, float centerY) {
    // 使用 centerX/centerY 作为预览中心点绘制
}
```

配置页通过 `renderAt` 复用相同渲染逻辑，并统一控制裁剪与混合模式。

---

## Combo 预览轮播逻辑

`ElementConfigContent` 内置 combo 预览触发：

- 触发间隔由 `PREVIEW_COMBO_TRIGGER_INTERVAL_MS` 控制。  
- `comboCount` 以 1～5 循环。  
- `killType` 仅使用 NORMAL、HEADSHOT、EXPLOSION、CRIT，排除 ASSIST 与 DESTROY_VEHICLE。  
- 渲染使用 `ComboIconRenderer.renderAt` 保持与实际渲染一致。

---

## 新增一个元素预览的步骤

1. **确认元素渲染器支持定点预览**  
   - 若没有 `renderAt`，为对应渲染器添加 `renderAt` 方法。  

2. **在配置页内添加预览触发**  
   - 在 `ElementConfigContent` 添加新的 `renderXxxPreview` 方法。  
   - 在 `render()` 中按需调用该方法。  

3. **复用网格裁剪**  
   - 从 `gridWidget` 读取 `x/y/width/height` 作为裁剪区域。  
   - 渲染前 `enableScissor`，结束后 `disableScissor`。  

4. **触发上下文构造**  
   - 使用 `IHudRenderer.TriggerContext` 填充预览所需的 type、entityId、comboCount 或 extraData。  

---

## 调试与验证要点

1. 在配置页内观察预览是否跟随网格原点。  
2. 将预览拖到网格外，确认裁剪生效。  
3. 对具有多状态的元素（如 combo），检查轮播逻辑与渲染表现一致。  
4. 如果渲染器依赖配置，确保 `ElementConfigManager` 中已存在可用配置。

