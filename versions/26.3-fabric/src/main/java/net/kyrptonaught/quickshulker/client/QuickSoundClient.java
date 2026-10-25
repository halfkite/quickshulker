package net.kyrptonaught.quickshulker.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.kyrptonaught.quickshulker.network.QuickSoundPacket;
import net.minecraft.sounds.SoundSource;

public final class QuickSoundClient {
    private QuickSoundClient(){}
    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(QuickSoundPacket.ID,(sound,context)->{
            var client=context.client();
            if(client.level!=null && client.player!=null && QuickShulkerMod.getConfig().shouldPlayQuickSound(sound.shulker()))
                client.level.playSeededSound(client.player,sound.x(),sound.y(),sound.z(),net.minecraft.core.Holder.direct(sound.sound()),SoundSource.BLOCKS,1.0F,1.0F,sound.seed());
        });
    }
}
