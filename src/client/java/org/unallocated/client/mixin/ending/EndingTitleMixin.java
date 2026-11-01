package org.unallocated.client.mixin.ending;

import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unallocated.client.ending.EndingClient;
import org.unallocated.ending.EndingText;

@Mixin(TitleScreen.class)
public abstract class EndingTitleMixin {
    @Shadow private SplashRenderer splash;

    @Inject(method = "init", at = @At("TAIL"))
    private void unallocated$endingSplash(CallbackInfo callback) {
        if (EndingClient.hasEndingSplash()) splash = new SplashRenderer(Component.literal(EndingText.SPLASH));
    }
}
