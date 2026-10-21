# 更新日志 / Changelog

## 1.0.1 — 2026-10-21

### 中文

- 修复服务端安装 QuickShulkerMore、客户端未安装时，因无法识别 `quickshulker` 菜单注册项而断开连接的问题。
- 登录、重生、切换维度及重开背包时，仅向支持相应功能的客户端发送扩展数据包。
- 陶罐、展示架、合成器和收纳袋改用原版菜单类型传输，由新版客户端显示对应的真实界面；不支持的客户端会安全拒绝打开扩展菜单。
- 修正 1.21.11 工作站 Mixin 在安装包重新映射时丢失方法参数类型、误选重载方法的问题。
- 同步 Minecraft 1.21–26.3 的全部 15 个 Fabric 版本，保留兼容区间共用 JAR 的方式，共提供 10 份安装包。

完整扩展菜单需要客户端与服务端安装同版本 QuickShulkerMore。

验证：26.3 使用官方纯原版、Fabric 未安装 More、Fabric 安装 More 三种客户端完成实连测试；全部 15 个版本通过安装包回归和客户端加载检查，共通过 101 项世界测试。其他版本未逐一进行多人实连，本次未进行真实 AMS 联测。

### English

- Fixed disconnections caused by an unknown `quickshulker` menu registry entry when QuickShulkerMore is installed only on the server.
- Extension packets sent during login, respawn, dimension changes, and inventory reopening now require the corresponding client capability.
- Decorated pots, shelves, crafters, and bundles now use a vanilla menu type for transport. Updated clients display the correct interfaces; unsupported clients are safely prevented from opening extended menus.
- Fixed the 1.21.11 workstation Mixin selector losing its argument type during packaged-JAR remapping and selecting the wrong overload.
- Updated all 15 Fabric targets from Minecraft 1.21 through 26.3, retaining shared JARs for compatible version ranges: 10 installation JARs in total.

Full extended menus require the same QuickShulkerMore version on the client and server.

Validation: Minecraft 26.3 passed real connection tests with an official vanilla client, Fabric without More, and Fabric with More. All 15 runtimes passed packaged-JAR regressions and client class-loading checks, totaling 101 world tests. Multiplayer connections were not individually tested on the other versions, and no real AMS integration test was performed in this round.
