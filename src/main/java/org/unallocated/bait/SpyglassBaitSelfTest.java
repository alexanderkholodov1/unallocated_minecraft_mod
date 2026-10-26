package org.unallocated.bait;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.unallocated.UnallocatedMod;
import org.unallocated.mixin.bait.BaitMannequinInvoker;

/**
 * {@code /ubait selftest}: proves on a real server (CI's dedicated-server smoke run, nobody online) that the figure
 * can be made, dressed through the mixin accessor, placed on the ground, moved, kept out of the save, and removed.
 * Logs {@code UNALLOCATED bait selftest passed} or {@code ... FAILED: <reason>}.
 */
final class SpyglassBaitSelfTest {
    private static final int TIMEOUT_TICKS = 200;
    private static @Nullable Run run;

    private SpyglassBaitSelfTest() { }

    private record Run(ServerLevel level, BlockPos origin, int[] waited) { }

    private static @Nullable UUID figureId;

    /** Force-loads the chunk at {@code origin}; the checks run once it is loaded. */
    static void start(ServerLevel level, BlockPos origin) {
        level.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);
        run = new Run(level, origin, new int[1]);
        UnallocatedMod.LOGGER.info("UNALLOCATED bait selftest started at {}", origin.toShortString());
    }

    static boolean owns(Entity entity) {
        return entity.getUUID().equals(figureId);
    }

    static void tick(MinecraftServer server) {
        Run current = run;
        if (current == null || current.level().getServer() != server) return;
        if (++current.waited()[0] < 5 || !current.level().hasChunkAt(current.origin())) {
            if (current.waited()[0] > TIMEOUT_TICKS) finish(current, "chunk never loaded");
            return;
        }
        String failure;
        try {
            failure = check(current.level(), current.origin());
        } catch (RuntimeException exception) {
            UnallocatedMod.LOGGER.error("UNALLOCATED bait selftest threw", exception);
            failure = exception.toString();
        }
        finish(current, failure);
    }

    private static void finish(Run current, @Nullable String failure) {
        run = null;
        figureId = null;
        current.level().setChunkForced(current.origin().getX() >> 4, current.origin().getZ() >> 4, false);
        if (failure == null) {
            UnallocatedMod.LOGGER.info("UNALLOCATED bait selftest passed");
        } else {
            UnallocatedMod.LOGGER.error("UNALLOCATED bait selftest FAILED: {}", failure);
        }
    }

    private static @Nullable String check(ServerLevel level, BlockPos origin) {
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX(), origin.getZ());
        double eyeY = surface + 1.62;
        Vec3 spot = SpyglassBaitFigure.groundAt(level, origin.getX() + 0.5, origin.getZ() + 0.5, eyeY);
        Vec3 next = SpyglassBaitFigure.groundAt(level, origin.getX() + 3.5, origin.getZ() + 0.5, eyeY);
        if (spot == null || next == null) return "no standable ground at " + origin.toShortString();

        ResolvableProfile profile = ResolvableProfile.createUnresolved(UUID.fromString("00000000-0000-4000-8000-000000000412"));
        Mannequin figure = SpyglassBaitFigure.create(level, profile, HumanoidArm.LEFT);
        if (figure == null) return "mannequin not created";
        List<String> problems = new ArrayList<>();
        if (!((Object) figure instanceof BaitMannequinInvoker invoker)) {
            problems.add("mannequin accessor mixin not applied");
        } else {
            if (figure.getProfile() != profile) problems.add("profile not set");
            // Back to the built-in profile so the self-test never asks anything over the network.
            invoker.unallocated$setProfile(Mannequin.DEFAULT_PROFILE);
        }
        if (!SpyglassBaitFigure.isBait(figure)) problems.add("tag missing");
        if (!figure.isNoGravity() || !figure.isSilent()) problems.add("gravity/silence");
        if (figure.getMainArm() != HumanoidArm.LEFT) problems.add("main arm");
        if (figure.getMainHandItem().getItem() != Items.SPYGLASS) problems.add("no spyglass");
        AttributeInstance scale = figure.getAttribute(Attributes.SCALE);
        if (scale == null || Math.abs(scale.getValue() - SpyglassBaitFigure.SCALE) > 1.0E-6) problems.add("scale");

        Vec3 viewer = spot.add(0, 1.62, 10);
        if (!SpyglassBaitFigure.spawn(level, figure, spot, viewer)) return "addFreshEntity refused: " + problems;
        figureId = figure.getUUID();
        try {
            if (level.getEntity(figure.getUUID()) != figure) problems.add("not found in level");
            if (!figure.isUsingItem()) problems.add("spyglass not raised");
            if (figure.shouldBeSaved()) problems.add("would be saved (save mixin not applied)");
            double turn = (SpyglassBaitFigure.yawToward(spot, viewer) - figure.getYHeadRot()) % 360.0;
            if (Math.abs((turn + 540.0) % 360.0 - 180.0) > 1.0) problems.add("not facing");
            SpyglassBaitFigure.moveTo(figure, next, viewer);
            if (figure.position().distanceTo(next) > 1.0E-6) problems.add("move");
        } finally {
            figure.discard();
        }
        if (!figure.isRemoved()) problems.add("not removed");
        UnallocatedMod.LOGGER.info(String.format(Locale.ROOT, "UNALLOCATED bait selftest figure at %.1f %.1f %.1f, %d problems",
                spot.x, spot.y, spot.z, problems.size()));
        return problems.isEmpty() ? null : String.join(", ", problems);
    }
}
