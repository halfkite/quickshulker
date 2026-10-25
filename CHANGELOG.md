# 更新日志 / Changelog

## 1.0.2 — 2026-10-25

### 中文

- 增加独立的“快捷潜影盒静音”选项，统一控制快捷打开和关闭音效，不影响放置在世界中的方块声音。
- 声音设置仅影响本客户端，不再向服务端发送个人声音偏好，也不改变其他玩家的声音；未安装 More 的客户端继续接收原版声音包。
- 快捷容器的每个子项均支持 `创造可用 / true / false`，配置界面与 OP 指令同步支持；总开关仍为权限上限，旧配置保留原有权限。
- Minecraft `26.1`、`26.1.1`、`26.1.2` 合并使用 `mc26.1-26.1.2` 安装包，并加入独立的补丁版本兼容验证目标。
- Fabric 版本统一升级至 `1.0.2`；本地测试包保留时间戳，GitHub 正式构建不带时间戳。

完整的新配置及可独立静音的快捷音效需要客户端、服务端均使用本次修正后的 `1.0.2` 构建，不要与 10 月 24 日旧测试包混用。声音偏好只保存在本地，不受服务端声音配置控制。原有不安装客户端模组即可加入服务器的行为保持不变。本次不包含 NeoForge 移植。

验证：本次修正后的 10 个安装包覆盖 17 个 Fabric 运行版本，均通过实际安装 JAR 的组件/协议回归、世界测试和客户端加载检查，共通过 118 项世界测试；12 项构建流程测试也通过。26.3 额外完成静音 More、有声 More、未安装 More 的 Fabric 三客户端实连的 15 项声音引擎调用检查。未逐版本进行多人实连，未做实际听感或 AMS 联测。10 月 24 日旧测试合集不包含此修正。

### English

- Added a dedicated “Silence quick shulker boxes” option for quick opening and closing sounds, without changing the sounds of placed blocks.
- Sound settings now affect only the local client. Personal preferences are no longer sent to the server and cannot change what other players hear. Clients without More continue receiving vanilla sound packets.
- Every portable-container child option now supports `creative / true / false`, both in the configuration screen and OP commands. The master setting remains the permission ceiling, and existing configuration permissions are preserved.
- Minecraft `26.1`, `26.1.1`, and `26.1.2` now share the `mc26.1-26.1.2` artifact, with separate runtime-validation targets for both patch releases.
- Updated the Fabric versions to `1.0.2`. Local test artifacts retain timestamps; official GitHub builds do not include timestamps.

Both client and server must use matching corrected `1.0.2` builds for the complete new configuration and independently muted quick sounds; do not mix them with the October 24 test JARs. Sound preferences stay local and are not controlled by server sound settings. Clients without the mod can still join the server. This update does not port NeoForge.

Validation: all 10 corrected installation JARs passed packaged-artifact component/protocol regressions, world tests, and client class-loading checks across 17 Fabric runtimes, totaling 118 world tests. All 12 build-pipeline tests also passed. On 26.3, 15 playback-engine checks additionally passed over simultaneous real connections from quiet More, loud More, and Fabric-without-More clients. Multiplayer was not tested individually on every version, and physical audibility and actual AMS integration were not verified. The October 24 test ZIP does not contain this correction.

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
