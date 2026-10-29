package org.unallocated.client.mixin.bait;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unallocated.client.bait.SpyglassBaitClient;

/** The bait must be visible as a distant figure; vanilla would cull it at about 64 blocks. */
@Mixin(Entity.class)
abstract class BaitEntityRenderDistanceMixin {
    @Inject(method = "shouldRenderAtSqrDistance(D)Z", at = @At("HEAD"), cancellable = true)
    private void unallocated$renderBaitFar(double distanceSquared, CallbackInfoReturnable<Boolean> cir) {
        if (SpyglassBaitClient.renderFar((Entity) (Object) this, distanceSquared)) cir.setReturnValue(true);
    }
}
