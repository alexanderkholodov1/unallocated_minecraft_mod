package org.unallocated.client.mixin.ending;

import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unallocated.ending.EndingText;

/** Publish our tagged farewell before Netty makes channel closure visible to the render thread. */
@Mixin(Connection.class)
abstract class EndingConnectionMixin {
    @Shadow private DisconnectionDetails disconnectionDetails;

    @Inject(method = "disconnect(Lnet/minecraft/network/DisconnectionDetails;)V", at = @At("HEAD"))
    private void unallocated$publishEndingReason(DisconnectionDetails details, CallbackInfo ci) {
        // Vanilla 26.3 assigns this field after channel.close().awaitUninterruptibly().
        // A concurrent handleDisconnection() can otherwise consume the generic fallback first.
        if (details.reason().getContents() instanceof TranslatableContents text
                && EndingText.REASON_KEY.equals(text.getKey())) {
            disconnectionDetails = details;
        }
    }
}
