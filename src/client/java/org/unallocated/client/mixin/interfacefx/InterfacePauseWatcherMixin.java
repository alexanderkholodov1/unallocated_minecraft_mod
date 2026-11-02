package org.unallocated.client.mixin.interfacefx;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The pause menu feels like a safe place, so once, maybe never, it is not (owner, Test 2). After the Heap, a pause menu
 * left alone with a still mouse slowly shows something standing at its edge, and "Back to Game" becomes "Back to me".
 * Move the mouse and it is gone, the button is itself again, and it never happens again in this game. No sound, no
 * screamer.
 */
@Mixin(PauseScreen.class)
abstract class InterfacePauseWatcherMixin extends Screen {
    @Unique private static boolean unallocated$spent;
    @Unique private boolean unallocated$armed;
    @Unique private int unallocated$still;
    @Unique private double unallocated$mouseX = -1, unallocated$mouseY = -1;
    @Unique private float unallocated$alpha;
    @Unique private Button unallocated$back;
    @Unique private Component unallocated$original;

    protected InterfacePauseWatcherMixin(Component title) { super(title); }

    @Inject(require = 0, method = "init()V", at = @At("TAIL"))
    private void interface$watch(CallbackInfo info) {
        var act = org.unallocated.client.core.ClientSession.act();
        if (unallocated$spent || unallocated$armed) { unallocated$findBack(); return; }
        if (act != org.unallocated.core.Act.LOOP && act != org.unallocated.core.Act.POCKET) return;
        if (!Boolean.getBoolean("unallocated.pause.watcher.force") && ThreadLocalRandom.current().nextFloat() > 0.3F) return;
        unallocated$armed = true;
        unallocated$findBack();
    }

    @Unique private void unallocated$findBack() {
        for (var child : children()) if (child instanceof Button button && button.getMessage().getContents() instanceof TranslatableContents key
                && key.getKey().equals("menu.returnToGame")) { unallocated$back = button; if (unallocated$original == null) unallocated$original = button.getMessage(); }
    }

    @Inject(require = 0, method = "tick()V", at = @At("TAIL"))
    private void interface$stillness(CallbackInfo info) {
        if (!unallocated$armed) return;
        Minecraft client = Minecraft.getInstance();
        double x = client.mouseHandler.xpos(), y = client.mouseHandler.ypos();
        boolean moved = unallocated$mouseX >= 0 && (Math.abs(x - unallocated$mouseX) > 2 || Math.abs(y - unallocated$mouseY) > 2);
        unallocated$mouseX = x; unallocated$mouseY = y;
        if (moved) {
            if (unallocated$still > 60) { // it was there: it leaves, and never comes back
                unallocated$armed = false; unallocated$alpha = 0;
                if (unallocated$back != null && unallocated$original != null) unallocated$back.setMessage(unallocated$original);
            }
            unallocated$still = 0;
            return;
        }
        unallocated$still++;
        if (unallocated$still > 60) {
            unallocated$spent = true;
            unallocated$alpha = Math.min(0.55F, unallocated$alpha + 0.006F);
            if (unallocated$still == 200 && unallocated$back != null) unallocated$back.setMessage(Component.translatable("pause.unallocated.back_to_me"));
        }
    }

    @Inject(require = 0, method = "extractRenderState", at = @At("TAIL"))
    private void interface$figure(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo info) {
        if (!unallocated$armed || unallocated$alpha <= 0) return;
        int a = (int) (unallocated$alpha * 255) << 24;
        int body = a | 0x070405, eye = ((int) (unallocated$alpha * 200) << 24) | 0xB01020;
        int x = width - 46, top = height / 3;
        graphics.fill(x + 6, top, x + 30, top + 26, body);          // head, half out of the frame
        graphics.fill(x, top + 28, x + 36, top + 92, body);         // shoulders and chest
        graphics.fill(x - 6, top + 30, x, top + 120, body);         // the long arm
        graphics.fill(x + 4, top + 92, x + 32, height, body);       // standing there
        graphics.fill(x + 11, top + 10, x + 14, top + 13, eye);
        graphics.fill(x + 21, top + 10, x + 24, top + 13, eye);
    }
}
