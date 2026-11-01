package org.unallocated.client;

import net.fabricmc.api.ClientModInitializer;
import org.unallocated.UnallocatedMod;

public final class UnallocatedClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        org.unallocated.client.mixin.creature.EntityRenderersInvoker.unallocated$register(
            org.unallocated.content.UnallocatedContent.RECLAIMER, org.unallocated.client.creature.ReclaimerRenderer::new);
        org.unallocated.client.core.ClientSession.initialize();
        org.unallocated.client.creature.PuppetThreads.initialize();
        org.unallocated.client.ending.EndingClient.initialize();
        org.unallocated.client.effects.SoundEffects.initialize();
        org.unallocated.client.story.StoryClient.initialize();
        org.unallocated.client.launcher.LauncherClient.initialize();
        UnallocatedMod.LOGGER.info("UNALLOCATED initialized (client).");
    }
}
