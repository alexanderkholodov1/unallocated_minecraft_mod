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

    // AbstractSoundInstance multiplies these fields by the resolved vanilla/resource-pack
    // sample's gain and pitch. Apply authored bounds AFTER that resolution, retaining fades.
    @Override
    public float getVolume() {
        return isStopped() ? 0f : Math.clamp(super.getVolume(), 0f, Math.max(0f, volume));
    }

    @Override
    public float getPitch() { return Math.clamp(super.getPitch(), 0.5f, 2f); }

    void finish() {
        volume = 0f;
        stop();
    }

    // A short vanilla sample can finish before SoundEngine ticks it again. Keep the logical lifetime
    // bounded independently, including while paused, so bookkeeping never retains expired layers.
    void expireTick() {
        if (isStopped()) return;
        elapsedTicks++;
        if (cue.durationTicks() > 0 && elapsedTicks >= cue.durationTicks()) finish();
    }

    @Override
    public void tick() {
        // Vanilla stop() clears looping, and the engine can still tick a retired instance.
        // It must never fall through to the finite-cue envelope with ambient's zero duration.
        if (isStopped()) return;
        age++;
        if (Minecraft.getInstance().level == null
                || (cue.durationTicks() > 0 && age >= cue.durationTicks())) {
            finish();
            return;
        }
        if (cue == AudioCue.AMBIENT) {
            volume = targetVolume * Math.min(1f, (age + 1) / 40f);
        } else {
            int fadeTicks = cue.loud() ? 4 : 10;
            volume = targetVolume * Math.clamp((cue.durationTicks() - age) / (float) fadeTicks, 0f, 1f);
        }
    }
}
