package net.kyrptonaught.quickshulker.soundtest;
import java.util.*;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.kyrptonaught.quickshulker.network.QuickSoundPacket;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.level.block.Blocks;
public final class SoundServer implements DedicatedServerModInitializer {
    private static final Set<String> ready=new HashSet<>(),acked=new HashSet<>();
    private static int phase=-1,ticks,delay;
    private static boolean sent,checking,done;
    public void onInitializeServer(){
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->{
            dispatcher.register(Commands.literal("qssoundready").executes(c->{ready.add(c.getSource().getPlayerOrException().getGameProfile().name());return 1;}));
            dispatcher.register(Commands.literal("qssoundack").executes(c->{acked.add(c.getSource().getPlayerOrException().getGameProfile().name());return 1;}));
        });
        ServerTickEvents.END_SERVER_TICK.register(SoundServer::tick);
    }
    private static void tick(MinecraftServer server){
        if(++ticks>20*60*7)throw new AssertionError("Sound test server timeout");
        var players=server.getPlayerList().getPlayers();
        if(done){if(++delay==30)server.halt(false);return;}
        if(players.size()!=3)return;
        if(phase<0){
            var level=players.getFirst().level();
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState());
            for(var player:players){
                boolean plain=player.getGameProfile().name().equals("Sound_plain");
                if(ServerPlayNetworking.canSend(player,QuickSoundPacket.ID)==plain)throw new AssertionError("Wrong sound capability for "+player.getGameProfile().name());
                player.teleportTo(0.5,100,0.5);
            }
            // Deliberately conflicting server values must not affect any listener.
            QuickShulkerMod.getConfig().playSound=false;QuickShulkerMod.getConfig().silentShulker=true;
            phase=0;announce(server);return;
        }
        if(!sent&&ready.size()==3){
            var actor=players.stream().filter(p->p.getGameProfile().name().equals("Sound_loud")).findFirst().orElseThrow();
            if(phase==1){
                actor.level().playSound(null,0.5,100,4.5,SoundEvents.SHULKER_BOX_OPEN,SoundSource.BLOCKS);
                actor.level().playSound(null,0.5,100,4.5,SoundEvents.SHULKER_BOX_CLOSE,SoundSource.BLOCKS);
            }else{
                if(phase!=2){QuickSoundPacket.broadcast(actor,SoundEvents.SHULKER_BOX_OPEN,true);QuickSoundPacket.broadcast(actor,SoundEvents.SHULKER_BOX_CLOSE,true);}
                if(phase==2||phase==3){QuickSoundPacket.broadcast(actor,SoundEvents.ENDER_CHEST_OPEN,false);QuickSoundPacket.broadcast(actor,SoundEvents.ENDER_CHEST_CLOSE,false);}
            }
            sent=true;delay=0;
        }else if(sent&&!checking&&++delay>=30){checking=true;message(server,"SOUND_CHECK");}
        if(checking&&acked.size()==3){
            System.out.println("SOUND_SERVER_PASS: phase="+phase);
            if(phase==4){message(server,"SOUND_SUCCESS");System.out.println("SOUND_SERVER_SUCCESS: three real clients, five phases");done=true;delay=0;}
            else{phase++;announce(server);}
        }
    }
    private static void announce(MinecraftServer server){ready.clear();acked.clear();sent=checking=false;delay=0;message(server,"SOUND_PHASE:"+phase);}
    private static void message(MinecraftServer server,String text){for(var player:server.getPlayerList().getPlayers())player.sendSystemMessage(Component.literal(text));}
}
