package org.unallocated.client.story;

import com.google.gson.*;
import org.unallocated.story.mixin.client.StorySplashAccessor;
import java.util.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.LevelResource;
import org.unallocated.client.core.ClientSession;
import org.unallocated.client.interfacefx.*;
import org.unallocated.core.Features;
import org.unallocated.core.network.CorePayloads.EffectPayload;
import org.unallocated.ending.ContractReceiptLookup;
import org.unallocated.story.StoryTexts;
import static org.unallocated.story.StoryData.*;

/** Render-thread effects. No receipts or client-only placeholder values are sent to the server. */
public final class StoryClient {
    private static long blackoutStart;
    private static double fadeIn, hold, fadeOut;
    private static int blackoutColor;
    private static Features features;
    private StoryClient() {}
    public static void initialize() {
        features = Features.load();
        ClientSession.onEffect(StoryClient::effect);
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof net.minecraft.client.gui.screens.TitleScreen && org.unallocated.client.ending.EndingClient.hasEndingSplash())
                ((StorySplashAccessor) screen).unallocated$storySplash(new net.minecraft.client.gui.components.SplashRenderer(org.unallocated.client.ending.EndingClient.splash()));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> blackoutStart = 0);
    }
    private static Map<String, String> locals(Minecraft client) {
        Map<String, String> values = new HashMap<>();
        values.put("player", client.getUser().getName());
        values.put("local_time", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        values.put("world", client.getSingleplayerServer() == null ? "" : client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString());
        ContractReceiptLookup.read(client.gameDirectory.toPath()).ifPresent(text -> values.put("agreement", text));
        return values;
    }
    public static Component text(JsonObject value, JsonObject envelope) {
        Minecraft client = Minecraft.getInstance();
        var component = Component.literal(StoryTexts.render(value, envelope, client.options.languageCode, locals(client)));
        Style style = Style.EMPTY;
        if (value.has("color")) style = style.withColor(Integer.parseInt(string(value, "color", "#ffffff").substring(1), 16));
        if (value.has("bold")) style = style.withBold(bool(value, "bold", false));
        if (value.has("italic")) style = style.withItalic(bool(value, "italic", false));
        if (value.has("obfuscated")) style = style.withObfuscated(bool(value, "obfuscated", false));
        return component.setStyle(style);
    }
    public static void effect(EffectPayload payload) {
        if (!payload.effectId().startsWith("story.")) return;
        Minecraft client = Minecraft.getInstance();
        JsonObject envelope = JsonParser.parseString(payload.args().getStringOr("json", "{}")).getAsJsonObject();
        JsonObject data = object(envelope, "data");
        switch (payload.effectId()) {
            case "story.title" -> {
                client.gui.hud.setTimes((int) (number(data, "fade_in", .5) * 20), (int) (number(data, "stay", 3) * 20), (int) (number(data, "fade_out", 1) * 20));
                client.gui.hud.setSubtitle(text(object(data, "subtitle"), envelope)); client.gui.hud.setTitle(text(object(data, "title"), envelope));
            }
            case "story.actionbar" -> client.gui.hud.setOverlayMessage(text(object(data, "text"), envelope), false);
            case "story.chat" -> {
                Component line = text(object(data, "text"), envelope);
                if (data.has("from")) line = Component.literal("<").append(text(object(data, "from"), envelope)).append("> ").append(line);
                client.gui.chatListener().handleSystemMessage(line, false);
            }
            case "story.blackout" -> {
                fadeIn = number(data, "fade_in", .1); hold = number(data, "hold", .5); fadeOut = number(data, "fade_out", .4);
                blackoutColor = Integer.parseInt(string(data, "color", "#000000").substring(1), 16); blackoutStart = System.nanoTime();
            }
            case "story.interface" -> {
                var effect = InterfaceEffects.Effect.valueOf(string(data, "effect", "F3"));
                boolean allowed = !Set.of(InterfaceEffects.Effect.SENSITIVITY, InterfaceEffects.Effect.FOV, InterfaceEffects.Effect.GAMMA).contains(effect) || features.enabled("effects.options");
                BlockPos sign = null;
                if (data.has("sign")) { JsonArray p = array(object(data, "sign"), "pos"); sign = BlockPos.containing(p.get(0).getAsDouble(), p.get(1).getAsDouble(), p.get(2).getAsDouble()); }
                var request = new InterfaceEffects.Request(effect, Math.min(InterfaceEffects.maxTicks(effect), payload.durationTicks()), (int) number(data, "generation", 0), number(data, "strength", 0),
                    InterfaceTexts.LeakLanguage.parse(string(data, "leak", "es")), (long) number(data, "seed", 0), sign,
                    data.has("advancement") ? Identifier.parse(string(data, "advancement", "")) : null, data.has("text") ? text(object(data, "text"), envelope) : null);
                boolean accepted = allowed && InterfaceEffects.trigger(request);
                org.unallocated.UnallocatedMod.LOGGER.info("UNALLOCATED story action beat={} do=interface result={}", string(data, "beat", "interface"), accepted ? "ok" : "refused");
            }
            case "story.ending_text" -> org.unallocated.client.ending.EndingClient.storyEnding(envelope);
            default -> { }
        }
    }
    public static void overlay(GuiGraphicsExtractor graphics) {
        if (blackoutStart == 0 || Minecraft.getInstance().level == null) return;
        double elapsed = (System.nanoTime() - blackoutStart) / 1_000_000_000.0;
        double alpha = elapsed < fadeIn ? elapsed / Math.max(.001, fadeIn) : elapsed < fadeIn + hold ? 1 : 1 - (elapsed - fadeIn - hold) / Math.max(.001, fadeOut);
        if (elapsed >= fadeIn + hold + fadeOut) { blackoutStart = 0; return; }
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), ((int) (Math.clamp(alpha, 0, 1) * 255) << 24) | blackoutColor);
    }
}
