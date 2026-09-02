# PetiteInventory

PetiteInventory 是一个适用于 Minecraft Forge 的模块化网格物品栏模组。物品可以占用可配置的矩形区域，放置、替换、堆叠、旋转和快速转移都会遵循同一套占位规则。

## 功能

- 在 `config/PetiteInventory/border_items.json` 中为物品配置尺寸和标签规则。
- 拿起物品后按 `R` 或滚轮即可翻转长宽，不改变物品身份或堆叠数据；长宽相等的物品不会响应。
- 编辑模式支持从边缘和角落调整物品尺寸、使用 `Ctrl` 多选并应用预制边框颜色。
- 每个容器界面都可以单独启用或关闭 Petite 布局；玩家物品栏和快捷栏仍由独立配置控制。
- 兼容普通容器以及精妙核心、精妙背包的容器界面。
- 不同尺寸的物品支持替换，并在快速转移时按占位区域查找可用位置。
- 快捷栏默认不使用 Petite 布局，以保留原版操作方式。

规则可以使用 `/reload` 重新加载。要配置其他容器时，将鼠标放在该容器的槽位上，按复制容器标识键（默认为 `U`），再把得到的 Screen 标识加入配置。

## 边框颜色

编辑模式下右键物品可以打开预制颜色调色盘。选择结果会保存到 `config/PetiteInventory/border_colors.json`。配置文件支持物品 ID、物品标签以及 TACZ 枪械的 NBT 匹配：

```json
[
  {
    "match": ["minecraft:diamond"],
    "theme": "orange"
  },
  {
    "match": ["#minecraft:tools"],
    "theme": "blue"
  },
  {
    "match": ["tacz:modern_kinetic_gun{GunId:\"your_gun_id\"}"],
    "theme": "red"
  }
]
```

内置主题包括 `default`、`blue`、`purple`、`orange` 和 `red`。调色盘目前保存的是物品 ID 规则；如果要只给某个 TACZ `GunId` 设置颜色，需要手动编辑配置文件。当前不支持任意 RGB 颜色和调色指令。

## 编辑模式

编辑模式下，普通左键仍然用于拿起和放置物品。按住 `Ctrl` 可以进入多选模式；按住 `Ctrl` 拖动可以框选区域，`Ctrl+Shift` 可以扩展选择。右键会打开预制颜色调色盘，右键按住 0.5 秒则进入批量设置占位区域模式。

## 兼容性

### 已适配

- **原版和标准 Forge 容器界面：** Petite 使用通用容器界面和容器菜单钩子，普通箱子等容器可以按 Screen 单独启用。玩家主物品栏仍由配置控制，快捷栏默认不纳入 Petite 网格。
- **精妙核心和精妙背包：** 提供专用适配，能够识别背包存储槽位，渲染物品实际占位和完整区域的悬停高亮，并保留背包存储与玩家主物品栏之间的占位感知快速转移。背包内的快捷栏不会纳入 Petite 网格。
- **KubeJS：** 项目包含可选的脚本侧面积事件桥接入口；正常使用 PetiteInventory 不需要安装 KubeJS。
- **Item Borders：** 可以与 PetiteInventory 同时使用，两套物品边框和占位背景可以共存。
- **JEI：** PetiteInventory 不会替换 JEI 的配方或搜索逻辑，尺寸规则只作用于支持的物品栏界面。
- **排序、扩展槽位及其他改动物品栏的模组：** 对槽位顺序、坐标或点击逻辑进行改写的模组可能需要单独关闭或配置对应 Screen。

Screen 默认使用白名单模式，对所有容器关闭布局；开关使用具体的 `Screen` 类作为标识，而不只依据容器菜单类型。因此，即使两个界面使用同一种 Menu，也可以分别配置。编辑模式下，容器界面顶部的勾选框控制当前 Screen 是否启用 Petite 布局；`/petiteinventory blacklist` 和 `/petiteinventory whitelist` 可以切换默认启用逻辑，无需列出所有 Screen。

## 注意事项

自动整理、扩展槽位和其他会改动物品栏结构的模组不保证完全兼容。手动整理是本模组设计的一部分；快捷栏默认排除在外，创造模式物品栏也保持原版布局。PetiteInventory 的布局可以和 Item Borders 一起使用。

## 后续计划

- 移植到更多 Minecraft 版本和加载器。
- 为第三方容器界面增加更多兼容适配器。
- 支持更多物品栏扩展机制。
- 探索非矩形的俄罗斯方块式物品形状。

英文功能说明和使用说明见 [EN.md](EN.md)。
