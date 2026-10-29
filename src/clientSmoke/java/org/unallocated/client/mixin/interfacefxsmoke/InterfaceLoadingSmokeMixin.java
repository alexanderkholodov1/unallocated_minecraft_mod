package org.unallocated.client.mixin.interfacefxsmoke;

import net.minecraft.client.gui.screens.LevelLoadingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unallocated.smoke.LoadingTransitionProbe;

@Mixin(LevelLoadingScreen.class)
abstract class InterfaceLoadingSmokeMixin {
    // Only defer dismissal: the real tracker, transition and render path keep running unchanged.
    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
    private void interfaceSmoke$holdRealTransition(CallbackInfo ci) {
        if (LoadingTransitionProbe.holding()) ci.cancel();
    }
}
