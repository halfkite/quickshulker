package net.kyrptonaught.quickshulker.soundtest.mixin;
import net.kyrptonaught.quickshulker.soundtest.SoundClient;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(SoundManager.class)
public class SoundObserverMixin {
    @Inject(method="play",at=@At("HEAD"))
    private void observe(SoundInstance sound,CallbackInfoReturnable<?> callback){SoundClient.observe(sound);}
}
