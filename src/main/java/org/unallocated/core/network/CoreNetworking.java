package org.unallocated.core.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.unallocated.core.SessionState;
import static org.unallocated.core.network.CorePayloads.*;

public final class CoreNetworking {
    private static final Map<UUID, ClientSignalsPayload> SIGNALS = new HashMap<>();
    private static final Map<UUID, Set<String>> KNOWLEDGE = new HashMap<>();
    private CoreNetworking() {}
    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(ActPayload.TYPE, ActPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(KnowledgePayload.TYPE, KnowledgePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EffectPayload.TYPE, EffectPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ClientSignalsPayload.TYPE, ClientSignalsPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ClientSignalsPayload.TYPE, (payload, context) -> {
            // Telemetry only: clients cannot change the authoritative act or clock.
            SIGNALS.put(context.player().getUUID(), payload);
        });
    }
    public static ClientSignalsPayload signals(ServerPlayer player) {
        return SIGNALS.getOrDefault(player.getUUID(), new ClientSignalsPayload(false, false, true));
    }
    public static void forget(ServerPlayer player) {
        SIGNALS.remove(player.getUUID()); KNOWLEDGE.remove(player.getUUID());
    }
    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.type())) ServerPlayNetworking.send(player, payload);
    }
    public static void sync(ServerPlayer player, SessionState state) {
        send(player, new ActPayload(state.act()));
        send(player, new KnowledgePayload(state.flags()));
        KNOWLEDGE.put(player.getUUID(), Set.copyOf(state.flags()));
    }
    public static void sync(MinecraftServer server, SessionState state) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sync(player, state);
    }
    public static void syncKnowledge(MinecraftServer server, SessionState state) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!state.flags().equals(KNOWLEDGE.get(player.getUUID()))) {
                send(player, new KnowledgePayload(state.flags()));
                KNOWLEDGE.put(player.getUUID(), Set.copyOf(state.flags()));
            }
        }
    }
    public static void clear() { SIGNALS.clear(); KNOWLEDGE.clear(); }
}
