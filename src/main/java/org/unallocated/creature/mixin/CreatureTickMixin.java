package org.unallocated.creature.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unallocated.creature.Creatures;

/** Story bodies have runtime steering; vanilla projectiles and mobs must also obey hold. */
@Mixin(ServerLevel.class)
abstract class CreatureTickMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void unallocated$runtimeControlsBody(Entity entity, CallbackInfo result) {
        if (entity.entityTags().contains(Creatures.TAG)) result.cancel();
    }
}
