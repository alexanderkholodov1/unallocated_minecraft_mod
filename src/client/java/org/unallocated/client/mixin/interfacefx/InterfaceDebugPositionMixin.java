package org.unallocated.client.mixin.interfacefx;

import java.util.Collection;
import net.minecraft.client.gui.components.debug.DebugEntryPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.unallocated.client.interfacefx.InterfaceEffects;

@Mixin(DebugEntryPosition.class)
abstract class InterfaceDebugPositionMixin {
    @ModifyArg(require = 0, method = "display", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/components/debug/DebugScreenDisplayer;addToGroup(Lnet/minecraft/resources/Identifier;Ljava/util/Collection;)V"), index = 1)
    private Collection<String> interface$impossibleReadout(Collection<String> lines) {
        return InterfaceEffects.debugLines(lines);
    }
}
