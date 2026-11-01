package org.unallocated.client.mixin.bait;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.unallocated.client.bait.SpyglassBaitClient;

/** Draws the screamer over the whole HUD (including the spyglass vignette), even with the HUD hidden (F1). */
@Mixin(Hud.class)
abstract class BaitHudMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void unallocated$baitScreamer(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        SpyglassBaitClient.extractOverlay(graphics);
    }
}
