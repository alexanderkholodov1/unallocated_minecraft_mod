package org.unallocated.audio;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Named audio contract. Only SCREAMER is a loud/startling cue. */
public enum AudioCue {
    AMBIENT("ambient", false, 0, List.of(
            new Layer("audio.ambient_drone", 0.55f, 1.0f, 0),
            new Layer("audio.ambient_cave", 0.5f, 1.0f, 900))),
    THREADS("threads", false, 48, List.of(
            new Layer("audio.thread_string", 0.7f, 1.0f, 0),
            new Layer("audio.thread_chime", 0.35f, 1.0f, 0))),
    MUFFLE("muffle", false, 12, List.of(
            new Layer("audio.breath", 0.8f, 1.0f, 0))),
    SCREAMER("screamer", true, 10, List.of(
            new Layer("audio.screamer_impact", 1.0f, 1.0f, 0),
            new Layer("audio.screamer_crack", 0.7f, 1.0f, 0)));

    private final String id;
    private final boolean loud;
    private final int durationTicks;
    private final List<Layer> layers;

    AudioCue(String id, boolean loud, int durationTicks, List<Layer> layers) {
        this.id = id;
        this.loud = loud;
        this.durationTicks = durationTicks;
        this.layers = layers;
    }

    public String id() { return id; }
    public boolean loud() { return loud; }
    /** Zero denotes a bed that runs until explicitly stopped. */
    public int durationTicks() { return durationTicks; }
    public List<Layer> layers() { return layers; }

    public static AudioCue byId(String id) {
        for (AudioCue cue : values()) {
            if (cue.id.equals(id)) return cue;
        }
        throw new IllegalArgumentException("Unknown audio cue: " + id);
    }

    public record Layer(SoundEvent sound, float volume, float pitch, int loopDelay) {
        Layer(String path, float volume, float pitch, int loopDelay) {
            this(SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath("unallocated", path)), volume, pitch, loopDelay);
        }
    }
}
