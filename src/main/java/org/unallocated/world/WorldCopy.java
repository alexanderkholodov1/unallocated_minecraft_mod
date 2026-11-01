package org.unallocated.world;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.level.gamerules.GameRules;
import org.unallocated.story.StoryRuntime;

/** Delegate each bounded batch to vanilla clone, including block entities and scheduled ticks. */
public final class WorldCopy {
    private WorldCopy() {}
    static WorldPlan.Region region(StoryRuntime.Box b) {
        int x = (int) Math.floor(b.minX()), y = (int) Math.floor(b.minY()), z = (int) Math.floor(b.minZ());
        return new WorldPlan.Region(x, y, z, (int) Math.floor(b.maxX()) - x + 1,
            (int) Math.floor(b.maxY()) - y + 1, (int) Math.floor(b.maxZ()) - z + 1);
    }
    public static int copy(StoryRuntime runtime, StoryRuntime.Box box, StoryRuntime.Position to) {
        ServerLevel source = runtime.level(box.dimension()), target = runtime.level(to.dimension());
        var area = region(box); var destination = to.block();
        int dx = destination.getX() - area.x(), dy = destination.getY() - area.y(), dz = destination.getZ() - area.z();
        if (source == target && dx == 0 && dy == 0 && dz == 0) return area.size();
        checkHeight(source, area.y(), area.height()); checkHeight(target, destination.getY(), area.height());
        int limit = source.getGameRules().get(GameRules.MAX_BLOCK_MODIFICATIONS);
        boolean overlap = source == target && Math.abs((long) dx) < area.width()
            && Math.abs((long) dy) < area.height() && Math.abs((long) dz) < area.depth();
        for (var batch : WorldPlan.batches(area, limit, dx, dy, dz, overlap)) {
            try (var chunks = new Chunks()) {
                chunks.load(source, batch.x(), batch.z(), batch.width(), batch.depth());
                chunks.load(target, batch.x() + dx, batch.z() + dz, batch.width(), batch.depth());
                String command = "clone from " + source.dimension().identifier() + " " + batch.x() + " " + batch.y() + " " + batch.z()
                    + " " + (batch.x() + batch.width() - 1) + " " + (batch.y() + batch.height() - 1) + " " + (batch.z() + batch.depth() - 1)
                    + " to " + target.dimension().identifier() + " " + (batch.x() + dx) + " " + (batch.y() + dy) + " " + (batch.z() + dz) + " replace force";
                try {
                    runtime.server().getCommands().getDispatcher().execute(command,
                        runtime.server().createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER).withSuppressedOutput());
                } catch (com.mojang.brigadier.exceptions.CommandSyntaxException error) {
                    // Vanilla reports an unchanged batch as commands.clone.failed; it is a valid copy.
                    if (!error.getRawMessage().getString().equals(net.minecraft.network.chat.Component.translatable("commands.clone.failed").getString()))
                        throw new IllegalStateException("Copy batch failed: " + command, error);
                }
            }
        }
        org.unallocated.UnallocatedMod.LOGGER.info("UNALLOCATED world copy blocks={} from={} to={}", area.size(), box.dimension(), to.dimension());
        return area.size();
    }
    static void checkHeight(ServerLevel level, int y, int height) {
        if (y < level.getMinY() || (long) y + height > (long) level.getMinY() + level.getHeight())
            throw new IllegalArgumentException("World operation outside build height");
    }
    static final class Chunks implements AutoCloseable {
        private record Owned(ServerLevel level, int x, int z) {}
        private final List<Owned> owned = new ArrayList<>();
        void load(ServerLevel level, int x, int z, int width, int depth) {
            for (int cx = x >> 4; cx <= (x + width - 1) >> 4; cx++)
                for (int cz = z >> 4; cz <= (z + depth - 1) >> 4; cz++) {
                    if (level.setChunkForced(cx, cz, true)) owned.add(new Owned(level, cx, cz));
                    level.getChunk(cx, cz);
                }
        }
        public void close() { for (var chunk : owned) chunk.level.setChunkForced(chunk.x, chunk.z, false); }
    }
}
