# GD656Killicon 语言键名规范（统一ID规则）

## 目标
- 统一键名层级结构，避免“杂乱、无层级、难读”
- 保证可读性、可检索性与长期扩展性
- 杜绝重复键与语义冲突

## 总体规则
- 全部键名以固定前缀开头：`gd656killicon`
- 使用“点分层级”结构：`gd656killicon.<scope>.<module>.<feature>.<item>`
- 仅使用小写字母、数字和下划线，避免驼峰与混合大小写
- 不在键名中出现具体文本或数值（这些应属于 value）
- 避免同一语义在多个键中重复（明确唯一归属）

## 层级定义
1. **scope（作用域）**  
   - `client`：客户端界面、提示、显示文本  
   - `server`：服务端消息、提示、指令反馈  
   - `common`：两端共享（必要时）  

2. **module（模块）**  
   按功能域划分，例如：  
   - `gui`：界面  
   - `format`：显示格式  
   - `prompt`：提示/弹窗  
   - `command`：命令输出  
   - `config`：配置  
   - `subtitle`：字幕  
   - `scoreboard`：计分板  
   - `sound`：音效  
   - `element`：元素配置  
   - `text`：通用短文本  

3. **feature（功能/页面/子系统）**  
   - 具体页面、子功能或业务域  
   - 例如：`preset`、`kill_icon`、`bonus_list`、`score_subtitle`、`export`、`import`

4. **item（具体项）**  
   - `title` / `label` / `button` / `hint` / `error` / `success` / `confirm` / `format` / `name`

## 示例模板
```
gd656killicon.client.gui.preset.title
gd656killicon.client.gui.preset.button.export
gd656killicon.client.prompt.sound.replace_success
gd656killicon.server.command.scoreboard.set_success
gd656killicon.client.format.kill.headshot
gd656killicon.client.text.bare_hand
```

## 现有键名迁移建议（示例）
> 仅作为方向示意，不要求一次性改完。

- `gd656killicon.client.gui.config.sound.title`  
  -> `gd656killicon.client.gui.sound.title`

- `gd656killicon.client.gui.prompt.sound_replace_success`  
  -> `gd656killicon.client.prompt.sound.replace_success`

- `gd656killicon.client.format.bonus_kill_headshot`  
  -> `gd656killicon.client.format.bonus.kill_headshot`

- `gd656killicon.client.command.preset_reset_success`  
  -> `gd656killicon.client.command.preset.reset_success`

## 防重复与一致性规则
- 同一语义只允许存在一个键  
- 相同页面内的按钮/标题必须使用一致的模块与 feature 路径  
- 同类消息统一落在 `prompt` / `command` / `format` 中，避免交叉

## 命名清单建议（模块化归档）
- `client.gui.*`：界面标题、按钮、提示文本  
- `client.prompt.*`：弹窗/提示  
- `client.format.*`：击杀/分数/加分项等格式化文本  
- `client.text.*`：基础短文本（如“拳头”“未知”）  
- `client.config.*`：配置项名称  
- `server.command.*`：服务端命令输出  

## 新增键名流程
1. 确定 scope（client/server/common）
2. 选择 module（gui/format/prompt/command/config/text）
3. 选择 feature（页面/系统/功能）
4. 统一 item（title/button/hint/error/success/format/name）
5. 检查现有键是否已覆盖语义，避免重复

## 版本与维护
- 文档用于统一规范与未来迁移
- 后续新增键必须遵循本规范
