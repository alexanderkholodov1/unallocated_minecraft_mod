package org.unallocated.client.mixin.interfacefx;

import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.unallocated.client.interfacefx.InterfaceEffects;

@Mixin(SplashRenderer.class)
abstract class InterfaceSplashMixin {
    @Redirect(method = "extractRenderState", at = @At(value = "FIELD", target =
            "Lnet/minecraft/client/gui/components/SplashRenderer;splash:Lnet/minecraft/network/chat/Component;"))
    private Component interface$leakedSplash(SplashRenderer renderer) {
        return InterfaceEffects.replacement(InterfaceEffects.Effect.SPLASH,
                ((InterfaceSplashAccessor) renderer).interface$originalSplash());
    }
}
