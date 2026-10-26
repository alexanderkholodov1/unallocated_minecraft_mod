package org.unallocated.client.interfacefx;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.SignText;
import org.unallocated.client.interfacefx.InterfaceTexts.LeakLanguage;

/** Render-thread-only toolkit. The future director owns all story timing. */
public final class InterfaceEffects {
    public enum Effect { F3, DEATH, SPLASH, LOADING, ADVANCEMENT, SENSITIVITY, FOV, GAMMA, SIGN, LOG }
    public record Request(Effect effect, int ticks, int generation, double strength,
                          LeakLanguage language, long seed, BlockPos signPosition) {
        public Request {
            if (effect == null || language == null || generation < 0 || generation > 5
                    || ticks < 1 || ticks > maxTicks(effect) || !Double.isFinite(strength)
                    || strength < -1 || strength > 1 || (effect == Effect.SIGN && signPosition == null)) {
                throw new IllegalArgumentException("Invalid interface effect request");
            }
            if (signPosition != null) signPosition = signPosition.immutable();
        }
    }
    private record Active(Request request, long start, long end) { }
    private static final EnumMap<Effect, Active> ACTIVE = new EnumMap<>(Effect.class);
    private static final EnumMap<Effect, Long> LAST = new EnumMap<>(Effect.class);
    private static final Set<Effect> SETTINGS = Set.of(Effect.SENSITIVITY, Effect.FOV, Effect.GAMMA);
    private static long lastAny = Long.MIN_VALUE;
    private static final long COOLDOWN = 60_000_000_000L;
    private static final long GLOBAL_COOLDOWN = 15_000_000_000L;
    private static final Map<AdvancementToast, Long> TOASTS = new IdentityHashMap<>();
    private static final Map<String, Integer> OBSERVED = new HashMap<>();

    private InterfaceEffects() { }

    public static int maxTicks(Effect effect) {
        return switch (effect) {
            case F3 -> 60;
            case SENSITIVITY, FOV, GAMMA, SIGN -> 100;
            case ADVANCEMENT, LOG -> 100;
            case DEATH, SPLASH, LOADING -> 600;
        };
    }

    public static boolean trigger(Request request) { return trigger(request, false); }

    public static boolean debugTrigger(Request request) {
        if (!Boolean.getBoolean("unallocated.interface.debug")) return false;
        return trigger(request, true);
    }

    private static boolean trigger(Request request, boolean debug) {
        Minecraft client = Minecraft.getInstance();
        if (!client.isSameThread()) throw new IllegalStateException("Interface triggers require the client thread");
        tick();
        long now = System.nanoTime();
        if (request.effect != Effect.SPLASH && request.effect != Effect.LOADING && client.level == null) return false;
        if (!debug && ((lastAny != Long.MIN_VALUE && now - lastAny < GLOBAL_COOLDOWN)
                || (LAST.containsKey(request.effect) && now - LAST.get(request.effect) < COOLDOWN))) return false;
        if (SETTINGS.contains(request.effect)) {
            if (client.gui.screen() instanceof net.minecraft.client.gui.screens.options.OptionsScreen
                    || client.gui.screen() instanceof net.minecraft.client.gui.screens.options.OptionsSubScreen) return false;
            if (ACTIVE.keySet().stream().anyMatch(SETTINGS::contains)) return false;
            if (!InterfaceOptionsGuard.begin(client.options)) return false;
        }
        ACTIVE.put(request.effect, new Active(request, now, now + request.ticks * 50_000_000L));
        LAST.put(request.effect, now);
        lastAny = now;
        switch (request.effect) {
            case SENSITIVITY -> client.options.sensitivity().set(clamp(client.options.sensitivity().get() + request.strength * .04, 0, 1));
            case FOV -> client.options.fov().set((int) clamp(client.options.fov().get() + Math.round(request.strength * 3), 30, 110));
            case GAMMA -> applyGamma(now);
            case ADVANCEMENT -> {
                var holder = Advancement.Builder.advancement()
                        .parent(Identifier.withDefaultNamespace("story/root"))
                        .display(Items.STRING, InterfaceTexts.inventedAdvancement(request.generation, request.language, request.seed),
                                text(request, "goal"), AdvancementType.TASK, true, false, true)
                        .build(Identifier.fromNamespaceAndPath("unallocated", "interface/phantom_" + Long.toUnsignedString(request.seed)));
                var toast = new AdvancementToast(holder);
                TOASTS.put(toast, now + request.ticks * 50_000_000L);
                client.gui.toastManager().addToast(toast);
            }
            case LOG -> client.gui.hud.getChat().addMessage(text(request, "log"));
            default -> { }
        }
        return true;
    }

    private static double gammaOriginal;
    private static boolean gammaCaptured;
    private static void applyGamma(long now) {
        Active active = ACTIVE.get(Effect.GAMMA);
        if (active == null) return;
        var option = Minecraft.getInstance().options.gamma();
        if (!gammaCaptured) {
            gammaOriginal = option.get();
            gammaCaptured = true;
        }
        // Two gentle dips per second, at most 8 percentage points, never a full-screen flash.
        double pulse = (1 - Math.cos((now - active.start) / 1_000_000_000.0 * Math.PI * 4)) * .5;
        option.set(clamp(gammaOriginal - Math.abs(active.request.strength) * .08 * pulse, 0, 1));
    }

    public static void tick() {
        long now = System.nanoTime();
        boolean hadSettings = ACTIVE.keySet().stream().anyMatch(SETTINGS::contains);
        ACTIVE.entrySet().removeIf(entry -> now >= entry.getValue().end);
        if (hadSettings && ACTIVE.keySet().stream().noneMatch(SETTINGS::contains)) restoreSettings();
        applyGamma(now);
    }

    public static void restoreSettings() {
        SETTINGS.forEach(ACTIVE::remove);
        gammaCaptured = false;
        InterfaceOptionsGuard.restoreMemoryAndFile();
    }

    /** World effects never survive world unload. Title/loading arms may span that transition. */
    public static void leaveWorld() {
        restoreSettings();
        ACTIVE.keySet().removeIf(effect -> effect != Effect.SPLASH && effect != Effect.LOADING);
        TOASTS.replaceAll((toast, end) -> 0L);
    }

    /** Director cancellation and debug emergency stop; cooldowns are deliberately retained. */
    public static void clear() {
        restoreSettings();
        ACTIVE.clear();
        TOASTS.replaceAll((toast, end) -> 0L);
    }

    public static boolean active(Effect effect) {
        Active active = ACTIVE.get(effect);
        return active != null && System.nanoTime() < active.end;
    }

    public static Component replacement(Effect effect, Component original) {
        if (!active(effect)) return original;
        observe(effect.name());
        return text(ACTIVE.get(effect).request, effect == Effect.DEATH ? "death" : "goal");
    }

    public static Collection<String> debugLines(Collection<String> original) {
        if (!active(Effect.F3)) return original;
        observe("F3");
        // Keep Block/Chunk/Facing coordinates truthful; clipboard, targeting and physics are untouched.
        return original.stream().map(line -> line.startsWith("XYZ:")
                ? "XYZ: 30000001.000 / -4096.00000 / 30000001.000" : line).toList();
    }

    public static SignText signText(BlockPos position, SignText original) {
        if (!active(Effect.SIGN) || original == null) return original;
        Request request = ACTIVE.get(Effect.SIGN).request;
        if (!position.equals(request.signPosition)) return original;
        observe("SIGN");
        var copy = original.asMutable();
        for (int line = 0; line < 4; line++) {
            copy.setLine(line, InterfaceTexts.line("sign" + line, request.generation, request.language));
        }
        return copy.asImmutable();
    }

    public static boolean hideExpiredToast(AdvancementToast toast) {
        Long end = TOASTS.get(toast);
        if (end == null) return false;
        observe("ADVANCEMENT");
        return System.nanoTime() >= end;
    }

    private static Component text(Request request, String channel) {
        return InterfaceTexts.line(channel, request.generation, request.language);
    }

    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

    // Lightweight evidence for the real client smoke helper; no telemetry is written or sent.
    private static void observe(String channel) { OBSERVED.merge(channel, 1, Integer::sum); }
    public static int observed(String channel) { return OBSERVED.getOrDefault(channel, 0); }
}
