package org.unallocated.world;

import com.google.gson.*;
import com.mojang.brigadier.arguments.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import org.unallocated.story.*;
import static org.unallocated.story.StoryData.*;

/** Package 4 extensions register before StoryRuntime loads any authored files. */
public final class WorldActions {
    private static boolean registered;
    private WorldActions() {}
    public static synchronized void registerActions() {
        if (registered) return; registered = true;
        StoryActions.register("copy", (json, context) -> {
            context.keys(json, "do at from to", "do from to"); context.box(json.get("from"), "from"); context.position(json.get("to"), "to");
            return (runtime, player) -> { WorldCopy.copy(runtime, runtime.resolveBox(json.get("from")), runtime.resolvePosition(json.get("to"))); return "ok"; };
        });
        StoryActions.register("corrupt", (json, context) -> {
            context.keys(json, "do at area rules seed over_seconds", "do area rules"); context.box(json.get("area"), "area");
            if (json.has("seed") && !integer(json.get("seed"))) context.error("/seed", "bad-value", "Expected signed 64-bit integer seed");
            double seconds = number(json, "over_seconds", 0);
            if (json.has("over_seconds") && (!numeric(json.get("over_seconds")) || seconds < 0))
                context.error("/over_seconds", "bad-time", "Expected nonnegative seconds");
            var list = array(json, "rules");
            if (!json.has("rules") || !json.get("rules").isJsonArray()) context.error("/rules", "bad-value", "Expected rule list");
            for (int i = 0; i < list.size(); i++) {
                var c = context.child("/rules/" + i);
                if (!list.get(i).isJsonObject()) { c.error("", "bad-value", "Expected rule object"); continue; }
                var rule = list.get(i).getAsJsonObject(); c.keys(rule, "from to ratio", "from to ratio");
                for (String key : new String[]{"from", "to"}) {
                    var blocks = array(rule, key);
                    if (!rule.has(key) || !rule.get(key).isJsonArray()) c.error("/" + key, "bad-value", "Expected block list");
                    for (var block : blocks) if (!block.isJsonPrimitive() || !block.getAsJsonPrimitive().isString() || !block.getAsString().matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
                        c.error("/" + key, "bad-value", "Expected block resource ID");
                }
                if (!numeric(rule.get("ratio")) || number(rule, "ratio", -1) < 0 || number(rule, "ratio", 2) > 1)
                    c.error("/ratio", "bad-value", "Expected ratio 0..1");
            }
            return (runtime, player) -> { WorldCorruption.start(runtime, runtime.resolveBox(json.get("area")), array(json, "rules"), json.has("seed") ? json.get("seed").getAsLong() : 0L, seconds); return "ok"; };
        });
    }
    private static boolean numeric(JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() && Double.isFinite(value.getAsDouble());
    }
    private static boolean integer(JsonElement value) {
        if (!numeric(value)) return false;
        try { value.getAsBigDecimal().toBigIntegerExact().longValueExact(); return true; }
        catch (ArithmeticException error) { return false; }
    }
    public static void initialize() {
        registerActions(); WorldCorruption.initialize();
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            var destination = Commands.argument("dimension", StringArgumentType.word()).executes(c -> {
                var runtime = StoryRuntime.get(c.getSource().getServer()); String dimension = StringArgumentType.getString(c, "dimension");
                if (!dimension.equals("overworld") && !dimension.equals("pocket")) throw new IllegalArgumentException("Expected overworld or pocket");
                return WorldCopy.copy(runtime, zone(runtime, StringArgumentType.getString(c, "zone")),
                    new StoryRuntime.Position(dimension, IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "y"), IntegerArgumentType.getInteger(c, "z"), 0, 0));
            });
            var copy = Commands.literal("copy").then(Commands.argument("zone", StringArgumentType.word())
                .then(Commands.argument("x", IntegerArgumentType.integer()).then(Commands.argument("y", IntegerArgumentType.integer())
                .then(Commands.argument("z", IntegerArgumentType.integer()).then(destination)))));
            var corrupt = Commands.literal("corrupt").then(Commands.argument("zone", StringArgumentType.word())
                .then(Commands.argument("seed", LongArgumentType.longArg()).executes(c -> {
                    var runtime = StoryRuntime.get(c.getSource().getServer());
                    var rules = JsonParser.parseString("[{\"from\":[\"minecraft:stone\",\"minecraft:cobblestone\"],\"to\":[\"minecraft:cracked_stone_bricks\"],\"ratio\":0.25}]").getAsJsonArray();
                    return WorldCorruption.start(runtime, zone(runtime, StringArgumentType.getString(c, "zone")), rules, LongArgumentType.getLong(c, "seed"), 0);
                })));
            dispatcher.register(Commands.literal("unallocated").then(Commands.literal("story")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(copy).then(corrupt)));
        });
    }
    private static StoryRuntime.Box zone(StoryRuntime runtime, String name) {
        if (org.unallocated.map.MapLayout.sections().containsKey(name)) {
            JsonObject section = new JsonObject(); section.addProperty("section", name); return runtime.resolveBox(section);
        }
        if (!object(runtime.root(), "zones").has(name) && !org.unallocated.map.MapLayout.triggers().containsKey(name))
            throw new IllegalArgumentException("Unknown story zone " + name);
        return runtime.resolveBox(new JsonPrimitive(name));
    }
}
