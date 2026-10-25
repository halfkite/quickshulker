package net.kyrptonaught.quickshulker.soundtest;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
public final class SoundClient implements ClientModInitializer {
    private static final String MODE=System.getProperty("quickshulker.sound.mode");
    private static int quick,placed,phase=-1;
    private boolean connecting;
    private final long started=System.nanoTime();
    public void onInitializeClient(){
        if(FabricLoader.getInstance().isModLoaded("quickshulker")==MODE.equals("plain"))fail("Wrong client mod set");
        ClientReceiveMessageEvents.GAME.register((message,overlay)->{
            String text=message.getString();
            if(text.startsWith("SOUND_PHASE:")){
                phase=Integer.parseInt(text.substring(12));quick=placed=0;
                if(!MODE.equals("plain"))configure(phase);
                Minecraft.getInstance().getConnection().sendCommand("qssoundready");
            }else if(text.equals("SOUND_CHECK")){
                int expected=phase==1?0:MODE.equals("quiet")&&(phase==0||phase==3)?0:phase==3?4:2;
                int expectedPlaced=phase==1?2:0;
                if(quick!=expected||placed!=expectedPlaced)fail("phase="+phase+" quick="+quick+" expected="+expected+" placed="+placed+" expected="+expectedPlaced);
                System.out.println("SOUND_CLIENT_PASS: "+MODE+" phase="+phase+" quick="+quick+" placed="+placed);
                Minecraft.getInstance().getConnection().sendCommand("qssoundack");
            }else if(text.equals("SOUND_SUCCESS")){
                System.out.println("SOUND_CLIENT_SUCCESS: "+MODE);System.exit(0);
            }else if(text.startsWith("SOUND_FAIL"))fail(text);
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(System.nanoTime()-started>java.time.Duration.ofMinutes(6).toNanos())fail("Timeout phase="+phase);
            if(client.gui.screen() instanceof DisconnectedScreen)fail("Disconnected phase="+phase);
            if(!connecting&&client.gui.screen()!=null&&client.gui.overlay()==null){
                connecting=true;
                ConnectScreen.startConnecting(new TitleScreen(),client,ServerAddress.parseString("127.0.0.1:25590"),new ServerData("Sound regression","127.0.0.1:25590",ServerData.Type.OTHER),false,null);
            }
        });
    }
    private static void configure(int phase){
        var c=net.kyrptonaught.quickshulker.QuickShulkerMod.getConfig();
        c.playSound=!MODE.equals("quiet")||phase!=3;
        c.silentShulker=MODE.equals("quiet")&&phase<4;
    }
    public static void observe(SoundInstance sound){
        if(phase<0||sound.getSource()!=SoundSource.BLOCKS)return;
        String id=sound.getIdentifier().toString();
        if(!id.startsWith("minecraft:block.shulker_box.")&&!id.startsWith("minecraft:block.ender_chest."))return;
        if(sound.getZ()>3)placed++;else quick++;
    }
    private static void fail(String text){System.err.println("SOUND_CLIENT_FAILURE: "+MODE+" "+text);System.exit(1);}
}
