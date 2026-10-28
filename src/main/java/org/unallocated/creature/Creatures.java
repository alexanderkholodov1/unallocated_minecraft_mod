package org.unallocated.creature;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.unallocated.core.SessionState;
import org.unallocated.world.StoryRecords;
import org.unallocated.mixin.bait.BaitMannequinInvoker;
import org.unallocated.story.StoryRuntime;
import static org.unallocated.story.StoryData.*;

/** One participant's transient bodies; only their runtime descriptions are persisted. */
public final class Creatures {
    public static final String TAG = "unallocated.story_creature";
    private static final String PREFIX = "story.creature.";
    private static final Map<MinecraftServer, Map<String, Body>> SERVERS = new IdentityHashMap<>();
    private static final class Body {
        JsonObject data; String flag; Entity entity;
        Body(JsonObject data, String flag) { this.data = data; this.flag = flag; }
    }
    private Creatures() {}
    static void initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Map<String, Body> bodies = new LinkedHashMap<>();
            var state = SessionState.get(server);
            for (String flag : List.copyOf(state.flags())) if (flag.startsWith(PREFIX)) {
                String key = flag;
                JsonObject data;
                if (flag.contains("=")) {
                    data = JsonParser.parseString(flag.substring(flag.indexOf('=') + 1)).getAsJsonObject();
                    key = PREFIX + string(data, "id", ""); StoryRecords.write(state, key, data); state.removeFlag(flag);
                } else data = StoryRecords.read(state, flag);
                bodies.put(string(data, "id", ""), new Body(data, key));
            }
            SERVERS.put(server, bodies);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(SERVERS::remove);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var bodies = SERVERS.get(server); if (bodies == null || bodies.isEmpty()) return;
            var state = SessionState.get(server); var players = server.getPlayerList().getPlayers();
            if (!state.active() || players.isEmpty()) { for (var b : bodies.values()) discard(b); return; }
            ServerPlayer player = players.getFirst();
            for (var body : bodies.values()) {
                if (body.entity == null || body.entity.isRemoved()) spawn(server, body, player);
                Entity entity = body.entity; entity.setDeltaMovement(Vec3.ZERO);
                Vec3 saved = position(body); entity.snapTo(saved, (float) number(body.data, "yaw", 0), 0);
                if (entity.level() != player.level()) continue;
                String op = string(body.data, "op", "hold");
                if (op.equals("face_player")) face(body, player);
                if (op.equals("stalk") || op.equals("chase")) {
                    if (op.equals("stalk") && state.ticks() % 20 != 0) continue;
                    boolean seen = op.equals("stalk") && seen(player, entity);
                    double step = op.equals("stalk") ? number(body.data, "step", 1) : number(body.data, "speed", 3) / 20;
                    double minimum = op.equals("stalk") ? number(body.data, "min_distance", 3) : 0;
                    var from = point(saved); var target = point(player.position());
                    var next = CreatureMotion.toward(from, target, step, minimum, seen,
                        candidate -> safe(entity, saved, new Vec3(candidate.x(), candidate.y(), candidate.z()), player.position(), minimum) != null);
                    Vec3 grounded = safe(entity, saved, new Vec3(next.x(), next.y(), next.z()), player.position(), minimum);
                    if (grounded != null && !grounded.equals(saved)) {
                        move(body, grounded); face(body, player); persist(server, body);
                    }
                }
            }
        });
    }
    private static CreatureMotion.Point point(Vec3 value) { return new CreatureMotion.Point(value.x, value.y, value.z); }
    private static Vec3 position(Body body) { return new Vec3(number(body.data, "x", 0), number(body.data, "y", 64), number(body.data, "z", 0)); }
    private static void move(Body body, Vec3 to) {
        body.entity.snapTo(to); body.data.addProperty("x", to.x); body.data.addProperty("y", to.y); body.data.addProperty("z", to.z);
    }
    private static void face(Body body, ServerPlayer player) {
        Vec3 delta = player.position().subtract(body.entity.position());
        float yaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90);
        body.entity.setYRot(yaw);
        if (body.entity instanceof LivingEntity living) { living.setYHeadRot(yaw); living.setYBodyRot(yaw); }
        body.data.addProperty("yaw", yaw);
    }
    private static void persist(MinecraftServer server, Body body) {
        body.flag = PREFIX + string(body.data, "id", ""); StoryRecords.write(SessionState.get(server), body.flag, body.data);
    }
    private static void discard(Body body) {
        if (body.entity != null) { body.entity.getSelfAndPassengers().toList().forEach(Entity::discard); body.entity = null; }
    }
    public static String execute(StoryRuntime runtime, ServerPlayer player, JsonObject action) {
        var bodies = SERVERS.computeIfAbsent(runtime.server(), s -> new LinkedHashMap<>()); String id = string(action, "id", "");
        Body body = bodies.get(id); String op = string(action, "op", "");
        if (op.equals("despawn")) {
            if (body == null) return "skipped";
            discard(body); StoryRecords.remove(runtime.state(), body.flag); bodies.remove(id); return "ok";
        }
        if (op.equals("spawn")) {
            if (body != null) { discard(body); StoryRecords.remove(runtime.state(), body.flag); }
            JsonObject data = new JsonObject(); data.addProperty("id", id); data.add("body", action.get("body").deepCopy()); data.addProperty("op", "hold");
            var where = runtime.resolvePosition(action.get("where")); var p = where.center();
            data.addProperty("dimension", where.dimension()); data.addProperty("x", p.x); data.addProperty("y", p.y); data.addProperty("z", p.z);
            data.addProperty("yaw", action.has("facing") && action.get("facing").isJsonPrimitive() && action.getAsJsonPrimitive("facing").isNumber()
                ? action.get("facing").getAsDouble() : where.yaw());
            body = new Body(data, null); spawn(runtime.server(), body, player);
            if (string(action, "facing", "").equals("player")) face(body, player);
            bodies.put(id, body);
        } else {
            if (body == null) return "skipped";
            body.data.addProperty("op", op);
            for (String key : List.of("step", "min_distance", "speed", "catch_distance")) if (action.has(key)) body.data.add(key, action.get(key).deepCopy());
            if (op.equals("face_player") && body.entity != null) face(body, player);
        }
        persist(runtime.server(), body); return "ok";
    }
    private static void spawn(MinecraftServer server, Body body, ServerPlayer player) {
        var runtime = StoryRuntime.get(server); var level = runtime.level(string(body.data, "dimension", "overworld"));
        Vec3 position = position(body); level.getChunk(BlockPos.containing(position));
        JsonObject spec = object(body.data, "body"); Entity entity;
        if (spec.has("mannequin")) {
            JsonObject mannequin = object(spec, "mannequin");
            entity = EntityTypes.MANNEQUIN.create(level, EntitySpawnReason.TRIGGERED);
            if (!(entity instanceof Mannequin figure) || !((Object) figure instanceof BaitMannequinInvoker)) throw new IllegalStateException("Mannequin accessor unavailable");
            var profile = string(mannequin, "profile", "player").equals("player") ? ResolvableProfile.createResolved(player.getGameProfile())
                : ResolvableProfile.createUnresolved(string(mannequin, "profile", "player"));
            if (mannequin.has("texture")) {
                var json = ResolvableProfile.CODEC.encodeStart(JsonOps.INSTANCE, profile).getOrThrow().getAsJsonObject();
                String texture = string(mannequin, "texture", ""); Identifier asset = Identifier.parse(texture);
                String path = asset.getPath(); if (path.startsWith("textures/")) path = path.substring(9);
                if (path.endsWith(".png")) path = path.substring(0, path.length() - 4);
                json.addProperty("texture", asset.getNamespace() + ":" + path);
                profile = ResolvableProfile.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
            }
            var accessor = (BaitMannequinInvoker) (Object) figure;
            accessor.unallocated$setProfile(profile); accessor.unallocated$setImmovable(true); accessor.unallocated$setHideDescription(true);
            var scale = figure.getAttribute(Attributes.SCALE); if (scale != null) scale.setBaseValue(number(mannequin, "scale", 1));
        } else {
            String summon = string(spec, "summon", ""); int brace = summon.indexOf('{');
            String type = brace < 0 ? summon : summon.substring(0, brace).strip();
            CompoundTag tag;
            try { tag = brace < 0 ? new CompoundTag() : TagParser.parseCompoundFully(summon.substring(brace)); }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException error) { throw new IllegalArgumentException("Invalid creature SNBT", error); }
            tag.putString("id", type); tag.remove("UUID");
            entity = EntityType.loadEntityRecursive(tag, level, new EntitySpawnRequest(EntitySpawnReason.TRIGGERED, false), created -> created);
            if (entity == null) throw new IllegalArgumentException("Cannot summon creature " + type);
        }
        body.entity = Objects.requireNonNull(entity); entity.snapTo(position, (float) number(body.data, "yaw", 0), 0);
        entity.getSelfAndPassengers().forEach(created -> {
            created.addTag(TAG); created.setNoGravity(true); created.setPermanentlyInvulnerable(true); created.setSilent(true);
            if (created instanceof Mob mob) mob.setNoAi(true);
        });
        level.addFreshEntityWithPassengers(entity);
        if (entity.shouldBeSaved()) throw new IllegalStateException("Creature save exclusion is missing");
    }
    /** Same conservative 80-degree plain view / 5.5-degree scope and visual raycast as the bait. */
    public static boolean seen(ServerPlayer player, Entity entity) {
        if (player.level() != entity.level()) return false;
        Vec3 eye = player.getEyePosition();
        for (Vec3 target : List.of(entity.getEyePosition(), entity.position().add(0, entity.getBbHeight() * .5, 0))) {
            Vec3 delta = target.subtract(eye); double distance = delta.length();
            boolean clear = player.level().clip(new ClipContext(eye, target, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty())).getType() == HitResult.Type.MISS;
            if (CreatureMotion.inView(distance < .01 ? 1 : player.getLookAngle().dot(delta.scale(1 / distance)), distance, player.isScoping(), clear)) return true;
        }
        return false;
    }
    /** Ground within one block, then sweep the entire body to prevent crossing walls at any speed. */
    private static Vec3 safe(Entity entity, Vec3 from, Vec3 candidate, Vec3 player, double minimum) {
        var level = (ServerLevel) entity.level(); Vec3 ground = null;
        for (int offset : new int[]{0, 1, -1}) {
            double y = Math.floor(from.y) + offset; BlockPos feet = BlockPos.containing(candidate.x, y, candidate.z), below = feet.below();
            if (!level.hasChunkAt(feet) || !level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) continue;
            Vec3 at = new Vec3(candidate.x, y, candidate.z);
            if (level.noCollision(entity, bounds(entity).move(at.subtract(from)))) { ground = at; break; }
        }
        if (ground == null || ground.distanceTo(player) + 1.0e-7 < minimum) return null;
        Vec3 delta = ground.subtract(from); int samples = Math.max(1, (int) Math.ceil(delta.length() / .2));
        for (int i = 1; i <= samples; i++) if (!level.noCollision(entity, bounds(entity).move(delta.scale((double) i / samples)))) return null;
        return ground;
    }
    private static AABB bounds(Entity entity) {
        var bounds = entity.getBoundingBox();
        return bounds.getXsize() < .1 || bounds.getYsize() < .1
            ? AABB.ofSize(entity.position().add(0, .9, 0), .6, 1.8, .6) : bounds;
    }
    public static Entity entity(ServerPlayer player, String id) {
        var body = SERVERS.getOrDefault(player.level().getServer(), Map.of()).get(id); return body == null ? null : body.entity;
    }
    public static boolean chasing(ServerPlayer player, String id) {
        var body = SERVERS.getOrDefault(player.level().getServer(), Map.of()).get(id); return body != null && string(body.data, "op", "").equals("chase");
    }
    public static double catchDistance(ServerPlayer player, String id) {
        var body = SERVERS.getOrDefault(player.level().getServer(), Map.of()).get(id); return body == null ? 1 : number(body.data, "catch_distance", 1);
    }
    public static void simulate(ServerPlayer player, String id, String event, double distance) {
        var body = SERVERS.getOrDefault(player.level().getServer(), Map.of()).get(id); if (body == null) return;
        if (body.entity == null || body.entity.isRemoved()) spawn(player.level().getServer(), body, player);
        if (event.equals("caught") && !chasing(player, id)) return;
        var direction = player.getLookAngle().multiply(1, 0, 1).normalize();
        double offset = event.equals("seen") ? 3 : distance * .5;
        if (event.equals("seen")) {
            Vec3 end = player.getEyePosition().add(direction.scale(offset));
            if (player.level().clip(new ClipContext(player.getEyePosition(), end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty())).getType() != HitResult.Type.MISS) offset = .75;
        }
        Vec3 target = player.position().add(direction.scale(offset));
        if (body.entity.level() != player.level()) {
            discard(body); body.data.addProperty("dimension", StoryRuntime.dimensionName(player.level()));
            body.data.addProperty("x", target.x); body.data.addProperty("y", target.y); body.data.addProperty("z", target.z);
            spawn(player.level().getServer(), body, player);
        } else move(body, target);
        persist(player.level().getServer(), body);
    }
}
