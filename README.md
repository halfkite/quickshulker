# Quick Shulker — AMS 兼容扩展版

> 本仓库是 [MoRanpcy/quickshulker](https://github.com/MoRanpcy/quickshulker) 的非官方 fork。
> 原项目、原作者及其许可归原仓库所有。**主要开发版本现为 Minecraft 26.3**，同时维护 Fabric / NeoForge；Minecraft 1.21.1 保留为兼容维护版本。

## 主要开发入口：26.3

同一 Git 分支内保存版本源码，不要求切换到上游的不同版本分支：

| 目标 | 源码目录 | Java |
| --- | --- | --- |
| 26.3 Fabric（默认） | `versions/26.3-fabric/` | 25 |
| 26.3 NeoForge | `versions/26.3-neoforge/` | 25 |
| 1.21.1 Fabric（维护） | 根目录 `src/` | 21 |

默认执行 `./build.ps1 -JavaHome '<JDK 25 路径>'`；NeoForge 使用 `./build.ps1 -Loader neoforge -JavaHome '<JDK 25 路径>'`。也可进入对应目录执行自己的 `gradlew.bat build`。根目录旧的 `gradlew.bat` 仍只构建 1.21.1，避免用新工具链破坏旧版本。

开发版本和默认加载器由 `development.json` 声明，项目开发指引见 `AGENTS.md`。26.3 的功能、差异和验证状态见 [26.3 移植说明](docs/3.2.0-26.3-validation.md)。以下 1.21.1 记录保留作为历史基线，不代表它仍是主要开发版本。

## 项目链接

- 上游原址：https://github.com/MoRanpcy/quickshulker
- 本 fork：https://github.com/halfkite/quickshulker
- 1.21.1 修改分支：https://github.com/halfkite/quickshulker/tree/codex/ams-large-container-1.21.1
- 构建下载：https://github.com/halfkite/quickshulker/releases

## 本 fork 的改动

- 兼容 Carpet AMS Addition 的 54 格大潜影盒。
- 根据末影箱实际容量自动使用 27 格或 54 格界面，兼容 AMS 大末影箱。
- 新增砂轮、锻造台和制图台的手持/背包快捷打开。
- 新增独立配置，可右击打开箱子、末影箱等真实容器槽中的潜影盒。
- 服务端验证菜单同步 ID、槽位和物品类型，并跟踪、保护原库存槽，降低错槽和复制风险。
- 修复大潜影盒长按滑动、批量存取和潜影盒间转移时按 27 格保存而丢失后 27 格物品的问题。
- 增加物品组件防截断保护：即使 AMS 规则检测失败，已有的高位槽物品也不会被 27 格临时库存覆盖。
- 在配置界面加入容器槽打开、砂轮、锻造台和制图台开关，并补充多语言文本。
- 保留上游 Reinforced Shulker Boxes 兼容。

## 历史基线：1.21.1

- Minecraft：1.21.1
- Mod 版本：3.2.0-1.21.1（开发候选）
- 加载器：Fabric
- Java：21 或更高版本

客户端和服务端应安装同一版本。AMS 为可选依赖；未安装或未启用大容器规则时保持原版 27 格行为。

## 构建与测试

使用 JDK 21 执行 `gradlew.bat build`。离线环境可以使用 `-PlocalCompatDependencies=true`，需自行在 `libs/` 放入官方 Reinforced Core 4.0.2+1.21.1、Cloth Config Fabric 15.0.130 和 JetBrains annotations 25.0.0 对应文件；这些文件不随源码提交，默认构建仍使用 Maven 依赖。

`build` 包含 `regressionTest`：检查旧协议长度、新协议通道隔离、组件持久化、大潜影盒高位槽保留、过期会话写入拒绝、陶罐填充槽和合成器禁用槽。游戏中的 AMS 规则、并发操作和指令同步仍需客户端/服务器联合验证；构建成功不等于游戏内测试通过。

## 3.2.0 快捷容器（1.21.1 验证阶段）

可快捷打开手持或背包内数量为 **1** 的箱子、陷阱箱、木桶、漏斗、投掷器、发射器、陶罐、合成器、熔炉、烟熏炉、高炉、酿造台物品，内容保存回原物品组件。不拆分堆叠，不在物品中执行烧炼、酿造、红石或自动合成。

- 默认 `quickContainers=true`、`quickContainersCreativeOnly=true`，即功能开启且仅创造模式可用；服务端策略优先。
- OP（权限等级 2）可执行 `/quickshulker quickContainers true|false` 和 `/quickshulker quickContainersCreativeOnly true|false`，修改立即生效并保存配置；省略布尔值查询当前值。
- 陶罐仅一个有效槽，熔炉类三个；使用漏斗式界面的额外槽不可操作。合成器保留并禁止操作禁用槽。为避免递归嵌套，不允许放入与宿主同类型的容器。
- 带未展开战利品表的物品拒绝打开，防止覆盖未知内容；其他物品组件保持不变。
- 原版服务端不支持此新增容器协议，客户端检测通道后才发送。普通潜影盒继续使用原版 QuickShulker 的旧协议；新增容器功能需服务端安装本版本。
- AMS 两个大容器规则通过可选反射读取，不注入 AMS 的 Mixin 节点。末影箱仍以玩家实际库存容量为准，已有大潜影盒高位内容始终保留。

1.21.1 基线合并了上游的音效、快捷键和更新检查改动，并修正服务端网络回调线程与可选槽索引检查。26.3 已在同一仓库的版本目录继续移植，开发入口见上方；其他旧版本尚未同步。更新检查指向本 fork 的 Releases。

## 许可与致谢

项目继续遵循上游的 [MIT License](LICENSE)。感谢 Quick Shulker 原作者和贡献者，以及 Carpet AMS Addition 项目。
