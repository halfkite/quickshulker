# QuickShulkerMore 1.0.0 — Fabric / Minecraft 26.1

非官方 fork，原址：[MoRanpcy/quickshulker](https://github.com/MoRanpcy/quickshulker)。所有版本在同一分支维护，26.3 为主开发版本。

## 构建与架构

Java 25，Gradle 9.5.1，Loom 1.17.21；游戏 26.1、Loader 0.18.4、Fabric API 0.144.3+26.1、Mod Menu 18.0.0。按路径优先使用本目录的 API 覆盖层，其余源码、资源、无反射 AMS 字段签名和独立上游协议夹具复用 `../26.3-fabric`，不依赖旧 Yarn 源码。

从仓库根目录运行 `./build.ps1 -Version 26.1 -JavaHome '<JDK 25 路径>'`；或进入本目录执行 `gradlew.bat build printArtifactName`。GitHub 构建自动去除文件名时间戳，元数据固定为 `1.0.0-26.1` 并限制实际游戏版本。

已审查上游 26.1 提交 `085cec74393640e7bf10730c1f3b3a0152cbc24f` 及到 26.3 的差异。本覆盖层适配 GLFW、界面访问、收纳袋组件 API、方块实体类型及注册表初始化。保存容量使用包含空槽的 `allItemsCopyStream()`；不会按非空物品数截断高位槽。铜箱子、展示架按原版方块类型识别。

## 验证结果

完整 build、135 项组件/协议检查、4 组真实世界 GameTest 加 1 项原版控制测试通过。世界测试覆盖原生熔炉/高炉/烟熏炉与酿造台槽位、酿造台完整初始状态同步、陶罐、织布机策略独立性、真实合成器配方/锁定/满背包与返还物事务、全部铜箱子/展示架保存重开与源槽保护、原版各权限级别的指令门槛。

同步测试修复前实际复现 `Index 2 out of bounds for length 2`：本游戏酿造台仅有 2 项状态，不能沿用 26.3 的 4 项。现在使用原版 `BrewingStandBlockEntity.NUM_DATA_VALUES`，没有硬编码跨版本状态数或吞异常。

修复后安装包：`../../mod-builds/20261020-083029/quickshulkermore-1.0.0-26.1-test-20261020-082956.jar`。目录附 SHA-256 清单、组件日志和世界测试 XML。此前 `082520` 归档早于新增世界测试与同步修复，不建议使用。

这不是实际客户端窗口点击、两端原版/More 混装登录或真实 AMS 联合运行验证。保留 Loader 的 Java 25 兼容级别提示、原生 API 弃用提示、测试目录初始化和本机 Gradle 旧 Java 路径告警，不称为零告警。
