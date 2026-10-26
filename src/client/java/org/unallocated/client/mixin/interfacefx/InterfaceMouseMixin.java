package org.unallocated.client.mixin.interfacefx;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.unallocated.client.interfacefx.InterfaceEffects;

@Mixin(MouseHandler.class)
abstract class InterfaceMouseMixin {
    // The first get() in 26.3 turnPlayer reads sensitivity; later reads are mouse inversion booleans.
    @Redirect(require = 0, method = "turnPlayer", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 0))
    private Object interface$perceivedSensitivity(OptionInstance<?> option) {
        return InterfaceEffects.perceivedSensitivity((Double) option.get());
    }
}
