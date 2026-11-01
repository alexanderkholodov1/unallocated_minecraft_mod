package org.unallocated.client.mixin.interfacefx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No player may ever be stuck: in a session the pause menu always offers the rescue (/unallocated next). */
@Mixin(PauseScreen.class)
abstract class InterfacePauseStuckMixin extends Screen {
    protected InterfacePauseStuckMixin(Component title) { super(title); }

    @Inject(require = 0, method = "init()V", at = @At("TAIL"))
    private void interface$stuck(CallbackInfo info) {
        var act = org.unallocated.client.core.ClientSession.act();
        if (act == org.unallocated.core.Act.CONTRACT || act == org.unallocated.core.Act.EPILOGUE) return;
        addRenderableWidget(Button.builder(Component.translatable("pause.unallocated.stuck"), button -> {
            Minecraft client = Minecraft.getInstance();
            if (client.getConnection() != null) client.getConnection().sendCommand("unallocated next");
            client.gui.setScreen(null);
        }).bounds(8, 8, 140, 20).build());
    }
}
