package org.unallocated.client.mixin.interfacefx;

import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unallocated.client.interfacefx.InterfaceDebugCommand;
import org.unallocated.client.interfacefx.InterfaceEffects;

@Mixin(ChatScreen.class)
abstract class InterfaceChatMixin {
    @Inject(require = 0, method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void interface$localDebugCommand(String message, boolean addToRecent, CallbackInfo ci) {
        if (InterfaceDebugCommand.handle(message)) {
            InterfaceEffects.observedChatCommand();
            ci.cancel();
        }
    }
}
