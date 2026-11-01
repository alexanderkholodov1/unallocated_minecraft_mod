package org.unallocated.content;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.unallocated.story.StoryRuntime;

/**
 * One of the Reclaimer's threads (LORE 1.9): a listener stretched through the world where its scripts run. Only a
 * Puppet sees it (LORE 1.10). Its position is the knot in the middle; it spans (dx, dy, dz) blocks end to end. Shears
 * cut it at the knot, which the story hears exactly like a broken "unallocated:thread" block at that spot.
 */
public final class ThreadEntity extends Mob {
    private static final EntityDataAccessor<Vector3fc> SPAN = SynchedEntityData.defineId(ThreadEntity.class, EntityDataSerializers.VECTOR3);
    /** Set by the client while its player is a Puppet; threads are invisible and unpickable otherwise. */
    public static volatile boolean clientPuppet;

    public ThreadEntity(EntityType<? extends ThreadEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setNoGravity(true);
        setSilent(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public Vector3fc span() { return entityData.get(SPAN); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SPAN, new Vector3f(4F, 0F, 0F));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        Vector3fc span = span();
        output.putFloat("dx", span.x()); output.putFloat("dy", span.y()); output.putFloat("dz", span.z());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(SPAN, new Vector3f(input.getFloatOr("dx", 4F), input.getFloatOr("dy", 0F), input.getFloatOr("dz", 0F)));
    }

    @Override
    public void tick() {
        super.tick();
        setYRot(0F); setYBodyRot(0F); setYHeadRot(0F); setXRot(0F);
        setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    }

    @Override
    public boolean isPickable() { return !level().isClientSide() || clientPuppet; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected void doPush(Entity entity) {}

    @Override
    public boolean canBeCollidedWith(Entity other) { return false; }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getEntity() instanceof ServerPlayer player && StoryRuntime.get(level.getServer()).puppet()) {
            if (player.getMainHandItem().is(Items.SHEARS)) cut(level, player);
            else level.playSound(null, blockPosition(), UnallocatedContent.THREAD_VIBRATE, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        return false;
    }

    private void cut(ServerLevel level, ServerPlayer player) {
        level.playSound(null, blockPosition(), UnallocatedContent.THREAD_SNAP, SoundSource.BLOCKS, 1.2F, 0.9F + random.nextFloat() * 0.2F);
        level.sendParticles(new DustParticleOptions(0x8A1020, 1.0F), getX(), getY() + 0.3, getZ(), 24, 0.4, 0.2, 0.4, 0.0);
        StoryRuntime.signal(player, "break", "unallocated:thread", blockPosition());
        discard();
    }
}
