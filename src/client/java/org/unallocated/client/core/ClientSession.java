package org.unallocated.client.core;

import java.util.Set;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.unallocated.core.Act;
import org.unallocated.core.Features;
import static org.unallocated.core.network.CorePayloads.*;

/** Client-side mirror only. Effects package connects its ClientEffects.dispatch through onEffect.
 * No rendering or option mutations live in core. All receivers run on the client thread (Fabric).
 */
public final class ClientSession {
    private static Act act = Act.CONTRACT;
    private static Set<String> flags = Set.of();
    private static ClientSignalsPayload lastSignals;
    private static Consumer<EffectPayload> effects = ignored -> {};
    private static Features features;
    private ClientSession() {}
    public static Act act() { return act; }
    public static Set<String> flags() { return flags; }
    public static void onEffect(Consumer<EffectPayload> handler) { effects = java.util.Objects.requireNonNull(handler); }
    public static void initialize() {
        features = Features.load();
        ClientPlayNetworking.registerGlobalReceiver(ActPayload.TYPE, (payload, context) -> act = payload.act());
        ClientPlayNetworking.registerGlobalReceiver(KnowledgePayload.TYPE, (payload, context) -> flags = payload.flags());
        ClientPlayNetworking.registerGlobalReceiver(EffectPayload.TYPE, (payload, context) -> {
            if (payload.effectId().startsWith("options.") && !features.enabled("effects.options")) return;
            if (payload.effectId().startsWith("screamer.") && !features.enabled("effects.screamer")) return;
            effects.accept(payload);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            act = Act.CONTRACT; flags = Set.of(); lastSignals = null;
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || !ClientPlayNetworking.canSend(ClientSignalsPayload.TYPE)) { lastSignals = null; return; }
            var signals = new ClientSignalsPayload(client.getDebugOverlay().showDebugScreen(), client.isPaused(), client.isWindowActive());
            if (!signals.equals(lastSignals)) { ClientPlayNetworking.send(signals); lastSignals = signals; }
        });
    }
}
