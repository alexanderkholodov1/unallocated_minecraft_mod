package org.unallocated.story;

import com.google.gson.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.unallocated.UnallocatedMod;
import org.unallocated.core.*;
import org.unallocated.core.network.CoreNetworking;
import org.unallocated.core.network.CorePayloads.EffectPayload;
import org.unallocated.ending.*;
import org.unallocated.map.MapLayout;
import static org.unallocated.story.StoryData.*;

/** One server-thread runtime per world. The core alone owns clock, consent and progression. */
public final class StoryRuntime {
    public static final List<Act> SLOTS = List.of(Act.LOBBY, Act.THEFT, Act.LOOP, Act.POCKET, Act.CLIMAX);
    private static final Map<MinecraftServer, StoryRuntime> SERVERS = new IdentityHashMap<>();
    private static int runIndex = Integer.getInteger("unallocated.story.autoplay.run", 0);
    private final MinecraftServer server;
    private final StoryLoader.Result loaded;
    private final SessionState state;
    private final StoryScheduler scheduler;
    private final ServerEventBus events;
    private final Map<String, JsonObject> sources = new LinkedHashMap<>(), beats = new LinkedHashMap<>();
    private final Map<String, StoryTriggers.Trigger> triggers = new HashMap<>();
    private final Map<String, List<StoryActions.Action>> actions = new HashMap<>();
    private final Map<String, List<StoryScheduler.Step>> steps = new HashMap<>();
    private final Set<String> simulated = new HashSet<>();
    private final Map<String, Long> autoplayReady = new HashMap<>();
    private String executingBeat = "";
    private final List<Signal> signals = new ArrayList<>();
    public record Signal(String type, String value, BlockPos position) {}
    private ServerPlayer participant;
    private boolean dead;
    private int generation;
    private String chapterId = "";
    private final boolean autoplay = Boolean.getBoolean("unallocated.story.autoplay");
    private final int run;
    private StoryRuntime(MinecraftServer server) {
        this.server = server; this.state = SessionState.get(server); scheduler = new StoryScheduler(state);
        events = new ServerEventBus(e -> UnallocatedMod.LOGGER.error("UNALLOCATED story listener failed", e));
        run = runIndex;
        loaded = StoryLoader.load(StoryLoader.resources(), new StoryValidator(server));
        for (var finding : loaded.findings()) UnallocatedMod.LOGGER.warn(finding.marker());
        if (loaded.valid()) {
            var validator = new StoryValidator(server);
            for (JsonObject chapter : loaded.data().chapters()) {
                String id = string(chapter, "id", "");
                for (String key : List.of("on_enter", "on_resume", "escape")) compile(id + "." + key, array(chapter, key), new JsonObject(), validator);
                for (JsonElement e : array(chapter, "beats")) {
                    JsonObject beat = e.getAsJsonObject(); String beatId = string(beat, "id", ""); beats.put(beatId, beat);
                    compile(beatId, array(beat, "actions"), beat, validator);
                    JsonObject when = object(beat, "when");
                    triggers.put(beatId, StoryTriggers.parser(string(when, "type", "")).parse(when, validator.context("", "", beat)));
                }
            }
            log("loaded chapters=" + loaded.data().chapters().size() + " beats=" + beats.size() + " endings=" + object(loaded.data().root(), "endings").size() + " format=1");
        }
    }
    public static void initialize() {
        StoryBuiltins.initialize();
        for (int i = 0; i < SLOTS.size(); i++) CoreRuntime.register(new ChapterModule(i));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> SERVERS.put(server, new StoryRuntime(server)));
        ServerLifecycleEvents.SERVER_STOPPED.register(SERVERS::remove);
        StoryCommands.register();
    }
    public static StoryRuntime get(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("Story runtime requires the server thread");
        return Objects.requireNonNull(SERVERS.get(server), "Story server not started");
    }
    public static void setAutoplayRun(int index) {
        if (!Boolean.getBoolean("unallocated.story.autoplay")) throw new IllegalStateException("Autoplay JVM property required");
        if (index < 0) throw new IllegalArgumentException("Negative autoplay run"); runIndex = index;
    }
    public static void signal(ServerPlayer p, String type, String value, BlockPos position) {
        var r = SERVERS.get(p.level().getServer());
        if (r != null && r.loaded.valid() && r.state.active()) r.signals.add(new Signal(type, value, position));
    }
    public boolean signalMatches(String type, String value, BlockPos pos) {
        return signals.stream().anyMatch(s -> s.type.equals(type) && (value == null || value.equals(s.value)) && (pos == null || pos.equals(s.position)));
    }
    public boolean chatMatches(String pattern) {
        return signals.stream().anyMatch(s -> s.type.equals("chat") && java.util.regex.Pattern.compile(pattern).matcher(s.value).find());
    }
    public boolean broken(String block, Box box, ServerPlayer player) {
        return signals.stream().anyMatch(s -> s.type.equals("break") && block.equals(s.value) && s.position != null
            && (box == null || box.dimension.equals(dimensionName(player.level())) && box.contains(s.position.getX(), s.position.getY(), s.position.getZ())));
    }
    private static boolean chatTrigger(JsonObject trigger, String text, boolean hiddenOnly) {
        if (string(trigger, "type", "").equals("chat")) return (!hiddenOnly || bool(trigger, "hide", false))
            && java.util.regex.Pattern.compile(string(trigger, "pattern", "")).matcher(text).find();
        if (string(trigger, "type", "").equals("any")) return array(trigger, "of").asList().stream().anyMatch(e -> chatTrigger(e.getAsJsonObject(), text, hiddenOnly));
        return false;
    }
    public boolean chat(ServerPlayer player, String text) {
        participant = player;
        if (!loaded.valid()) return false;
        signal(player, "chat", text, null);
        boolean hide = false;
        JsonObject chapter = currentChapter();
        if (chapter == null || !state.active()) return false;
        for (JsonElement e : array(chapter, "beats")) {
            JsonObject beat = e.getAsJsonObject(); JsonObject when = object(beat, "when");
            if (chatTrigger(when, text, false) && eligible(beat)) { hide |= chatTrigger(when, text, true); fire(string(beat, "id", "")); }
        }
        return hide;
    }
    public ServerEventBus events() { return events; }
    public SessionState state() { return state; }
    public MinecraftServer server() { return server; }
    public StoryLoader.Result loaded() { return loaded; }
    public JsonObject root() { return loaded.data().root(); }
    public JsonObject currentChapter() {
        int index = SLOTS.indexOf(state.act());
        return loaded.valid() && index >= 0 && index < loaded.data().chapters().size() ? loaded.data().chapters().get(index) : null;
    }
    public int ticks(double seconds) { return Math.max(0, (int) Math.ceil(seconds * 20 * (autoplay ? number(object(root(), "autoplay"), "time_scale", 1) : 1))); }
    private void compile(String source, JsonArray list, JsonObject beat, StoryValidator validator) {
        List<StoryActions.Action> compiled = new ArrayList<>(); List<StoryScheduler.Step> timeline = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            JsonObject action = list.get(i).getAsJsonObject();
            compiled.add(StoryActions.parser(string(action, "do", "")).parse(action, validator.context("", "", beat)));
            timeline.add(new StoryScheduler.Step(ticks(number(action, "at", 0)), i));
        }
        if (beat.has("capture")) {
            JsonObject capture = object(beat, "capture"); int index = compiled.size();
            compiled.add((r, p) -> { String name = string(capture, "name", ""); log("capture=" + name); events.post(new StoryCapture(name)); return "ok"; });
            timeline.add(new StoryScheduler.Step(ticks(number(capture, "at", 0)), index));
        }
        timeline.sort(Comparator.comparingInt(StoryScheduler.Step::at));
        steps.put(source, List.copyOf(timeline)); actions.put(source, List.copyOf(compiled)); JsonObject metadata = beat.deepCopy(); metadata.add("actions", list); sources.put(source, metadata);
    }
    private void enter(ServerPlayer p) {
        boolean respawn = participant != null && participant != p && dead;
        participant = p; JsonObject chapter = currentChapter();
        if (chapter == null) return;
        if (respawn) { signal(p, "respawn", "", null); dead = false; }
        chapterId = string(chapter, "id", ""); String entered = "story.entered." + chapterId;
        boolean resume = state.flags().contains(entered); generation++;
        if (!resume) { scheduler.cancel(null); state.addFlag(entered); }
        teleport(p, resolvePosition(chapter.get(respawn && chapter.has("respawn") ? "respawn" : "checkpoint")));
        scheduler.schedule(chapterId + (resume ? ".on_resume" : ".on_enter"), steps.get(chapterId + (resume ? ".on_resume" : ".on_enter")));
        log("chapter=" + chapterId + " slot=" + state.act() + (resume ? " resume" : " enter"));
        simulated.clear(); autoplayReady.clear(); triggers.values().forEach(StoryTriggers.Trigger::reset);
        drain(false);
    }
    private void tick(ServerPlayer p) {
        participant = p; JsonObject chapter = currentChapter(); if (chapter == null) return;
        if (p.isDeadOrDying() && !dead) { state.setCounter("story.deaths", state.counters().getOrDefault("story.deaths", 0) + 1); signal(p, "death", "", null); }
        if (!p.isDeadOrDying() && dead) signal(p, "respawn", "", null);
        dead = p.isDeadOrDying();
        drain(true); int before = generation;
        for (JsonElement e : array(chapter, "beats")) {
            if (before != generation || !state.active()) break;
            JsonObject beat = e.getAsJsonObject(); if (!eligible(beat)) continue;
            String id = string(beat, "id", ""); StoryTriggers.Trigger trigger = triggers.get(id);
            if (autoplay && p.connection.hasClientLoaded()) autoplayReady.putIfAbsent(id, state.ticks());
            if (autoplay && p.connection.hasClientLoaded() && !trigger.automatic() && !simulated.contains(id) && !skipped(id)
                    && state.ticks() - autoplayReady.getOrDefault(id, state.ticks()) >= ticks(number(object(beat, "autoplay"), "wait_seconds", 1))) {
                log("autoplay simulate beat=" + id + " trigger=" + string(object(beat, "when"), "type", ""));
                simulated.add(id); trigger.simulate(p);
            }
            if (!skipped(id) && trigger.test(p)) fire(id);
        }
        signals.clear();
    }
    private boolean skipped(String id) {
        if (!autoplay) return false;
        JsonArray runs = array(object(root(), "autoplay"), "runs");
        if (run >= runs.size()) throw new IllegalStateException("Autoplay run outside story runs");
        return array(runs.get(run).getAsJsonObject(), "skip").asList().stream().anyMatch(e -> e.getAsString().equals(id));
    }
    private boolean eligible(JsonObject beat) {
        return StoryProgress.eligible(state, beat, this::ticks);
    }
    public boolean fire(String beatId) {
        if (participant == null) participant = server.getPlayerList().getPlayers().stream().findFirst().orElse(null);
        if (participant == null) return false;
        if (!state.active() || currentChapter() == null || !beats.containsKey(beatId)
                || !beatId.startsWith(string(currentChapter(), "id", "") + ".") || !eligible(beats.get(beatId))) return false;
        state.addFlag("story.fired." + beatId);
        state.setCounter("story.fired_at." + beatId, (int) state.elapsedSessionTicks());
        state.setCounter("story.repeat." + beatId, state.counters().getOrDefault("story.repeat." + beatId, 0) + 1);
        scheduler.schedule(beatId, steps.get(beatId));
        log("beat=" + beatId + " fired t=" + state.elapsedSessionTicks() / 20);
        events.post(new StoryBeatFired(beatId, string(currentChapter(), "id", "")));
        triggers.get(beatId).reset(); drain(false); return true;
    }
    private void drain(boolean clock) {
        int before = generation;
        scheduler.tick(id -> steps.getOrDefault(id, List.of()), (id, index) -> {
            JsonObject source = sources.get(id);
            String actionName = index < array(source, "actions").size() ? string(array(source, "actions").get(index).getAsJsonObject(), "do", "capture") : "capture";
            String result;
            try { executingBeat = id; result = actions.get(id).get(index).execute(this, participant); }
            catch (RuntimeException e) { UnallocatedMod.LOGGER.error("UNALLOCATED story action failed beat=" + id, e); result = "refused"; }
            executingBeat = "";
            log("action beat=" + id + " do=" + actionName + " result=" + result);
            return before == generation && state.active();
        }, clock);
    }
    public void cancel(String id) { scheduler.cancel(id); }
    public void next(ServerPlayer player) {
        scheduler.cancel(null); generation++;
        int slot = SLOTS.indexOf(state.act());
        if (slot + 1 >= loaded.data().chapters().size()) end(player, string(root(), "fallback_ending", "SERVER_CLOSED"));
        else CoreRuntime.get(server).sequencer().advance(player);
    }
    public void end(ServerPlayer player, String ending) {
        scheduler.cancel(null); generation++;
        state.addFlag("story.ending." + ending);
        CoreRuntime.get(server).sequencer().jump(Act.EPILOGUE, player);
    }
    public boolean closeEnding(ServerPlayer player) {
        if (!loaded.valid()) return false;
        String chosen = string(root(), "fallback_ending", "SERVER_CLOSED");
        for (String id : object(root(), "endings").keySet()) if (state.flags().contains("story.ending." + id)) { chosen = id; break; }
        JsonObject ending = object(object(root(), "endings"), chosen);
        state.addFlag("story.ending." + chosen); log("ending=" + chosen); events.post(new StoryEnding(chosen));
        if (!bool(ending, "builtin", false)) effect(player, "ending_text", ending, 0);
        if (autoplay) {
            String expected = string(array(object(root(), "autoplay"), "runs").get(run).getAsJsonObject(), "ending", "");
            if (!expected.equals(chosen)) throw new IllegalStateException("Autoplay reached " + chosen + " expected " + expected);
            log("autoplay run=" + run + " ending=" + chosen + " complete");
        }
        EndingController.close(player, new EndingController.Ending(chosen, EndingText.reason())); return true;
    }
    public void effect(ServerPlayer p, String id, JsonObject data, int duration) {
        JsonObject envelope = new JsonObject(); JsonObject wire = data.deepCopy();
        if (autoplay) for (String key : List.of("fade_in", "hold", "fade_out", "stay")) {
            double fallback = key.equals("fade_in") ? (id.equals("title") ? .5 : .1) : key.equals("hold") ? .5 : key.equals("stay") ? 3 : id.equals("title") ? 1 : .4;
            wire.addProperty(key, number(wire, key, fallback) * number(object(root(), "autoplay"), "time_scale", 1));
        }
        if (id.equals("interface")) wire.addProperty("beat", executingBeat);
        envelope.add("data", wire);
        for (String key : List.of("languages", "default_language", "language_select", "agreement_fallback")) if (root().has(key)) envelope.add(key, root().get(key).deepCopy());
        envelope.addProperty("elapsed_min", state.elapsedSessionTicks() / 1200); envelope.addProperty("deaths", state.counters().getOrDefault("story.deaths", 0));
        JsonObject counters = new JsonObject(); state.counters().forEach((key, value) -> { if (key.startsWith("story.counter.")) counters.addProperty(key.substring(14), value); }); envelope.add("counters", counters);
        CompoundTag args = new CompoundTag(); args.putString("json", envelope.toString());
        CoreNetworking.send(p, new EffectPayload("story." + id, args, duration));
    }
    public record Position(String dimension, double x, double y, double z, float yaw, float pitch) {
        public BlockPos block() { return BlockPos.containing(x, y, z); }
        public Vec3 center() { return new Vec3(x + .5, y, z + .5); }
    }
    public record Box(String dimension, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        public boolean contains(double x, double y, double z) { return x >= minX && x < maxX + 1 && y >= minY && y < maxY + 1 && z >= minZ && z < maxZ + 1; }
        public boolean contains(ServerPlayer p) { return dimension.equals(dimensionName(p.level())) && contains(p.getX(), p.getY(), p.getZ()); }
        public Position center() { return new Position(dimension, (minX + maxX) / 2, minY, (minZ + maxZ) / 2, 0, 0); }
    }
    public static String dimensionName(ServerLevel level) { return level.dimension().identifier().equals(Identifier.parse("minecraft:overworld")) ? "overworld" : "pocket"; }
    public ServerLevel level(String dimension) { return Objects.requireNonNull(server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimension.equals("pocket") ? "unallocated:pocket" : "minecraft:overworld"))), "Missing dimension"); }
    public Position resolvePosition(JsonElement e) {
        if (e.isJsonPrimitive()) {
            String name = e.getAsString();
            if (object(root(), "anchors").has(name)) return resolvePosition(object(root(), "anchors").get(name));
            if (MapLayout.checkpoints().containsKey(name)) { var cp = MapLayout.checkpoint(name); return new Position("overworld", cp.x(), cp.y(), cp.z(), cp.yaw(), cp.pitch()); }
            BlockPos p = MapLayout.landmark(name); return new Position("overworld", p.getX(), p.getY(), p.getZ(), 0, 0);
        }
        JsonObject o = e.getAsJsonObject();
        if (o.has("anchor")) {
            Position base = resolvePosition(o.get("anchor")); JsonArray off = array(o, "offset");
            return off.isEmpty() ? base : new Position(base.dimension, base.x + off.get(0).getAsDouble(), base.y + off.get(1).getAsDouble(), base.z + off.get(2).getAsDouble(), base.yaw, base.pitch);
        }
        JsonArray pos = array(o, "pos"); return new Position(string(o, "dimension", "overworld"), pos.get(0).getAsDouble(), pos.get(1).getAsDouble(), pos.get(2).getAsDouble(), (float) number(o, "yaw", 0), (float) number(o, "pitch", 0));
    }
    public Box resolveBox(JsonElement e) {
        if (e.isJsonPrimitive()) {
            String name = e.getAsString(); if (object(root(), "zones").has(name)) return resolveBox(object(root(), "zones").get(name));
            return mapBox(MapLayout.trigger(name));
        }
        JsonObject o = e.getAsJsonObject(); if (o.has("section")) return mapBox(MapLayout.section(string(o, "section", "")).bounds());
        JsonArray a = array(o, "box"); double[] v = a.asList().stream().mapToDouble(JsonElement::getAsDouble).toArray();
        return new Box(string(o, "dimension", "overworld"), Math.min(v[0], v[3]), Math.min(v[1], v[4]), Math.min(v[2], v[5]), Math.max(v[0], v[3]), Math.max(v[1], v[4]), Math.max(v[2], v[5]));
    }
    private static Box mapBox(MapLayout.BlockBox b) { return new Box("overworld", b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()); }
    public boolean teleport(ServerPlayer p, Position pos) { Vec3 v = pos.center(); return p.teleportTo(level(pos.dimension), v.x, v.y, v.z, Set.of(), pos.yaw, pos.pitch, true); }
    static void log(String text) { UnallocatedMod.LOGGER.info("UNALLOCATED story " + text); }
    private record ChapterModule(int index) implements ActModule {
        public Act act() { return SLOTS.get(index); }
        private StoryRuntime runtime() {
            if (SERVERS.size() != 1) throw new IllegalStateException("Expected one active story server"); return SERVERS.values().iterator().next();
        }
        public boolean enabled(Features f) { StoryRuntime r = runtime(); return !r.loaded.valid() || index < r.loaded.data().chapters().size(); }
        public void enter(ServerPlayer p, SessionState s) { StoryRuntime r = get(p.level().getServer()); if (r.loaded.valid()) r.enter(p); }
        public void tick(ServerPlayer p, SessionState s) { StoryRuntime r = get(p.level().getServer()); if (r.loaded.valid()) r.tick(p); }
        public boolean complete(ServerPlayer p, SessionState s) { return false; }
        public int budgetTicks() {
            StoryRuntime r = runtime(); return r.loaded.valid() && index < r.loaded.data().chapters().size() ? Math.max(1, r.ticks(number(r.loaded.data().chapters().get(index), "budget_seconds", 420))) : new int[]{6000,1800,2400,3600,2400}[index];
        }
        public void forceAdvance(ServerPlayer p, SessionState s) {
            StoryRuntime r = get(p.level().getServer()); JsonObject chapter = r.currentChapter(); if (chapter == null) return;
            log("chapter=" + string(chapter, "id", "") + " slot=" + act() + " escape"); r.scheduler.cancel(null); r.generation++;
            // Rescue is synchronous by core contract: execute escape in stable at/file order.
            String source = string(chapter, "id", "") + ".escape";
            for (var step : r.steps.get(source)) r.actions.get(source).get(step.index()).execute(r, p);
        }
        public String checkpointId() {
            StoryRuntime r = runtime(); return r.loaded.valid() && index < r.loaded.data().chapters().size() ? r.loaded.data().chapters().get(index).get("checkpoint").toString().replace("\"", "") : "lobby.spawn";
        }
    }
}
