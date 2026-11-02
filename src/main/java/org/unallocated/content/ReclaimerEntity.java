package org.unallocated.content;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Reclaimer's body. The story runtime steers it on the server (no AI); this class only gives it presence on the
 * client: steps when it moves, chitinous clicks and breathing when it waits, a scream when it starts to hunt and
 * whispers when the player is close. All of them are our own sounds.
 */
public final class ReclaimerEntity extends PathfinderMob {
    private Vec3 lastPosition;
    private int nextClick = 40;
    private int nextBreath = 20;
    private int nextStep;
    private int nextWhisper = 100;
    private int roarCooldown;
    private double speed;

    public ReclaimerEntity(EntityType<? extends ReclaimerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 300.0).add(Attributes.MOVEMENT_SPEED, 0.3F)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64.0);
    }

    /** Smoothed horizontal speed in blocks per tick, as seen by this side. */
    public double speed() { return speed; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected void doPush(Entity entity) {}

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) return;
        Vec3 now = position();
        double moved = lastPosition == null ? 0 : now.subtract(lastPosition).horizontalDistance();
        lastPosition = now;
        speed = speed * 0.8 + moved * 0.2;
        if (roarCooldown > 0) roarCooldown--;
        if (speed > 0.12 && roarCooldown == 0) {
            play(UnallocatedContent.RECLAIMER_ROAR, 1.6F, 0.85F + random.nextFloat() * 0.2F);
            roarCooldown = 400;
        }
        if (moved > 0.02 && --nextStep <= 0) {
            play(UnallocatedContent.RECLAIMER_STEP, 0.9F, 0.7F + random.nextFloat() * 0.3F);
            nextStep = speed > 0.12 ? 5 : 9;
        }
        if (--nextClick <= 0) {
            play(UnallocatedContent.RECLAIMER_CLICK, 0.8F, 0.8F + random.nextFloat() * 0.4F);
            nextClick = 50 + random.nextInt(90);
        }
        if (speed < 0.02 && --nextBreath <= 0) {
            play(UnallocatedContent.RECLAIMER_BREATH, 0.6F, 0.9F + random.nextFloat() * 0.15F);
            nextBreath = 80;
        }
        Player near = level().getNearestPlayer(this, 9.0);
        if (near != null && --nextWhisper <= 0) {
            play(UnallocatedContent.RECLAIMER_WHISPER, 0.7F, 0.85F + random.nextFloat() * 0.3F);
            nextWhisper = 120 + random.nextInt(160);
        }
    }

    private void play(SoundEvent sound, float volume, float pitch) {
        level().playLocalSound(getX(), getY() + 1.5, getZ(), sound, SoundSource.HOSTILE, volume, pitch, false);
    }
}
