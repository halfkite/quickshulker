# Quick Shulker — AMS 兼容扩展版

> 本仓库是 [MoRanpcy/quickshulker](https://github.com/MoRanpcy/quickshulker) 的非官方 fork。
> 原项目、原作者及其许可归原仓库所有；本 fork 用于维护 Minecraft 1.21.1 的 AMS 兼容扩展。

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

## 当前版本

- Minecraft：1.21.1
- Mod 版本：3.1.2-1.21.1
- 加载器：Fabric
- Java：21 或更高版本

客户端和服务端应安装同一版本。AMS 为可选依赖；未安装或未启用大容器规则时保持原版 27 格行为。

## 构建与测试

使用 JDK 21 执行 Gradle `clean build`，Java 编译、Mixin、Access Widener 和 remap 均已通过。服务端开发启动检查能够加载 Quick Shulker 及其 Mixin，并正常运行到 EULA 检查阶段。

项目当前没有自动测试源码。AMS 1.21.1 的 54 格界面、容器槽并发修改和长按滑动仍建议在实际客户端/服务器中验证；不要把“构建成功”理解为已经完成游戏内测试。

## 许可与致谢

项目继续遵循上游的 [MIT License](LICENSE)。感谢 Quick Shulker 原作者和贡献者，以及 Carpet AMS Addition 项目。
