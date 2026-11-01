package org.unallocated.client.mixin.contract;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.unallocated.client.contract.ContractClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gui.class)
abstract class ContractGuiMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen contract$beforeTitle(Screen requested) {
        return ContractClient.beforeScreen(requested);
    }
}
