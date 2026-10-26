package org.unallocated.client.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.unallocated.audio.AudioCue;

/** Bounded one-shots and a quiet bed; no long scream tails or stacking loops. */
final class AudioLayerSound extends AbstractTickableSoundInstance {
    private final AudioCue cue;
    private final float targetVolume;
    private int age;
    private int elapsedTicks;

    AudioLayerSound(AudioCue cue, AudioCue.Layer layer, Vec3 position) {
        super(layer.sound(), cue.loud() ? SoundSource.HOSTILE : SoundSource.AMBIENT, RandomSource.create());
        this.cue = cue;
        targetVolume = layer.volume();
        pitch = layer.pitch();
        looping = cue == AudioCue.AMBIENT;
        delay = layer.loopDelay();
        relative = cue != AudioCue.THREADS;
        attenuation = relative ? SoundInstance.Attenuation.NONE : SoundInstance.Attenuation.LINEAR;
        x = relative ? 0 : position.x;
        y = relative ? 0 : position.y;
        z = relative ? 0 : position.z;
        volume = looping ? targetVolume / 40 : targetVolume;
    }

    void finish() { stop(); }

    // A short vanilla sample can finish before SoundEngine ticks it again. Keep the logical lifetime
    // bounded independently, including while paused, so bookkeeping never retains expired layers.
    void expireTick() {
        elapsedTicks++;
        if (cue.durationTicks() > 0 && elapsedTicks >= cue.durationTicks()) stop();
    }

    @Override
    public void tick() {
        age++;
        if (Minecraft.getInstance().level == null
                || (cue.durationTicks() > 0 && age >= cue.durationTicks())) {
            stop();
            return;
        }
        if (looping) {
            volume = targetVolume * Math.min(1f, (age + 1) / 40f);
        } else {
            int fadeTicks = cue.loud() ? 4 : 10;
            volume = targetVolume * Math.min(1f, (cue.durationTicks() - age) / (float) fadeTicks);
        }
    }
}
