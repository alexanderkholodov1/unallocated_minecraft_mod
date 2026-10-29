package org.unallocated.mixin.bait;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unallocated.bait.SpyglassBaitFigure;

/** The figure is never written to the save: quitting mid-sequence leaves the player's world unchanged. */
@Mixin(Entity.class)
abstract class BaitEntitySaveMixin {
    @Inject(method = "shouldBeSaved", at = @At("HEAD"), cancellable = true)
    private void unallocated$neverSaveBait(CallbackInfoReturnable<Boolean> cir) {
        if (SpyglassBaitFigure.isBait((Entity) (Object) this)) cir.setReturnValue(false);
    }
}
