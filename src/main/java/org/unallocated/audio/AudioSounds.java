package org.unallocated.audio;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.unallocated.UnallocatedMod;

/** Server-thread entry points for the sequencer, bait and director. Does not schedule scares. */
public final class AudioSounds {
    private AudioSounds() {}

    public static void initialize() {
        int events = 0;
        for (AudioCue cue : AudioCue.values()) {
            for (AudioCue.Layer layer : cue.layers()) {
                Registry.register(BuiltInRegistries.SOUND_EVENT, layer.sound().location(), layer.sound());
                events++;
            }
        }
        PayloadTypeRegistry.clientboundPlay().register(AudioPayload.TYPE, AudioPayload.CODEC);
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            var audio = Commands.literal("audio")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
            for (AudioCue cue : AudioCue.values()) {
                audio.then(Commands.literal(cue.id()).executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (cue.loud()) context.getSource().sendSuccess(
                            () -> Component.literal("Audio: golpe fuerte y repentino (screamer)."), false);
                    return play(player, cue, player.getEyePosition().add(2, 0, 0)) ? 1 : 0;
                }));
            }
            audio.then(Commands.literal("stop").executes(context -> {
                stop(context.getSource().getPlayerOrException());
                return 1;
            }));
            dispatcher.register(Commands.literal("unallocated").then(audio));
        });
        UnallocatedMod.LOGGER.info("UNALLOCATED audio: registered {} sound events; loud=screamer.", events);
    }

    public static boolean play(ServerPlayer player, String cueId, Vec3 position) {
        return play(player, AudioCue.byId(cueId), position);
    }

    public static boolean play(ServerPlayer player, AudioCue cue, Vec3 position) {
        if (!ServerPlayNetworking.canSend(player, AudioPayload.TYPE)) return false;
        ServerPlayNetworking.send(player, new AudioPayload(cue.id(), position.x, position.y, position.z));
        UnallocatedMod.LOGGER.info("UNALLOCATED audio: sent cue={} loud={}", cue.id(), cue.loud());
        return true;
    }

    public static void stop(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, AudioPayload.TYPE)) {
            ServerPlayNetworking.send(player, new AudioPayload("stop", 0, 0, 0));
        }
    }
}
