# QuickShulkerMore

[简体中文](https://github.com/halfkite/quickshulkermore/blob/main/README.md) · [GitHub Releases](https://github.com/halfkite/quickshulkermore/releases)

QuickShulkerMore is an unofficial extension of [QuickShulker](https://github.com/MoRanpcy/quickshulker), adding Carpet AMS Addition large-container compatibility, quick workstations, and quick containers. The project follows the upstream MIT license.

The current mod version is **1.0.1**. The primary development target is Minecraft `26.3` / Fabric, and supported versions range from Minecraft `1.21` to `26.3` on Fabric.

## Original features

- Press K or right-click to open the interface for an item in your hand or inventory. Built-in support includes crafting tables, stonecutters, shulker boxes, ender chests, anvils, and bundles.
- Right-click an item while dragging a container to store it; right-click an empty inventory slot to retrieve it. Hold right-click to store or retrieve multiple items.

## Added features

- Supports Carpet AMS Addition's 54-slot `largeShulkerBox` and `largeEnderChest`.
- Adds grindstones, smithing tables, cartography tables, and looms to quick-open workstations.
- Adds quick containers: chests, barrels, hoppers, droppers, dispensers, decorated pots, crafters, furnaces, brewing stands, copper chests, and shelves. Open them from your inventory using their actual slot layouts. Crafters support crafting and locking slots. Furnaces and brewing stands accept items but only operate when placed. This feature is enabled by default in Creative mode; set it to `true` to use it in Survival.
- Right-click a shulker box inside another container to open it. The server validates and protects the source item slot.

## Installation

- Put the JAR in the instance's `mods` folder.
- Clients can join a server running QuickShulkerMore without installing the mod. For the full quick-access feature set, install the same version of QuickShulkerMore on both client and server.
- Do not install the original QuickShulker and QuickShulkerMore together; they use the same internal mod ID.

## Project

- Upstream: [MoRanpcy/quickshulker](https://github.com/MoRanpcy/quickshulker)
- This project: [halfkite/quickshulkermore](https://github.com/halfkite/quickshulkermore)
- Downloads: [GitHub Releases](https://github.com/halfkite/quickshulkermore/releases)

Thanks to the original QuickShulker authors and contributors.
