package org.unallocated.smoke;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.permissions.PermissionSet;
import org.slf4j.LoggerFactory;
import org.unallocated.client.core.ClientSession;
import org.unallocated.core.Act;
import org.unallocated.core.ActSequencer;
import org.unallocated.core.CoreRuntime;

/** Real player + integrated save/reopen, using the production JAR already checked at title. */
public final class CoreSessionSmokeTest {
    private CoreSessionSmokeTest() {}
    public static void run(ClientGameTestContext context) {
        TestWorldSave save;
        try (var world = context.worldBuilder().adjustSettings(settings -> settings.setGenerateStructures(false)).create()) {
            save = world.getWorldSave();
            world.getServer().runOnServer(server -> {
                var runtime = CoreRuntime.get(server);
                var state = runtime.state();
                var player = world.getConnection().getServerPlayer();
                int result = rescue(server, player);
                if (result != 0 || state.started() || state.armed() || state.isDirty()) {
                    throw new AssertionError("No-cheats rescue changed an ordinary world");
                }
            });
        }
        assertSessionFile(save.getSaveDirectory(), false);

        // Authoring hook runs on the server thread, before the real player's JOIN. This is
        // exactly where a direct Java template builder calls arm after verifying its map.
        var armOnStart = new AtomicBoolean(true);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (armOnStart.compareAndSet(true, false)) CoreRuntime.arm(server);
        });
        try (var world = save.open()) {
            if (armOnStart.get()) throw new AssertionError("Server authoring hook was not executed");
            world.getServer().runOnServer(server -> {
                var runtime = CoreRuntime.get(server);
                if (!runtime.state().active() || runtime.state().act() != Act.LOBBY) {
                    throw new AssertionError("Armed JOIN did not start immediately at LOBBY");
                }
            });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(client -> ClientSession.act() == Act.LOBBY);
            world.getServer().runOnServer(server -> {
                var runtime = CoreRuntime.get(server);
                var player = world.getConnection().getServerPlayer();
                runtime.sequencer().skipTo(Act.LOBBY, player); // Make the cooldown boundary independent of loading time.
                if (rescue(server, player) != 0) {
                    throw new AssertionError("Rescue ignored its per-act delay");
                }
            });
            world.getServer().waitFor(server -> CoreRuntime.get(server).state().elapsedActTicks() >= ActSequencer.RESCUE_DELAY_TICKS);
            world.getServer().runOnServer(server -> {
                var runtime = CoreRuntime.get(server);
                var player = world.getConnection().getServerPlayer();
                int first = rescue(server, player);
                int second = rescue(server, player);
                if (first != 1 || second != 0 || runtime.state().act() != Act.CLIMAX) {
                    throw new AssertionError("No-cheats rescue did not advance exactly one act");
                }
            });
            world.getConnection().waitForClientboundPackets();
            context.waitFor(client -> ClientSession.act() == Act.CLIMAX);
        } finally { armOnStart.set(false); }
        assertSessionFile(save.getSaveDirectory(), true);
        try (var world = save.open()) {
            world.getServer().runOnServer(server -> {
                var runtime = CoreRuntime.get(server);
                if (!runtime.state().active() || runtime.state().act() != Act.CLIMAX) {
                    throw new AssertionError("Single-player reload lost session progress");
                }
                runtime.sequencer().advance(world.getConnection().getServerPlayer());
                runtime.sequencer().advance(world.getConnection().getServerPlayer());
                var player = world.getConnection().getServerPlayer();
                if (!runtime.state().finished() || rescue(server, player) != 0) {
                    throw new AssertionError("Rescue reactivated a finished session");
                }
            });
        }
        LoggerFactory.getLogger("unallocated-smoke").info(
            "UNALLOCATED smoke: core armed join, no-cheats rescue guards, client act sync and single-player reload verified.");
    }
    private static int rescue(net.minecraft.server.MinecraftServer server, net.minecraft.server.level.ServerPlayer player) {
        var source = player.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS);
        try { return server.getCommands().getDispatcher().execute("unallocated siguiente", source); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new AssertionError("No-cheats rescue command was unavailable", e);
        }
    }
    private static void assertSessionFile(Path save, boolean expected) {
        try (var paths = Files.walk(save)) {
            boolean found = paths.anyMatch(path -> path.getFileName().toString().equals("unallocated_session.dat"));
            if (found != expected) throw new AssertionError("Session file presence differs from armed state: " + found);
        } catch (java.io.IOException e) { throw new AssertionError("Could not inspect session save", e); }
    }
}
