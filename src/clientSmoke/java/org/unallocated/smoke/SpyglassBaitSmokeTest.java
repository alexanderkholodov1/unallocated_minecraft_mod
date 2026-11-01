package org.unallocated.smoke;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.MinecraftServer;
import org.slf4j.LoggerFactory;
import org.unallocated.bait.SpyglassBait;
import org.unallocated.client.bait.SpyglassBaitClient;

/**
 * Plays the spyglass bait in a real single-player world with real input: the figure appears ahead, three spyglass
 * uses bring it closer (screenshots of each), the third ends in the screamer (screenshot), then it is gone.
 */
@SuppressWarnings("UnstableApiUsage")
public final class SpyglassBaitSmokeTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("/time set noon");
            world.getServer().runCommand("/tp @a 0 -60 0 0 0");
            world.getServer().runCommand("/give @a minecraft:spyglass");
            context.waitTicks(10);
            world.getServer().runCommand("/execute as @p at @s run ubait ahead 3 40 12 true");
            world.getServer().waitFor(server -> status(server).contains("phase=WATCHING"), 100);
            context.waitTicks(10);
            context.takeScreenshot("unallocated-bait-1-plain-view");

            look(context, "unallocated-bait-2-first-sighting");
            require(world, "step=0/2 sightings=1", "first sighting registered without moving the figure");
            look(context, "unallocated-bait-3-closer");
            require(world, "step=1/2 sightings=2", "second use found it closer");

            context.getInput().holdKey(options -> options.keyUse);
            context.waitFor(client -> SpyglassBaitClient.scareVisible(), 160);
            context.waitTicks(6); // past the black cut: the face, the red stain and the scanlines
            context.takeScreenshot("unallocated-bait-4-screamer");
            context.getInput().releaseKey(options -> options.keyUse);
            world.getServer().waitFor(server -> status(server).equals("none"), 100);
            if (SpyglassBaitClient.scaresShown() != 1) {
                throw new AssertionError("Expected exactly one screamer, got " + SpyglassBaitClient.scaresShown());
            }
        }
        LoggerFactory.getLogger("unallocated-smoke").info(
                "UNALLOCATED spyglass smoke: three looks, closer each time, screamer, figure gone.");
    }

    /** One spyglass use: raise, hold on the figure, screenshot through the scope, lower. */
    private static void look(ClientGameTestContext context, String screenshot) {
        context.getInput().holdKey(options -> options.keyUse);
        context.waitTicks(24);
        context.takeScreenshot(screenshot);
        context.getInput().releaseKey(options -> options.keyUse);
        context.waitTicks(10);
    }

    private static void require(TestSingleplayerContext world, String expected, String meaning) {
        String status = world.getServer().computeOnServer(SpyglassBaitSmokeTest::status);
        if (!status.contains(expected)) throw new AssertionError(meaning + ": expected " + expected + " in " + status);
    }

    private static String status(MinecraftServer server) {
        return server.getPlayerList().getPlayers().isEmpty() ? "no player"
                : SpyglassBait.status(server.getPlayerList().getPlayers().getFirst());
    }
}
