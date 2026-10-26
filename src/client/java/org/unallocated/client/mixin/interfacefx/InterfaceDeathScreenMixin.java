package org.unallocated.client.mixin.interfacefx;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.unallocated.client.interfacefx.InterfaceEffects;

@Mixin(DeathScreen.class)
abstract class InterfaceDeathScreenMixin {
    @Redirect(method = "visitText(Lnet/minecraft/client/gui/ActiveTextCollector;)V", at = @At(value = "FIELD", target =
            "Lnet/minecraft/client/gui/screens/DeathScreen;causeOfDeath:Lnet/minecraft/network/chat/Component;"))
    private Component interface$entityVoice(DeathScreen screen) {
        return InterfaceEffects.replacement(InterfaceEffects.Effect.DEATH,
                ((InterfaceDeathScreenAccessor) screen).interface$originalCause());
    }
}
