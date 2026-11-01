package org.unallocated.smoke;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import org.unallocated.client.contract.ContractScreen;

/** Installed before startup, so the test cannot miss a title initialized too early. */
public final class ContractSmokeObserver implements ClientModInitializer {
    static volatile int titleInitializations;
    static volatile int contractInitializations;
    // Proves the bundled screen API's AFTER_INIT hook runs for a real title screen.
    static volatile boolean titleHookCalled;

    @Override
    public void onInitializeClient() {
        ScreenEvents.BEFORE_INIT.register((client, screen, width, height) -> {
            if (screen instanceof TitleScreen) titleInitializations++;
            if (screen instanceof ContractScreen) contractInitializations++;
        });
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof TitleScreen) titleHookCalled = true;
        });
    }
}
