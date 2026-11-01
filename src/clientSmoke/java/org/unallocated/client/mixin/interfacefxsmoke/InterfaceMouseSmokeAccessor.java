package org.unallocated.client.mixin.interfacefxsmoke;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** CI drives the actual consumer without depending on headless SDL focus/capture. */
@Mixin(MouseHandler.class)
public interface InterfaceMouseSmokeAccessor {
    @Accessor("accumulatedDX") void interfaceSmoke$setDeltaX(double value);
    @Accessor("accumulatedDY") void interfaceSmoke$setDeltaY(double value);
    @Invoker("turnPlayer") void interfaceSmoke$turnPlayer(double elapsedSeconds);
}
