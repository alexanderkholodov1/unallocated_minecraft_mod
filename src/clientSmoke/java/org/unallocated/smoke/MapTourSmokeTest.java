package org.unallocated.smoke;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import org.slf4j.LoggerFactory;
import org.unallocated.map.MapBuilder;
import org.unallocated.map.MapLayout;
import org.unallocated.map.MapVerifier;

/** Real integrated server, real chunk packets and rendered first-person views from every checkpoint. */
public final class MapTourSmokeTest {
    private MapTourSmokeTest() {}
    public static void run(ClientGameTestContext context) {
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
                try {
                    world.getConnection().waitForChunksRender();
                } catch (AssertionError failure) {
                    context.runOnClient(client -> {
                        int radius = client.options.getEffectiveRenderDistance();
                        int cx = client.player.blockPosition().getX() >> 4;
                        int cz = client.player.blockPosition().getZ() >> 4;
                        var missing = new java.util.ArrayList<String>();
                        for (int dz = -radius; dz <= radius; dz++) for (int dx = -radius; dx <= radius; dx++) {
                            if (client.level.getChunk(cx + dx, cz + dz,
                                    net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false) == null) {
                                missing.add(dx + "," + dz);
                            }
                        }
                        LoggerFactory.getLogger("unallocated-smoke").error(
                                "UNALLOCATED map render wait checkpoint={} radius={} rendered={} missing={}",
                                entry.getKey(), radius, client.levelRenderer.hasRenderedAllSections(), missing);
                    });
                    throw failure;
                }
                context.waitTicks(10);
                context.takeScreenshot("map-" + entry.getKey().replace('.', '-'));
                LoggerFactory.getLogger("unallocated-smoke").info("UNALLOCATED map screenshot checkpoint={}", entry.getKey());
            }
        } finally { authorOnStart.set(false); }
        LoggerFactory.getLogger("unallocated-smoke").info("UNALLOCATED smoke: map tour completed.");
    }
}
