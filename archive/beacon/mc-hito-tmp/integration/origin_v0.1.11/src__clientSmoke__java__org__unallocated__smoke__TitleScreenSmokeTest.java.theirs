package org.unallocated.smoke;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.server.packs.PackType;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public final class TitleScreenSmokeTest implements FabricClientGameTest, ClientModInitializer {
    private static boolean titleHookCalled;

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof TitleScreen) {
                titleHookCalled = true;
            }
        });
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            throw new AssertionError("Expected a real client environment");
        }
        var mod = FabricLoader.getInstance().getModContainer("unallocated").orElseThrow();
        List<Path> origins = mod.getOrigin().getPaths();
        Path expected = Path.of(System.getProperty("unallocated.smoke.jar"));
        try {
            if (origins.size() != 1 || !Files.isRegularFile(origins.getFirst())
                    || Files.mismatch(expected, origins.getFirst()) != -1) {
                throw new AssertionError("Client must load the exact player JAR, got " + origins);
            }
        } catch (IOException exception) {
            throw new AssertionError("Could not verify the loaded player JAR", exception);
        }
        context.waitForScreen(TitleScreen.class);
        context.waitTicks(2);
        if (!titleHookCalled || CommandRegistrationCallback.EVENT == null
                || ServerLifecycleEvents.SERVER_STARTED == null
                || PayloadTypeRegistry.clientboundPlay() == null
                || ResourceLoader.get(PackType.CLIENT_RESOURCES) == null) {
            throw new AssertionError("Bundled API interfaces or screen hook unavailable");
        }
        int bundledModules = 0;
        for (var loaded : FabricLoader.getInstance().getAllMods()) {
            String id = loaded.getMetadata().getId();
            if (id.equals("fabric-api")) {
                throw new AssertionError("Full Fabric API masks player packaging");
            }
            if (id.startsWith("fabric-") && !id.equals("fabric-client-gametest-api-v1")) {
                if (loaded.getOrigin().getKind() != ModOrigin.Kind.NESTED
                        || !loaded.getOrigin().getParentModId().equals("unallocated")) {
                    throw new AssertionError("Player module supplied outside player JAR: " + id);
                }
                bundledModules++;
            }
        }
        if (bundledModules != 7) {
            throw new AssertionError("Expected exactly seven bundled API modules, got " + bundledModules);
        }
        LoggerFactory.getLogger("unallocated-smoke").info(
                "UNALLOCATED smoke: seven Fabric API modules loaded from player JAR; screen hook verified.");
        context.takeScreenshot("unallocated-title");
        LoggerFactory.getLogger("unallocated-smoke").info(
                "UNALLOCATED smoke: title screen reached from player JAR.");
        AudioSmokeTest.run(context);
    }
}
