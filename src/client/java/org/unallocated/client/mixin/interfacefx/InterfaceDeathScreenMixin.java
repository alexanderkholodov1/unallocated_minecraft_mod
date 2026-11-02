package org.unallocated.client.mixin.interfacefx;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.unallocated.client.interfacefx.InterfaceEffects;

@Mixin(DeathScreen.class)
abstract class InterfaceDeathScreenMixin {
    @Redirect(require = 0, method = "visitText(Lnet/minecraft/client/gui/ActiveTextCollector;)V", at = @At(value = "FIELD", target =
            "Lnet/minecraft/client/gui/screens/DeathScreen;causeOfDeath:Lnet/minecraft/network/chat/Component;"))
    private Component interface$entityVoice(DeathScreen screen) {
        return InterfaceEffects.replacement(InterfaceEffects.Effect.DEATH,
                ((InterfaceDeathScreenAccessor) screen).interface$originalCause());
    }

    /** Inside a session the protocol asks for consent again on every trial: the respawn button reads "I agree". */
    @ModifyVariable(require = 0, method = "init()V", at = @At("STORE"), ordinal = 0)
    private Component interface$consentAgain(Component respawn) {
        var act = org.unallocated.client.core.ClientSession.act();
        return act == org.unallocated.core.Act.CONTRACT || act == org.unallocated.core.Act.EPILOGUE
            ? respawn : Component.translatable("death.unallocated.consent");
    }
}
