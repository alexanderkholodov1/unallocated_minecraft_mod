package org.unallocated.smoke;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.*;
import org.unallocated.core.CoreRuntime;
import org.unallocated.ending.EndingText;
import org.unallocated.map.MapLayout;
import org.unallocated.story.*;

/** Two independent flat-air worlds, real trigger simulation, vanilla shutdown and local text. */
final class StorySmokeTest {
    private static byte[] read(Path path) {
        try { return Files.readAllBytes(path); }
        catch (java.io.IOException exception) { throw new AssertionError("Could not snapshot " + path.getFileName(), exception); }
    }
    static void run(ClientGameTestContext context) {
        Map<Path, byte[]> before = context.computeOnClient(client -> {
            Map<Path, byte[]> result = new LinkedHashMap<>();
            for (String name : List.of("options.txt", "unallocated/contract.properties")) {
                Path path = client.gameDirectory.toPath().resolve(name); result.put(path, read(path));
            }
            return result;
        });
        Set<String> captures = java.util.concurrent.ConcurrentHashMap.newKeySet();
        for (int index = 0; index < 2; index++) {
            int run = index;
            StoryRuntime.setAutoplayRun(run);
            AtomicBoolean author = new AtomicBoolean(true);
            AtomicBoolean complete = new AtomicBoolean();
            ServerLifecycleEvents.SERVER_STARTED.register(server -> {
                if (!author.compareAndSet(true, false)) return;
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "unallocated build all");
                StoryBuild.run(server); CoreRuntime.arm(server);
                var runtime = StoryRuntime.get(server);
                runtime.events().subscribe(StoryEnding.class, event -> {
                    if (!event.id().equals(run == 0 ? "EXAMPLE_A" : "EXAMPLE_B")) throw new AssertionError("Wrong story ending");
                    complete.set(true);
                });
                runtime.events().subscribe(StoryCapture.class, event -> {
                    Minecraft client = Minecraft.getInstance();
                    client.execute(() -> Screenshot.grab(client.gameDirectory, "story-" + event.name() + ".png",
                        client.gameRenderer.mainRenderTarget(), 1, message -> captures.add(event.name())));
                });
            });
            // This context is ended by the story itself; close() assumes a live integrated server.
            var world = context.worldBuilder().adjustSettings(settings -> {
                settings.setSeed("412"); settings.setGenerateStructures(false);
                settings.updateDimensions((registries, dimensions) -> {
                    var flat = new FlatLevelGeneratorSettings(Optional.of(HolderSet.direct()), registries.lookupOrThrow(Registries.BIOME)
                        .getOrThrow(ResourceKey.create(Registries.BIOME, Identifier.parse(MapLayout.BIOME_ID))), List.of());
                    flat.getLayersInfo().add(new FlatLayerInfo(1, Blocks.AIR)); flat.updateLayers();
                    return dimensions.replaceOverworldGenerator(registries, new FlatLevelSource(flat));
                });
            }).create();
            context.waitForScreen(DisconnectedScreen.class);
            if (!complete.get()) throw new AssertionError("Story did not emit its expected ending");
            context.runOnClient(client -> {
                var screen = client.gui.screen();
                String letter = run == 0 ? "A" : "B";
                String expected = client.options.languageCode.startsWith("es") ? "[ES] Final " + letter + " título" : "[EN] Ending " + letter + " title";
                if (!screen.getTitle().getString().equals(expected)) throw new AssertionError("Farewell did not render client language: " + screen.getTitle().getString());
                if (client.level != null || client.getSingleplayerServer() != null) throw new AssertionError("Story farewell kept world open");
            });
            context.takeScreenshot("unallocated-story-farewell-" + (run == 0 ? "a" : "b"));
            context.clickScreenButton(EndingText.RETURN_BUTTON); context.waitForScreen(TitleScreen.class);
            author.set(false);
        }
        context.waitFor(client -> captures.containsAll(Set.of("example-sign", "example-end")));
        before.forEach((path, bytes) -> {
            try { if (!Arrays.equals(bytes, Files.readAllBytes(path))) throw new AssertionError("Story changed " + path.getFileName()); }
            catch (java.io.IOException e) { throw new AssertionError(e); }
        });
        org.slf4j.LoggerFactory.getLogger("unallocated-smoke").info("UNALLOCATED smoke: story both endings, captures, options and receipt verified.");
    }
}
