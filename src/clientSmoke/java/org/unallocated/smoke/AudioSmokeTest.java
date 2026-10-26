package org.unallocated.smoke;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.slf4j.LoggerFactory;
import org.unallocated.audio.AudioCue;
import org.unallocated.client.effects.SoundEffects;

/** Real sound engine, bundled resources, server commands and S2C transport. No external audio device needed. */
final class AudioSmokeTest {
    private AudioSmokeTest() {}

    static void run(ClientGameTestContext context) {
        Map<Identifier, List<SoundInstance>> played = new HashMap<>();
        Map<SoundSource, Double> originalOptions = new EnumMap<>(SoundSource.class);
        SoundEventListener listener = (sound, event, range) -> {
            if (sound.getIdentifier().getNamespace().equals("unallocated")) {
                played.computeIfAbsent(sound.getIdentifier(), key -> new ArrayList<>()).add(sound);
            }
        };
        context.runOnClient(client -> {
            for (SoundSource source : SoundSource.values()) {
                originalOptions.put(source, client.options.getSoundSourceOptionInstance(source).get());
                // Test-only: the harness may start muted. Restore these options in finally.
                client.options.getSoundSourceOptionInstance(source).set(1.0);
            }
            client.getSoundManager().refreshCategoryVolume(SoundSource.MASTER);
            client.getSoundManager().addListener(listener);
            int count = 0;
            for (AudioCue cue : AudioCue.values()) {
                for (AudioCue.Layer layer : cue.layers()) {
                    if (BuiltInRegistries.SOUND_EVENT.getValue(layer.sound().location()) != layer.sound()) {
                        throw new AssertionError("Audio event is not registered: " + layer.sound());
                    }
                    var resolved = client.getSoundManager().getSoundEvent(layer.sound().location());
                    if (resolved == null || resolved.getWeight() <= 0) {
                        throw new AssertionError("Empty/missing audio alias: " + layer.sound());
                    }
                    count++;
                }
            }
            if (count != 7) throw new AssertionError("Expected seven registered audio layers");
        });
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksDownload();
            context.waitFor(client -> client.level != null && client.player != null);
            byte[] optionsBefore = context.computeOnClient(AudioSmokeTest::optionsBytes);
            for (AudioCue cue : AudioCue.values()) {
                world.getServer().runCommand("execute as @p at @s run unallocated audio " + cue.id());
                world.getConnection().waitForClientboundPackets();
                context.waitFor(client -> cue.layers().stream().allMatch(layer -> played.containsKey(layer.sound().location())));
                context.runOnClient(client -> {
                    for (AudioCue.Layer layer : cue.layers()) {
                        SoundInstance sound = played.get(layer.sound().location()).getLast();
                        if (!client.getSoundManager().isActive(sound)) {
                            throw new AssertionError("Sound engine did not activate " + layer.sound());
                        }
                        if ((cue == AudioCue.THREADS) == sound.isRelative()) {
                            throw new AssertionError("Incorrect spatialization for " + cue);
                        }
                        if (sound.getVolume() > 0.55f || sound.getPitch() < 0.5f) {
                            throw new AssertionError("Audio exceeded authored gain/pitch bounds");
                        }
                    }
                    if (cue == AudioCue.MUFFLE) {
                        float gain = SoundEffects.muffleGain();
                        if (gain >= 1f) throw new AssertionError("Muffle did not start");
                        for (SoundSource source : SoundSource.values()) {
                            assertNear(engineVolume(client, source), 0.8f * gain);
                        }
                    }
                });
                context.waitTicks(cue == AudioCue.AMBIENT ? 4 : cue.durationTicks() + 3);
                if (cue != AudioCue.AMBIENT) context.runOnClient(client -> {
                    for (AudioCue.Layer layer : cue.layers()) {
                        var sound = played.get(layer.sound().location()).getLast();
                        if (!((TickableSoundInstance) sound).isStopped()) {
                            throw new AssertionError("One-shot did not end: " + cue);
                        }
                    }
                    assertNear(SoundEffects.muffleGain(), 1f);
                    assertNear(engineVolume(client, SoundSource.AMBIENT), 0.8f);
                });
                LoggerFactory.getLogger("unallocated-smoke").info(
                        "UNALLOCATED smoke: audio cue={} registered, resolved and active.", cue.id());
            }
            // Repeated bed requests must replace the previous loops, including delayed cave layers.
            world.getServer().runCommand("execute as @p at @s run unallocated audio ambient");
            world.getConnection().waitForClientboundPackets();
            context.waitFor(client -> played.get(AudioCue.AMBIENT.layers().getFirst().sound().location()).size() >= 2);
            context.runOnClient(client -> {
                for (AudioCue.Layer layer : AudioCue.AMBIENT.layers()) {
                    if (!((TickableSoundInstance) played.get(layer.sound().location()).getFirst()).isStopped()) {
                        throw new AssertionError("Previous ambient loop can restart after replacement");
                    }
                }
            });
            world.getServer().runCommand("execute as @p at @s run unallocated audio stop");
            world.getConnection().waitForClientboundPackets();
            context.runOnClient(client -> {
                assertStopped(played);
                // Reflection evaluates the actual private engine method: this fails if the mixin is absent.
                assertNear(engineVolume(client, SoundSource.AMBIENT), 0.8f);
                if (!Arrays.equals(optionsBefore, optionsBytes(client))) {
                    throw new AssertionError("Audio wrote options.txt");
                }
                for (SoundSource source : SoundSource.values()) {
                    if (client.options.getSoundSourceVolume(source) != 1f) {
                        throw new AssertionError("Audio changed an Options sound slider");
                    }
                }
                SoundEffects.play(AudioCue.MUFFLE, client.player.getEyePosition());
                assertNear(engineVolume(client, SoundSource.HOSTILE), 0.16f);
                client.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
                assertNear(engineVolume(client, SoundSource.HOSTILE), 0f);
                client.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(1.0);
                client.options.getSoundSourceOptionInstance(SoundSource.AMBIENT).set(0.0);
                assertNear(engineVolume(client, SoundSource.AMBIENT), 0f);
                client.options.getSoundSourceOptionInstance(SoundSource.AMBIENT).set(1.0);
                SoundEffects.play(AudioCue.AMBIENT, client.player.getEyePosition());
            });
        } finally {
            context.runOnClient(client -> {
                client.getSoundManager().removeListener(listener);
                originalOptions.forEach((source, value) -> client.options.getSoundSourceOptionInstance(source).set(value));
                client.getSoundManager().refreshCategoryVolume(SoundSource.MASTER);
            });
        }
        context.runOnClient(client -> {
            assertNear(SoundEffects.muffleGain(), 1f);
            assertStopped(played);
        });
        LoggerFactory.getLogger("unallocated-smoke").info(
                "UNALLOCATED smoke: audio attenuation, expiry, replacement, stop, disconnect and options verified.");
    }

    private static void assertStopped(Map<Identifier, List<SoundInstance>> played) {
        for (var sounds : played.values()) for (SoundInstance sound : sounds) {
            if (!((TickableSoundInstance) sound).isStopped()) throw new AssertionError("Audio left running");
        }
    }

    private static byte[] optionsBytes(Minecraft client) throws Exception {
        Path options = client.gameDirectory.toPath().resolve("options.txt");
        return Files.exists(options) ? Files.readAllBytes(options) : new byte[0];
    }

    private static float engineVolume(Minecraft client, SoundSource source) throws Exception {
        Field field = SoundManager.class.getDeclaredField("soundEngine");
        field.setAccessible(true);
        SoundEngine engine = (SoundEngine) field.get(client.getSoundManager());
        Method method = SoundEngine.class.getDeclaredMethod("calculateVolume", float.class, SoundSource.class);
        method.setAccessible(true);
        return (float) method.invoke(engine, 0.8f, source);
    }

    private static void assertNear(float actual, float expected) {
        if (Math.abs(actual - expected) > 0.001f) {
            throw new AssertionError("Audio gain " + actual + " != " + expected);
        }
    }
}
