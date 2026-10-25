package net.kyrptonaught.quickshulker.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/** Marks quick-open sounds without registering new sound events or sharing listener preferences. */
public record QuickSoundPacket(SoundEvent sound,double x,double y,double z,long seed,boolean shulker) implements CustomPacketPayload {
    public static final Type<QuickSoundPacket> ID=new Type<>(Identifier.fromNamespaceAndPath("quickshulker","quick_sound_v1"));
    public static final StreamCodec<FriendlyByteBuf,QuickSoundPacket> CODEC=StreamCodec.ofMember(
        (p,b)->{SoundEvent.DIRECT_STREAM_CODEC.encode(b,p.sound);b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);b.writeLong(p.seed);b.writeBoolean(p.shulker);},
        b->new QuickSoundPacket(SoundEvent.DIRECT_STREAM_CODEC.decode(b),b.readDouble(),b.readDouble(),b.readDouble(),b.readLong(),b.readBoolean()));
    public static void register(){PayloadTypeRegistry.clientboundPlay().register(ID,CODEC);}
    public static boolean inRange(double distanceSquared,float range){return distanceSquared < (double)range*range;}
    public static void broadcast(Player actor,SoundEvent sound,boolean shulker){
        if(sound==null || !(actor instanceof ServerPlayer player))return;
        var level=player.level();
        var payload=new QuickSoundPacket(sound,actor.getX(),actor.getY(),actor.getZ(),level.getRandom().nextLong(),shulker);
        var vanilla=new ClientboundSoundPacket(Holder.direct(sound),SoundSource.BLOCKS,payload.x,payload.y,payload.z,1.0F,1.0F,payload.seed);
        for(var listener:level.players()){
            if(!inRange(listener.distanceToSqr(payload.x,payload.y,payload.z),sound.getRange(1.0F)))continue;
            if(ServerPlayNetworking.canSend(listener,ID))ServerPlayNetworking.send(listener,payload);
            else listener.connection.send(vanilla);
        }
    }
    @Override public Type<? extends CustomPacketPayload> type(){return ID;}
}
