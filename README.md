# Block Info

Minecraft **1.21.1** / NeoForge **21.1.x** 的方块信息 HUD 模组：准星指向方块时，在屏幕顶部
显示它的名称、所属模组、能否挖掘、作物生长进度、容器内容、熔炉烧制/燃烧进度以及挖掘进度。

客户端与服务端在**同一个 jar** 里：

* 只装客户端就有完整 HUD（方块名、所属模组、可否挖掘、作物生长、挖掘进度、容器内容）；
* 装到服务端则额外同步熔炉与容器的精确状态；
* 单人游戏只装这一个 jar 即可完整工作。

两个网络通道都是 **optional** 的：没装本模组的服务器可以正常进入，装了本模组的服务器
也接受没装本模组的客户端，只是收不到同步数据。

## 版本

| 项目 | 值 |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | `[21.1.1,)` —— 覆盖整条 21.1.x（21.1.1 ~ 21.1.264+） |
| Java | 21 |
| jar | `build/libs/blockinfo-1.5.0.jar` |

编译基线刻意取该线的**最低**版本 `21.1.1`：针对最低版本编译，产物就只可能引用 21.1.1
已有的 API，因此在整条 21.1.x 上都能加载。只针对最新版编译的话，一旦用到较晚加入的 API，
在旧构建上就会 `NoSuchMethodError`。

## 安装

把 `build/libs/blockinfo-1.5.0.jar` 放进 `mods/` 目录即可。客户端放客户端，服务端放服务端
（同一个文件）。

## 构建

需要 JDK 21。

```bash
./gradlew build
```

产物在 `build/libs/`。

开发环境可直接起客户端 / 服务端：

```bash
./gradlew runClient
./gradlew runServer
```

> 本机（Windows）上 JVM 与 `maven.neoforged.net` 的 TLS 握手会被重置，需要用
> `.buildtools/nf_proxy.py` 这个本地缓存代理绕开，详见 [BUILD.md](BUILD.md)。

## 目录

```
src/main/java/com/example/blockinfo/
    BlockInfo.java              唯一的 @Mod 入口
    BlockInfoNetwork.java       网络通道注册
    network/                    FurnaceDataPacket / ContainerDataPacket
    server/ServerSyncHandler.java
    client/ClientBootstrap.java         客户端初始化（只在 dist == CLIENT 时执行）
    client/BlockInfoTracker.java        采集准星目标方块的信息
    client/BlockInfoRenderer.java       订阅 GUI 渲染事件
    client/BlockInfoHud.java            排版与绘制
    client/BlockInfoData.java
    client/ClientFurnaceCache.java / ClientContainerCache.java / BlockInfoUtil.java
src/main/templates/META-INF/neoforge.mods.toml
```

**侧别隔离**：`BlockInfo` 先注册服务端同步，再用
`if (FMLEnvironment.dist == Dist.CLIENT) ClientBootstrap.init();` 才进入客户端部分。
JVM 到那一行才会解析客户端类，因此专用服务器不会加载 `Minecraft` / `GuiGraphics`。
两个 `playToClient` 包的 `handle()` 里也做了同样的判断。

## 说明

* HUD 只使用 `GuiGraphics.fill` / `drawString`，不碰 `RenderSystem` 或 `blit`——
  这类渲染接口在不同版本间变动频繁。
* 挖掘进度来自 `MultiPlayerGameMode.getDestroyStage()`（0..10 档位）；是否显示进度条由
  `isDestroying() || keyAttack.isDown()` 决定，避免刚开始挖（0%）和连挖破块空隙时闪烁。
* 熔炉内部字段用反射读取（`cookingProgress`/`cookingTimer`、`litTime`/`litTimeRemaining` 等），
  对不同版本字段名做了容错。

## 许可证

MIT，见 [LICENSE](LICENSE)。
