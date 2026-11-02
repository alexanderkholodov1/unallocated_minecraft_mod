package org.unallocated.smoke;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unallocated.client.ending.EndingScreen;
import org.unallocated.core.Act;
import org.unallocated.story.StoryRuntime;

/**
 * A guided real-time playthrough for review, not a judge: it plays the shipped story with real timing, types into the
 * real chat, jumps between scenes with server commands where waiting would add nothing, and takes a screenshot of every
 * scene a human should look at (Heap, death screen, Puppet threads and souls, the console, the descent, the fight, the
 * watchdog window, its wake, the chosen ending's animation and every card of the typed ending). It also asserts the
 * promises made to the owner: falling into the void returns you to the fight, the window opens on kill, and the ending
 * is typed on black. Run: python scripts/local_play.py --phase tour [--tour-ending POISON].
 */
final class TourSmokeTest {
    private static final Logger LOG = LoggerFactory.getLogger("unallocated-smoke");
    private static int shot;

    private TourSmokeTest() {}

    static void run(ClientGameTestContext context) {
        String ending = System.getProperty("unallocated.tour.ending", "COMPLETE");
        context.clickScreenButton("menu.unallocated.play");
        IntroSmoke.join(context);
        context.waitFor(client -> client.player != null && client.level != null && client.gui.screen() == null, 2400);
        context.waitTicks(120);
        shot(context, "hub");

        // Noor's mine: the floor gives way at the far end.
        cmd(context, "execute in minecraft:overworld run tp @p 98.5 64 20.5 -90 0");
        waitAct(context, Act.THEFT, 400);
        context.waitTicks(70);
        shot(context, "heap-mine");
        cmd(context, "execute in unallocated:pocket run tp @p 70.5 70 28.5 facing 53 65 12");
        context.waitTicks(40);
        shot(context, "heap-world");

        // The first death: a scare, never a chase.
        onServer(context, server -> StoryRuntime.get(server).fire("heap.take"));
        context.waitTicks(9);
        shot(context, "heap-screamer");
        deathScreen(context, true);

        // The Puppet: threads to the sky, fallen souls replaying loops.
        waitAct(context, Act.LOOP, 400);
        context.waitFor(client -> client.player != null && client.player.isAlive() && client.gui.screen() == null, 400);
        cmd(context, "execute in unallocated:pocket run tp @p 53.5 64 12.5 90 0");
        context.waitTicks(80);
        shot(context, "puppet-souls");
        cmd(context, "execute as @p at @s run tp @s ~ ~ ~ ~ -55");
        context.waitTicks(20);
        shot(context, "puppet-sky-threads");
        cmd(context, "execute as @p at @s run tp @s ~ ~ ~ ~ 0");

        // The pause menu is not always safe (forced for the tour): still mouse, something at its edge.
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(260);
        shot(context, "pause-watcher");
        context.getInput().moveCursor(40, 10);
        context.waitTicks(5);
        shot(context, "pause-watcher-gone");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(10);

        // The console answers the dead.
        type(context, "help");
        type(context, "cat README");
        type(context, "cat home/noor/1.txt");
        chatShot(context, "shell-gen1");
        // Reading the note of your own depth brings it down 12 s later.
        context.waitTicks(250);
        cmd(context, "execute as @p at @s run tp @s ~ ~ ~ ~ -80");
        context.waitTicks(40);
        shot(context, "descent");
        deathScreen(context, false);

        // Straight to the third depth: the first word typed brings it down for good.
        waitAlive(context);
        onServer(context, server -> { StoryRuntime.get(server).state().setCounter("story.deaths", 3); return null; });
        type(context, "ps");
        chatShot(context, "fight-ps");
        context.waitTicks(90);
        cmd(context, "execute as @p at @s run tp @s ~ ~ ~ facing entity @e[tag=unallocated.giant,limit=1] eyes");
        context.waitTicks(5);
        shot(context, "fight-giant");
        context.waitTicks(25);
        shot(context, "fight-lash");
        context.waitTicks(250);
        cmd(context, "execute as @p at @s run tp @s ~ ~ ~ ~ 25");
        context.waitTicks(5);
        shot(context, "fight-collapse");
        // A fall into the void is never the end: it spits you back into the fight.
        cmd(context, "execute as @p at @s run tp @s ~ 20 ~");
        context.waitTicks(40);
        double y = context.computeOnClient(client -> client.player.getY());
        if (y < 50) throw new AssertionError("A fall into the void did not return the player to the fight (y=" + y + ")");
        if (context.computeOnClient(client -> client.gui.screen() instanceof EndingScreen))
            throw new AssertionError("A fall into the void ended the game");
        LOG.info("UNALLOCATED tour: void fall returned the player to the fight at y={}", y);

        // One kill on the right number stops everything; the watchdog wakes it after 15 s of frozen time.
        type(context, "kill 7331");
        context.waitTicks(20);
        shot(context, "window");
        chatShot(context, "window-notes");
        if (!onServer(context, server -> StoryRuntime.get(server).state().flags().contains("story.flag.suspended")))
            throw new AssertionError("kill 7331 did not open the watchdog window");
        context.waitTicks(320);
        shot(context, "window-wake");
        type(context, "kill 7331");
        context.waitTicks(20);
        shot(context, "window-refrozen");

        // The ending the reviewer asked for, its animation, then the typed text.
        type(context, ending.equals("POISON") ? "echo reclaimer >> dataset" : "set success_threshold 0.82");
        int[] at = ending.equals("POISON") ? new int[] {100, 200, 400} : new int[] {120, 300, 480};
        int waited = 0;
        for (int i = 0; i < at.length; i++) {
            context.waitTicks(at[i] - waited); waited = at[i];
            shot(context, ending.toLowerCase() + "-" + (i + 1));
        }
        context.waitFor(client -> client.gui.screen() instanceof EndingScreen, 2400);
        context.waitTicks(100);
        shot(context, "ending-card-1");
        for (int card = 2; card <= 12 && !context.computeOnClient(client -> client.gui.screen() instanceof EndingScreen e && e.finished()); card++) {
            context.getInput().pressKey(InputConstants.KEY_SPACE);
            context.waitTicks(4);
            context.getInput().pressKey(InputConstants.KEY_SPACE);
            context.waitTicks(4);
            shot(context, "ending-card-" + card);
        }
        EndingSmoke.returnToTitle(context);
        context.waitForScreen(TitleScreen.class);
        LOG.info("UNALLOCATED tour: {} reached and typed on black; {} screenshots", ending, shot);
    }

    private static void deathScreen(ClientGameTestContext context, boolean capture) {
        context.waitFor(client -> client.gui.screen() instanceof DeathScreen, 1200);
        context.waitTicks(50);
        if (capture) shot(context, "death-screen-lag");
        context.waitTicks(40);
        if (capture) shot(context, "death-screen");
        context.clickScreenButton("death.unallocated.not_yet");
        context.waitFor(client -> !(client.gui.screen() instanceof DeathScreen), 600);
    }

    private static void waitAlive(ClientGameTestContext context) {
        context.waitFor(client -> client.player != null && client.player.isAlive() && client.gui.screen() == null, 1200);
        context.waitTicks(60);
    }

    private static void waitAct(ClientGameTestContext context, Act act, int ticks) {
        context.waitFor(client -> org.unallocated.client.core.ClientSession.act() == act, ticks);
    }

    /** Types a line into the real chat and sends it, like a player. */
    private static void type(ClientGameTestContext context, String line) {
        context.getInput().pressKey(options -> options.keyChat);
        context.waitTicks(5);
        context.getInput().typeChars(line);
        context.waitTicks(3);
        context.getInput().pressKey(InputConstants.KEY_RETURN);
        context.waitTicks(20);
    }

    /** A screenshot with the chat open, so the console's answers are readable. */
    private static void chatShot(ClientGameTestContext context, String name) {
        context.getInput().pressKey(options -> options.keyChat);
        context.waitTicks(5);
        shot(context, name);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(3);
    }

    private static void shot(ClientGameTestContext context, String name) {
        context.takeScreenshot(String.format("tour-%02d-%s", ++shot, name));
    }

    private static void cmd(ClientGameTestContext context, String command) {
        onServer(context, server -> { server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command); return null; });
    }

    /** The game is frozen between test steps: queue the task on the server, then let ticks run until it is done. */
    private static <T> T onServer(ClientGameTestContext context, Function<MinecraftServer, T> action) {
        MinecraftServer server = context.computeOnClient(Minecraft::getSingleplayerServer);
        if (server == null) throw new AssertionError("No integrated server");
        var result = new java.util.concurrent.atomic.AtomicReference<T>();
        var done = new java.util.concurrent.atomic.AtomicBoolean();
        server.execute(() -> { try { result.set(action.apply(server)); } finally { done.set(true); } });
        context.waitFor(client -> done.get(), 200);
        return result.get();
    }
}
