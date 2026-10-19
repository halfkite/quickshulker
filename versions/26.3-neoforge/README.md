# Quick Shulker 26.3 — NeoForge

本目录是 [MoRanpcy/quickshulker](https://github.com/MoRanpcy/quickshulker/tree/26.3-neo) 的非官方扩展移植，原作者及 MIT 许可保留。本 fork：[halfkite/quickshulker](https://github.com/halfkite/quickshulker)。

Minecraft 26.3 是主要开发版本。新增 AMS 可选规则检测、大容器容量保留、三个工作站、容器槽潜影盒、创造容器物品存储编辑、OP 配置指令及源会话保护。上游的收纳袋和 26.3 SDL 输入支持保留。

使用 Java 25，在仓库根执行 `./build.ps1 -Loader neoforge`，或在本目录执行 `gradlew.bat build`。JUnit 通过官方 NeoForge 临时服务器运行组件回归测试。可安装 JAR 不包含测试框架。

详见 [仓库说明](../../README.md) 和 [26.3 验证记录与游戏内测试清单](../../docs/3.2.0-26.3-validation.md)。AMS 与客户端交互仍需游戏内联合验收。
