package org.unallocated.client.mixin.interfacefx;

import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Inside a session the Reclaimer writes the death screen. It lags: "I agree" slips away from the cursor a few times,
 * and a button nobody asked for, "Not yet", flickers in late. "Not yet" keeps you playing dead (LORE 1.10).
 */
@Mixin(DeathScreen.class)
abstract class InterfaceDeathPuppetMixin extends Screen {
    @Shadow @Final private List<Button> exitButtons;
    @Shadow @Final private LocalPlayer player;
    @Unique private Button unallocated$stay;
    @Unique private int unallocated$age, unallocated$escapes;
    @Unique private final Random unallocated$random = new Random();

    protected InterfaceDeathPuppetMixin(Component title) { super(title); }

    @Unique private static boolean unallocated$session() {
        var act = org.unallocated.client.core.ClientSession.act();
        return act != org.unallocated.core.Act.CONTRACT && act != org.unallocated.core.Act.EPILOGUE;
    }

    @Inject(require = 0, method = "init()V", at = @At("TAIL"))
    private void interface$stayDead(CallbackInfo info) {
        if (!unallocated$session()) return;
        unallocated$age = 0; unallocated$escapes = 0;
        unallocated$stay = addRenderableWidget(Button.builder(Component.translatable("death.unallocated.not_yet"), button -> {
                button.active = false;
                player.connection.sendCommand("unallocated puppet");
                player.respawn();
            }).bounds(width / 2 - 100, height / 4 + 120, 200, 20).build());
        unallocated$stay.active = false;
        unallocated$stay.visible = false;
    }

    @Inject(require = 0, method = "tick()V", at = @At("TAIL"))
    private void interface$lag(CallbackInfo info) {
        if (unallocated$stay == null || !unallocated$session()) return;
        unallocated$age++;
        // "Not yet" arrives late, stuttering in like a frame that did not load.
        if (unallocated$age < 45) unallocated$stay.visible = false;
        else if (unallocated$age < 70) unallocated$stay.visible = unallocated$random.nextInt(3) != 0;
        else unallocated$stay.visible = true;
        unallocated$stay.active = unallocated$age >= 70;
        // "I agree" slips away from the cursor, a few times.
        if (exitButtons.isEmpty() || unallocated$escapes >= 3 || unallocated$age < 20) return;
        Button agree = exitButtons.getFirst();
        Minecraft mc = Minecraft.getInstance();
        double mx = mc.mouseHandler.xpos() * width / Math.max(1, mc.getWindow().getScreenWidth());
        double my = mc.mouseHandler.ypos() * height / Math.max(1, mc.getWindow().getScreenHeight());
        if (agree.isMouseOver(mx, my)) {
            unallocated$escapes++;
            int nx = Math.max(4, Math.min(width - agree.getWidth() - 4, (int) (width / 2 - 100 + (unallocated$random.nextBoolean() ? 1 : -1) * (60 + unallocated$random.nextInt(80)))));
            int ny = Math.max(height / 4, Math.min(height - 30, height / 4 + 40 + unallocated$random.nextInt(90)));
            agree.setPosition(nx, ny);
        }
    }
}
