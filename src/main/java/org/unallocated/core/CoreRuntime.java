package org.unallocated.core;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.unallocated.UnallocatedMod;
import org.unallocated.core.network.CoreNetworking;
import org.unallocated.debug.UnallocatedCommand;

/** Integration entry point: register content modules during mod initialization, then use get(server).
 * World setup/game rules/teleports belong to MapBuilder and each module, never to ordinary worlds.
 */
public final class CoreRuntime {
    private static final Map<Act, ActModule> MODULES = new EnumMap<>(Act.class);
    private static final Map<MinecraftServer, CoreRuntime> SERVERS = new IdentityHashMap<>();
    private final ServerEventBus events;
    private final ActSequencer sequencer;
    private final Features features;
    private ServerPlayer participant;

    private CoreRuntime(MinecraftServer server) {
        features = Features.load();
        events = new ServerEventBus(e -> UnallocatedMod.LOGGER.error("UNALLOCATED event listener failed", e));
        Map<Act, ActModule> modules = new EnumMap<>(Act.class);
        for (Act act : Act.values()) modules.put(act, new EmptyAct(act));
        modules.putAll(MODULES);
        sequencer = new ActSequencer(SessionState.get(server), features, modules.values(), events, UnallocatedMod.LOGGER::info);
        events.subscribe(ServerEvents.ActChanged.class, event -> CoreNetworking.sync(server, state()));
    }
    public static void initialize() {
        CoreNetworking.initialize();
        UnallocatedCommand.register();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> SERVERS.put(server, new CoreRuntime(server)));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { SERVERS.remove(server); CoreNetworking.clear(); });
        ServerTickEvents.END_SERVER_TICK.register(server -> get(server).tick(server));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            CoreNetworking.forget(handler.getPlayer());
            CoreRuntime runtime = SERVERS.get(server);
            if (runtime != null && runtime.participant == handler.getPlayer()) runtime.participant = null;
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> CoreNetworking.sync(handler.getPlayer(), get(server).state()));
    }
    public static void register(ActModule module) {
        if (!SERVERS.isEmpty()) throw new IllegalStateException("Register act modules before a server starts");
        if (MODULES.putIfAbsent(module.act(), module) != null) throw new IllegalArgumentException("Duplicate act " + module.act());
    }
    public static CoreRuntime get(MinecraftServer server) {
        return java.util.Objects.requireNonNull(SERVERS.get(server), "UNALLOCATED server is not started");
    }
    public ActSequencer sequencer() { return sequencer; }
    public SessionState state() { return sequencer.state(); }
    public ServerEventBus events() { return events; }
    public Features features() { return features; }
    public void start(ServerPlayer player) { participant = player; sequencer.start(player); }
    private void tick(MinecraftServer server) {
        var players = server.getPlayerList().getPlayers();
        if (players.isEmpty() || !state().started() || state().finished()) return;
        // One shared session, one participant drives callbacks. A respawn creates a new instance.
        ServerPlayer player = players.getFirst();
        if (player != participant) { participant = player; sequencer.resume(player); }
        sequencer.tick(player);
        CoreNetworking.syncKnowledge(server, state());
    }

    /** Content-free fallback keeps every debug target available. Future packages replace these.
     * Budgets: contract 60s, lobby 300s, theft 90s, loop 120s, pocket 180s, climax 120s,
     * epilogue 30s (full path 15min). Optional acts are off until integrated. */
    private record EmptyAct(Act act) implements ActModule {
        public boolean enabled(Features f) { return f.actEnabled(act); }
        public void enter(ServerPlayer p, SessionState s) {}
        public void tick(ServerPlayer p, SessionState s) {}
        public boolean complete(ServerPlayer p, SessionState s) { return false; }
        public int budgetTicks() { return switch (act) {
            case CONTRACT -> 1200; case LOBBY -> 6000; case THEFT -> 1800;
            case LOOP -> 2400; case POCKET -> 3600; case CLIMAX -> 2400; case EPILOGUE -> 600;
        }; }
        public void forceAdvance(ServerPlayer p, SessionState s) {}
        public String checkpointId() { return switch (act) {
            case CONTRACT, LOBBY -> "lobby.spawn"; case THEFT -> "base.theft_zone";
            case LOOP -> "mine.entrance"; case POCKET -> "pocket.spawn";
            case CLIMAX -> "core.spawn"; case EPILOGUE -> "lobby.spawn";
        }; }
    }
}
