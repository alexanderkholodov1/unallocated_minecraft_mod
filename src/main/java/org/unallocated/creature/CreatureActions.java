package org.unallocated.creature;

import com.google.gson.*;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.unallocated.story.*;
import static org.unallocated.story.StoryData.*;

public final class CreatureActions {
    private static boolean registered;
    private CreatureActions() {}
    public static synchronized void registerActions() {
        if (registered) return; registered = true;
        StoryActions.register("creature", (json, context) -> {
            org.unallocated.world.WorldActions.timing(json, context);
            String op = string(json, "op", "");
            String extra = switch (op) {
                case "spawn" -> "body where facing";
                case "stalk" -> "step min_distance";
                case "chase" -> "speed catch_distance";
                default -> "";
            };
            context.keys(json, "do at op id " + extra, "do op id" + (op.equals("spawn") ? " body where" : ""));
            if (!Set.of("spawn", "stalk", "chase", "hold", "face_player", "despawn").contains(op)) context.error("/op", "bad-value", "Unknown creature operation");
            id(json, "id", context);
            for (String key : List.of("step", "min_distance", "speed", "catch_distance"))
                if (json.has(key) && (!numeric(json.get(key)) || number(json, key, -1) < 0 || number(json, key, 0) > 128))
                    context.error("/" + key, "bad-value", "Expected number 0..128");
            if (op.equals("spawn")) {
                context.position(json.get("where"), "where");
                if (json.has("facing") && !string(json, "facing", "").equals("player") && !numeric(json.get("facing"))) context.error("/facing", "bad-value", "Expected player or yaw");
                var body = object(json, "body"); var c = context.child("/body"); c.keys(body, "mannequin summon", "");
                if (body.has("mannequin") == body.has("summon")) c.error("", "bad-value", "Exactly one mannequin or summon body required");
                if (body.has("mannequin")) {
                    var spec = object(body, "mannequin"); var m = c.child("/mannequin"); m.keys(spec, "profile texture scale", "profile");
                    if (!string(spec, "profile", "").matches("[A-Za-z0-9_]{1,16}")) m.error("/profile", "bad-value", "Expected player or a profile name");
                    if (spec.has("scale") && (!numeric(spec.get("scale")) || number(spec, "scale", 0) <= 0 || number(spec, "scale", 0) > 16)) m.error("/scale", "bad-value", "Expected scale greater than zero and at most 16");
                    if (spec.has("texture")) {
                        String path = string(spec, "texture", "");
                        if (!path.matches("unallocated:(?:textures/)?[a-z0-9_./-]+\\.png") || path.contains("..")) m.error("/texture", "bad-value", "Expected bundled unallocated PNG path");
                        else {
                            String resource = path.substring("unallocated:".length()); if (!resource.startsWith("textures/")) resource = "textures/" + resource;
                            try (var stream = CreatureActions.class.getResourceAsStream("/assets/unallocated/" + resource)) {
                                if (stream == null) m.error("/texture", "bad-value", "Texture does not exist in player JAR");
                            } catch (java.io.IOException error) { m.error("/texture", "bad-value", "Cannot read texture"); }
                        }
                    }
                }
                if (body.has("summon")) {
                    String summon = string(body, "summon", "");
                    if (!summon.matches("[a-z0-9_.-]+:[a-z0-9_./-]+(?:\\s*\\{[\\s\\S]*})?")) c.error("/summon", "bad-value", "Expected entity ID and optional SNBT compound");
                    else {
                        int brace = summon.indexOf('{');
                        try { if (brace >= 0) net.minecraft.nbt.TagParser.parseCompoundFully(summon.substring(brace)); }
                        catch (com.mojang.brigadier.exceptions.CommandSyntaxException error) { c.error("/summon", "bad-value", "Invalid SNBT compound"); }
                        JsonObject command = new JsonObject(); command.addProperty("do", "command");
                        command.addProperty("run", "summon " + (brace < 0 ? summon : summon.substring(0, brace).strip()) + " ~ ~ ~" + (brace < 0 ? "" : " " + summon.substring(brace)));
                        c.child("/summon").action(command); // The real server dispatcher also checks summonable entity IDs and SNBT.
                    }
                }
            }
            return (runtime, player) -> Creatures.execute(runtime, player, json);
        });
        StoryTriggers.register("creature", (json, context) -> {
            context.keys(json, "type creature event distance seconds", "type creature event"); id(json, "creature", context);
            String event = string(json, "event", "");
            if (!Set.of("near", "seen", "caught").contains(event)) context.error("/event", "bad-value", "Expected near, seen or caught");
            for (String key : List.of("distance", "seconds")) if (json.has(key) && (!numeric(json.get(key)) || number(json, key, -1) < 0)) context.error("/" + key, "bad-value", "Expected nonnegative number");
            return new StoryTriggers.Trigger() {
                long since = -1;
                public boolean test(ServerPlayer player) {
                    String id = string(json, "creature", ""); Entity entity = Creatures.entity(player, id);
                    boolean matches = entity != null && !entity.isRemoved() && entity.level() == player.level();
                    if (matches) matches = switch (event) {
                        case "near" -> entity.position().distanceTo(player.position()) <= number(json, "distance", 3);
                        case "seen" -> Creatures.seen(player, entity);
                        case "caught" -> Creatures.chasing(player, id) && entity.position().distanceTo(player.position()) <= Creatures.catchDistance(player, id);
                        default -> false;
                    };
                    if (!matches) { since = -1; return false; }
                    var runtime = StoryRuntime.get(player.level().getServer()); long now = runtime.state().ticks();
                    if (since < 0) since = now;
                    return now - since >= runtime.ticks(number(json, "seconds", event.equals("seen") ? .5 : 0));
                }
                public void simulate(ServerPlayer player) {
                    String id = string(json, "creature", "");
                    Creatures.simulate(player, id, event, event.equals("caught") ? Creatures.catchDistance(player, id) : number(json, "distance", 3));
                }
                public void reset() { since = -1; }
            };
        });
    }
    private static void id(JsonObject json, String key, StoryValidator.Context context) {
        if (!string(json, key, "").matches("[a-z][a-z0-9_.]{0,47}")) context.error("/" + key, "bad-id", "Invalid creature ID");
    }
    private static boolean numeric(JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() && Double.isFinite(value.getAsDouble());
    }
    public static void initialize() { registerActions(); Creatures.initialize(); }
}
