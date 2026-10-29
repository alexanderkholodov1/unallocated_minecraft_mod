package org.unallocated.client.mixin.interfacefx;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unallocated.client.interfacefx.InterfaceEffects;

@Mixin(Camera.class)
abstract class InterfaceCameraMixin {
    // Camera.update stores this result and passes that field to setupPerspective; getFov is only a reader.
    @Inject(require = 0, method = "calculateFov(F)F", at = @At("RETURN"), cancellable = true)
    private void interface$perceivedFov(float partialTick, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(InterfaceEffects.perceivedFov(cir.getReturnValueF()));
    }
}
