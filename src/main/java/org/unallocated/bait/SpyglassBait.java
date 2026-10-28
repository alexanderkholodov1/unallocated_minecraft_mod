package org.unallocated.bait;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.unallocated.UnallocatedMod;
import org.unallocated.bait.SpyglassBaitLogic.Decision;
import org.unallocated.bait.SpyglassBaitLogic.Observation;

/**
 * The spyglass bait (plan §3.3 act 1, primitives {@code figure.spyglass_step} and {@code screamer.close}): what a story
 * beat or the director calls. Server thread only. One bait per player; nothing is saved, so leaving the world ends it.
 *
 * <p>Log markers for CI and the coordinator: {@code UNALLOCATED bait place|step=|scare|gone ...}.
 */
public final class SpyglassBait {
    private static final Map<UUID, Active> ACTIVE = new HashMap<>();
    private static final int SWEEP_TICKS = 100;
    private static int serverTicks;

    private SpyglassBait() { }

    /** Starts a bait for {@code player}; it appears out of view after the spec's delay. Replaces any earlier one. */
    public static void place(ServerPlayer player, SpyglassBaitSpec spec) {
        place(player, spec, null, false);
    }

    /**
     * @param lure  where the figure leads the player when followed (e.g. {@code MapLayout} "hill.bait_path"); it never
     *              goes past it and waits there. Null: it recedes straight away from the player.
     * @param ahead debug/video: the first placement is straight ahead, in plain view, instead of out of view
     */
    public static void place(ServerPlayer player, SpyglassBaitSpec spec, @Nullable BlockPos lure, boolean ahead) {
        clear(player, "replaced");
        spec = spec.fittedTo(Active.reachable(player, SpyglassBaitSpec.MAX_DISTANCE));
        ACTIVE.put(player.getUUID(), new Active(player.level().getServer(), spec,
                lure == null ? null : Vec3.atBottomCenterOf(lure), ahead));
        UnallocatedMod.LOGGER.info("UNALLOCATED bait start distances={} scare={} ahead={}",
                spec.distances(), spec.scare(), ahead);
    }

    /** Removes the bait and its figure at once. Returns whether there was one. */
    public static boolean clear(ServerPlayer player, String cause) {
        Active active = ACTIVE.remove(player.getUUID());
        if (active == null) return false;
        active.logic.cancel();
        active.discard(player.level());
        UnallocatedMod.LOGGER.info("UNALLOCATED bait gone cause={}", cause);
        org.unallocated.story.StoryRuntime.signal(player, "bait", "gone", null);
        return true;
    }

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    /** Act budget / director: one step closer at the next unseen moment. */
    public static boolean forceStep(ServerPlayer player) {
        Active active = ACTIVE.get(player.getUUID());
        return active != null && active.logic.forceStep();
    }

    /** Act budget / director: the screamer now (or a quiet vanish if the spec has the scare off). */
    public static boolean forceScare(ServerPlayer player) {
        Active active = ACTIVE.get(player.getUUID());
        return active != null && active.logic.forceScare();
    }

    /** One line for {@code /ubait status} and tests. */
    public static String status(ServerPlayer player) {
        Active active = ACTIVE.get(player.getUUID());
        if (active == null) return "none";
        SpyglassBaitLogic logic = active.logic;
        Mannequin figure = active.figure(player.level());
        return String.format(Locale.ROOT, "phase=%s step=%d/%d sightings=%d scopeUses=%d distance=%.1f figure=%s",
                logic.phase(), logic.step(), logic.spec().lastStep(), logic.sightings(), logic.scopeUses(),
                figure == null ? -1.0 : figure.position().distanceTo(player.getEyePosition()),
                figure == null ? "none" : figure.getUUID());
    }

    /** Called at the end of every server tick. */
    public static void tickServer(MinecraftServer server) {
        serverTicks++;
        if (!ACTIVE.isEmpty()) {
            for (UUID id : new ArrayList<>(ACTIVE.keySet())) {
                Active active = ACTIVE.get(id);
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (active == null) continue;
                if (active.server.get() != server || player == null || player.isRemoved()) {
                    ACTIVE.remove(id);
                    if (player != null) active.discard(player.level());
                    UnallocatedMod.LOGGER.info("UNALLOCATED bait gone cause=player-left");
                    continue;
                }
                try {
                    active.tick(player);
                } catch (RuntimeException failure) {
                    // Never let the bait take the server down: log it and drop this bait.
                    UnallocatedMod.LOGGER.error("UNALLOCATED bait failed; removing it", failure);
                    clear(player, "error");
                }
                if (active.logic.phase() == SpyglassBaitLogic.Phase.GONE) ACTIVE.remove(id, active);
            }
        }
        if (serverTicks % SWEEP_TICKS == 0) sweepOrphans(server);
        SpyglassBaitSelfTest.tick(server);
    }

    /** Figures that lost their bait (respawn, crash, world reopened) are removed whenever they are loaded. */
    private static void sweepOrphans(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            List<? extends Mannequin> orphans = level.getEntities(EntityTypes.MANNEQUIN,
                    figure -> SpyglassBaitFigure.isBait(figure) && !isManaged(figure));
            for (Mannequin orphan : orphans) orphan.discard();
        }
    }

    private static boolean isManaged(Entity figure) {
        for (Active active : ACTIVE.values()) {
            if (figure.getUUID().equals(active.figureId)) return true;
        }
        return SpyglassBaitSelfTest.owns(figure);
    }

    private static final class Active {
        final WeakReference<MinecraftServer> server;
        final SpyglassBaitLogic logic;
        final @Nullable Vec3 lure;
        final SplittableRandom random;
        boolean ahead;
        @Nullable UUID figureId;
        int lastStep;
        int ticks;

        Active(MinecraftServer server, SpyglassBaitSpec spec, @Nullable Vec3 lure, boolean ahead) {
            this.server = new WeakReference<>(server);
            this.logic = new SpyglassBaitLogic(spec);
            this.lure = lure;
            this.ahead = ahead;
            this.random = new SplittableRandom(spec.seed());
        }

        @Nullable Mannequin figure(ServerLevel level) {
            if (figureId == null) return null;
            return level.getEntity(figureId) instanceof Mannequin figure && !figure.isRemoved() ? figure : null;
        }

        void discard(ServerLevel level) {
            Mannequin figure = figure(level);
            if (figure != null) figure.discard();
            figureId = null;
        }

        void tick(ServerPlayer player) {
            ticks++;
            ServerLevel level = player.level();
            Mannequin figure = figure(level);
            Observation seen = SpyglassBaitFigure.observe(player, figure);
            Decision decision = logic.tick(seen);
            switch (decision.action()) {
                case PLACE -> logic.placed(place(player, level, decision));
                case MOVE -> logic.moved(figure != null && move(player, figure, decision));
                case SCARE -> {
                    if (figure != null) SpyglassBaitFigure.lunge(player, figure);
                    SpyglassBaitSignals.scare(player.getUUID());
                    org.unallocated.story.StoryRuntime.signal(player, "bait", "screamer", null);
                    UnallocatedMod.LOGGER.info("UNALLOCATED bait scare cause={} sightings={}", decision.cause(),
                            logic.sightings());
                }
                case REMOVE -> {
                    discard(level);
                    UnallocatedMod.LOGGER.info("UNALLOCATED bait gone cause={}", decision.cause());
                    org.unallocated.story.StoryRuntime.signal(player, "bait", "gone", null);
                }
                case NONE -> {
                    if (figure != null && decision.unseen() && ticks % 10 == 0) {
                        SpyglassBaitFigure.face(figure, player.getEyePosition());
                    }
                }
            }
            if (logic.step() != lastStep) {
                lastStep = logic.step();
                org.unallocated.story.StoryRuntime.signal(player, "bait", "step", null);
                UnallocatedMod.LOGGER.info(String.format(Locale.ROOT,
                        "UNALLOCATED bait step=%d/%d distance=%.1f cause=%s sightings=%d", lastStep,
                        logic.spec().lastStep(), logic.currentDistance(), decision.cause().isEmpty() ? "walked"
                                : decision.cause(), logic.sightings()));
            }
        }

        private boolean place(ServerPlayer player, ServerLevel level, Decision decision) {
            discard(level);
            Vec3 spot = ahead ? SpyglassBaitFigure.findSpot(player, reachable(player, decision.distance()),
                    new double[] {player.getYRot()}, false, true, null, random) : spotFor(player, decision, null);
            if (spot == null) return false;
            Mannequin figure = SpyglassBaitFigure.create(level, ResolvableProfile.createResolved(player.getGameProfile()),
                    player.getMainArm().getOpposite());
            if (figure == null || !SpyglassBaitFigure.spawn(level, figure, spot, player.getEyePosition())) return false;
            figureId = figure.getUUID();
            ahead = false;
            UnallocatedMod.LOGGER.info(String.format(Locale.ROOT, "UNALLOCATED bait place distance=%.1f cause=%s",
                    spot.distanceTo(player.getEyePosition()), decision.cause()));
            return true;
        }

        private boolean move(ServerPlayer player, Mannequin figure, Decision decision) {
            Vec3 spot = spotFor(player, decision, figure);
            if (spot == null) return false;
            SpyglassBaitFigure.moveTo(figure, spot, player.getEyePosition());
            return true;
        }

        /**
         * Same bearing as now (it approaches along your line of sight), or toward the lure when one is set; at the
         * lure itself once the player is closer to it than the wanted distance. New spots must be out of view unless
         * only the zoom-in hides the jump, in which case the bearing is kept exactly.
         */
        private @Nullable Vec3 spotFor(ServerPlayer player, Decision decision, @Nullable Mannequin figure) {
            Vec3 eye = player.getEyePosition();
            double distance = reachable(player, decision.distance());
            double bearing;
            if (lure != null) {
                if (eye.distanceTo(lure) <= distance) {
                    Vec3 waiting = SpyglassBaitFigure.groundAt(player.level(), lure.x, lure.z, eye.y);
                    if (waiting != null && (decision.zoomMasked() || hiddenFrom(player, waiting))) return waiting;
                }
                bearing = SpyglassBaitFigure.yawToward(eye, lure);
            } else if (figure != null) {
                bearing = SpyglassBaitFigure.yawToward(eye, figure.position());
            } else {
                // First appearance: beside or behind the player, so it is discovered rather than spawned in view.
                float yaw = player.getYRot();
                return SpyglassBaitFigure.findSpot(player, distance,
                        new double[] {yaw + 110, yaw - 110, yaw + 145, yaw - 145, yaw + 180}, true, false, null, random);
            }
            return SpyglassBaitFigure.findSpot(player, distance, new double[] {bearing}, !decision.zoomMasked(),
                    decision.zoomMasked(), lure, random);
        }

        /** A judge with a short render distance still gets a figure inside loaded, unfogged terrain. */
        private static double reachable(ServerPlayer player, double distance) {
            int viewBlocks = player.level().getServer().getPlayerList().getViewDistance() * 16;
            return Math.max(SpyglassBaitSpec.MIN_DISTANCE, Math.min(distance, viewBlocks * 0.8));
        }

        private static boolean hiddenFrom(ServerPlayer player, Vec3 spot) {
            Vec3 eye = player.getEyePosition();
            Vec3 head = spot.add(0, 1.7, 0);
            double limit = player.isScoping() ? SpyglassBaitFigure.SCOPE_HALF_ANGLE + Math.toRadians(3)
                    : SpyglassBaitFigure.VIEW_HALF_ANGLE;
            return SpyglassBaitFigure.angle(player.getLookAngle(), head.subtract(eye)) > limit
                    || !SpyglassBaitFigure.clear(player.level(), eye, head);
        }
    }
}
