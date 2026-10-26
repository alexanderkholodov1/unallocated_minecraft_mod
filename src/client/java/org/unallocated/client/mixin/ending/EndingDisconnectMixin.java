package org.unallocated.client.mixin.ending;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.DisconnectionDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.unallocated.client.ending.EndingClient;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class EndingDisconnectMixin {
    @Inject(method = "createDisconnectScreen", at = @At("HEAD"), cancellable = true)
    private void unallocated$endingScreen(DisconnectionDetails details, CallbackInfoReturnable<Screen> callback) {
        Screen screen = EndingClient.screen(details);
        if (screen != null) callback.setReturnValue(screen);
    }
}
