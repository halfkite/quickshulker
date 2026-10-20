package net.kyrptonaught.quickshulker.command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.kyrptonaught.quickshulker.QuickShulker;
import net.kyrptonaught.quickshulker.config.ContainerKind;
import net.kyrptonaught.quickshulker.container.PortableContainers;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
public final class QuickShulkerCommands{
    private QuickShulkerCommands(){}
    public static void register(RegisterCommandsEvent event){register(event.getDispatcher());}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        var root=Commands.literal("quickshulker").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        root.then(Commands.literal("quickContainers")
            .executes(c->{var o=QuickShulker.getConfig();c.getSource().sendSuccess(()->Component.literal("quickContainers = "+(!o.quickContainers?"false":o.quickContainersCreativeOnly?"creative":"true")),false);return 1;})
            .then(Commands.argument("value",StringArgumentType.word()).suggests((c,b)->{
                b.suggest("creative");b.suggest("true");b.suggest("false");return b.buildFuture();
            }).executes(c->{
                String value=StringArgumentType.getString(c,"value");
                if(!value.equals("creative")&&!value.equals("true")&&!value.equals("false")){
                    c.getSource().sendFailure(Component.literal("Expected creative, true or false"));return 0;
                }
                var o=QuickShulker.getConfig();o.quickContainers=!value.equals("false");o.quickContainersCreativeOnly=value.equals("creative");
                return saved(c.getSource(),"quickContainers",value);
            })));
        root.then(bool("quickContainersCreativeOnly",null));
        for(var kind:ContainerKind.values())root.then(bool(kind.key,kind));
        dispatcher.register(root);
    }
    private static LiteralArgumentBuilder<CommandSourceStack> bool(String name,ContainerKind kind){
        return Commands.literal(name).executes(c->{
            boolean v=kind==null?QuickShulker.getConfig().quickContainersCreativeOnly:kind.enabled(QuickShulker.getConfig());
            c.getSource().sendSuccess(()->Component.literal(name+" = "+v),false);return 1;
        }).then(Commands.argument("value",BoolArgumentType.bool()).executes(c->{
            boolean v=BoolArgumentType.getBool(c,"value");
            if(kind==null)QuickShulker.getConfig().quickContainersCreativeOnly=v;else kind.set(QuickShulker.getConfig(),v);
            return saved(c.getSource(),name,Boolean.toString(v));
        }));
    }
    private static int saved(CommandSourceStack source,String name,String value){
        QuickShulker.config.save();PortableContainers.refresh(source.getServer());
        source.sendSuccess(()->Component.literal(name+" = "+value),true);return 1;
    }
}
