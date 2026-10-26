package org.unallocated.smoke;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import org.slf4j.LoggerFactory;
import org.unallocated.map.MapBuilder;
import org.unallocated.map.MapLayout;
import org.unallocated.map.MapVerifier;

/** Real integrated server, real chunk packets and rendered first-person views from every checkpoint. */
public final class MapTourSmokeTest {
    private record ViewOptions(int distance, boolean bob, int fov) {}
    private MapTourSmokeTest() {}
    public static void run(ClientGameTestContext context) {
        var originalOptions = context.computeOnClient(client -> new ViewOptions(
                client.options.renderDistance().get(), client.options.bobView().get(), client.options.fov().get()));
        context.runOnClient(client -> {
            client.options.renderDistance().set(8);
            client.options.bobView().set(false);
            client.options.fov().set(70);
        });
        // Author before login: an unbuilt flat-air world has no safe place for its first player.
        var authorOnStart = new AtomicBoolean(true);
        ServerLifecycleEvents.SERVER_STARTED.register(instance -> {
            if (authorOnStart.compareAndSet(true, false)) {
                instance.getCommands().performPrefixedCommand(instance.createCommandSourceStack(), "unallocated build all");
                MapVerifier.verify(instance.overworld(), "all");
            }
        });
        try (var world = context.worldBuilder().adjustSettings(settings -> {
            settings.setSeed("412");
            settings.setGenerateStructures(false);
            settings.updateDimensions((registries, dimensions) -> {
                var flat = new FlatLevelGeneratorSettings(Optional.of(HolderSet.direct()),
                    registries.lookupOrThrow(Registries.BIOME).getOrThrow(
                        ResourceKey.create(Registries.BIOME, Identifier.parse(MapLayout.BIOME_ID))), List.of());
                flat.getLayersInfo().add(new FlatLayerInfo(1, Blocks.AIR));
                flat.updateLayers();
                return dimensions.replaceOverworldGenerator(registries, new FlatLevelSource(flat));
            });
        }).create()) {
            var server = world.getServer();
            if (authorOnStart.get()) throw new AssertionError("Map authoring startup hook did not run");
            server.runOnServer(instance -> {
                MapVerifier.verify(instance.overworld(), "all");
                var spawn = MapLayout.checkpoint("lobby.spawn");
                if (!instance.overworld().getBiome(spawn.pos()).is(Identifier.parse(MapLayout.BIOME_ID))) {
                    throw new AssertionError("Map tour did not create the template biome");
                }
                world.getConnection().getServerPlayer().teleportTo(instance.overworld(), spawn.x() + 0.5, spawn.y(), spawn.z() + 0.5,
                    Set.of(), spawn.yaw(), spawn.pitch(), true);
            });
            for (var entry : MapLayout.checkpoints().entrySet()) {
                var cp = entry.getValue();
                if (!MapBuilder.sectionIds().contains(cp.section())) continue;
                server.runOnServer(instance -> {
                    var player = world.getConnection().getServerPlayer();
                    if (!player.teleportTo(instance.overworld(), cp.x() + 0.5, cp.y(), cp.z() + 0.5,
                            Set.of(), cp.yaw(), cp.pitch(), true)) {
                        throw new AssertionError("Teleport failed: " + entry.getKey());
                    }
                });
                world.getConnection().waitForClientboundPackets();
                context.waitFor(client -> client.player != null && client.player.blockPosition().equals(cp.pos()));
                context.waitTicks(5);
                // The server streams a rounded footprint. Fabric's download helper instead
                // requires a square: at radius 8 its four corners are never sent by vanilla.
                // Require the ENTIRE actual tracking view (including its outer buffer), then
                // retain Fabric's light-queue/render-completion wait before every screenshot.
                var expectedChunks = server.computeOnServer(instance -> {
                    var view = world.getConnection().getServerPlayer().getChunkTrackingView();
                    if (!(view instanceof ChunkTrackingView.Positioned positioned)
                            || positioned.viewDistance() != 8
                            || positioned.center().x() != (cp.x() >> 4)
                            || positioned.center().z() != (cp.z() >> 4)) {
                        throw new AssertionError("Map tracking view did not follow checkpoint " + entry.getKey() + ": " + view);
                    }
                    var chunks = new java.util.ArrayList<ChunkPos>();
                    view.forEach(chunks::add);
                    if (chunks.isEmpty()) throw new AssertionError("Map tracking view is empty");
                    return List.copyOf(chunks);
                });
                try {
                    context.waitFor(client -> expectedChunks.stream().allMatch(pos ->
                            client.level.getChunk(pos.x(), pos.z(), ChunkStatus.FULL, false) != null),
                            net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection.DEFAULT_CHUNK_LOAD_TIMEOUT);
                    world.getConnection().waitForChunksRender(false);
                } catch (AssertionError failure) {
                    context.runOnClient(client -> {
                        int radius = client.options.getEffectiveRenderDistance();
                        var missing = new java.util.ArrayList<String>();
                        for (var pos : expectedChunks) {
                            if (client.level.getChunk(pos.x(), pos.z(), ChunkStatus.FULL, false) == null) {
                                missing.add(pos.x() + "," + pos.z());
                            }
                        }
                        LoggerFactory.getLogger("unallocated-smoke").error(
                                "UNALLOCATED map render wait checkpoint={} radius={} expected={} rendered={} missing={}",
                                entry.getKey(), radius, expectedChunks.size(), client.levelRenderer.hasRenderedAllSections(), missing);
                    });
                    throw failure;
                }
                LoggerFactory.getLogger("unallocated-smoke").info(
                        "UNALLOCATED map chunks checkpoint={} streamed={} downloaded and rendered",
                        entry.getKey(), expectedChunks.size());
                context.waitTicks(10);
                context.takeScreenshot("map-" + entry.getKey().replace('.', '-'));
                LoggerFactory.getLogger("unallocated-smoke").info("UNALLOCATED map screenshot checkpoint={}", entry.getKey());
            }
        } finally {
            authorOnStart.set(false);
            context.runOnClient(client -> {
                client.options.renderDistance().set(originalOptions.distance());
                client.options.bobView().set(originalOptions.bob());
                client.options.fov().set(originalOptions.fov());
            });
        }
        LoggerFactory.getLogger("unallocated-smoke").info("UNALLOCATED smoke: map tour completed.");
    }
}
