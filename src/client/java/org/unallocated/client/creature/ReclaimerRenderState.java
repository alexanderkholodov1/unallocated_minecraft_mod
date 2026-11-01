package org.unallocated.client.creature;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Vanilla living state plus what the Reclaimer's pose reads: how fast it moves and a per-body seed. */
public final class ReclaimerRenderState extends LivingEntityRenderState {
    public float speed;
    public int seed;
}
