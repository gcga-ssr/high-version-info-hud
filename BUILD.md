# 构建说明 · Block Info（Minecraft 1.21.1 / NeoForge）

客户端与服务端已经合并为**一个模组、一个 jar**。

## 产物

| 文件 | 放到哪里 | 大小 |
| --- | --- | --- |
| [`build/libs/blockinfo-1.5.0.jar`](build/libs/blockinfo-1.5.0.jar) | 客户端 `mods/`，需要同步时也放服务端 `mods/` | 28 KB |

* **客户端**：只装这一个 jar 就够了。HUD 的方块名、所属模组、可否挖掘、作物生长、
  挖掘进度、容器内容全部可用；熔炉/容器的精确数据在本机/服务器装了服务端侧时同步。
* **服务端**：同一个 jar 放进服务端 `mods/` 即可，额外提供熔炉与容器的状态同步。
* **单人游戏**：只装这一个 jar 就完整可用 —— 集成服务器同样会加载同步逻辑。

> 版本号保持 `1.5.0`，文件名与旧的客户端 jar 相同，直接覆盖 `mods/` 里的旧文件即可，
> 不会出现两个 `modId` 相同的 jar。

---

## 本次改动

### 1. 合并成一个模组

原来 `client/` 与 `server/` 是两个独立的 Gradle 工程，各产出一个 jar，而且**两个 jar 的
`modId` 都是 `blockinfo`，不能共存**（FML 会报 `Duplicate mod`），单人游戏因此无法完整工作。

现在合并为根目录下的单一工程：

```
build.gradle / settings.gradle / gradle.properties
src/main/java/com/example/blockinfo/
    BlockInfo.java              ← 唯一的 @Mod 入口
    BlockInfoNetwork.java       ← 网络通道注册（两侧共用）
    network/FurnaceDataPacket.java
    network/ContainerDataPacket.java
    server/ServerSyncHandler.java
    client/ClientBootstrap.java
    client/BlockInfoData.java / BlockInfoTracker.java
    client/BlockInfoRenderer.java        ← 只订阅 RenderGuiEvent.Post
    client/BlockInfoHud.java             ← 排版与绘制
    client/BlockInfoUtil.java / ClientFurnaceCache.java / ClientContainerCache.java
src/main/templates/META-INF/neoforge.mods.toml
```

**侧别隔离**：`BlockInfo` 先注册服务端同步，然后用
`if (FMLEnvironment.dist == Dist.CLIENT) ClientBootstrap.init();` 才进入客户端部分。
JVM 到那一行才会解析客户端类，所以专用服务器不会去加载 `Minecraft` / `GuiGraphics`。
两个 `playToClient` 包的 `handle()` 里也做了同样的 dist 判断。

`verify_side_safety.py` 会对产物做静态核验：只有 `client` 包里的类引用
`net/minecraft/client/**`；除 `client` 包外，只有 `BlockInfo`、`FurnaceDataPacket`、
`ContainerDataPacket` 引用客户端类，且都在 dist 判断之后。

> **绘制接口**：`BlockInfoHud` 只用 `GuiGraphics.fill` / `drawString`，进度条改为纯色填充，
> 不再使用 `RenderSystem.setShaderTexture/setShaderColor` + `GuiGraphics.blit`。
> 那套贴图绘制在不同 Minecraft 版本间签名反复变化，是跨版本编译的主要障碍；
> 代价是熔炉界面的箭头贴图换成了普通进度条。

### 2. 修复挖掘时 HUD 闪烁

**根因**（`BlockInfoTracker.onClientTick`）：

```java
currentData.mineProgress = getBlockBreakingProgress(mc, pos);   // 先写进度
tickCounter++;
if (!pos.equals(lastPos) || tickCounter % UPDATE_INTERVAL == 0) {
    collectBlockInfo(...);      // 内部 data.clear() → mineProgress 被归零
}
```

`collectBlockInfo()` 第一行就是 `data.clear()`，而 `clear()` 会把 `mineProgress` 置 0。
赋值写在它前面，于是**每 2 个 tick 进度就被抹掉一次**：进度值在「真实值 / 0」之间以
10Hz 来回跳，HUD 的高度和宽度（含进度条那一行）跟着反复变化 —— 这就是看到的闪烁。

另外，进度条原先用 `mineProgress > 0` 决定是否显示，而进度是 0..10 的整数档位：

* 刚开始挖的那一档（0%）和「没在挖」无法区分 → 进度条要到 10% 才冒出来；
* 每挖破一块，`destroyProgress` 归零、`isDestroying()` 会 false 约 5 个 tick →
  连挖时进度条每块闪一下。

**修复**：

1. 把挖掘状态的更新移到 `collectBlockInfo()` **之后**，不再被 `clear()` 抹掉。
2. `BlockInfoData` 新增 `isMining` 布尔量，专门表示「正在挖掘」，进度条/占位高度改由它
   控制，而不是用 `mineProgress > 0` 这种数值判断。
3. `isMining = gameMode.isDestroying() || options.keyAttack.isDown()`：
   * 只用 `isDestroying()` 不行 —— 连挖破块时有 5 tick 空窗，创造模式瞬破时它根本不变 true；
   * 只用按键不够 —— 手柄等输入下 `isDestroying()` 才是权威信号。
   两者取或，连挖期间 `isMining` 始终为 true，HUD 高度不再跳。

已在产物字节码里确认：`onClientTick` 中 `collectBlockInfo` 先于 `updateMiningState`
调用，`updateMiningState` 使用 `isDestroying()` / `KeyMapping.isDown()` / `getDestroyStage()`。

---

## 「1.21.1.xxx 小版本通用」的保证

1. **编译基线取整条线的最低版本。** `neo_version=21.1.1` —— NeoForge 1.21.1（21.1.x 线）
   的首次发布就是 `21.1.1`，不存在 `21.1.0`。针对最低版本编译，产物只可能引用 21.1.1
   已有的 API，因此 **21.1.1 → 21.1.252+** 都能加载。反之只针对最新版编译，一旦用到较晚
   加入的 API，在旧构建上就会 `NoSuchMethodError`。

2. **版本区间放宽到整条线**（`gradle.properties` → 渲染进 `META-INF/neoforge.mods.toml`）：

   | 依赖 | 区间 | 含义 |
   | --- | --- | --- |
   | `minecraft` | `[1.21.1,1.21.2)` | 接受 `1.21.1` 及任何 `1.21.1.x` 补丁串，但不放进 1.21.2+ |
   | `neoforge` | `[21.1.1,)` | 接受整条 21.1.x，包含将来的构建 |
   | 两者的 `side` | `BOTH` | 合并后两侧都要 |

3. **交叉核验。** `verify_api_coverage.py` 扫描产物全部 class 的常量池，取出 17 个
   `net/neoforged/**` 引用，逐一确认在 **21.1.1** 与 **21.1.249** 两端的 jar 中都存在 ——
   0 缺失。

---

## 重新构建

环境：

* JDK 21 —— `C:\Users\Administrator\.jdks\temurin-21`（Temurin 21.0.7）
* Gradle 8.9 —— 项目根目录已有 `gradlew` / `gradlew.bat`（发行版本机已缓存，无需下载）
* Python —— `D:\dsh\resources\runtime\primary-runtime\dependencies\python\python.exe`

### ⚠️ 本机网络：JVM 连不上 maven.neoforged.net

这台机器上 **JVM 的 TLS 栈无法与 `maven.neoforged.net` 完成握手**：服务端（CDN77）在握手
过程中直接重置连接，报 `SSLHandshakeException: Remote host terminated the handshake` /
`Connection reset`。同一地址用 PowerShell(.NET) 和 Python(OpenSSL) 访问都正常，所以是 JVM
侧的问题，不是断网，也不是代理设置问题。

后果：Gradle 和 NeoForm Runtime 都会在拉取 NeoForge 依赖时失败。解决办法是
`.buildtools/nf_proxy.py`：本地缓存代理，用 Python 从上游取回构件，再以纯 HTTP 提供给 JVM。
**构建前先启动它**：

```powershell
# 1) 启动代理（保持运行）
& 'D:\dsh\resources\runtime\primary-runtime\dependencies\python\python.exe' `
    'D:\xiangmu\mc\.buildtools\nf_proxy.py' 8123

# 2) 构建
cd D:\xiangmu\mc
.\gradlew.bat build -I .buildtools\local-mirror.init.gradle
```

代理缓存放在 `.buildtools/local-repo`，重复构建会直接命中。网络恢复正常后去掉 `-I` 即可，
项目本身没有任何硬编码的代理设置。

`run/eula.txt` 已准备好（`eula=true`），需要验证服务端时可以
`.\gradlew.bat runServer -I .buildtools\local-mirror.init.gradle`。

### `.buildtools/` 里的东西

| 文件 | 用途 |
| --- | --- |
| `nf_proxy.py` | 本地缓存代理，绕开 JVM 与 `maven.neoforged.net` 的 TLS 问题 |
| `local-mirror.init.gradle` | Gradle init 脚本，把 NeoForge Maven 指向上面的代理 |
| `verify_api_coverage.py` | 核验产物引用的 NeoForge API 在 21.1.x 两端都存在 |
| `verify_side_safety.py` | 核验专用服务器不会加载 Minecraft 客户端类 |
| `wrapper.init.gradle` | 生成 wrapper 时用（跳过发行版 URL 的可达性检测） |
| `print-classpath.init.gradle` | 诊断用：打印编译类路径 |
| `nf_mirror.py` | 早期版本，直接把少量构件镜像到 `local-repo`（已被代理取代，保留备用） |
| `legacy-client-server-*.zip` | 合并前 `client/` 与 `server/` 两个工程的原始源码备份 |
| `local-repo/` | 代理缓存（Maven 目录结构），可随时删除 |

> 原来的 `client/` 和 `server/` 目录已在合并验证通过后删除，源码完整保存在上面的 zip 里。
