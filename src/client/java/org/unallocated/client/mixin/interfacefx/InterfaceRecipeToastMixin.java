package org.unallocated.client.mixin.interfacefx;

import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Inside a session no "New Recipe(s) Unlocked!" toast breaks the illusion of an old server. */
@Mixin(RecipeToast.class)
abstract class InterfaceRecipeToastMixin {
    @Inject(require = 0, method = "addOrUpdate", at = @At("HEAD"), cancellable = true)
    private static void interface$quietRecipes(ToastManager toasts, RecipeDisplay recipe, CallbackInfo info) {
        var act = org.unallocated.client.core.ClientSession.act();
        if (act != org.unallocated.core.Act.CONTRACT && act != org.unallocated.core.Act.EPILOGUE) info.cancel();
    }
}
