package org.unallocated.smoke;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import org.slf4j.LoggerFactory;
import org.unallocated.client.ending.EndingClient;
import org.unallocated.core.Act;
import org.unallocated.core.CoreRuntime;
import org.unallocated.core.ServerEvents;
import org.unallocated.ending.EndingController;
import org.unallocated.ending.EndingText;

/** Runs against the installed player JAR, not development classes. Never closes an ended test
 * context: its close() assumes a live world, whereas this test exercises the game's own teardown.
 */
@SuppressWarnings("UnstableApiUsage")
final class EndingSmokeTest {
    private EndingSmokeTest() {}

    static void run(ClientGameTestContext context) {
        // No receipt package is bundled in this worktree; the tested farewell must still work.
        TestSingleplayerContext world = context.worldBuilder().create();
        Path save = world.getWorldSave().getSaveDirectory();
        var server = world.getServer().computeOnServer(s -> s);
        var events = new AtomicInteger();
        world.getServer().runOnServer(s -> {
            var runtime = CoreRuntime.get(s);
            if (runtime.state().started()) throw new AssertionError("Ordinary world started a narrative session");
            runtime.state().setCounter("ending.smoke", 271);
            runtime.events().subscribe(ServerEvents.EndingReached.class, event -> {
                if (!event.id().equals("SERVER_CLOSED")) throw new AssertionError("Wrong ending event");
                events.incrementAndGet();
            });
        });
        Map<Path, byte[]> before = snapshots(context);
        world.getServer().runOnServer(s -> {
            var runtime = CoreRuntime.get(s);
            var player = s.getPlayerList().getPlayers().getFirst();
            // Normal sequencer path, without the debug EPILOGUE jump.
            runtime.sequencer().skipTo(Act.CLIMAX, player);
            runtime.sequencer().advance(player);
            if (!runtime.state().finished()) throw new AssertionError("Ending was not terminal before disconnect");
            // Idempotency: a second call cannot emit another event or disconnect again.
            EndingController.close(player, new EndingController.Ending("SERVER_CLOSED", EndingText.reason()));
        });
        assertDisconnect(context, "unallocated-ending-disconnect");
        if (!server.isShutdown() || events.get() != 1) {
            throw new AssertionError("World did not stop cleanly or ending event was not exactly once");
        }
        assertTitle(context, "unallocated-ending-title");
        assertUnchanged(before);
        if (!Files.isRegularFile(save.resolve("level.dat"))) throw new AssertionError("World was not saved");

        // A second server instance must reload the terminal checkpoint and release the old world lock.
        try (var reloaded = world.getWorldSave().open()) {
            reloaded.getServer().runOnServer(s -> {
                var state = CoreRuntime.get(s).state();
                if (!state.finished() || state.act() != Act.EPILOGUE
                        || !state.flags().contains("ending.SERVER_CLOSED")
                        || state.counters().getOrDefault("ending.smoke", 0) != 271) {
                    throw new AssertionError("Ending checkpoint was not persisted before shutdown");
                }
            });
            context.waitTicks(20);
            context.runOnClient(client -> {
                if (client.level == null || EndingClient.hasEndingSplash()) {
                    throw new AssertionError("Reload re-triggered ending or leaked previous session splash");
                }
                if (EndingClient.screen(new DisconnectionDetails(Component.literal("Control"))) != null) {
                    throw new AssertionError("Ending adapter changed an unrelated disconnect");
                }
            });
        }
        context.waitForScreen(TitleScreen.class);
        context.runOnClient(client -> {
            if (EndingText.SPLASH.equals(splashText((TitleScreen) client.gui.screen()))) {
                throw new AssertionError("Ending splash leaked to a later ordinary session");
            }
        });
        LoggerFactory.getLogger("unallocated-smoke").info(
            "UNALLOCATED smoke: ending save reloaded as finished; options and receipt unchanged; splash reset.");

        // Exercise the real player's debug command independently of the normal sequencer transition.
        context.worldBuilder().create();
        context.runOnClient(client -> client.getConnection().sendCommand("unallocated act EPILOGUE"));
        assertDisconnect(context, "unallocated-ending-command-disconnect");
        assertTitle(context, "unallocated-ending-command-title");
        LoggerFactory.getLogger("unallocated-smoke").info(
            "UNALLOCATED smoke: ending command showed vanilla disconnect and returned to title with changed splash.");
    }

    private static void assertDisconnect(ClientGameTestContext context, String screenshot) {
        context.waitFor(client -> client.gui.screen() instanceof DisconnectedScreen, 1200);
        context.runOnClient(client -> {
            if (client.level != null || client.player != null || client.getSingleplayerServer() != null) {
                throw new AssertionError("Disconnect screen still has an open world/player/server");
            }
            var screen = (DisconnectedScreen) client.gui.screen();
            String text = screen.getNarrationMessage().getString();
            if (!screen.getTitle().getString().equals(EndingText.TITLE)
                    || !text.contains("estimar respuestas de miedo humano")
                    || !text.contains("Gracias por tus datos.")
                    || !text.contains("Gracias por poner fin a esto.")) {
                throw new AssertionError("Ending disconnect mixin did not apply or farewell is missing");
            }
            if (screen.children().stream().noneMatch(child -> child instanceof AbstractWidget button
                    && button.getMessage().getString().equals(EndingText.RETURN_BUTTON))) {
                throw new AssertionError("Missing title-return button");
            }
        });
        context.waitTicks(2);
        context.takeScreenshot(screenshot);
    }

    private static void assertTitle(ClientGameTestContext context, String screenshot) {
        context.clickScreenButton(EndingText.RETURN_BUTTON);
        context.waitForScreen(TitleScreen.class);
        context.runOnClient(client -> {
            if (!EndingText.SPLASH.equals(splashText((TitleScreen) client.gui.screen()))) {
                throw new AssertionError("Title splash mixin did not change the actual rendered splash");
            }
            // Reinitializing the title (as after a resize) retains the session's changed splash.
            client.gui.setScreen(new TitleScreen());
            if (!EndingText.SPLASH.equals(splashText((TitleScreen) client.gui.screen()))) {
                throw new AssertionError("Title reinitialization lost the ending splash");
            }
        });
        context.waitTicks(2);
        context.takeScreenshot(screenshot);
    }

    private static String splashText(TitleScreen screen) throws ReflectiveOperationException {
        // Verify actual vanilla state instead of trusting a flag set by our own feature code.
        var field = TitleScreen.class.getDeclaredField("splash");
        field.setAccessible(true);
        var text = SplashRenderer.class.getDeclaredField("splash");
        text.setAccessible(true);
        Object renderer = field.get(screen);
        return renderer == null ? "" : ((Component) text.get(renderer)).getString();
    }

    private static Map<Path, byte[]> snapshots(ClientGameTestContext context) {
        return context.computeOnClient(client -> {
            var result = new LinkedHashMap<Path, byte[]>();
            Path game = client.gameDirectory.toPath();
            for (String name : new String[] {"options.txt", "servers.dat", "unallocated/contract.properties"}) {
                Path path = game.resolve(name);
                result.put(path, Files.exists(path) ? Files.readAllBytes(path) : null);
            }
            return result;
        });
    }

    private static void assertUnchanged(Map<Path, byte[]> before) {
        before.forEach((path, bytes) -> {
            try {
                byte[] after = Files.exists(path) ? Files.readAllBytes(path) : null;
                if (!Arrays.equals(bytes, after)) throw new AssertionError("Ending changed " + path.getFileName());
            } catch (IOException exception) {
                throw new AssertionError("Could not verify unchanged game files", exception);
            }
        });
    }
}
