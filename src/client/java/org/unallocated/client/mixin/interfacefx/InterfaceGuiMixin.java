package org.unallocated.client.mixin.interfacefx;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unallocated.client.interfacefx.InterfaceEffects;

@Mixin(Gui.class)
abstract class InterfaceGuiMixin {
    @Inject(method = "setScreen", at = @At("HEAD"))
    private void interface$restoreBeforePreferenceEditor(Screen screen, CallbackInfo ci) {
        if (screen instanceof OptionsScreen || screen instanceof OptionsSubScreen) {
            InterfaceEffects.restoreSettings();
        }
    }
}
