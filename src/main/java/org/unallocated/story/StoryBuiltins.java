package org.unallocated.story;

import com.google.gson.*;
import java.util.*;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import org.unallocated.audio.AudioSounds;
import org.unallocated.bait.*;
import static org.unallocated.story.StoryData.*;

/** Vanilla adapters. No client classes, duplicate payload registration or extra Fabric modules. */
final class StoryBuiltins {
    private static boolean initialized;
    static synchronized void initialize() {
        if (initialized) return; initialized = true;
        for (String name : List.of("flag", "counter", "next_chapter", "end", "cancel", "title", "actionbar", "chat", "blackout", "interface", "audio", "teleport", "bait", "command", "function"))
            StoryActions.register(name, (json, context) -> { context.action(json); return (runtime, player) -> execute(runtime, player, json); });
        for (String name : List.of("chapter_start", "after", "flag", "zone", "look", "interact", "use_item", "has_item", "break", "chat", "still", "sprint", "sneak", "death", "respawn", "bait", "any"))
            StoryTriggers.register(name, (json, context) -> { context.trigger(json); return new Trigger(json, context.beat); });
    }
    private static String execute(StoryRuntime r, ServerPlayer p, JsonObject a) {
        String name = string(a, "do", "");
        switch (name) {
            case "flag" -> { if (a.has("set")) r.state().addFlag("story.flag." + string(a, "set", "")); else r.state().removeFlag("story.flag." + string(a, "clear", "")); }
            case "counter" -> { String id = "story.counter." + string(a, "id", ""); r.state().setCounter(id, a.has("set") ? (int) number(a, "set", 0) : r.state().counters().getOrDefault(id, 0) + (int) number(a, "add", 0)); }
            case "next_chapter" -> r.next(p);
            case "end" -> r.end(p, string(a, "ending", ""));
            case "cancel" -> r.cancel(string(a, "beat", ""));
            case "title", "actionbar", "chat", "blackout" -> r.effect(p, name, a, r.ticks(number(a, "seconds", 0)));
            case "interface" -> {
                JsonObject wire = a.deepCopy(); if (a.has("sign")) { var pos = r.resolvePosition(a.get("sign")); JsonArray xyz = new JsonArray(); xyz.add(pos.x()); xyz.add(pos.y()); xyz.add(pos.z()); JsonObject object = new JsonObject(); object.add("pos", xyz); wire.add("sign", object); }
                wire.addProperty("beat", string(a, "_beat", "interface"));
                r.effect(p, name, wire, Math.max(1, r.ticks(number(a, "seconds", 1))));
            }
            case "audio" -> {
                String cue = string(a, "cue", ""); if (cue.equals("stop")) AudioSounds.stop(p);
                else if (!AudioSounds.play(p, cue, a.has("where") ? r.resolvePosition(a.get("where")).center() : p.position())) return "skipped";
            }
            case "teleport" -> {
                if (bool(a, "blackout", false)) r.effect(p, "blackout", new JsonObject(), 0);
                if (!r.teleport(p, r.resolvePosition(a.get("to")))) return "refused";
            }
            case "bait" -> {
                switch (string(a, "op", "")) {
                    case "place" -> {
                        SpyglassBaitSpec spec = SpyglassBaitSpec.defaults().withScare(bool(a, "scare", false));
                        if (a.has("distances")) spec = spec.withDistances(array(a, "distances").asList().stream().map(JsonElement::getAsDouble).toList());
                        spec = spec.withAppearDelayTicks(r.ticks(number(a, "appear_delay", 0))).withFallbackAfterTicks(r.ticks(number(a, "fallback_after", 90)))
                            .withMaxLifetimeTicks(Math.max(200, r.ticks(number(a, "max_lifetime", 300)))).withSeed((long) number(a, "seed", 412));
                        SpyglassBait.place(p, spec, a.has("lure") ? r.resolvePosition(a.get("lure")).block() : null, false);
                    }
                    case "step" -> { if (!SpyglassBait.forceStep(p)) return "skipped"; }
                    case "scare" -> { if (!SpyglassBait.forceScare(p)) return "skipped"; }
                    case "clear" -> SpyglassBait.clear(p, "story");
                    default -> { return "refused"; }
                }
            }
            case "command", "function" -> {
                String command = name.equals("command") ? string(a, "run", "") : "function " + string(a, "id", "");
                r.server().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER).withSuppressedOutput(), command);
            }
            default -> { return "refused"; }
        }
        return "ok";
    }
    private static final class Trigger implements StoryTriggers.Trigger {
        private final JsonObject json, beat;
        private final String type;
        private final List<StoryTriggers.Trigger> children;
        private long since = -1;
        private Vec3 last;
        Trigger(JsonObject json, JsonObject beat) {
            this.json = json; this.beat = beat; type = string(json, "type", "");
            children = array(json, "of").asList().stream().map(e -> (StoryTriggers.Trigger) new Trigger(e.getAsJsonObject(), beat)).toList();
        }
        public boolean automatic() { return Set.of("chapter_start", "after", "flag").contains(type) || type.equals("any") && children.stream().allMatch(StoryTriggers.Trigger::automatic); }
        public void reset() { since = -1; last = null; children.forEach(StoryTriggers.Trigger::reset); }
        public boolean test(ServerPlayer p) {
            StoryRuntime r = StoryRuntime.get(p.level().getServer());
            boolean condition = switch (type) {
                case "chapter_start" -> true;
                case "after" -> {
                    String ref = string(json, "beat", "");
                    long start = ref.isEmpty() ? r.state().actStartTick() - r.state().sessionStartTick() : r.state().counters().getOrDefault("story.fired_at." + ref, -1);
                    yield start >= 0 && r.state().elapsedSessionTicks() - start >= r.ticks(number(json, "seconds", 0));
                }
                case "flag" -> r.state().flags().contains("story.flag." + string(json, "flag", ""));
                case "zone" -> r.resolveBox(json.get("zone")).contains(p);
                case "look" -> {
                    var target = r.resolvePosition(json.get("at")); Vec3 diff = target.center().subtract(p.getEyePosition());
                    boolean clear = p.level().clip(new ClipContext(p.getEyePosition(), target.center(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS;
                    yield StoryRuntime.dimensionName(p.level()).equals(target.dimension()) && diff.length() <= number(json, "max_distance", 96)
                        && p.getLookAngle().normalize().dot(diff.normalize()) >= Math.cos(Math.toRadians(number(json, "degrees", 8)))
                        && clear && (!bool(json, "spyglass", false) || p.isScoping());
                }
                case "interact" -> r.signalMatches("interact", null, r.resolvePosition(json.get("at")).block());
                case "use_item" -> r.signalMatches("use_item", string(json, "item", ""), null);
                case "has_item" -> p.getInventory().countItem(BuiltInRegistries.ITEM.getValue(Identifier.parse(string(json, "item", "")))) >= number(json, "count", 1);
                case "break" -> r.signalMatches("break", string(json, "block", ""), null) && (!json.has("zone") || r.resolveBox(json.get("zone")).contains(p));
                case "chat" -> false; // Match/hide synchronously at the signed broadcast boundary.
                case "still" -> last != null && p.position().distanceToSqr(last) < .0001;
                case "sprint" -> p.isSprinting();
                case "sneak" -> p.isShiftKeyDown();
                case "death", "respawn" -> r.signalMatches(type, null, null);
                case "bait" -> r.signalMatches("bait", string(json, "event", ""), null);
                case "any" -> children.stream().anyMatch(t -> t.test(p));
                default -> false;
            };
            last = p.position();
            if (!condition) { since = -1; return false; }
            long now = r.state().ticks(); if (since < 0) since = now;
            double dwell = Set.of("zone", "look", "still", "sprint", "sneak").contains(type) ? number(json, "seconds", type.equals("look") ? .5 : 0) : 0;
            return now - since >= r.ticks(dwell);
        }
        public void simulate(ServerPlayer p) {
            StoryRuntime r = StoryRuntime.get(p.level().getServer());
            switch (type) {
                case "zone" -> r.teleport(p, r.resolveBox(json.get("zone")).center());
                case "look" -> {
                    var target = r.resolvePosition(json.get("at")); Vec3 pos = target.center();
                    if (!StoryRuntime.dimensionName(p.level()).equals(target.dimension()) || p.position().distanceTo(pos) > number(json, "max_distance", 96))
                        r.teleport(p, new StoryRuntime.Position(target.dimension(), target.x() - 3, target.y(), target.z(), 0, 0));
                    Vec3 diff = pos.subtract(p.getEyePosition()); float yaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90);
                    float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z)));
                    p.teleportTo(p.level(), p.getX(), p.getY(), p.getZ(), Set.of(), yaw, pitch, true);
                    if (bool(json, "spyglass", false)) { p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SPYGLASS)); p.startUsingItem(InteractionHand.MAIN_HAND); }
                }
                case "interact" -> {
                    var pos = r.resolvePosition(json.get("at")); r.teleport(p, new StoryRuntime.Position(pos.dimension(), pos.x(), pos.y(), pos.z() - 2, 0, 0));
                    p.gameMode.useItemOn(p, p.level(), p.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(pos.center(), Direction.NORTH, pos.block(), false));
                }
                case "use_item", "has_item" -> {
                    var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(string(json, "item", "")));
                    ItemStack stack = new ItemStack(item, (int) number(json, "count", 1)); p.getInventory().add(stack);
                    if (type.equals("use_item")) { p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item)); p.gameMode.useItem(p, p.level(), p.getMainHandItem(), InteractionHand.MAIN_HAND); }
                }
                case "break" -> {
                    BlockPos pos = json.has("zone") ? r.resolveBox(json.get("zone")).center().block() : p.blockPosition().east();
                    p.level().setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.getValue(Identifier.parse(string(json, "block", ""))).defaultBlockState());
                    p.gameMode.destroyBlock(pos);
                }
                case "chat" -> r.chat(p, string(object(beat, "autoplay"), "input", ""));
                case "still" -> p.setDeltaMovement(Vec3.ZERO);
                case "sprint" -> p.setSprinting(true);
                case "sneak" -> p.setShiftKeyDown(true);
                case "death" -> r.server().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER), "kill @s");
                case "respawn" -> { r.server().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER), "kill @s"); p.connection.player = r.server().getPlayerList().respawn(p, false, net.minecraft.world.entity.Entity.RemovalReason.KILLED); }
                case "bait" -> { if (string(json, "event", "").equals("step")) SpyglassBait.forceStep(p); else SpyglassBait.forceScare(p); }
                case "any" -> { if (!children.isEmpty()) children.getFirst().simulate(p); }
                default -> { }
            }
        }
    }
}
