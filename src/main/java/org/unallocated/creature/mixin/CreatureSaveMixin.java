package org.unallocated.creature.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unallocated.creature.Creatures;

@Mixin(Entity.class)
abstract class CreatureSaveMixin {
    @Inject(method = "shouldBeSaved", at = @At("HEAD"), cancellable = true)
    private void unallocated$neverSaveCreature(CallbackInfoReturnable<Boolean> result) {
        if (((Entity) (Object) this).entityTags().contains(Creatures.TAG)) result.setReturnValue(false);
    }
}
