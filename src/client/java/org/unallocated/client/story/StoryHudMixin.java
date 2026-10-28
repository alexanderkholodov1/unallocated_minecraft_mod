package org.unallocated.story.mixin.client;

import org.unallocated.client.story.StoryClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
abstract class StoryHudMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void unallocated$blackout(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) { StoryClient.overlay(graphics); }
}
