package org.unallocated.client.mixin.interfacefx;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unallocated.client.interfacefx.InterfaceEffects;

@Mixin(Camera.class)
abstract class InterfaceCameraMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void interface$perceivedFov(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(InterfaceEffects.perceivedFov(cir.getReturnValueF()));
    }
}
