package org.unallocated.client.mixin.interfacefx;

import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unallocated.client.interfacefx.InterfaceEffects;
import org.unallocated.client.interfacefx.InterfaceTexts;

@Mixin(AdvancementToast.class)
abstract class InterfaceToastMixin {
    @Redirect(require = 0, method = "extractRenderState", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"))
    private List<FormattedCharSequence> interface$alwaysShowTitle(Font font, FormattedText title, int width) {
        if (InterfaceEffects.inventedToast((AdvancementToast) (Object) this)) {
            // Native multiline toasts hide the title during their initial header-only phase.
            title = InterfaceTexts.fitInventedTitle(font, title, width);
            InterfaceEffects.observedToastTitle();
        } else if (InterfaceEffects.ownsToast((AdvancementToast) (Object) this)) {
            InterfaceEffects.observedVanillaToastTitle();
        }
        return font.split(title, width);
    }

    @Inject(require = 0, method = "getWantedVisibility", at = @At("HEAD"), cancellable = true)
    private void interface$cancelUnownedAdvancement(CallbackInfoReturnable<Toast.Visibility> cir) {
        if (InterfaceEffects.hideExpiredToast((AdvancementToast) (Object) this)) cir.setReturnValue(Toast.Visibility.HIDE);
    }
}
