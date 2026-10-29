package org.unallocated.client.mixin.interfacefx;

import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.unallocated.client.interfacefx.InterfaceEffects;

@Mixin(LevelLoadingScreen.class)
abstract class InterfaceLoadingMixin {
    @Shadow @Final private static Component DOWNLOADING_TERRAIN_TEXT;

    @Redirect(require = 0, method = "extractRenderState", at = @At(value = "FIELD", target =
            "Lnet/minecraft/client/gui/screens/LevelLoadingScreen;DOWNLOADING_TERRAIN_TEXT:Lnet/minecraft/network/chat/Component;"))
    private Component interface$loadingLeak() {
        return InterfaceEffects.replacement(InterfaceEffects.Effect.LOADING, DOWNLOADING_TERRAIN_TEXT);
    }
}
