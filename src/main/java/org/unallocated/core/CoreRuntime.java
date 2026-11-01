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
import org.unallocated.debug.BuildArming;

/** Integration entry point: register content modules during mod initialization, then use get(server).
 * World setup/game rules/teleports belong to MapBuilder and each module, never to ordinary worlds.
 */
public final class CoreRuntime {
    private static final Map<Act, ActModule> MODULES = new EnumMap<>(Act.class);
    private static final Map<MinecraftServer, CoreRuntime> SERVERS = new IdentityHashMap<>();
    private final MinecraftServer server;
    private final ServerEventBus events;
    private final ActSequencer sequencer;
    private final Features features;
    private ServerPlayer participant;

    private CoreRuntime(MinecraftServer server) {
        this.server = server;
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
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            SERVERS.put(server, new CoreRuntime(server));
            // The map command may have registered after the core's initialization callback.
            BuildArming.install(server.getCommands().getDispatcher(), source -> arm(source.getServer()));
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success && SERVERS.containsKey(server)) {
                BuildArming.install(server.getCommands().getDispatcher(), source -> arm(source.getServer()));
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { SERVERS.remove(server); CoreNetworking.clear(); });
        ServerTickEvents.END_SERVER_TICK.register(server -> get(server).tick(server));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            CoreNetworking.forget(handler.getPlayer());
            CoreRuntime runtime = SERVERS.get(server);
            if (runtime != null && runtime.participant == handler.getPlayer()) runtime.participant = null;
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> get(server).joined(handler.getPlayer()));
    }
    public static void register(ActModule module) {
        if (!SERVERS.isEmpty()) throw new IllegalStateException("Register act modules before a server starts");
        Act act;
        try { act = java.util.Objects.requireNonNull(module.act()); }
        catch (RuntimeException e) {
            UnallocatedMod.LOGGER.error("UNALLOCATED invalid act module; keeping fallback", e);
            return;
        }
        if (MODULES.putIfAbsent(act, module) != null) throw new IllegalArgumentException("Duplicate act " + act);
    }
    public static CoreRuntime get(MinecraftServer server) {
        requireServerThread(server);
        return java.util.Objects.requireNonNull(SERVERS.get(server), "UNALLOCATED server is not started");
    }
    public ActSequencer sequencer() { return sequencer; }
    public SessionState state() { return sequencer.state(); }
    public ServerEventBus events() { return events; }
    public Features features() { return features; }
    /** Public authoring hook for template builders; build-all commands are decorated automatically. */
    public static void arm(MinecraftServer server) {
        requireServerThread(server);
        SessionState.get(server).arm();
        UnallocatedMod.LOGGER.info("UNALLOCATED session armed");
    }
    private static void requireServerThread(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("UNALLOCATED state requires the server thread");
    }
    /** Kept for existing callers, but never arms a world or permits client-thread state access. */
    public void start(ServerPlayer player) {
        requireServerThread(server);
        if (!state().armed()) return;
        participant = player;
        sequencer.start(player);
    }
    private void joined(ServerPlayer player) {
        if (!state().armed()) return;
        if (participant == null && !state().finished()) {
            participant = player;
            if (state().started()) sequencer.resume(player);
            else sequencer.start(player);
        }
        CoreNetworking.sync(player, state());
    }
    private void tick(MinecraftServer server) {
        var players = server.getPlayerList().getPlayers();
        if (players.isEmpty() || !state().active()) return;
        // One shared session, one participant drives callbacks. A respawn creates a new instance.
        ServerPlayer player = players.getFirst();
        if (player != participant) { participant = player; sequencer.resume(player); }
        sequencer.tick(player);
        CoreNetworking.syncKnowledge(server, state());
    }

    /** Content-free fallback keeps every debug target available. Future packages replace these.
     * CONTRACT is client-only and never entered here. Server budgets: lobby 300s, theft 90s,
     * loop 120s, pocket 180s, climax 120s, epilogue 30s. Optional acts stay off until integrated. */
    private record EmptyAct(Act act) implements ActModule {
        public boolean enabled(Features f) { return f.actEnabled(act); }
        public void enter(ServerPlayer p, SessionState s) {}
        public void tick(ServerPlayer p, SessionState s) {}
        public boolean complete(ServerPlayer p, SessionState s) { return act == Act.CONTRACT; }
        public int budgetTicks() { return switch (act) {
            case CONTRACT -> 1; case LOBBY -> 6000; case THEFT -> 1800;
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
