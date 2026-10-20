# QuickShulkerMore 1.0.0 — Fabric / Minecraft 26.2

非官方 fork，原址：[MoRanpcy/quickshulker](https://github.com/MoRanpcy/quickshulker)。所有版本在同一分支维护，26.3 为主开发版本。

Java 25、Gradle 9.5.1、Loom 1.17.21；实际游戏 26.2、Loader 0.19.3、Fabric API 0.152.1+26.2、Mod Menu 20.0.1。本目录的 API 差异覆盖层优先，其余共享 `../26.3-fabric`。AMS 仍为可选、直接公开字段读取，不用反射或 AMS 方法注入。

已审查上游 26.2 提交 `e77e519f72ee478a9e051724d56a4795f1c12a56`。区别于 26.1，26.2 已使用新 GUI 访问路径和 `BlockEntityTypes`；区别于 26.3，仍使用 GLFW、旧收纳袋 API、12 种木材展示架及 2 项酿造台状态。完整组件容量计算保留空槽，酿造台状态数量来自该游戏自身的 `NUM_DATA_VALUES`，不借用漏斗布局。

从根目录运行 `./build.ps1 -Version 26.2 -JavaHome '<JDK 25 路径>'`；或进入本目录运行 `gradlew.bat build printArtifactName`。元数据为 `1.0.0-26.2`，本地 JAR 带香港时区时间戳；GitHub 正式构建自动去除。

完整 build、135 项组件/协议检查、4 组真实世界 GameTest 加原版控制测试全部通过。覆盖原生容器槽位与完整初始同步、真实合成器配方/锁定/返还物及满背包原子拒绝、全部铜箱子/展示架完整物品编码解码与源槽保护、各权限级别的 OP 指令门槛。

修复后安装包：`../../mod-builds/20261020-083108/quickshulkermore-1.0.0-26.2-test-20261020-083035.jar`。SHA-256、组件日志和世界测试 XML 随归档保存；此前 `082651` 归档早于新增世界测试与同步修复，不建议使用。

未验证真实 AMS 联合运行、客户端视觉/输入或原版/More 混装登录。日志保留本机 Windows 性能计数器读取、Gradle 旧 Java 路径、原生 API 弃用及首次测试目录初始化告警，不称为零告警，也不以屏蔽日志代替测试。
