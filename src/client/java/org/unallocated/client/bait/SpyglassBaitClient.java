package org.unallocated.client.bait;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Mannequin;
import org.unallocated.UnallocatedMod;
import org.unallocated.bait.SpyglassBaitSignals;

/**
 * Client half of the spyglass bait's screamer. The figure itself lunges into the player's face in the world (server
 * side); here: one hard black cut that hides its last approach, then a red stain and torn scanlines that fade within
 * 0.7 s, with three vanilla sounds pitched down at once (the enderman's stare, the player's own hurt voice, the elder
 * guardian's curse). Short on purpose, and covered by the contract's warnings ("sonidos fuertes, sustos"): one cut,
 * no strobing. Also lets mannequins render up to {@link #FAR_RENDER_BLOCKS}, so the figure can be a distant speck
 * (vanilla culls a body-sized entity at about 64 blocks).
 */
public final class SpyglassBaitClient {
    public static final double FAR_RENDER_BLOCKS = 160;
    static final long CUT_NANOS = 110_000_000L;
    static final long SCARE_NANOS = 700_000_000L;
    private static final long STALE_NANOS = 2_000_000_000L;
    private static final float[][] TEARS = {{0.18f, 0.035f}, {0.47f, 0.012f}, {0.71f, 0.05f}};

    private static long consumed;
    private static long startedNanos = Long.MIN_VALUE;
    private static int scaresShown;

    private SpyglassBaitClient() { }

    /** Called by the HUD mixin every frame, after vanilla drew the HUD. */
    public static void extractOverlay(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        long now = System.nanoTime();
        SpyglassBaitSignals.Scare scare = SpyglassBaitSignals.latest();
        if (scare != null && scare.sequence() != consumed) {
            consumed = scare.sequence();
            if (scare.player().equals(player.getUUID()) && now - scare.nanos() < STALE_NANOS) start(minecraft, now);
        }
        long elapsed = now - startedNanos;
        if (startedNanos == Long.MIN_VALUE || elapsed < 0 || elapsed > SCARE_NANOS) return;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (elapsed < CUT_NANOS) {
            graphics.fill(0, 0, width, height, 0xFF000000);
            return;
        }
        float fade = 1.0f - (float) (elapsed - CUT_NANOS) / (SCARE_NANOS - CUT_NANOS);
        graphics.fill(0, 0, width, height, (int) (fade * 0x70) << 24 | 0x5A0000);
        if (fade > 0.55f) {
            for (float[] tear : TEARS) {
                int top = (int) (tear[0] * height);
                graphics.fill(0, top, width, top + Math.max(1, (int) (tear[1] * height)), 0xE0000000);
            }
        }
    }

    private static void start(Minecraft minecraft, long now) {
        startedNanos = now;
        scaresShown++;
        play(minecraft, SoundEvents.ENDERMAN_STARE, 0.55f, 1.0f);
        play(minecraft, SoundEvents.PLAYER_HURT, 0.5f, 1.0f);
        play(minecraft, SoundEvents.ELDER_GUARDIAN_CURSE, 0.75f, 0.9f);
        UnallocatedMod.LOGGER.info("UNALLOCATED bait screamer shown");
    }

    private static void play(Minecraft minecraft, SoundEvent event, float pitch, float volume) {
        minecraft.getSoundManager().play(new ScreamLayer(event, pitch, volume));
    }

    /** Mannequins stay visible to {@link #FAR_RENDER_BLOCKS}; everything else keeps vanilla culling. */
    public static boolean renderFar(Entity entity, double distanceSquared) {
        return entity instanceof Mannequin && distanceSquared < FAR_RENDER_BLOCKS * FAR_RENDER_BLOCKS;
    }

    /** True while the screamer image is on screen (game tests take their screenshot then). */
    public static boolean scareVisible() {
        long elapsed = System.nanoTime() - startedNanos;
        return startedNanos != Long.MIN_VALUE && elapsed >= 0 && elapsed <= SCARE_NANOS;
    }

    public static int scaresShown() {
        return scaresShown;
    }

    /** A vanilla sound at the listener's head, at full volume regardless of where the player stands. */
    private static final class ScreamLayer extends AbstractSoundInstance {
        ScreamLayer(SoundEvent event, float pitch, float volume) {
            super(event, SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
            this.pitch = pitch;
            this.volume = volume;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }
    }
}
