package org.unallocated.client;

import net.fabricmc.api.ClientModInitializer;
import org.unallocated.UnallocatedMod;

public final class UnallocatedClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        UnallocatedMod.LOGGER.info("UNALLOCATED initialized (client).");
    }
}
