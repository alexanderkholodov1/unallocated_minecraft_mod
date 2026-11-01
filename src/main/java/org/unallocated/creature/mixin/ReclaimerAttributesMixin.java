package org.unallocated.creature.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unallocated.content.UnallocatedContent;

/** Vanilla keeps living-entity attributes in an immutable map; the Reclaimer's are answered here instead. */
@Mixin(DefaultAttributes.class)
abstract class ReclaimerAttributesMixin {
    @Inject(method = "getSupplier", at = @At("HEAD"), cancellable = true)
    private static void unallocated$reclaimerSupplier(EntityType<? extends LivingEntity> type, CallbackInfoReturnable<AttributeSupplier> result) {
        if (type == UnallocatedContent.RECLAIMER) result.setReturnValue(UnallocatedContent.reclaimerAttributes());
    }

    @Inject(method = "hasSupplier", at = @At("HEAD"), cancellable = true)
    private static void unallocated$reclaimerHasSupplier(EntityType<?> type, CallbackInfoReturnable<Boolean> result) {
        if (type == UnallocatedContent.RECLAIMER) result.setReturnValue(true);
    }
}
