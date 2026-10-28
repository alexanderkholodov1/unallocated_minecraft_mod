package org.unallocated.smoke;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import org.unallocated.client.launcher.LauncherClient;
import org.unallocated.core.SessionState;
import org.unallocated.ending.EndingText;
import org.unallocated.story.*;
import org.slf4j.LoggerFactory;

/** Judge path only: no world builder, commands, arming or story directory override. */
final class PlaySmokeTest {
    static void run(ClientGameTestContext context) {
        JsonObject story;
        try (var input = PlaySmokeTest.class.getResourceAsStream("/data/unallocated/story/story.json")) {
            if (input == null) throw new AssertionError("Missing shipped story");
            story = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException exception) { throw new AssertionError(exception); }
        var runs = story.getAsJsonObject("autoplay").getAsJsonArray("runs");
        String first = story.getAsJsonArray("chapters").get(0).getAsString();
        double budget = 0;
        Set<String> expectedCaptures = new HashSet<>();
        for (var chapter : story.getAsJsonArray("chapters")) {
            try (var input = PlaySmokeTest.class.getResourceAsStream("/data/unallocated/story/chapters/" + chapter.getAsString() + ".json")) {
                if (input == null) throw new AssertionError("Missing shipped chapter");
                var chapterData = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
                budget += chapterData.get("budget_seconds").getAsDouble();
                for (var beat : chapterData.getAsJsonArray("beats")) {
                    if (beat.getAsJsonObject().has("capture")) expectedCaptures.add(
                        beat.getAsJsonObject().getAsJsonObject("capture").get("name").getAsString());
                }
            } catch (IOException exception) { throw new AssertionError(exception); }
        }
        int endingTimeout = (int) Math.ceil((180 + budget * story.getAsJsonObject("autoplay").get("time_scale").getAsDouble()) * 20);
        AtomicInteger active = new AtomicInteger(-1);
        AtomicBoolean pristine = new AtomicBoolean(), joined = new AtomicBoolean(), complete = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Set<String> captures = java.util.concurrent.ConcurrentHashMap.newKeySet();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (active.get() < 0) return;
            try {
                var state = SessionState.get(server);
                if (!state.armed() || state.started() || state.finished()
                    || server.getWorldData().getGameType() != GameType.ADVENTURE || server.getWorldData().isAllowCommands()) {
                    throw new AssertionError("Launcher save must be armed, unstarted, Adventure, cheats off before JOIN");
                }
                var rules = server.getGameRules();
                if (rules.get(GameRules.SPAWN_MOBS) || rules.get(GameRules.ADVANCE_TIME)
                    || rules.get(GameRules.ADVANCE_WEATHER) || !rules.get(GameRules.KEEP_INVENTORY)
                    || rules.get(GameRules.RESPAWN_RADIUS) != 0) throw new AssertionError("Template lost map rules");
                pristine.set(true);
                var runtime = StoryRuntime.get(server);
                runtime.events().subscribe(StoryEnding.class, event -> {
                    String expected = runs.get(active.get()).getAsJsonObject().get("ending").getAsString();
                    if (!event.id().equals(expected)) failure.set(new AssertionError("Wrong shipped ending: " + event.id()));
                    else complete.set(true);
                });
                runtime.events().subscribe(StoryCapture.class, event -> {
                    Minecraft client = Minecraft.getInstance();
                    client.execute(() -> Screenshot.grab(client.gameDirectory, "story-" + event.name() + ".png",
                        client.gameRenderer.mainRenderTarget(), 1, message -> captures.add(event.name())));
                });
            } catch (Throwable error) { failure.set(error); }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (active.get() < 0) return;
            try {
                var runtime = StoryRuntime.get(server);
                if (!pristine.get() || !runtime.state().started() || runtime.currentChapter() == null
                        || !runtime.currentChapter().get("id").getAsString().equals(first)
                        || handler.player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
                    throw new AssertionError("Judge did not start in the first shipped chapter in Adventure");
                }
                joined.set(true);
                LoggerFactory.getLogger("unallocated-smoke").info(
                    "UNALLOCATED smoke: play run={} armed unstarted Adventure cheats=false first={}", active.get(), first);
            } catch (Throwable error) { failure.set(error); }
        });
        Set<Path> copies = new HashSet<>();
        for (int index = 0; index < runs.size(); index++) {
            active.set(index); pristine.set(false); joined.set(false); complete.set(false);
            StoryRuntime.setAutoplayRun(index);
            context.waitForScreen(TitleScreen.class);
            context.waitTicks(45); // Let vanilla's initial title fade finish for the release capture.
            context.takeScreenshot("unallocated-title-button");
            context.clickScreenButton("menu.unallocated.play");
            context.waitForScreen(GenericMessageScreen.class);
            context.waitTicks(2);
            context.takeScreenshot("unallocated-connecting");
            context.waitFor(client -> failure.get() != null || joined.get() || client.gui.screen() instanceof DisconnectedScreen, 2400);
            if (failure.get() != null || !joined.get()) throw new AssertionError("Launcher preflight failed", failure.get());
            Path copy = context.computeOnClient(client -> client.getSingleplayerServer().getWorldPath(
                net.minecraft.world.level.storage.LevelResource.ROOT).normalize());
            if (!copies.add(copy)) throw new AssertionError("Ending reused a finished save");
            context.waitFor(client -> client.gui.screen() instanceof DisconnectedScreen, endingTimeout);
            if (failure.get() != null || !complete.get()) throw new AssertionError("Shipped story did not reach expected ending", failure.get());
            context.runOnClient(client -> {
                if (client.level != null || client.getSingleplayerServer() != null) throw new AssertionError("Farewell kept server open");
            });
            context.takeScreenshot("unallocated-play-farewell-" + index);
            context.clickScreenButton(EndingText.RETURN_BUTTON);
            context.waitForScreen(TitleScreen.class);
            context.takeScreenshot("unallocated-play-title-" + index);
            LoggerFactory.getLogger("unallocated-smoke").info("UNALLOCATED smoke: play run={} farewell and title verified", index);
        }
        context.waitFor(client -> captures.containsAll(expectedCaptures), 1200);
        // The neutral shipped story has one run; still prove the next click preserves its finished save.
        try {
            for (Path copy : copies) {
                if (!LauncherClient.readSession(copy).getBooleanOr("finished", false)) {
                    throw new AssertionError("Shipped ending did not persist a finished session");
                }
            }
        } catch (IOException exception) { throw new AssertionError(exception); }
        active.set(0); pristine.set(false); joined.set(false); complete.set(false);
        StoryRuntime.setAutoplayRun(0);
        context.clickScreenButton("menu.unallocated.play");
        context.waitForScreen(GenericMessageScreen.class);
        context.waitFor(client -> failure.get() != null || joined.get() || client.gui.screen() instanceof DisconnectedScreen, 2400);
        if (failure.get() != null || !joined.get()) throw new AssertionError("Finished save did not start a new copy", failure.get());
        Path nextCopy = context.computeOnClient(client -> client.getSingleplayerServer().getWorldPath(
            net.minecraft.world.level.storage.LevelResource.ROOT).normalize());
        if (copies.contains(nextCopy)) throw new AssertionError("Launcher reused a finished save");
        context.runOnClient(client -> client.disconnect(new TitleScreen(), false));
        context.waitForScreen(TitleScreen.class);
        context.waitFor(client -> client.getSingleplayerServer() == null && client.level == null);
        LoggerFactory.getLogger("unallocated-smoke").info("UNALLOCATED smoke: play finished save preserved and new copy opened");
        active.set(-1);
        LoggerFactory.getLogger("unallocated-smoke").info("UNALLOCATED smoke: play all shipped runs verified");
        context.waitTicks(40); // Keep the title/display alive while the recorder finalizes its MP4.
    }
}
