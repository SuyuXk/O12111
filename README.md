# O123456789 — Minecraft 1.21.11 / Fabric 移植版

把物品堆叠数量改成 Create 模组风格的 **3x5 像素字体**（带 1px 描边、超长数字自动缩放）。
本仓库是 [catboybinary/O123456789](https://github.com/catboybinary/O123456789)（`1.21` 分支，面向 1.21.1 的 Architectury 多加载器工程）的 **Fabric 单加载器移植版**。

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.21.11（2025-12-09 正式版，resource pack format 75） |
| Fabric Loader | 0.17.2（开发用；`fabric.mod.json` 声明 `>=0.16.10`） |
| Fabric API | 0.141.6+1.21.11（编译期依赖，运行期**非必需**，见下） |
| Yarn / Mappings | Mojang 官方映射（`loom.officialMojangMappings()`，与上游一致） |
| Java | 21 |
| Loom | 1.13.4 |

产物：`build/libs/O123456789-2.2+1.21.11.jar`

## 构建

```bash
./gradlew build        # 输出 build/libs/*.jar
./gradlew mixinAudit   # 不开游戏窗口，验证 mixin 是否真的注入成功
./gradlew runClient    # 启动 1.21.11 开发客户端
```

首次构建需要联网下载 Minecraft 1.21.11、映射表与依赖（约 300 MB）。

## 安装

1. 安装 Fabric Loader（≥0.16.10）+ Minecraft 1.21.11；
2. 把 `O123456789-2.2+1.21.11.jar` 放进 `.minecraft/mods/`；
3. Fabric API 0.141.6 可装可不装 —— 本模组没有调用任何 Fabric API 代码，只是按目标环境用它做编译校验。

## 移植改动（1.21.1 → 1.21.11）

上游由 `common` + `fabric` + `neoforge` 三个子工程组成，本移植只保留 Fabric 单模块，源码放在 `src/main`。

| 位置 | 1.21.1 上游 | 1.21.11 移植 | 原因 |
| --- | --- | --- | --- |
| 工程 | Architectury 多加载器 + Shadow | 单模块 fabric-loom 工程 | 只交付 Fabric，去掉 architectury/shadow 依赖链 |
| `O123456789.FONT` | `ResourceLocation.fromNamespaceAndPath(...)` | `new FontDescription.Resource(Identifier.fromNamespaceAndPath(...))` | 1.21.9 起 `ResourceLocation` 改名 `Identifier`；`Style#withFont` 参数改成 `FontDescription` |
| `Style.EMPTY.withFont(id)` | 传 `ResourceLocation` | 传 `FontDescription.Resource` | 同上 |
| 注入点 | `GuiGraphics#renderItemDecorations` 内的 `drawString` | `GuiGraphics#renderItemCount` 内的 `drawString` | 1.21.11 把数量绘制拆到私有方法 `renderItemCount` |
| `drawString` 返回值 | `int` | `void` | 1.21.11 的 GUI 文字改为提交渲染状态，不再返回宽度（**这点如果不改，mixin 会在启动时直接注入失败**） |
| 描边（原 `FontMixin`） | 在 `Font#drawInternal` 取消原绘制并调用 `Font#drawInBatch8xOutline` | 删除 `FontMixin`，改为在同一位置用 9 次 `drawString` 提交：8 个 ±1px 偏移的暗色副本 + 1 个正常色副本 | 1.21.11 的 `Font` 已没有 `drawInternal`，文字走 `prepareText → PreparedText#visit(GlyphVisitor)`（GUI 侧由 `GuiRenderer#prepareText` 在帧末统一提交）；`drawInBatch8xOutline` 内部也只是画 9 遍偏移字形，所以改在提交侧等价实现 |
| 坐标栈 | `PoseStack` + `pushPose/translate/scale/popPose`，`Matrix4f` | `Matrix3x2fStack` + `pushMatrix/translate/scale/popMatrix` | 1.21.6 起 `GuiGraphics#pose()` 返回 2D 矩阵栈 |
| `RenderSystem.disableBlend()/enableBlend()` | 有 | 移除 | 1.21.5 起混合状态由渲染管线（`RenderPipelines`）管理，不再需要手动开关 |
| `pack.mcmeta` | `pack_format: 15` | `pack_format: 75` | 1.21.11 资源包格式号（`version.json` 的 `resource_major`） |
| `fabric.mod.json` | `minecraft: ">=1.21.1 <1.21.2"`、`fabric-api: "*"` | `minecraft: "1.21.11"`、`fabric-api` 改为 `suggests` | 精确到目标版本；本模组不需要 Fabric API 运行期存在 |
| EMI 兼容 | `EmiRenderHelperMixin`（`EmiRenderHelper#renderAmount`） | **已移除** | Modrinth 上 EMI 还没有 1.21.11 的构建（查询 `game_versions=["1.21.11"]` 返回空），无对应 API 可编译；等 EMI 更新后可参照上游重新加回 |

字体资源（`3x5.ttf`、`3x5_tiny.ttf`、两个 `font/*.json`）与图标、许可证均原样保留；1.21.11 的字体 provider 格式（`ttf`/`space`）与上游写法兼容，无需修改。

## 绘制逻辑说明

`O123456789#drawItemCount`（由 mixin 调用）：

1. 用 3x5 字体构造 `Component`，取它的宽度；
2. 从原版传入的坐标反推格子原点（原版是 `x = itemX + 19 - 2 - font.width(text)`、`y = itemY + 9`）；
3. `scale = min(1, 15 / 文本宽度)`，右对齐到 `itemX + 16`，垂直保持原版基线；
4. 先在按下沉（暗色，RGB × 0.22、保留 alpha）画 8 个 ±1px 偏移副本，再画正常色本体 —— 顺序保证本体在最上层。

## 验证情况

- `./gradlew build` 通过（Java 21，Loom 1.13.4，无警告级错误）。
- 产物内 mixin 注解已被 Loom 重映射为 intermediary：`class_332`（GuiGraphics）、`method_64859`（renderItemCount）、`method_51433`（drawString，返回 `void`）。
- `./gradlew mixinAudit` 通过 Fabric Knot 类加载器真实加载 `net.minecraft.client.gui.GuiGraphics`，输出：

```
[audit] target method = renderItemCount[class net.minecraft.client.gui.Font, class net.minecraft.world.item.ItemStack, int, int, class java.lang.String]
[audit] merged redirect handler = redirect$zza000$o123456789$drawItemCount
[audit] outline colour for 0xFFFFFFFF = 0xFF383838
[audit] OK
```

  即：mixin 配置被加载、重定向处理器已合并进目标类，说明注入在 1.21.11 运行时是有效的（该任务不开窗口、不下载资源）。

## 目录

```
build.gradle                 构建脚本（fabric-loom 1.13.4，含 mixinAudit 任务）
gradle.properties            版本号集中配置
settings.gradle
src/main/java/meow/binary/o123456789/
    O123456789.java          字体常量 + 数量绘制/描边逻辑
    mixin/GuiGraphicsMixin.java   @Redirect renderItemCount 里的 drawString
    fabric/O123456789Fabric.java          入口
    fabric/client/O123456789FabricClient.java
src/main/resources/
    fabric.mod.json  o123456789.mixins.json  pack.mcmeta
    assets/o123456789/font/{3x5,3x5_tiny}.{json,ttf}   icon.png
src/audit/java/...          仅开发期用的 mixin 注入自检（不进产物）
LICENSE.txt                 GPL-3.0（上游原文件）
```
