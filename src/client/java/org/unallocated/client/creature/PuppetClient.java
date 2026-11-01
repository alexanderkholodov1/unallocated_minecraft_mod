package org.unallocated.client.creature;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.unallocated.content.ThreadEntity;

/**
 * The Puppet state on this client (LORE 1.10): after "Not yet" on the death screen you keep playing dead, see the
 * threads, and come back whenever you want by pressing R and agreeing again.
 */
public final class PuppetClient {
    private static boolean puppet;
    private static boolean rWasDown;

    private PuppetClient() {}

    public static boolean active() { return puppet; }

    public static void set(boolean on) {
        puppet = on;
        ThreadEntity.clientPuppet = on;
        Minecraft client = Minecraft.getInstance();
        if (!on && client.gui.screen() instanceof ReviveScreen) client.gui.setScreen(null);
    }

    public static void initialize() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> set(false));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean down = puppet && client.player != null && client.gui.screen() == null
                && InputConstants.isKeyDown(InputConstants.KEY_R);
            if (down && !rWasDown) client.gui.setScreen(new ReviveScreen());
            rWasDown = down;
        });
    }

    /** Red-black vignette and the two lines a Puppet needs: what you are, and how to come back. */
    public static void overlay(GuiGraphicsExtractor graphics) {
        if (!puppet) return;
        Minecraft client = Minecraft.getInstance();
        int w = graphics.guiWidth(), h = graphics.guiHeight();
        for (int i = 0; i < 12; i++) {
            int alpha = (int) (0x70 * (1 - i / 12F)) << 24;
            int color = alpha | 0x3A0006;
            graphics.fill(0, i * 3, w, i * 3 + 3, color);
            graphics.fill(0, h - i * 3 - 3, w, h - i * 3, color);
            graphics.fill(i * 3, 0, i * 3 + 3, h, color);
            graphics.fill(w - i * 3 - 3, 0, w - i * 3, h, color);
        }
        var font = client.font;
        Component top = Component.translatable("puppet.unallocated.state");
        graphics.text(font, top, (w - font.width(top)) / 2, 8, 0xFFE05050, true);
        Component hint = Component.translatable("puppet.unallocated.revive_hint");
        graphics.text(font, hint, (w - font.width(hint)) / 2, h - 58, 0xFFD8D4CB, true);
    }
}
