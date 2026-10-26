package org.unallocated.smoke;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biomes;
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
        try (var world = context.worldBuilder().adjustSettings(settings -> {
            settings.setSeed("412");
            settings.setGenerateStructures(false);
            settings.updateDimensions((registries, dimensions) -> {
                var flat = new FlatLevelGeneratorSettings(Optional.of(HolderSet.direct()),
                    registries.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS), List.of());
                flat.getLayersInfo().add(new FlatLayerInfo(1, Blocks.AIR));
                flat.updateLayers();
                return dimensions.replaceOverworldGenerator(registries, new FlatLevelSource(flat));
            });
        }).create()) {
            var server = world.getServer();
            // Exercise exactly the same command and common builder as dedicated/template authoring.
            server.runCommand("unallocated build all");
            server.runOnServer(instance -> MapVerifier.verify(instance.overworld(), "all"));
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
                context.waitTicks(5);
                world.getConnection().waitForChunksRender();
                context.waitTicks(10);
                context.takeScreenshot("map-" + entry.getKey().replace('.', '-'));
                LoggerFactory.getLogger("unallocated-smoke").info("UNALLOCATED map screenshot checkpoint={}", entry.getKey());
            }
        }
        LoggerFactory.getLogger("unallocated-smoke").info("UNALLOCATED smoke: map tour completed.");
    }
}
