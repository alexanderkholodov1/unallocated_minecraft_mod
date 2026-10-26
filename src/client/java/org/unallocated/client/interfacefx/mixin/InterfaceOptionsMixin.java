package org.unallocated.client.interfacefx.mixin;

import java.io.File;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unallocated.client.interfacefx.InterfaceOptionsGuard;

@Mixin(Options.class)
abstract class InterfaceOptionsMixin {
    @Shadow @Final private File optionsFile;

    @Inject(method = "load", at = @At("HEAD"))
    private void interface$recoverBeforeRead(CallbackInfo ci) {
        InterfaceOptionsGuard.beforeLoad(optionsFile);
    }

    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void interface$protectOriginalBytes(CallbackInfo ci) {
        if (InterfaceOptionsGuard.blocksSave()) ci.cancel();
    }
}
