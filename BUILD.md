# 构建说明

## 环境

| 项 | 值 |
| --- | --- |
| JDK 21 | `C:\Users\Administrator\.jdks\temurin-21`（1.21.x 版本线） |
| JDK 25 | `C:\Users\Administrator\.jdks\temurin-25`（26.x 版本线，NeoForge 26.x 要求 Java 25） |
| Gradle | 9.2.0，由 `gradlew` 提供 |
| 两个 JDK 都注册在 | `gradle.properties` 的 `org.gradle.java.installations.paths` |

> Gradle 8.9 解析不了 NeoForge 26.x 的模块元数据（`modDevApiElements` 与 `universalJar`
> 变体冲突），所以 wrapper 升到了 **9.2.0**。

## 构建命令

```powershell
# 全部 16 个版本
.\gradlew.bat build

# 单个版本（工程名 = mc + 版本号里的 . 换成 _）
.\gradlew.bat :mc1_21_5:build
.\gradlew.bat :mc26_3:build

# 清理后重建
.\gradlew.bat clean :mc26_3:build
```

产物：`versions/<MC版本>/build/libs/blockinfo-1.1.0-<MC版本>.jar`

## 本机网络注意事项（重要）

这台机器上有三个互相独立的网络问题，都已绕过：

### 1. JVM 与 maven.neoforged.net / libraries.minecraft.net 的 TLS 握手会被重置

现象：`SSLHandshakeException: Remote host terminated the handshake` / `Connection reset`。
PowerShell(.NET) 和 Python(OpenSSL) 访问同一地址正常，所以是 JVM 侧的问题。

解决：`.buildtools/nf_proxy.py` —— 本地缓存代理，用 Python 取回上游构件，再以纯 HTTP
提供给 JVM。**构建前必须先启动它**：

```powershell
& 'D:\dsh\resources\runtime\primary-runtime\dependencies\python\python.exe' `
    'D:\xiangmu\mc\.buildtools\nf_proxy.py' 8123
```

然后构建时带 init 脚本：

```powershell
.\gradlew.bat build -I .buildtools\local-mirror.init.gradle
```

`local-mirror.init.gradle` 会把 `maven.neoforged.net` 和 `libraries.minecraft.net`
的仓库地址改写到 `http://127.0.0.1:8123`。

### 2. 系统 HTTP 代理

`gradle.properties` 里默认开启了 `127.0.0.1:7897` 的代理加速下载。
`nonProxyHosts` 必须保留 `127.0.0.1`，否则上面那个本地代理会被绕一圈再回到自己。
不需要代理时把那几行注释掉即可。

### 3. NFRT 下载 Minecraft jar 会卡死

NeoForm Runtime 用自己的 JVM HTTP/2 客户端拉 `piston-data.mojang.com`，会停在 16MB 左右
（缓存目录里留下一堆 `*.dltmp`），既不报错也不超时。

解决：`.buildtools/preseed_minecraft_jars.py` 用 Python 把每个版本的
`client.jar` / `server.jar` 预先下载到 NFRT 的 artifact 缓存：

```
~/.gradle/caches/neoformruntime/artifacts/minecraft_<MC版本>_{client,server}.jar
```

预置之后单版本构建从 12–18 分钟降到 **1–2 分钟**。

## 批量构建

```powershell
# 带超时与重试（网络抖动时用）
.\buildtools\build-batch.ps1 -Targets '1.21.6','1.21.7','26.3' -Attempts 3 -TimeoutSec 1500
```

结果写入 `.buildtools/build-results.txt`，每个目标的完整日志在 `.buildtools/logs/`。

## 版本线切换是怎么做的

`versions/<MC版本>/gradle.properties` 里有五个关键字段：

| 字段 | 说明 |
| --- | --- |
| `minecraft_version` | 该 jar 只接受这一个 MC 版本 |
| `neo_version` | 编译基线，取该版本线**最低可用**发行版 |
| `neo_version_range` | 渲染进 `neoforge.mods.toml`，`[最低版,)` 覆盖整条线 |
| `java_version` | `21` 或 `25` |
| `source_line` | `1.21` → `common/`；`26` → `common26/` |

`build.gradle` 的 `subprojects` 块按 `source_line` 选择源码目录，并把
`common/src/main/resources` 作为两条线共用的资源目录。

## 已知的版本差异（已处理）

| 变化 | 出现版本 | 处理方式 |
| --- | --- | --- |
| `BuiltInRegistries.ITEM.get(id)` 返回类型从 `Item` 变成 `Optional<Reference<Item>>` | 1.21.2 | 改为遍历注册表 + `getKey`，两条线通用 |
| `ServerPlayer.serverLevel()` → `level()` | 1.21.6 | 统一写 `(ServerLevel) player.level()` |
| `ResourceLocation` → `Identifier` | 26.x | `common26` 分支 |
| `GuiGraphics` → `GuiGraphicsExtractor`，`drawString` → `text` | 26.x | `common26` 分支 |
| `FMLEnvironment.dist` 字段 → `getDist()` 方法 | 26.x | `common26` 分支 |

## 辅助脚本

| 文件 | 用途 |
| --- | --- |
| `.buildtools/nf_proxy.py` | 本地缓存代理（NeoForge Maven / Minecraft libraries） |
| `.buildtools/local-mirror.init.gradle` | 把上述两个仓库指到本地代理 |
| `.buildtools/preseed_minecraft_jars.py` | 预下载 Minecraft client/server 到 NFRT 缓存 |
| `.buildtools/build-batch.ps1` | 批量构建（超时 + 重试） |
| `.buildtools/verify_api_coverage.py` | 核验产物引用的 NeoForge API 在版本线两端都存在 |
| `.buildtools/verify_side_safety.py` | 核验专用服务器不会加载客户端类 |
| `.buildtools/map_versions.py` | NeoForge 版本号 → MC 发行版的映射 |
| `.buildtools/wrapper.init.gradle` | 生成 wrapper 时跳过发行版 URL 检测 |
