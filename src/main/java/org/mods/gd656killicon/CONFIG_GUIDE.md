# 如何修改默认配置
若要修改或添加 HUD 元素的默认配置，您需要更新 `src/main/java/org/mods/gd656killicon/client/config/ElementConfigManager.java` 文件。

## 1. 确定默认值
找到名为 `getDefaultElementConfig(String name)` 的方法。此方法会返回一个包含给定元素 ID 的默认键值对的 `JsonObject` 对象。

### 示例：添加新元素
```java
public static JsonObject 获取默认元素配置(String 名称) {
// 已有的元素...
如果（名称等于“kill_icon/滚动”） {
// ...
}}
// 在此处添加您的新元素
} else if (name.equals("my_category/my_element")) {
JsonObject config = new JsonObject()；
config.addProperty("x_offset"， 10)；
config.addProperty("visible"， true)；
config.addProperty("custom_color"， "#FFFFFF")；
return config；}
返回一个新的 JSON 对象。}
```

## 2. 使用默认预设进行注册
为确保在配置被重置或从头创建时您的新元素能够显示出来，请更新 `createDefaultConfig()` 方法。
```java
public static void 创建默认配置() {
    JsonObject root = new JsonObject()；
    JsonObject preset00001 = new JsonObject()；
}
// 添加现有元素
preset00001 添加“kill_icon/滚动”元素，并使用默认元素配置文件“kill_icon/滚动”；
preset00001 添加“subtitle/击杀动态信息”元素，并使用默认元素配置文件“subtitle/击杀动态信息”；
// 添加新元素
preset00001.add("my_category/my_element"， 获取默认元素配置("my_category/my_element")）
root.add("00001", preset00001); 
// ...}
```

## 3. 注册渲染器
别忘了在 `ClientSetup.java` 文件中注册实际的渲染器实现：
“HUD 元素管理器”注册了一个名为“my_category”的类别以及名为“my_element”的元素，并为其设置了“MyElementRenderer”渲染器。```

## 4. 添加命令建议（可选）
请将 `ClientCommand.java` 文件更新为：如果希望该元素能够自动补全，请在 `addElement` 建议中包含您新的元素 ID 。