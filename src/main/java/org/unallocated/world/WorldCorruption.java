package org.unallocated.world;

import com.google.gson.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.unallocated.core.SessionState;
import org.unallocated.story.StoryRuntime;
import static org.unallocated.story.StoryData.*;

/** Jobs and cursors live under story.* in the existing session save, with no schema change. */
public final class WorldCorruption {
    private static final String PREFIX = "story.world.job.";
    private static final Map<net.minecraft.server.MinecraftServer, List<Job>> JOBS = new IdentityHashMap<>();
    private WorldCorruption() {}
    public record Rule(Set<String> from, List<String> to, double ratio) {
        public Rule { from = Set.copyOf(from); to = List.copyOf(to); }
    }
    private record Job(String flag, String key, JsonObject data, WorldPlan.Region area, List<Rule> rules) {}
    static void initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            List<Job> jobs = new ArrayList<>();
            for (String flag : SessionState.get(server).flags()) if (flag.startsWith(PREFIX))
                jobs.add(decode(flag));
            jobs.sort(Comparator.comparingInt(job -> Integer.parseInt(job.key.substring("story.world.".length()))));
            JOBS.put(server, jobs);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(JOBS::remove);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var jobs = JOBS.get(server); if (jobs == null || jobs.isEmpty()) return;
            var state = SessionState.get(server); if (!state.active()) return;
            var runtime = StoryRuntime.get(server);
            for (var it = jobs.iterator(); it.hasNext();) {
                Job job = it.next();
                int elapsed = (int) Math.min(Integer.MAX_VALUE, Math.max(0, state.ticks() - job.data.get("started").getAsLong()));
                if (advance(runtime, job, elapsed)) { state.removeFlag(job.flag); it.remove(); }
            }
        });
    }
    static List<Rule> rules(JsonArray list) {
        List<Rule> result = new ArrayList<>();
        for (var entry : list) {
            var o = entry.getAsJsonObject();
            result.add(new Rule(new HashSet<>(array(o, "from").asList().stream().map(JsonElement::getAsString).toList()),
                array(o, "to").asList().stream().map(JsonElement::getAsString).toList(), number(o, "ratio", 1)));
        }
        return List.copyOf(result);
    }
    private static Job decode(String flag) {
        var data = JsonParser.parseString(flag.substring(PREFIX.length())).getAsJsonObject();
        var a = array(data, "box"); int x = a.get(0).getAsInt(), y = a.get(1).getAsInt(), z = a.get(2).getAsInt();
        return new Job(flag, string(data, "key", ""), data,
            new WorldPlan.Region(x, y, z, a.get(3).getAsInt(), a.get(4).getAsInt(), a.get(5).getAsInt()), rules(array(data, "rules")));
    }
    public static int start(StoryRuntime runtime, StoryRuntime.Box box, JsonArray rules, long seed, double seconds) {
        var area = WorldCopy.region(box); var level = runtime.level(box.dimension());
        WorldCopy.checkHeight(level, area.y(), area.height());
        // Resolve every ID before modifying blocks or persisting a job.
        for (Rule rule : rules(rules)) { rule.from.forEach(WorldCorruption::block); rule.to.forEach(WorldCorruption::block); }
        var state = runtime.state(); int id = state.counters().getOrDefault("story.world.sequence", 0) + 1;
        state.setCounter("story.world.sequence", id);
        JsonObject data = new JsonObject(); data.addProperty("key", "story.world." + id);
        data.addProperty("dimension", box.dimension()); data.addProperty("seed", seed);
        // Limit positive-duration operations to 2048 visited positions per active tick.
        int duration = seconds == 0 ? 0 : Math.max(runtime.ticks(seconds), (int) Math.ceil(area.size() / 2048.0));
        data.addProperty("started", state.ticks()); data.addProperty("duration", duration); data.add("rules", rules.deepCopy());
        JsonArray bounds = new JsonArray(); for (int v : new int[]{area.x(), area.y(), area.z(), area.width(), area.height(), area.depth()}) bounds.add(v);
        data.add("box", bounds); var job = decode(PREFIX + data);
        if (duration == 0) advance(runtime, job, 0);
        else { state.addFlag(job.flag); JOBS.computeIfAbsent(runtime.server(), s -> new ArrayList<>()).add(job); }
        return area.size();
    }
    private static Block block(String name) {
        var id = Identifier.parse(name);
        if (!BuiltInRegistries.BLOCK.containsKey(id)) throw new IllegalArgumentException("Unknown corruption block " + name);
        return BuiltInRegistries.BLOCK.getValue(id);
    }
    private static boolean advance(StoryRuntime runtime, Job job, int elapsed) {
        var state = runtime.state(); int cursor = state.counters().getOrDefault(job.key + ".cursor", 0);
        int until = WorldPlan.due(job.area.size(), elapsed, job.data.get("duration").getAsInt());
        var level = runtime.level(string(job.data, "dimension", "overworld"));
        long seed = job.data.get("seed").getAsLong();
        // A tick holds only the column currently being visited; existing force tickets stay owned by their author.
        var chunks = new WorldCopy.Chunks();
        try {
            int lastX = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;
            for (int i = cursor; i < until; i++) {
                BlockPos pos = new BlockPos(job.area.x(i), job.area.y(i), job.area.z(i));
                if ((pos.getX() >> 4) != lastX || (pos.getZ() >> 4) != lastZ) {
                    chunks.close(); chunks = new WorldCopy.Chunks();
                    chunks.load(level, pos.getX(), pos.getZ(), 1, 1); lastX = pos.getX() >> 4; lastZ = pos.getZ() >> 4;
                }
                String current = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
                for (int r = 0; r < job.rules.size(); r++) {
                    Rule rule = job.rules.get(r); if (!rule.from.contains(current)) continue;
                    if (rule.to.isEmpty()) break;
                    int choice = WorldPlan.replacement(seed, i, r, rule.ratio, rule.to.size());
                    if (choice >= 0) level.setBlock(pos, block(rule.to.get(choice)).defaultBlockState(), Block.UPDATE_ALL);
                    break; // Rules match the original block, never cascade through a replacement.
                }
            }
        }
        finally { chunks.close(); }
        state.setCounter(job.key + ".cursor", until);
        if (until == job.area.size()) org.unallocated.UnallocatedMod.LOGGER.info("UNALLOCATED world corrupt complete seed={} blocks={}", seed, until);
        return until == job.area.size();
    }
}
