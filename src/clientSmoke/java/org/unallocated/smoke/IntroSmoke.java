package org.unallocated.smoke;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import com.mojang.blaze3d.platform.InputConstants;
import org.unallocated.client.launcher.IntroScreen;

/** A new copy first shows the arrival intro; like a player who skips it, press Esc and join the listed server. */
final class IntroSmoke {
    private static boolean captured;

    private IntroSmoke() {}

    static void join(ClientGameTestContext context) {
        context.waitForScreen(IntroScreen.class);
        context.waitTicks(40);
        if (!captured) { context.takeScreenshot("unallocated-intro"); captured = true; }
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(5);
        context.takeScreenshot("unallocated-server-list");
        context.clickScreenButton("intro.unallocated.join");
    }
}
