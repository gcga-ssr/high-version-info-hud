# High Version Info HUD

把准星指到方块上，屏幕顶部立刻弹出它的信息面板 —— 不用打开背包，也不用按任何快捷键。

这个模组最初是为了解决 **NeoForge 上准星信息显示缺失**的问题而写的：高版本 NeoForge 一直
没有一个能在世界里直接"看着方块看内容"的预览工具。现在它还额外解决了老版本信息类模组在
NeoForge 上不工作的问题。

> 模组 ID 一直沿用 `blockinfo`（创建时的名字），仓库和发布页改名为 High Version Info HUD
> 后 ID 未变，以免已有的存档与整合包失效。看到源码里的 `blockinfo` 不用困惑。

---

## 为什么用它

现存的容器预览模组大多只支持**背包里的容器物品**，而且必须**按住快捷键**才能弹出预览。

这个 HUD 的做法不一样：只要把准星对准世界里的方块，完整的方块信息与容器内容就直接显示在
屏幕上方 —— 不需要打开任何界面，不需要额外按键。它补上了高版本 NeoForge 上
「准星预览」这个长期空缺。

对比：

| | 常见容器预览模组 | High Version Info HUD |
| --- | --- | --- |
| 触发方式 | 按住快捷键 | 准星对准即可 |
| 作用对象 | 背包里的容器物品 | 世界里的方块实体 |
| 是否需要开界面 | 是 | 否 |
| 显示内容 | 物品列表 | 方块名、所属模组、可否挖掘、作物生长、容器内容、熔炉状态、挖掘进度 |

## 功能

* **方块信息** —— 本地化名称、所属模组 ID、是否原版方块
* **能否挖掘** —— ✓ / ✗ 标记，取决于手上的工具
* **作物生长** —— 识别 `age` / `stage` / `growth` / `level` / `height` 等整数属性
* **容器内容** —— 箱子、木桶、潜影盒等按物品聚合并分列显示
* **熔炉状态** —— 输入 / 燃料 / 输出，以及烧制与燃烧两条实时进度
* **挖掘进度** —— 实时显示当前方块的破坏百分比
* **多语言** —— 简体中文 / English 内置，可自行扩展

## 支持的版本

Mojang 从 2026 年起把版本号改成 `年.版本`（`26.1` / `26.2` / `26.3`…），所以「1.21.1 到 26.3」
一共是 **16 个 MC 发行版**，每个发行版一个 jar：

| Minecraft | NeoForge 编译基线 | Java | 产物 |
| --- | --- | --- | --- |
| 1.21.1 | 21.1.1 | 21 | `blockinfo-1.1.0-1.21.1.jar` |
| 1.21.2 | 21.2.0-beta | 21 | `blockinfo-1.1.0-1.21.2.jar` |
| 1.21.3 | 21.3.56 | 21 | `blockinfo-1.1.0-1.21.3.jar` |
| 1.21.4 | 21.4.121 | 21 | `blockinfo-1.1.0-1.21.4.jar` |
| 1.21.5 | 21.5.74 | 21 | `blockinfo-1.1.0-1.21.5.jar` |
| 1.21.6 | 21.6.0-beta | 21 | `blockinfo-1.1.0-1.21.6.jar` |
| 1.21.7 | 21.7.0-beta | 21 | `blockinfo-1.1.0-1.21.7.jar` |
| 1.21.8 | 21.8.9 | 21 | `blockinfo-1.1.0-1.21.8.jar` |
| 1.21.9 | 21.9.0-beta | 21 | `blockinfo-1.1.0-1.21.9.jar` |
| 1.21.10 | 21.10.63 | 21 | `blockinfo-1.1.0-1.21.10.jar` |
| 1.21.11 | 21.11.42 | 21 | `blockinfo-1.1.0-1.21.11.jar` |
| 26.1 | 26.1.0.0-beta | 25 | `blockinfo-1.1.0-26.1.jar` |
| 26.1.1 | 26.1.1.0-beta | 25 | `blockinfo-1.1.0-26.1.1.jar` |
| 26.1.2 | 26.1.2.71 | 25 | `blockinfo-1.1.0-26.1.2.jar` |
| 26.2 | 26.2.0.57 | 25 | `blockinfo-1.1.0-26.2.jar` |
| 26.3 | 26.3.0.76 | 25 | `blockinfo-1.1.0-26.3.jar` |

**为什么不按 NeoForge 补丁号拆？** 编译基线刻意取每条版本线的**最低可用**发行版：
只针对最低版本编译，产物就只可能引用该版本已有的 API，因此整条线（比如全部 `21.1.x`：
`21.1.1` ~ `21.1.264`）都能加载。反过来针对最新版编译，一旦用到较晚加入的 API，
在旧构建上就会 `NoSuchMethodError`。

## 多语言

HUD 文案走原版语言文件：

| 语言 | 文件 |
| --- | --- |
| 简体中文 | `assets/blockinfo/lang/zh_cn.json` |
| English | `assets/blockinfo/lang/en_us.json` |

翻译键：`blockinfo.hud.input` / `.fuel` / `.output` / `.contents` / `.cooking` / `.burning` / `.mining`。
新增语言只要往 `common/src/main/resources/assets/blockinfo/lang/` 放一个 `<语言代码>.json`，
不需要改代码。

## 安装

1. 安装对应 MC 版本的 **NeoForge**，版本号不低于上表的「编译基线」；
2. 把对应版本的 jar 放进 `mods/`；
3. 客户端放客户端，服务端放服务端 —— 是同一个文件，不需要两个包。

**服务端装不装都可以**（这一点和早期版本不同）：网络通道已改为 optional，
只装客户端也能进任何服务器，方块名、所属模组、可否挖掘、作物生长、挖掘进度、容器内容照常显示；
把同一个 jar 也装到服务端，才会额外同步熔炉的精确烧制/燃烧进度。

> ⚠️ 所有版本的 `modId` 都是 `blockinfo`，**同一个 `mods/` 目录里只能放一个**，
> 否则 FML 会报 `Duplicate mod`。

## 构建

需要 **JDK 21**；构建 26.x 还需要 **JDK 25**（NeoForge 26.x 要求 Java 25）。
两个 JDK 都注册在 `gradle.properties` 的 `org.gradle.java.installations.paths` 里。

```bash
./gradlew build                 # 全部 16 个版本
./gradlew :mc1_21_5:build       # 单个版本（工程名 = mc + 版本号里的 . 换成 _）
./gradlew :mc26_3:build
```

产物在 `versions/<MC版本>/build/libs/`。

想改构建哪些版本，编辑 `settings.gradle` 里的 `mcVersions` 列表，
并在 `versions/<MC版本>/gradle.properties` 里给出该版本的 `minecraft_version` /
`neo_version` / `java_version` / `source_line`。

> 本机（Windows）上还有两个网络问题需要绕开：JVM 与 `maven.neoforged.net` 的 TLS 握手会被
> 重置、NeoForm Runtime 下载 Minecraft jar 会卡死。相关脚本和用法见 [BUILD.md](BUILD.md)。

## 目录结构

```
common/src/main/java/          1.21.x 版本线的源码（NeoForge 21.x API）
common26/src/main/java/        26.x 版本线的源码（改名后的 API）
common/src/main/resources/     两条线共用资源：语言文件
common/src/main/templates/     neoforge.mods.toml 模板
versions/<MC版本>/             每个版本一行配置
build.gradle                   子工程公共构建配置
settings.gradle                版本列表
```

`common` 与 `common26` 的区别只在 26.x 的几处改名（见下），其余逻辑完全一致。
源码就是全部内容 —— 想改、想重编译都可以直接拿去用。

## 实现要点

* **侧别隔离**：`BlockInfo` 先注册服务端同步，再用
  `if (FMLEnvironment.getDist() == Dist.CLIENT) ClientBootstrap.init();` 才进入客户端部分。
  JVM 到那一行才会解析客户端类，所以专用服务器不会加载 `Minecraft` / `GuiGraphics`。
  两个 `playToClient` 包的 `handle()` 里也做了同样的判断。
* **绘制只用 `fill` / `text`**（1.21.x 里叫 `drawString`），不碰 `RenderSystem` 或 `blit` ——
  这类渲染接口在不同版本间变动频繁，是跨版本编译最大的障碍。
* **物品名解析不用 `BuiltInRegistries.ITEM.get(id)`**：它在 1.21.1 返回 `Item`，
  从 1.21.2 起返回 `Optional<Holder.Reference<Item>>`；改为遍历注册表 + `getKey`，
  两条版本线都成立（结果有缓存，每个 id 只扫一次）。
* **挖掘进度** 来自 `MultiPlayerGameMode.getDestroyStage()`（0..10 档位）；进度条是否显示由
  `isDestroying() || keyAttack.isDown()` 决定 —— 只用前者会在连挖破块的 5 tick 空隙里闪，
  只用后者则手柄输入下失效。
* **熔炉内部字段** 用反射读取（`cookingProgress`/`cookingTimer`、`litTime`/`litTimeRemaining` 等），
  对不同版本的字段名都做了容错。

### 各版本之间的 API 差异（已处理）

| 变化 | 首次出现 | 处理方式 |
| --- | --- | --- |
| `BuiltInRegistries.ITEM.get(id)` 返回 `Optional<Reference<Item>>` | 1.21.2 | 改为遍历注册表 + `getKey` |
| `ServerPlayer.serverLevel()` → `level()` | 1.21.6 | 统一写 `(ServerLevel) player.level()` |
| `ResourceLocation` → `Identifier` | 26.x | `common26` 分支 |
| `GuiGraphics` → `GuiGraphicsExtractor`，`drawString` → `text` | 26.x | `common26` 分支 |
| `FMLEnvironment.dist` 字段 → `getDist()` 方法 | 26.x | `common26` 分支 |

## 许可证

Apache License 2.0，见 [LICENSE](LICENSE)。

---

## English

**High Version Info HUD** shows a live info panel at the top of the screen for whatever block
your crosshair is on. It was originally written to fix the missing crosshair info display on
NeoForge.

Unlike most container-preview mods, which only work on container *items* in your inventory and
require holding a hotkey, this HUD instantly shows full block and container details just by
aiming your crosshair at blocks in the world — no inventory screen, no extra key presses.

It shows the block name and source mod, whether your current tool can harvest it, crop growth,
container contents, furnace smelting/burning progress, and block breaking progress.

**Versions**: one jar per Minecraft release from `1.21.1` through `26.3` (16 releases; Mojang
switched to `year.release` numbering in 2026). Each jar is compiled against the *lowest*
NeoForge release of its line, so it loads across that whole line. Build with JDK 21
(JDK 25 for the 26.x line):

```bash
./gradlew build
```

**Network channels are optional**: a client-only install joins any server and still shows
everything except furnace progress; installing the same jar on the server adds real-time
furnace sync. The internal mod id is still `blockinfo` (its original name). Licensed under the Apache License 2.0 — see [LICENSE](LICENSE).
