# QuickShulkerMore

[简体中文](README.md) · [GitHub Releases](https://github.com/halfkite/quickshulkermore/releases)

QuickShulkerMore is an unofficial extension of [QuickShulker](https://github.com/MoRanpcy/quickshulker), adding Carpet AMS Addition large-container compatibility, portable workstations, and quick access to container items. The project follows the upstream MIT license.

The current mod version is **1.0.0**, with **Minecraft 26.3 / Fabric** as the primary development target. Source for multiple Minecraft versions lives on the same `main` branch. The internal mod ID remains `quickshulker`, replacing the original mod; do not install both at once.

## Features

- Quick-open shulker boxes, including AMS 54-slot large shulkers and large ender chests sized from the actual inventory. AMS is optional. Rules are read through their public settings API without reflection, and existing high-slot contents are preserved when capacity rules change.
- Quick-open grindstones, smithing tables, cartography tables, and looms from held items or the inventory.
- Quick access to supported container items, depending on the Minecraft version: chests, barrels, hoppers, droppers, dispensers, decorated pots, crafters, furnaces, brewing stands, and copper chests and shelves in game versions that include those vanilla blocks. Menus use the containers' real slot layouts; the crafter supports recipe crafting and locking empty slots.
- Right-click a shulker box in a real container slot to open it. The server validates and protects the source slot while it is open.
- Quick containers have a master policy and per-container toggles. The default master policy allows Creative mode, and child options are enabled by default. Operators can change the server policy with commands.

## Supported versions

Fabric source and per-version build targets cover **Minecraft 1.21, 1.21.1–1.21.11, 26.1, 26.2, and 26.3**. Version 26.3 is the primary development target. The 1.21.1 maintenance target lives at the repository root; other versions are under `versions/`.

Compatible releases share one JAR for each of these ranges: **1.21–1.21.1**, **1.21.2–1.21.3**, **1.21.6–1.21.8**, and **1.21.9–1.21.10**. Other versions have individual JARs. For example, `quickshulkermore-1.0.0-mc1.21.6-1.21.8-fabric.jar` covers all three versions in its filename. CI runs server regressions, world tests, and a client class-loading/Mixin smoke test against the same shared binary on every covered runtime.

The repository also contains 26.3 NeoForge source, but the complete 15-version port and regression record for this round covers Fabric. Check the download page and validation notes for the artifact and status matching your loader.

## Installation

1. Choose a JAR from [Releases](https://github.com/halfkite/quickshulkermore/releases) that matches your Minecraft version and loader.
2. Put it in the `mods` folder for that game instance.
3. For the full quick-container feature set, install the same QuickShulkerMore version on the client and server.

Do not install the original QuickShulker alongside this mod because both use the same internal mod ID. Ordinary shulker opening retains the legacy protocol; full compatibility between mixed original/More clients and servers has not been claimed as verified.

## Build and validation

The default build target is 26.3 Fabric. Use the matching Java version:

```powershell
.\build.ps1 -Version 26.3 -JavaHome '<path to JDK 25>'
.\build.ps1 -Version 1.21.1 -JavaHome '<path to JDK 21>'
```

As of 2026-10-20, all 15 Fabric targets have independent build and automated regression records. Real AMS server testing is documented only for 1.21.1; automated tests do not replace in-game GUI checks or mixed original/More multiplayer testing.

- [Current validation and version artifact list](docs/testing/1.0.0-container-fixes.md)
- [Fabric port progress](docs/testing/1.0.0-fabric-port-progress.md)
- [1.21.1 self-test and limitations](docs/testing/1.0.0-1.21.1-self-test.md)
- [Documentation index and historical records](docs/README.md)

## Project

- Upstream: [MoRanpcy/quickshulker](https://github.com/MoRanpcy/quickshulker)
- This project: [halfkite/quickshulkermore](https://github.com/halfkite/quickshulkermore)
- Downloads: [GitHub Releases](https://github.com/halfkite/quickshulkermore/releases)

Thanks to the original QuickShulker authors and contributors, and to the Carpet AMS Addition project.
