package org.unallocated.creature.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Our shipped dimension uses known vanilla codecs; do not mark other custom dimensions stable. */
@Mixin(WorldDimensions.class)
abstract class PocketStabilityMixin {
    @Inject(method = "checkStability", at = @At("HEAD"), cancellable = true)
    private static void unallocated$knownPocket(ResourceKey<LevelStem> key, LevelStem stem, CallbackInfoReturnable<Lifecycle> result) {
        if (key.identifier().toString().equals("unallocated:pocket")) result.setReturnValue(Lifecycle.stable());
    }
}
