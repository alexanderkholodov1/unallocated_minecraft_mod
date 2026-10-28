package org.unallocated.story;

import com.google.gson.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import static org.unallocated.story.StoryData.*;

public final class StoryBuild {
    private StoryBuild() {}
    public static void run(MinecraftServer server) {
        StoryRuntime r = StoryRuntime.get(server);
        if (!r.loaded().valid()) throw new IllegalStateException("Invalid story cannot build");
        record Chunk(ServerLevel level, int x, int z) {}
        List<Chunk> owned = new ArrayList<>(); JsonObject build = object(r.root(), "build"); int count = 0;
        try {
            for (JsonElement entry : array(build, "forceload")) {
                JsonArray a = entry.getAsJsonArray(); ServerLevel level = r.level(a.get(4).getAsString());
                int x1 = Math.min(a.get(0).getAsInt(), a.get(2).getAsInt()) >> 4, x2 = Math.max(a.get(0).getAsInt(), a.get(2).getAsInt()) >> 4;
                int z1 = Math.min(a.get(1).getAsInt(), a.get(3).getAsInt()) >> 4, z2 = Math.max(a.get(1).getAsInt(), a.get(3).getAsInt()) >> 4;
                if ((long) (x2 - x1 + 1) * (z2 - z1 + 1) > 256) throw new IllegalArgumentException("Forceload exceeds 256 chunks");
                for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
                    if (level.setChunkForced(x, z, true)) owned.add(new Chunk(level, x, z)); level.getChunk(x, z);
                }
            }
            var source = server.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER).withSuppressedOutput();
            for (JsonElement command : array(build, "run")) {
                int result = server.getCommands().getDispatcher().execute(command.getAsString(), source);
                if (result <= 0) throw new IllegalStateException("Build command failed: " + command.getAsString()); count++;
            }
            StoryRuntime.log("build ok commands=" + count);
        } catch (Exception e) {
            org.unallocated.UnallocatedMod.LOGGER.error("UNALLOCATED story build failed", e);
            throw new IllegalStateException("Story build failed", e);
        } finally { for (Chunk c : owned) c.level.setChunkForced(c.x, c.z, false); }
    }
}
