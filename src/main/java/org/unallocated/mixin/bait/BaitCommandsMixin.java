package org.unallocated.mixin.bait;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unallocated.bait.SpyglassBaitCommand;

/** Registers {@code /ubait} without Fabric API, at the same point Fabric's own command API uses in 26.3. */
@Mixin(Commands.class)
abstract class BaitCommandsMixin {
    @Shadow
    @Final
    private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>", at = @At(value = "INVOKE",
            target = "Lcom/mojang/brigadier/CommandDispatcher;setConsumer(Lcom/mojang/brigadier/ResultConsumer;)V"))
    private void unallocated$registerBait(Commands.CommandSelection selection, CommandBuildContext context,
            CallbackInfo ci) {
        SpyglassBaitCommand.register(this.dispatcher);
    }
}
