package org.unallocated.client.mixin.interfacefx;

import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unallocated.client.interfacefx.InterfaceEffects;

@Mixin(AdvancementToast.class)
abstract class InterfaceToastMixin {
    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"))
    private List<FormattedCharSequence> interface$alwaysShowTitle(Font font, FormattedText title, int width) {
        if (InterfaceEffects.ownsToast((AdvancementToast) (Object) this)) {
            // Native multiline toasts hide the title during their initial header-only phase.
            title = Component.literal(font.plainSubstrByWidth(title.getString(), width));
            InterfaceEffects.observedToastTitle();
        }
        return font.split(title, width);
    }

    @Inject(method = "getWantedVisibility", at = @At("HEAD"), cancellable = true)
    private void interface$cancelUnownedAdvancement(CallbackInfoReturnable<Toast.Visibility> cir) {
        if (InterfaceEffects.hideExpiredToast((AdvancementToast) (Object) this)) cir.setReturnValue(Toast.Visibility.HIDE);
    }
}
