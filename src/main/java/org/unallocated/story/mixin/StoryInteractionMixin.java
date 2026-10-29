package org.unallocated.story.mixin;

import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unallocated.story.StoryRuntime;

@Mixin(ServerPlayerGameMode.class)
abstract class StoryInteractionMixin {
    @Shadow protected ServerPlayer player;
    @Unique private String unallocated$broken;
    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void unallocated$interact(ServerPlayer p, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> ci) {
        StoryRuntime.signal(p, "interact", "", hit.getBlockPos());
    }
    @Inject(method = "useItem", at = @At("HEAD"))
    private void unallocated$use(ServerPlayer p, Level level, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> ci) {
        StoryRuntime.signal(p, "use_item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), null);
    }
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void unallocated$beforeBreak(BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
        unallocated$broken = BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(pos).getBlock()).toString();
    }
    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void unallocated$break(BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
        if (ci.getReturnValue()) StoryRuntime.signal(player, "break", unallocated$broken, pos);
    }
}
