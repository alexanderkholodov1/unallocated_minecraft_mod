package org.unallocated.client.mixin.interfacefx;

import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Inside a session the death screen offers to stay dead: "Not yet" respawns you as a Puppet (LORE 1.10). */
@Mixin(DeathScreen.class)
abstract class InterfaceDeathPuppetMixin extends Screen {
    @Shadow @Final private List<Button> exitButtons;
    @Shadow @Final private LocalPlayer player;

    protected InterfaceDeathPuppetMixin(Component title) { super(title); }

    @Inject(require = 0, method = "init()V", at = @At("TAIL"))
    private void interface$stayDead(CallbackInfo info) {
        var act = org.unallocated.client.core.ClientSession.act();
        if (act == org.unallocated.core.Act.CONTRACT || act == org.unallocated.core.Act.EPILOGUE) return;
        Button stay = addRenderableWidget(Button.builder(Component.translatable("death.unallocated.not_yet"), button -> {
                button.active = false;
                player.connection.sendCommand("unallocated puppet");
                player.respawn();
            }).bounds(width / 2 - 100, height / 4 + 120, 200, 20).build());
        stay.active = false;
        exitButtons.add(stay);
    }
}
