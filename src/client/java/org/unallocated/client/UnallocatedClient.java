package org.unallocated.client;

import net.fabricmc.api.ClientModInitializer;
import org.unallocated.UnallocatedMod;

public final class UnallocatedClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        org.unallocated.client.core.ClientSession.initialize();
        org.unallocated.client.ending.EndingClient.initialize();
        org.unallocated.client.effects.SoundEffects.initialize();
        UnallocatedMod.LOGGER.info("UNALLOCATED initialized (client).");
    }
}
