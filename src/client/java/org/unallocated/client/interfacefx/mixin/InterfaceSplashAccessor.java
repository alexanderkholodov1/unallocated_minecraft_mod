package org.unallocated.client.interfacefx.mixin;

import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SplashRenderer.class)
public interface InterfaceSplashAccessor {
    @Accessor("splash") Component interface$originalSplash();
}
