package org.unallocated.client.interfacefx.mixin;

import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unallocated.client.interfacefx.InterfaceEffects;

@Mixin(AdvancementToast.class)
abstract class InterfaceToastMixin {
    @Inject(method = "getWantedVisibility", at = @At("HEAD"), cancellable = true)
    private void interface$cancelUnownedAdvancement(CallbackInfoReturnable<Toast.Visibility> cir) {
        if (InterfaceEffects.hideExpiredToast((AdvancementToast) (Object) this)) cir.setReturnValue(Toast.Visibility.HIDE);
    }
}
