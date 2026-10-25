package net.kyrptonaught.quickshulker.config;

import net.kyrptonaught.jankson.Comment;
import net.kyrptonaught.kyrptconfig.config.AbstractConfigFile;
import net.kyrptonaught.kyrptconfig.keybinding.CustomKeyBinding;
import net.kyrptonaught.quickshulker.QuickShulkerMod;

public class ConfigOptions implements AbstractConfigFile {
    public boolean quickLoom = true;
    public boolean quickChest = true;
    public boolean quickTrappedChest = true;
    public boolean quickCopperChest = true;
    public boolean quickBarrel = true;
    public boolean quickShelf = true;
    public boolean quickHopper = true;
    public boolean quickDropper = true;
    public boolean quickDispenser = true;
    public boolean quickDecoratedPot = true;
    public boolean quickCrafter = true;
    public boolean quickFurnace = true;
    public boolean quickSmoker = true;
    public boolean quickBlastFurnace = true;
    public boolean quickBrewingStand = true;
    public boolean quickContainers = true;
    public boolean quickContainersCreativeOnly = true;
    @Comment("Per-container creative-only bit mask; zero preserves existing child permissions")
    public int containerCreativeMask = 0;
    public boolean rightClickContainerShulker = true;
    public boolean quickGrindstone = true;
    public boolean quickSmithingTable = true;
    public boolean quickCartographyTable = true;
    public static String defualtKeybind = "key.keyboard.k";
    @Comment("Activation key")
    public CustomKeyBinding keybinding = CustomKeyBinding.configDefault(QuickShulkerMod.MOD_ID, "key.keyboard.k");
    @Comment("Open setting gui key")
    public CustomKeyBinding openSettingGui = CustomKeyBinding.configDefault(QuickShulkerMod.MOD_ID, "key.keyboard.keypad.add");
    @Comment("Right Clicking with shulker in hand opens it")
    public boolean rightClickToOpen = true;
    @Comment("Hitting the keybind with shulker in hand opens it")
    public boolean keybind = true;
    @Comment("Hitting the keybind while hovering over shulker in inv opens it")
    public boolean keybingInInv = true;
    @Comment("Right Clicking a shulker in your inv opens it")
    public boolean rightClickInv = true;
    @Comment("Check for updates")
    public boolean checkUpdate = true;

    @Comment("Right Clicking the opened shulker in your inv closes it")
    public boolean rightClickClose = false;
    @Comment("Client only: play quick-open sounds on this client")
    public boolean playSound = true;
    @Comment("Client only: silence quick shulker sounds on this client; placed blocks are unaffected")
    public boolean silentShulker = false;
    public boolean shouldPlayQuickSound(boolean shulker){return playSound && (!shulker || !silentShulker);}
    @Comment("Right Clicking a shulker with an item inserts it")
    public boolean supportsBundlingInsert = true;
    @Comment("Right Clicking an item with a shulker inserts it")
    public boolean supportsBundlingPickup = true;
    @Comment("Right Clicking a shulker with a shulker transfer items")
    public boolean supportsBundlingTransfer = true;
    @Comment("Right Clicking an empty slot with a shulker extracts an item")
    public boolean supportsBundlingExtract = true;
    @Comment("Right Clicking and Dragging with a shulker to bulk insert or extract items")
    public boolean supportsMouseDragged = true;

    @Comment("Enable opening Shulker Boxes")
    public boolean quickShulkerBox = true;
    @Comment("Enable opening Crafting Tables")
    public boolean quickCraftingTables = true;
    @Comment("Enable opening Stonecutter")
    public boolean quickStonecutter = true;
    @Comment("Enable opening EnderChest")
    public boolean quickEChest = true;
    @Comment("Enable opening Bundle(Disable it when the mod run only on server side)")
    public boolean quickBundle = true;
    @Comment("Enable opening Anvil")
    public boolean quickAnvil = true;
}
