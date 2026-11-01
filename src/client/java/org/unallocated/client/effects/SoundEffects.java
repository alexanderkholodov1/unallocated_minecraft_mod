package org.unallocated.client.effects;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.unallocated.UnallocatedMod;
import org.unallocated.audio.AudioCue;
import org.unallocated.audio.AudioPayload;

/** Client-thread audio API. Never changes or saves Options; silence remains silence. */
public final class SoundEffects {
    private static final Map<AudioCue, List<AudioLayerSound>> ACTIVE = new EnumMap<>(AudioCue.class);
    private static int muffleTicks;

    private SoundEffects() {}

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(AudioPayload.TYPE, (payload, context) -> {
            if (payload.cue().equals("stop")) clearAll();
            else play(AudioCue.byId(payload.cue()), new Vec3(payload.x(), payload.y(), payload.z()));
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null) {
                if (!ACTIVE.isEmpty() || muffleTicks > 0) clearAll();
                return;
            }
            if (muffleTicks > 0) {
                // Also expires while paused: a pause must never leave the whole game muffled.
                muffleTicks--;
                client.getSoundManager().refreshCategoryVolume(SoundSource.MASTER);
            }
            ACTIVE.values().forEach(sounds -> sounds.removeIf(sound -> {
                sound.expireTick();
                if (!sound.isStopped()) return false;
                client.getSoundManager().stop(sound);
                return true;
            }));
            ACTIVE.values().removeIf(List::isEmpty);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clearAll());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> clearAll());
    }

    /** Other client effects may call by name after checking their session/consent and scare budget. */
    public static void play(String cueId, Vec3 position) { play(AudioCue.byId(cueId), position); }

    public static void play(AudioCue cue, Vec3 position) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        stop(cue);
        if (cue == AudioCue.MUFFLE) {
            muffleTicks = cue.durationTicks();
            client.getSoundManager().refreshCategoryVolume(SoundSource.MASTER);
        }
        List<AudioLayerSound> sounds = new ArrayList<>();
        for (AudioCue.Layer layer : cue.layers()) {
            AudioLayerSound sound = new AudioLayerSound(cue, layer, position);
            SoundEngine.PlayResult result = client.getSoundManager().play(sound);
            if (result != SoundEngine.PlayResult.NOT_STARTED) sounds.add(sound);
            UnallocatedMod.LOGGER.info("UNALLOCATED audio: play cue={} event={} loud={} result={}",
                    cue.id(), layer.sound().location(), cue.loud(), result);
        }
        ACTIVE.put(cue, sounds);
    }

    /** Low gain for 300 ms, then a 300 ms recovery. This is attenuation, not an EQ filter. */
    public static float muffleGain() {
        return muffleTicks == 0 ? 1f : muffleTicks >= 6 ? 0.2f : 1f - 0.8f * muffleTicks / 6f;
    }

    public static void stop(AudioCue cue) {
        var sounds = ACTIVE.remove(cue);
        if (sounds != null) sounds.forEach(sound -> {
            sound.finish();
            Minecraft.getInstance().getSoundManager().stop(sound);
        });
        if (cue == AudioCue.MUFFLE && muffleTicks > 0) {
            muffleTicks = 0;
            Minecraft.getInstance().getSoundManager().refreshCategoryVolume(SoundSource.MASTER);
        }
    }

    public static void clearAll() {
        for (AudioCue cue : AudioCue.values()) stop(cue);
    }
}
