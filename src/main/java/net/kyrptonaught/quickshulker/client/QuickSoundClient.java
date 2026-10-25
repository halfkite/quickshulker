package net.kyrptonaught.quickshulker.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.kyrptonaught.quickshulker.network.QuickSoundPacket;
import net.minecraft.sound.SoundCategory;

public final class QuickSoundClient {
    private QuickSoundClient(){}
    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(QuickSoundPacket.ID,(sound,context)->{
            var client=context.client();
            if(client.world!=null && client.player!=null && QuickShulkerMod.getConfig().shouldPlayQuickSound(sound.shulker()))
                client.world.playSound(client.player,sound.x(),sound.y(),sound.z(),net.minecraft.registry.entry.RegistryEntry.of(sound.sound()),SoundCategory.BLOCKS,1.0F,1.0F,sound.seed());
        });
    }
}
