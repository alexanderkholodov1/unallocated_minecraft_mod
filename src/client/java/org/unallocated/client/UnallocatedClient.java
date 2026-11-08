package org.unallocated.client;

import net.fabricmc.api.ClientModInitializer;
import org.unallocated.UnallocatedMod;

public final class UnallocatedClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        org.unallocated.client.mixin.creature.EntityRenderersInvoker.unallocated$register(
            org.unallocated.content.UnallocatedContent.RECLAIMER, org.unallocated.client.creature.ReclaimerRenderer::new);
        org.unallocated.client.mixin.creature.EntityRenderersInvoker.unallocated$register(
            org.unallocated.content.UnallocatedContent.THREAD_ENTITY, org.unallocated.client.creature.ThreadRenderer::new);
        org.unallocated.client.creature.PuppetClient.initialize();
        org.unallocated.client.core.ClientSession.initialize();
        // The vanilla soundtrack never plays inside a session: a cheerful track over the diary broke the night.
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null && org.unallocated.client.core.ClientSession.act() != org.unallocated.core.Act.CONTRACT) client.getMusicManager().stopPlaying();
        });
        org.unallocated.client.ending.EndingClient.initialize();
        org.unallocated.client.effects.SoundEffects.initialize();
        org.unallocated.client.story.StoryClient.initialize();
        org.unallocated.client.launcher.LauncherClient.initialize();
        UnallocatedMod.LOGGER.info("UNALLOCATED initialized (client).");
    }
}
