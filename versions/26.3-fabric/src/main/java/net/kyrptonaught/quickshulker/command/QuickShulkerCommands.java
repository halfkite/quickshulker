package net.kyrptonaught.quickshulker.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.kyrptonaught.quickshulker.network.ContainerPolicyPacket;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class QuickShulkerCommands {
    private QuickShulkerCommands() {}
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(
            Commands.literal("quickshulker").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(option("quickContainers", false))
                .then(option("quickContainersCreativeOnly", true))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack>
            option(String name, boolean creativeOnly) {
        return Commands.literal(name)
            .executes(context -> {
                boolean value = creativeOnly ? QuickShulkerMod.getConfig().quickContainersCreativeOnly
                    : QuickShulkerMod.getConfig().quickContainers;
                context.getSource().sendSuccess(() -> Component.literal(name + " = " + value), false);
                return 1;
            })
            .then(Commands.argument("value", BoolArgumentType.bool()).executes(context -> {
                boolean value = BoolArgumentType.getBool(context, "value");
                if (creativeOnly) QuickShulkerMod.getConfig().quickContainersCreativeOnly = value;
                else QuickShulkerMod.getConfig().quickContainers = value;
                QuickShulkerMod.config.save();
                for (var player : context.getSource().getServer().getPlayerList().getPlayers()) {
                    if (player.containerMenu instanceof net.kyrptonaught.quickshulker.container.PortableStorageHandler
                            && net.kyrptonaught.quickshulker.container.PortableContainers.supports(
                                ((net.kyrptonaught.quickshulker.api.ItemInventoryContainer) player.containerMenu).getSourceStack())
                            && !net.kyrptonaught.quickshulker.container.PortableContainers.allowed(player)) player.closeContainer();
                    ContainerPolicyPacket.send(player);
                }
                context.getSource().sendSuccess(() -> Component.literal(name + " = " + value), true);
                return 1;
            }));
    }
}
