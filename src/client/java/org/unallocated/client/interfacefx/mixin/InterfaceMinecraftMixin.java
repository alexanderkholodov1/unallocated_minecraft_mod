package org.unallocated.client.interfacefx.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unallocated.client.interfacefx.InterfaceEffects;
import org.unallocated.client.interfacefx.InterfaceOptionsGuard;

@Mixin(Minecraft.class)
abstract class InterfaceMinecraftMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void interface$expire(CallbackInfo ci) { InterfaceEffects.tick(); }

    @Inject(method = {"disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", "clearClientLevel"}, at = @At("HEAD"))
    private void interface$restoreOnWorldExit(CallbackInfo ci) { InterfaceEffects.leaveWorld(); }

    @Inject(method = "close", at = @At("HEAD"))
    private void interface$restoreBeforeClose(CallbackInfo ci) { InterfaceEffects.clear(); }

    @Inject(method = "close", at = @At("RETURN"))
    private void interface$restoreAfterFinalSave(CallbackInfo ci) { InterfaceOptionsGuard.shutdown(); }
}
