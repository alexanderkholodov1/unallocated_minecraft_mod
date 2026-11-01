package org.unallocated.client.mixin.interfacefx;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unallocated.client.interfacefx.InterfaceEffects;

@Mixin(Minecraft.class)
abstract class InterfaceMinecraftMixin {
    @Inject(require = 0, method = "tick", at = @At("HEAD"))
    private void interface$expire(CallbackInfo ci) { InterfaceEffects.tick(); }

    @Inject(require = 0, method = {"disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", "clearClientLevel"}, at = @At("HEAD"))
    private void interface$restoreOnWorldExit(CallbackInfo ci) { InterfaceEffects.leaveWorld(); }

    @Inject(require = 0, method = "close", at = @At("HEAD"))
    private void interface$restoreBeforeClose(CallbackInfo ci) { InterfaceEffects.clear(); }

}
