package org.unallocated.story.mixin.client;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(TitleScreen.class)
public interface StorySplashAccessor {
    @Accessor("splash") void unallocated$storySplash(SplashRenderer splash);
}
