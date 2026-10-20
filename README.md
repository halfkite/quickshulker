# QuickShulkerMore — AMS 兼容与快捷容器扩展

> 本仓库是 [MoRanpcy/quickshulker](https://github.com/MoRanpcy/quickshulker) 的非官方 fork。
> 原项目、原作者及其许可归原仓库所有。**主要开发版本现为 Minecraft 26.3**，同时维护 Fabric / NeoForge；Minecraft 1.21.1 保留为兼容维护版本。

## 主要开发入口：26.3

Fabric 逐版本同步的最新结果、归档及未验证边界见 [1.0.0 移植进度](docs/1.0.0-fabric-port-progress.md)；早期基线清单中的“待移植”状态以此进度为准。

当前优先目标为 **Fabric / Minecraft 26.3 / QuickShulkerMore 1.0.0**。展示名称和安装包前缀改为 QuickShulkerMore / `quickshulkermore`，内部模组 ID、配置文件、网络通道和资源命名空间仍为 `quickshulker`，作为原版 QuickShulker 的替代品，不能同时安装两者。版本号从旧扩展候选的 3.x 重置为 1.0.0，不代表功能回退。

新发布标签使用 `quickshulkermore-fabric-1.0.0-26.3` 格式，更新检查不再读取旧 `3.x` 标签作为 More 更新。仓库 URL 暂不改名。Fabric 1.21 至 26.3 移植顺序、版本缺口和混装验证状态见 [1.0.0 Fabric 基线与移植清单](docs/1.0.0-fabric-baseline.md)。NeoForge 暂保留上一轮候选，尚未完成本轮品牌/版本号同步。

同一 Git 分支内保存版本源码，不要求切换到上游的不同版本分支：

| 目标 | 源码目录 | Java |
| --- | --- | --- |
| 26.3 Fabric（默认） | `versions/26.3-fabric/` | 25 |
| 26.1、26.2 Fabric | `versions/<版本>-fabric/` | 25 |
| 26.3 NeoForge | `versions/26.3-neoforge/` | 25 |
| 1.21.1 Fabric（维护） | 根目录 `src/` | 21 |
| 1.21、1.21.2～1.21.11 Fabric | `versions/<版本>-fabric/` | 21 |

默认执行 `./build.ps1 -JavaHome '<JDK 25 路径>'`；NeoForge 使用 `./build.ps1 -Loader neoforge -JavaHome '<JDK 25 路径>'`。也可进入对应目录执行自己的 `gradlew.bat build`。根目录旧的 `gradlew.bat` 仍只构建 1.21.1，避免用新工具链破坏旧版本。

开发版本和默认加载器由 `development.json` 声明，项目开发指引见 `AGENTS.md`。当前 26.3 功能见 [3.3.0 容器与配置说明](docs/3.3.0-26.3-containers.md)，初始移植记录见 [26.3 移植说明](docs/3.2.0-26.3-validation.md)。以下 1.21.1 记录保留作为历史基线，不代表它仍是主要开发版本。

Fabric 自动构建矩阵已配置 1.21～1.21.11 和 26.1～26.3 共 15 个目标，自动选用 Java 21/25；本地测试包带时间戳，GitHub 正式构建包不带时间戳。本轮尚未推送或实际运行远端 Actions。逐版本产物和测试边界见上方移植进度。

### 1.0.0：1.21.1 Fabric 移植

2026-10-19 自测发现并修复新版 AMS 设置类包名兼容问题，已补真实 AMS 与无 AMS 两套服务端 GameTest；详细覆盖范围和未测试边界见 [1.21.1 自测记录](docs/1.0.0-1.21.1-self-test.md)。

根目录已移植三态快捷容器主开关、12 类容器子开关、OP 策略指令、织布机、真实陶罐/熔炉/高炉/烟熏炉/酿造台槽位、合成器锁定及带返还物的合成事务。1.21.1 不存在铜箱子和展示架，因此不注册这两类配置。

1.21.1 实验性玩法中的收纳袋也支持快捷编辑，使用原版 `BUNDLE_CONTENTS` 及重量计算，测试世界明确启用 `minecraft:bundle`。暂不快捷编辑嵌套收纳袋，也不允许通过快捷界面插入收纳袋，已有内容保留，仍可使用原版操作。新增界面先协商通道能力，不向旧客户端注册新增菜单类型；普通快捷物品保留原版 4 字节打开协议。协议测试不等于真实两端混装登录验证，AMS 1.21.1 联机和客户端界面仍需游戏内验收。

### 3.3.0：原生容器与细分配置（26.3 Fabric / NeoForge）

- 配置界面新增独立“快捷容器”页签。首项为主开关，默认“创造可用”，点击循环切换 `创造可用 → true（所有模式）→ false（禁用）`；下面 14 类容器分别点击开启/关闭，默认全部开启。关闭主开关不会清除子开关选择。
- OP 指令 `/quickshulker quickContainers creative|true|false` 控制主策略；`/quickshulker quickChest true|false` 等控制子项，保存立即生效。远程服务器以服务端策略为准；GUI 保存服务器策略也需要 OP。
- 新增全部原版铜箱子氧化/涂蜡变种、全部木材展示架。陶罐真实 1 格，展示架真实 3 格；熔炉/高炉/烟熏炉使用各自原生 3 格菜单，酿造台使用原生 5 格菜单（药水×3、原料、燃料），不再借用漏斗菜单。
- 合成器使用原生样式菜单：点击空格切换锁定，“合成一次”按真实配方消耗材料；产物和返还物进入背包，空间不足则不消耗、不掉落。仅空槽可锁；源合成器及祖先容器槽全程保护。
- 织布机是默认生存可用的独立工作站，允许成组物品，不受快捷容器主策略限制。熔炉和酿造台仅支持内部存储编辑，不在背包内持续烧炼或酿造。
- 本节描述历史 3.3.0 的 26.3 行为；1.0.0 的逐版本构建及测试结果见移植进度。普通潜影盒保留旧协议，新策略使用独立 v2 通道。

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
- 历史候选曾通过反射读取 AMS 规则；当前 1.0.0 已改为可选、延迟链接的公开字段读取，不使用反射或 AMS 方法注入。末影箱以实际库存容量为准，已有大潜影盒高位内容始终保留。

1.21.1 基线合并了上游的音效、快捷键和更新检查改动，并修正服务端网络回调线程与可选槽索引检查。其他版本在同一仓库的版本目录维护，开发入口及最新状态见上方。更新检查指向本 fork 的 Releases。

## 许可与致谢

项目继续遵循上游的 [MIT License](LICENSE)。感谢 Quick Shulker 原作者和贡献者，以及 Carpet AMS Addition 项目。
