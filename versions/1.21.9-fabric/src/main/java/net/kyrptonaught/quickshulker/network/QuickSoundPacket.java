package net.kyrptonaught.quickshulker.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundCategory;
import net.minecraft.entity.player.PlayerEntity;

/** Marks quick-open sounds without registering new sound events or sharing listener preferences. */
public record QuickSoundPacket(SoundEvent sound,double x,double y,double z,long seed,boolean shulker) implements CustomPayload {
    public static final Id<QuickSoundPacket> ID=new Id<>(Identifier.of("quickshulker","quick_sound_v1"));
    public static final PacketCodec<PacketByteBuf,QuickSoundPacket> CODEC=PacketCodec.of(
        (p,b)->{SoundEvent.PACKET_CODEC.encode(b,p.sound);b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);b.writeLong(p.seed);b.writeBoolean(p.shulker);},
        b->new QuickSoundPacket(SoundEvent.PACKET_CODEC.decode(b),b.readDouble(),b.readDouble(),b.readDouble(),b.readLong(),b.readBoolean()));
    public static void register(){PayloadTypeRegistry.playS2C().register(ID,CODEC);}
    public static boolean inRange(double distanceSquared,float range){return distanceSquared < (double)range*range;}
    public static void broadcast(PlayerEntity actor,SoundEvent sound,boolean shulker){
        if(sound==null || !(actor instanceof ServerPlayerEntity player))return;
        var level=player.getEntityWorld();
        var payload=new QuickSoundPacket(sound,actor.getX(),actor.getY(),actor.getZ(),level.getRandom().nextLong(),shulker);
        var vanilla=new PlaySoundS2CPacket(RegistryEntry.of(sound),SoundCategory.BLOCKS,payload.x,payload.y,payload.z,1.0F,1.0F,payload.seed);
        for(var listener:level.getPlayers()){
            if(!inRange(listener.squaredDistanceTo(payload.x,payload.y,payload.z),sound.getDistanceToTravel(1.0F)))continue;
            if(ServerPlayNetworking.canSend(listener,ID))ServerPlayNetworking.send(listener,payload);
            else listener.networkHandler.sendPacket(vanilla);
        }
    }
    @Override public Id<? extends CustomPayload> getId(){return ID;}
}
