package org.unallocated.smoke;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.*;
import org.unallocated.core.CoreRuntime;
import org.unallocated.ending.EndingText;
import org.unallocated.map.MapLayout;
import org.unallocated.story.*;
import org.unallocated.world.*;

/** Third fresh story world: cross-dimension copy, saved spreading corruption, travel and ending. */
final class WorldSmokeTest {
    static void run(ClientGameTestContext context) {
        String previous = System.getProperty("unallocated.story.dir");
        System.setProperty("unallocated.story.dir", Path.of(Objects.requireNonNull(previous, "Story phase requires its example directory")).getParent().resolve("world").toString());
        StoryRuntime.setAutoplayRun(0);
        AtomicBoolean enabled = new AtomicBoolean(true), first = new AtomicBoolean(true), complete = new AtomicBoolean(), returned = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Set<String> captures = java.util.concurrent.ConcurrentHashMap.newKeySet();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (!enabled.get()) return;
            try {
                var runtime = StoryRuntime.get(server); var pocket = runtime.level("pocket");
                if (first.getAndSet(false)) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "unallocated build all");
                    Set<Long> sourceTickets = new HashSet<>(server.overworld().getForceLoadedChunks());
                    Set<Long> targetTickets = new HashSet<>(pocket.getForceLoadedChunks());
                    StoryBuild.run(server); // Runs the public story copy command before any participant joins.
                    var base = MapLayout.section("base").bounds();
                    for (int x = base.minX(); x <= base.maxX(); x++) for (int y = base.minY(); y <= base.maxY(); y++)
                        for (int z = base.minZ(); z <= base.maxZ(); z++) {
                            BlockPos from = new BlockPos(x, y, z), to = from.offset(49, 0, 98);
                            if (!server.overworld().getBlockState(from).equals(pocket.getBlockState(to))) throw new AssertionError("Copy differs at " + from);
                            var entity = server.overworld().getBlockEntity(from);
                            if (entity != null) {
                                var copied = pocket.getBlockEntity(to);
                                if (copied == null || !entity.saveWithoutMetadata(server.registryAccess()).equals(copied.saveWithoutMetadata(server.registryAccess())))
                                    throw new AssertionError("Copy lost block entity at " + from);
                            }
                        }
                    if (!sourceTickets.equals(new HashSet<>(server.overworld().getForceLoadedChunks()))
                        || !targetTickets.equals(new HashSet<>(pocket.getForceLoadedChunks()))) throw new AssertionError("Copy leaked force tickets");
                    log("fresh pocket, copy blocks, block entities and force tickets verified"); CoreRuntime.arm(server);
                } else {
                    if (runtime.state().flags().stream().noneMatch(s -> s.startsWith("story.world.job."))) throw new AssertionError("Reload lost corruption job");
                    if (runtime.state().counters().entrySet().stream().noneMatch(e -> e.getKey().startsWith("story.world.") && e.getKey().endsWith(".cursor") && e.getValue() > 0))
                        throw new AssertionError("Reload lost corruption progress");
                    log("corruption job and cursor restored");
                }
                runtime.events().subscribe(StoryBeatFired.class, event -> {
                    if (event.beatId().equals("world.return")) {
                        var player = server.getPlayerList().getPlayers().getFirst();
                        if (player.level() != server.overworld()) failure.set(new AssertionError("Round trip did not return to overworld"));
                        else returned.set(true);
                        if (runtime.state().flags().stream().anyMatch(s -> s.startsWith("story.world.job."))) failure.set(new AssertionError("Corruption did not complete after reload"));
                        else log("pocket round trip and corruption completion verified");
                    }
                });
                runtime.events().subscribe(StoryEnding.class, event -> { if (event.id().equals("SERVER_CLOSED")) complete.set(true); });
                runtime.events().subscribe(StoryCapture.class, event -> {
                    if (!server.getPlayerList().getPlayers().getFirst().level().equals(pocket)) failure.set(new AssertionError("Pocket capture in wrong dimension"));
                    Minecraft client = Minecraft.getInstance();
                    client.execute(() -> Screenshot.grab(client.gameDirectory, "story-" + event.name() + ".png",
                        client.gameRenderer.mainRenderTarget(), 1, message -> captures.add(event.name())));
                });
            } catch (Throwable error) { failure.set(error); }
        });
        try {
            var world = context.worldBuilder().adjustSettings(settings -> {
                settings.setSeed("412"); settings.setGenerateStructures(false);
                settings.updateDimensions((registries, dimensions) -> {
                    var flat = new FlatLevelGeneratorSettings(Optional.of(HolderSet.direct()), registries.lookupOrThrow(Registries.BIOME)
                        .getOrThrow(ResourceKey.create(Registries.BIOME, Identifier.parse(MapLayout.BIOME_ID))), List.of());
                    flat.getLayersInfo().add(new FlatLayerInfo(1, Blocks.AIR)); flat.updateLayers();
                    return dimensions.replaceOverworldGenerator(registries, new FlatLevelSource(flat));
                });
            }).create();
            world.getServer().waitFor(server -> failure.get() != null || StoryRuntime.get(server).state().counters().entrySet().stream()
                .anyMatch(e -> e.getKey().startsWith("story.world.") && e.getKey().endsWith(".cursor") && e.getValue() > 0));
            if (failure.get() != null) throw new AssertionError("World authoring failed", failure.get());
            var save = world.getWorldSave(); world.close();
            save.open(); // Reopen the actual save with its datapack dimension and unfinished corruption.
            context.waitForScreen(DisconnectedScreen.class);
            if (failure.get() != null || !complete.get() || !returned.get()) throw new AssertionError("World fixture failed", failure.get());
            context.waitFor(client -> captures.contains("world-pocket"));
            context.takeScreenshot("unallocated-story-world-farewell");
            context.clickScreenButton(EndingText.RETURN_BUTTON); context.waitForScreen(TitleScreen.class);
            log("fixture ending, pocket capture and reload verified");
        } finally {
            enabled.set(false);
            if (previous == null) System.clearProperty("unallocated.story.dir"); else System.setProperty("unallocated.story.dir", previous);
        }
    }
    private static void log(String message) { org.slf4j.LoggerFactory.getLogger("unallocated-smoke").info("UNALLOCATED smoke: world " + message); }
}
