package org.unallocated.smoke;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import org.slf4j.LoggerFactory;
import org.unallocated.ending.EndingText;

/**
 * The laziest possible judge: joins, never moves, never reads, and agrees to come back on every death. The story must
 * still reach an ending on its own (nudges, chapter time limits, forced scares). Hanging here means a softlock.
 */
final class IdleSmokeTest {
    private IdleSmokeTest() {}

    static void run(ClientGameTestContext context) {
        var log = LoggerFactory.getLogger("unallocated-smoke");
        context.clickScreenButton("menu.unallocated.play");
        IntroSmoke.join(context);
        long start = System.currentTimeMillis(), deadline = start + 35L * 60_000L, nextShot = start + 90_000L;
        int shot = 0;
        while (System.currentTimeMillis() < deadline) {
            context.waitTicks(20);
            if (context.computeOnClient(client -> EndingSmoke.shown(client.gui.screen()))) {
                context.waitTicks(40);
                context.takeScreenshot("idle-farewell");
                log.info("UNALLOCATED smoke: idle player reached an ending after {} s", (System.currentTimeMillis() - start) / 1000);
                EndingSmoke.returnToTitle(context);
                return;
            }
            if (context.computeOnClient(client -> client.gui.screen() instanceof DeathScreen)) {
                context.waitTicks(30);
                context.clickScreenButton("death.unallocated.consent");
            }
            if (System.currentTimeMillis() > nextShot) {
                context.takeScreenshot("idle-" + shot++);
                nextShot += 90_000L;
            }
        }
        throw new AssertionError("An idle player never reached an ending: softlock");
    }
}
