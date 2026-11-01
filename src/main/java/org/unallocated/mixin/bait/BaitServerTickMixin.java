package org.unallocated.mixin.bait;

import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unallocated.bait.SpyglassBait;

/** One server tick drives every active bait (also on a dedicated server with nobody online, for the self-test). */
@Mixin(MinecraftServer.class)
abstract class BaitServerTickMixin {
    @Inject(method = "tickServer", at = @At("TAIL"))
    private void unallocated$tickBait(BooleanSupplier haveTime, CallbackInfo ci) {
        SpyglassBait.tickServer((MinecraftServer) (Object) this);
    }
}
