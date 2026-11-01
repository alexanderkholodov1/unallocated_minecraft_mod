package org.unallocated.bait;

import java.util.SplittableRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;
import org.unallocated.UnallocatedMod;
import org.unallocated.bait.SpyglassBaitLogic.Observation;
import org.unallocated.mixin.bait.BaitMannequinInvoker;

/**
 * The figure in the world: a vanilla mannequin (no registered content, no extra mod) wearing the player's own
 * profile, so the client draws the player's skin from the textures it already has, with no extra network lookup.
 * "Slightly wrong", like the owner's g1 artefact ("casi perfectos y parecidos al jugador original"): mirrored
 * (spyglass in the other hand, like a reflection), a little too tall, no name tag, and always looking back at you
 * through its own spyglass, which also tells the player, without any text, what to do.
 */
public final class SpyglassBaitFigure {
    public static final String TAG = "unallocated.spyglass_bait";
    /** Slightly taller than the player; unsettling up close, invisible as a difference far away. */
    static final double SCALE = 1.06;
    /** Half-angle of the spyglass circle at the widest FOV setting (110 x 0.1 / 2), so "in scope" errs on seen. */
    static final double SCOPE_HALF_ANGLE = Math.toRadians(5.5);
    /** Half-angle beyond which a point cannot be on screen for any FOV or aspect ratio a judge might use. */
    static final double VIEW_HALF_ANGLE = Math.toRadians(80);
    static final double LOOK_HALF_ANGLE = Math.toRadians(14);
    static final double MAX_HEIGHT_DIFFERENCE = 40;

    private SpyglassBaitFigure() { }

    public static boolean isBait(Entity entity) {
        return entity.entityTags().contains(TAG);
    }

    /** A configured figure, not yet in the world. Null if the vanilla entity could not be made. */
    static @Nullable Mannequin create(ServerLevel level, ResolvableProfile profile, HumanoidArm mainArm) {
        Entity created = EntityTypes.MANNEQUIN.create(level, EntitySpawnReason.TRIGGERED);
        if (!(created instanceof Mannequin figure)) return null;
        figure.addTag(TAG);
        figure.setNoGravity(true);
        figure.setSilent(true);
        figure.setMainArm(mainArm);
        figure.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SPYGLASS));
        AttributeInstance scale = figure.getAttribute(Attributes.SCALE);
        if (scale != null) scale.setBaseValue(SCALE);
        if ((Object) figure instanceof BaitMannequinInvoker invoker) {
            invoker.unallocated$setProfile(profile);
            invoker.unallocated$setImmovable(true);
            invoker.unallocated$setHideDescription(true);
        } else {
            UnallocatedMod.LOGGER.warn("UNALLOCATED bait: mannequin accessor missing; figure keeps the default skin");
        }
        return figure;
    }

    /** Puts a configured figure into the world at {@code spot}, facing {@code toward}. */
    static boolean spawn(ServerLevel level, Mannequin figure, Vec3 spot, Vec3 toward) {
        float yaw = yawToward(spot, toward);
        figure.snapTo(spot.x, spot.y, spot.z, yaw, 0.0F);
        figure.setYHeadRot(yaw);
        figure.setYBodyRot(yaw);
        if (!level.addFreshEntity(figure)) return false;
        raiseSpyglass(figure);
        return true;
    }

    static void moveTo(Mannequin figure, Vec3 spot, Vec3 toward) {
        float yaw = yawToward(spot, toward);
        figure.snapTo(spot.x, spot.y, spot.z, yaw, 0.0F);
        figure.setYHeadRot(yaw);
        figure.setYBodyRot(yaw);
        raiseSpyglass(figure);
    }

    /** Turn to face the player; only called while the figure is unseen. */
    static void face(Mannequin figure, Vec3 toward) {
        float yaw = yawToward(figure.position(), toward);
        figure.setYRot(yaw);
        figure.setYHeadRot(yaw);
        figure.setYBodyRot(yaw);
        if (!figure.isUsingItem() || figure.getUseItemRemainingTicks() < 200) raiseSpyglass(figure);
    }

    /** Holds its spyglass up to its eye, pointed at you. Re-armed while unseen so the arm never drops in view. */
    static void raiseSpyglass(Mannequin figure) {
        if (figure.isUsingItem()) figure.stopUsingItem();
        figure.startUsingItem(InteractionHand.MAIN_HAND);
    }

    /** The screamer: the figure is suddenly right in front of the player's eyes, staring, spyglass lowered. */
    static void lunge(ServerPlayer player, Mannequin figure) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        // Through the scope (x10 zoom) about 3.4 blocks fills the circle with its face; plain view needs it close.
        double wanted = player.isScoping() ? 3.4 : 1.15;
        Vec3 far = eye.add(look.scale(wanted));
        HitResult hit = player.level().clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        double distance = hit.getType() == HitResult.Type.MISS ? wanted
                : Math.max(0.6, hit.getLocation().distanceTo(eye) - 0.4);
        Vec3 face = eye.add(look.scale(distance));
        float yaw = player.getYRot() + 180.0F;
        if (figure.isUsingItem()) figure.stopUsingItem();
        figure.snapTo(face.x, face.y - figure.getEyeHeight(), face.z, yaw, -player.getXRot());
        figure.setYHeadRot(yaw);
        figure.setYBodyRot(yaw);
    }

    /** What the player can see of the figure this tick (raycasts only toward points that could be on screen). */
    static Observation observe(ServerPlayer player, @Nullable Mannequin figure) {
        boolean scoping = player.isScoping();
        if (figure == null) return new Observation(false, scoping, false, false, false, 0, false);
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 base = figure.position();
        double height = figure.getBbHeight();
        double nearest = Math.PI;
        for (double fraction : new double[] {0.9, 0.55, 0.15}) {
            Vec3 point = base.add(0, height * fraction, 0);
            double angle = angle(look, point.subtract(eye));
            if (angle < VIEW_HALF_ANGLE && angle < nearest && clear(level, eye, point)) nearest = angle;
        }
        boolean hurt = figure.hurtTime > 0 || figure.getHealth() < figure.getMaxHealth();
        return new Observation(true, scoping, scoping && nearest <= SCOPE_HALF_ANGLE, nearest <= VIEW_HALF_ANGLE,
                nearest <= LOOK_HALF_ANGLE, eye.distanceTo(base.add(0, height * 0.55, 0)), hurt);
    }

    /**
     * Where to put the figure {@code distance} blocks from the player around one of {@code bearings} (degrees, Minecraft
     * yaw). Needs solid ground, room for a body, no fluid, a loaded chunk. Prefers spots the player could see when
     * looking that way, on higher ground (a silhouette against the sky). With {@code mustBeHidden} the spot must be out
     * of the player's current view; with {@code exactBearing} only the given bearing is tried.
     */
    static @Nullable Vec3 findSpot(ServerPlayer player, double distance, double[] bearings, boolean mustBeHidden,
            boolean exactBearing, @Nullable Vec3 lure, SplittableRandom random) {
        ServerLevel level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        boolean scoping = player.isScoping();
        double[] offsets = exactBearing ? new double[] {0, 1.5, -1.5} : new double[] {0, 7, -7, 14, -14, 22, -22, 32, -32};
        double[] factors = exactBearing ? new double[] {1.0, 0.95, 1.05, 0.9, 1.1} : new double[] {1.0, 0.93, 1.07, 0.86, 1.14};
        Vec3 best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (double bearing : bearings) {
            for (double offset : offsets) {
                for (double factor : factors) {
                    double yaw = Math.toRadians(bearing + offset);
                    double reach = distance * factor;
                    Vec3 candidate = groundAt(level, eye.x - Math.sin(yaw) * reach, eye.z + Math.cos(yaw) * reach, eye.y);
                    if (candidate == null) continue;
                    Vec3 head = candidate.add(0, 1.7, 0);
                    Vec3 chest = candidate.add(0, 1.0, 0);
                    boolean visibleFromHere = clear(level, eye, head) || clear(level, eye, chest);
                    if (mustBeHidden && visibleFromHere) {
                        double limit = scoping ? SCOPE_HALF_ANGLE + Math.toRadians(3) : VIEW_HALF_ANGLE;
                        if (angle(look, head.subtract(eye)) <= limit || angle(look, chest.subtract(eye)) <= limit) continue;
                    }
                    double score = (visibleFromHere ? 100 : 0)
                            + Math.min(Math.max(head.y - eye.y, 0), 10) * 2
                            - Math.abs(factor - 1) * 40 - Math.abs(offset) * 0.6 + random.nextDouble();
                    if (lure != null) score -= candidate.distanceTo(lure) * 0.05;
                    if (score > bestScore) {
                        bestScore = score;
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    /** Feet position on the surface at (x, z), or null when that column cannot hold a standing figure. */
    static @Nullable Vec3 groundAt(ServerLevel level, double x, double z, double eyeY) {
        BlockPos column = BlockPos.containing(x, eyeY, z);
        if (!level.hasChunkAt(column)) return null;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
        BlockPos feet = new BlockPos(column.getX(), y, column.getZ());
        if (Math.abs(y - eyeY) > MAX_HEIGHT_DIFFERENCE) return null;
        BlockPos below = feet.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return null;
        for (BlockPos body : new BlockPos[] {feet, feet.above()}) {
            if (!level.getBlockState(body).getCollisionShape(level, body).isEmpty()) return null;
            if (!level.getFluidState(body).isEmpty()) return null;
        }
        return new Vec3(feet.getX() + 0.5, y, feet.getZ() + 0.5);
    }

    static boolean clear(Level level, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE,
                CollisionContext.empty())).getType() == HitResult.Type.MISS;
    }

    static double angle(Vec3 look, Vec3 toPoint) {
        double length = toPoint.length();
        if (length < 1.0E-6) return 0;
        return Math.acos(Math.max(-1, Math.min(1, look.dot(toPoint) / length)));
    }

    /** Minecraft yaw (degrees) for an entity at {@code from} to face {@code to}. */
    static float yawToward(Vec3 from, Vec3 to) {
        return (float) Math.toDegrees(Math.atan2(-(to.x - from.x), to.z - from.z));
    }
}
