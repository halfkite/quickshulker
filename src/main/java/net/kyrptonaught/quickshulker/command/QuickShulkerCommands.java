package net.kyrptonaught.quickshulker.command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.kyrptonaught.quickshulker.config.ContainerKind;
import net.kyrptonaught.quickshulker.container.PortableContainers;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
public final class QuickShulkerCommands{
    private QuickShulkerCommands(){}
    public static void register(){CommandRegistrationCallback.EVENT.register((d,r,e)->register(d));}
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher){
        var root=CommandManager.literal("quickshulker").requires(source -> source.hasPermissionLevel(2));
        root.then(CommandManager.literal("quickContainers")
            .executes(c->{var o=QuickShulkerMod.getConfig();c.getSource().sendFeedback(()->Text.literal("quickContainers = "+(!o.quickContainers?"false":o.quickContainersCreativeOnly?"creative":"true")),false);return 1;})
            .then(CommandManager.argument("value",StringArgumentType.word()).suggests((c,b)->{
                b.suggest("creative");b.suggest("true");b.suggest("false");return b.buildFuture();
            }).executes(c->{
                String value=StringArgumentType.getString(c,"value");
                if(!value.equals("creative")&&!value.equals("true")&&!value.equals("false")){
                    c.getSource().sendError(Text.literal("Expected creative, true or false"));return 0;
                }
                var o=QuickShulkerMod.getConfig();o.quickContainers=!value.equals("false");o.quickContainersCreativeOnly=value.equals("creative");
                return saved(c.getSource(),"quickContainers",value);
            })));
        root.then(bool("quickContainersCreativeOnly",null));
        for(var kind:ContainerKind.values())root.then(mode(kind));
        dispatcher.register(root);
    }
    private static LiteralArgumentBuilder<ServerCommandSource> mode(ContainerKind kind){
        return CommandManager.literal(kind.key).executes(c->{
            int mode=kind.mode(QuickShulkerMod.getConfig());
            c.getSource().sendFeedback(()->Text.literal(kind.key+" = "+(mode==0?"creative":mode==1?"true":"false")),false);return 1;
        }).then(CommandManager.argument("value",StringArgumentType.word()).suggests((c,b)->{
            b.suggest("creative");b.suggest("true");b.suggest("false");return b.buildFuture();
        }).executes(c->{
            String value=StringArgumentType.getString(c,"value");
            int mode=switch(value){case "creative"->0;case "true"->1;case "false"->2;default->-1;};
            if(mode<0){c.getSource().sendError(Text.literal("Expected creative, true or false"));return 0;}
            kind.setMode(QuickShulkerMod.getConfig(),mode);return saved(c.getSource(),kind.key,value);
        }));
    }
    private static LiteralArgumentBuilder<ServerCommandSource> bool(String name,ContainerKind kind){
        return CommandManager.literal(name).executes(c->{
            boolean v=kind==null?QuickShulkerMod.getConfig().quickContainersCreativeOnly:kind.enabled(QuickShulkerMod.getConfig());
            c.getSource().sendFeedback(()->Text.literal(name+" = "+v),false);return 1;
        }).then(CommandManager.argument("value",BoolArgumentType.bool()).executes(c->{
            boolean v=BoolArgumentType.getBool(c,"value");
            if(kind==null)QuickShulkerMod.getConfig().quickContainersCreativeOnly=v;else kind.set(QuickShulkerMod.getConfig(),v);
            return saved(c.getSource(),name,Boolean.toString(v));
        }));
    }
    private static int saved(ServerCommandSource source,String name,String value){
        QuickShulkerMod.config.save();PortableContainers.refresh(source.getServer());
        source.sendFeedback(()->Text.literal(name+" = "+value),true);return 1;
    }
}
