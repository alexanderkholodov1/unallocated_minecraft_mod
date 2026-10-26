package org.unallocated.client.mixin.endingsmoke;

import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unallocated.smoke.EndingDisconnectProbe;

@Mixin(Connection.class)
abstract class EndingConnectionSmokeMixin {
    @Inject(method = "disconnect(Lnet/minecraft/network/DisconnectionDetails;)V", at = @At(
            value = "INVOKE", target = "Lio/netty/channel/Channel;close()Lio/netty/channel/ChannelFuture;",
            shift = At.Shift.AFTER))
    private void unallocated$inspectCloseWindow(DisconnectionDetails details, CallbackInfo ci) {
        EndingDisconnectProbe.afterChannelClose((Connection) (Object) this, details);
    }
}
