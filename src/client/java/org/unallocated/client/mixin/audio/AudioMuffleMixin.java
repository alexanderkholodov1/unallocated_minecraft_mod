package org.unallocated.client.mixin.audio;

import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unallocated.client.effects.SoundEffects;

/** Preserves vanilla category, biome and master gains, including the player's mute settings. */
@Mixin(SoundEngine.class)
abstract class AudioMuffleMixin {
    @Inject(method = "calculateVolume(FLnet/minecraft/sounds/SoundSource;)F", at = @At("RETURN"), cancellable = true)
    private void audio$attenuate(float volume, SoundSource source, CallbackInfoReturnable<Float> result) {
        result.setReturnValue(result.getReturnValueF() * SoundEffects.muffleGain());
    }
}
