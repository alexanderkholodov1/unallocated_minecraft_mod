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
        AtomicBoolean creatureRestored = new AtomicBoolean(), seen = new AtomicBoolean(), caught = new AtomicBoolean();
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
                    WorldCopy.copy(runtime, runtime.resolveBox(new com.google.gson.JsonPrimitive("world.pocket_base")),
                        new StoryRuntime.Position("overworld", 200, 60, 300, 0, 0));
                    var base = MapLayout.section("base").bounds();
                    for (int x = base.minX(); x <= base.maxX(); x++) for (int y = base.minY(); y <= base.maxY(); y++)
                        for (int z = base.minZ(); z <= base.maxZ(); z++) {
                            BlockPos from = new BlockPos(x, y, z), to = from.offset(49, 0, 98);
                            if (!server.overworld().getBlockState(from).equals(pocket.getBlockState(to))) throw new AssertionError("Copy differs at " + from);
                            if (!server.overworld().getBlockState(from).equals(server.overworld().getBlockState(from.offset(149, 0, 298)))) throw new AssertionError("Reverse dimension copy differs at " + from);
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
                    if (runtime.state().flags().stream().noneMatch(s -> s.startsWith("story.creature."))) throw new AssertionError("Reload lost creature runtime state");
                    creatureRestored.set(true);
                    log("corruption job and cursor restored");
                }
                runtime.events().subscribe(StoryBeatFired.class, event -> {
                    if (event.beatId().equals("world.seen")) seen.set(true);
                    if (event.beatId().equals("world.caught")) caught.set(true);
                    if (event.beatId().equals("world.return")) {
                        var player = server.getPlayerList().getPlayers().getFirst();
                        if (player.level() != server.overworld()) failure.set(new AssertionError("Round trip did not return to overworld"));
                        else returned.set(true);
                        if (org.unallocated.creature.Creatures.entity(player, "world.figure") != null) failure.set(new AssertionError("Despawn kept creature alive"));
                        if (runtime.state().flags().stream().anyMatch(s -> s.startsWith("story.world.job."))) failure.set(new AssertionError("Corruption did not complete after reload"));
                        else log("pocket round trip and corruption completion verified");
                    }
                });
                runtime.events().subscribe(StoryEnding.class, event -> { if (event.id().equals("SERVER_CLOSED")) complete.set(true); });
                runtime.events().subscribe(StoryCapture.class, event -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    if (!player.level().equals(pocket)) failure.set(new AssertionError("Pocket capture in wrong dimension"));
                    if (event.name().equals("world-creature")) {
                        var entity = org.unallocated.creature.Creatures.entity(player, "world.figure");
                        if (!creatureRestored.get() || entity == null || entity.shouldBeSaved()
                            || !(entity instanceof net.minecraft.world.entity.decoration.Mannequin mannequin)
                            || !mannequin.getProfile().partialProfile().equals(player.getGameProfile())) failure.set(new AssertionError("Creature restoration/profile/save exclusion failed"));
                        else log("creature body restored and excluded from save");
                    }
                    Minecraft client = Minecraft.getInstance();
                    client.execute(() -> Screenshot.grab(client.gameDirectory, "story-" + event.name() + ".png",
                        client.gameRenderer.mainRenderTarget(), 1, message -> captures.add(event.name())));
                });
            } catch (Throwable error) { failure.set(error); }
        });
        try {
            var world = LoadingFailure.capture(context, "world-fresh", () -> context.worldBuilder().adjustSettings(settings -> {
                settings.setSeed("412"); settings.setGenerateStructures(false);
                settings.updateDimensions((registries, dimensions) -> {
                    var flat = new FlatLevelGeneratorSettings(Optional.of(HolderSet.direct()), registries.lookupOrThrow(Registries.BIOME)
                        .getOrThrow(ResourceKey.create(Registries.BIOME, Identifier.parse(MapLayout.BIOME_ID))), List.of());
                    flat.getLayersInfo().add(new FlatLayerInfo(1, Blocks.AIR)); flat.updateLayers();
                    return dimensions.replaceOverworldGenerator(registries, new FlatLevelSource(flat));
                });
            }).create());
            world.getServer().waitFor(server -> failure.get() != null || StoryRuntime.get(server).state().counters().entrySet().stream()
                .anyMatch(e -> e.getKey().startsWith("story.world.") && e.getKey().endsWith(".cursor") && e.getValue() > 0));
            if (failure.get() != null) throw new AssertionError("World authoring failed", failure.get());
            world.getServer().runOnServer(server -> {
                var runtime = StoryRuntime.get(server); var player = server.getPlayerList().getPlayers().getFirst();
                var action = com.google.gson.JsonParser.parseString("{\"do\":\"creature\",\"op\":\"spawn\",\"id\":\"probe\",\"body\":{\"summon\":\"minecraft:armor_stand{Small:1b}\"},\"where\":{\"pos\":[110,64,110],\"dimension\":\"pocket\"}}").getAsJsonObject();
                var validator = new StoryValidator(server);
                StoryActions.parser("creature").parse(action, validator.context("probe", "", new com.google.gson.JsonObject()));
                if (!validator.findings().isEmpty()) throw new AssertionError("Summon failed validation: " + validator.findings());
                org.unallocated.creature.Creatures.execute(runtime, player, action);
                var entity = org.unallocated.creature.Creatures.entity(player, "probe");
                if (!(entity instanceof net.minecraft.world.entity.decoration.ArmorStand stand) || !stand.isSmall() || stand.shouldBeSaved())
                    throw new AssertionError("Vanilla creature SNBT or save exclusion failed");
                action.addProperty("op", "despawn"); org.unallocated.creature.Creatures.execute(runtime, player, action);
                log("vanilla summon SNBT and despawn verified");
            });
            var save = world.getWorldSave(); world.close();
            LoadingFailure.capture(context, "world-resume", save::open); // Reopen the actual save with its datapack dimension and unfinished corruption.
            context.waitForScreen(DisconnectedScreen.class);
            if (failure.get() != null || !complete.get() || !returned.get()) throw new AssertionError("World fixture failed", failure.get());
            if (!seen.get() || !caught.get()) throw new AssertionError("Creature triggers were not exercised");
            context.waitFor(client -> captures.containsAll(Set.of("world-pocket", "world-creature")));
            log("creature seen, chase, caught and despawn verified");
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
