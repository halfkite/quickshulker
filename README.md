# QuickShulkerMore

[English](README_en.md) · [GitHub Releases](https://github.com/halfkite/quickshulkermore/releases)

QuickShulkerMore 是 [QuickShulker](https://github.com/MoRanpcy/quickshulker) 的非官方扩展版，增加 Carpet AMS Addition 大容器兼容、快捷工作站和快捷容器。项目遵循上游 MIT 许可证。

当前版本为 **1.0.0**，主要开发目标是 **Minecraft 26.3 / Fabric**。本仓库将多个游戏版本的源码放在同一个 `main` 分支中。内部模组 ID 仍为 `quickshulker`，用于替代原版 QuickShulker；两者不能同时安装。

## 功能

- 快捷打开潜影盒，并按实际容量处理 AMS 的 54 格大潜影盒和大末影箱。AMS 是可选依赖；通过公开设置 API 检查规则，不使用反射。容量规则变化时保留已有高位槽内容。
- 从手持物品或背包中快捷打开砂轮、锻造台、制图台和织布机。
- 按游戏版本支持快捷容器，例如箱子、木桶、漏斗、投掷器、发射器、陶罐、合成器、炉类、酿造台，以及原版提供这些方块的版本中的铜箱子和展示架。使用对应容器的实际槽位；合成器支持配方合成和锁定空槽。
- 可从真实容器槽右键打开其中的潜影盒。源物品槽会在服务端验证并受到保护。
- 快捷容器有主开关和分类子开关；默认主策略为“创造可用”，子项默认开启。OP 可用指令调整服务器策略。

## 支持版本

Fabric 源码和逐版本构建目标覆盖 **Minecraft 1.21、1.21.1–1.21.11、26.1、26.2、26.3**。26.3 是主要开发版本；1.21.1 在仓库根目录作为维护目标，其余版本位于 `versions/`。

仓库也包含 26.3 NeoForge 源码，但本轮完整的 15 版本移植与回归记录针对 Fabric。请在下载页和验证记录中确认所选加载器的对应产物与状态。

## 安装

1. 在 [Releases](https://github.com/halfkite/quickshulkermore/releases) 选择与 Minecraft 版本、加载器相符的 JAR。
2. 将 JAR 放入对应实例的 `mods` 文件夹。
3. 要使用完整快捷容器功能，请让客户端和服务端安装同版本 QuickShulkerMore。

不要同时安装原版 QuickShulker，因为两者使用相同的内部模组 ID。普通潜影盒沿用旧打开协议；客户端与服务端混装的完整兼容性尚未作为已验证功能承诺。

截至 2026-10-20，15 个 Fabric 目标均有独立构建和自动回归记录。真实 AMS 服务端测试记录仅覆盖 1.21.1；自动测试不等于真实客户端界面或原版/More 混装联机验收。

- [当前验证记录与版本包清单](docs/testing/1.0.0-container-fixes.md)
- [Fabric 移植进度](docs/testing/1.0.0-fabric-port-progress.md)
- [1.21.1 自测范围与未验证项](docs/testing/1.0.0-1.21.1-self-test.md)
- [文档索引与历史记录](docs/README.md)

## 项目

- 上游原址：[MoRanpcy/quickshulker](https://github.com/MoRanpcy/quickshulker)
- 本项目：[halfkite/quickshulkermore](https://github.com/halfkite/quickshulkermore)
- 下载：[GitHub Releases](https://github.com/halfkite/quickshulkermore/releases)

感谢 QuickShulker 原作者与贡献者，以及 Carpet AMS Addition 项目。
