package org.unallocated.client.mixin.interfacefx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unallocated.client.interfacefx.InterfaceEffects;

@Mixin(LightmapRenderStateExtractor.class)
abstract class InterfaceLightmapMixin {
    @Shadow private boolean needsUpdate;
    @Unique private float interface$darkness;
    @Unique private boolean interface$wasActive;

    @Inject(require = 0, method = "extract", at = @At("HEAD"))
    private void interface$updatePerception(LightmapRenderState state, float partialTick, CallbackInfo ci) {
        interface$darkness = InterfaceEffects.perceivedDarkness();
        if (interface$darkness > 0 || interface$wasActive) needsUpdate = true;
        interface$wasActive = interface$darkness > 0;
    }

    @Inject(require = 0, method = "extract", at = @At("RETURN"))
    private void interface$perceivedDarkness(LightmapRenderState state, float partialTick, CallbackInfo ci) {
        if (interface$darkness > 0) {
            float original = state.darknessEffectScale;
            state.darknessEffectScale += interface$darkness * Minecraft.getInstance().options.darknessEffectScale().get().floatValue();
            state.needsUpdate = true;
            InterfaceEffects.applied(InterfaceEffects.Effect.GAMMA, original, state.darknessEffectScale);
        }
    }
}
