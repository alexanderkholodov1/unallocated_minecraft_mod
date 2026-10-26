package org.unallocated.client.interfacefx.mixin;

import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unallocated.client.interfacefx.InterfaceEffects;

@Mixin(AbstractSignRenderer.class)
abstract class InterfaceSignMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at = @At("RETURN"))
    private void interface$languageInRenderState(SignBlockEntity sign, SignRenderState state,
            float partialTicks, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breaking, CallbackInfo ci) {
        state.frontText = InterfaceEffects.signText(sign.getBlockPos(), state.frontText);
    }
}
