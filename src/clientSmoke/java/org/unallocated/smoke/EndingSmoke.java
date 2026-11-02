package org.unallocated.smoke;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import org.unallocated.client.ending.EndingScreen;
import org.unallocated.ending.EndingText;

/** Story endings are typed on black (EndingScreen); built-in ones still use the vanilla disconnect screen. */
final class EndingSmoke {
    private EndingSmoke() {}

    static boolean shown(Screen screen) { return screen instanceof EndingScreen || screen instanceof DisconnectedScreen; }

    /** Reads every card of a typed ending (space skips the typing, then advances) until the return button exists. */
    static void readToTheEnd(ClientGameTestContext context) {
        for (int i = 0; i < 80 && context.computeOnClient(client -> client.gui.screen() instanceof EndingScreen ending && !ending.finished()); i++) {
            context.getInput().pressKey(InputConstants.KEY_SPACE);
            context.waitTicks(3);
        }
    }

    static void returnToTitle(ClientGameTestContext context) {
        readToTheEnd(context);
        context.clickScreenButton(EndingText.RETURN_BUTTON);
    }
}
