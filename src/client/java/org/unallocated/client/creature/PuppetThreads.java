package org.unallocated.client.creature;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * As a Puppet you finally see the threads that leave your own hands: the channel through which the Reclaimer copies
 * every movement to train your Artifact. They stretch, slightly sagging and trembling, toward the Kernel at the centre
 * of its web, so following them leads there.
 */
public final class PuppetThreads {
    private static final Vec3 KERNEL = new Vec3(141.5, 70.0, 20.5);
    private static final DustParticleOptions THREAD = new DustParticleOptions(0x8A1020, 0.45F);
    private static final double LENGTH = 9.0;

    private PuppetThreads() {}

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(PuppetThreads::tick);
    }

    private static void tick(Minecraft client) {
        var player = client.player;
        var level = client.level;
        if (player == null || level == null || client.isPaused() || !player.isAlive()) return;
        if (!PuppetClient.active()) return;
        if (!level.dimension().identifier().getPath().equals("pocket")) return;
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        Vec3 chest = player.position().add(0, player.getBbHeight() * 0.55, 0);
        for (int hand = -1; hand <= 1; hand += 2) {
            Vec3 start = chest.add(right.scale(hand * 0.38));
            Vec3 toward = KERNEL.subtract(start);
            double reach = Math.min(LENGTH, toward.length());
            if (reach < 1.0) continue;
            Vec3 direction = toward.normalize();
            for (int i = 0; i < 3; i++) {
                double t = level.getRandom().nextDouble();
                double along = t * reach;
                double sag = Math.sin(t * Math.PI) * 0.35 * (reach / LENGTH);
                double tremble = (level.getRandom().nextDouble() - 0.5) * 0.04;
                Vec3 point = start.add(direction.scale(along)).add(0, -sag + tremble, tremble);
                level.addParticle(THREAD, point.x, point.y, point.z, 0, 0, 0);
            }
        }
    }
}
