package net.kyrptonaught.quickshulker.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.kyrptonaught.quickshulker.network.ContainerPolicyPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

public final class QuickShulkerCommands {
    private QuickShulkerCommands() {}
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(
            CommandManager.literal("quickshulker").requires(source -> source.hasPermissionLevel(2))
                .then(option("quickContainers", false))
                .then(option("quickContainersCreativeOnly", true))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.server.command.ServerCommandSource>
            option(String name, boolean creativeOnly) {
        return CommandManager.literal(name)
            .executes(context -> {
                boolean value = creativeOnly ? QuickShulkerMod.getConfig().quickContainersCreativeOnly
                    : QuickShulkerMod.getConfig().quickContainers;
                context.getSource().sendFeedback(() -> Text.literal(name + " = " + value), false);
                return 1;
            })
            .then(CommandManager.argument("value", BoolArgumentType.bool()).executes(context -> {
                boolean value = BoolArgumentType.getBool(context, "value");
                if (creativeOnly) QuickShulkerMod.getConfig().quickContainersCreativeOnly = value;
                else QuickShulkerMod.getConfig().quickContainers = value;
                QuickShulkerMod.config.save();
                for (var player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
                    if (player.currentScreenHandler instanceof net.kyrptonaught.quickshulker.container.PortableStorageHandler
                            && net.kyrptonaught.quickshulker.container.PortableContainers.supports(
                                ((net.kyrptonaught.quickshulker.api.ItemInventoryContainer) player.currentScreenHandler).getSourceStack())
                            && !net.kyrptonaught.quickshulker.container.PortableContainers.allowed(player)) player.closeHandledScreen();
                    ContainerPolicyPacket.send(player);
                }
                context.getSource().sendFeedback(() -> Text.literal(name + " = " + value), true);
                return 1;
            }));
    }
}
