package org.unallocated.core;

import io.netty.buffer.Unpooled;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.unallocated.core.network.CorePayloads.*;

class CorePayloadsTest {
    private <T> T roundTrip(StreamCodec<RegistryFriendlyByteBuf, T> codec, T value) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            codec.encode(buffer, value);
            T decoded = codec.decode(buffer);
            assertEquals(0, buffer.readableBytes());
            return decoded;
        } finally { buffer.release(); }
    }
    @Test void allFourPayloadsRoundTripWithPlanIdsAndFields() {
        for (Act act : Act.values()) {
            var value = new ActPayload(act);
            assertEquals(value, roundTrip(ActPayload.CODEC, value));
        }
        var knowledge = new KnowledgePayload(Set.of("knows_name", "sees_threads"));
        assertEquals(knowledge, roundTrip(KnowledgePayload.CODEC, knowledge));
        var args = new CompoundTag(); args.putString("text", "Datos registrados."); args.putFloat("scale", 0.7f);
        var effect = new EffectPayload("ui.toast_unearned", args, 80);
        assertEquals(effect, roundTrip(EffectPayload.CODEC, effect));
        var signals = new ClientSignalsPayload(true, false, true);
        assertEquals(signals, roundTrip(ClientSignalsPayload.CODEC, signals));
        assertEquals("unallocated:effect", EffectPayload.TYPE.id().toString());
        assertEquals("unallocated:knowledge", KnowledgePayload.TYPE.id().toString());
        assertEquals("unallocated:act", ActPayload.TYPE.id().toString());
        assertEquals("unallocated:client_signals", ClientSignalsPayload.TYPE.id().toString());
    }
    @Test void knowledgeAndEffectCannotBeMutatedAfterConstruction() {
        var flags = new HashSet<>(Set.of("knows_name"));
        var knowledge = new KnowledgePayload(flags); flags.clear();
        assertEquals(Set.of("knows_name"), knowledge.flags());
        var args = new CompoundTag(); args.putInt("count", 2);
        var effect = new EffectPayload("ui.test", args, 20); args.putInt("count", 8);
        effect.args().putInt("count", 99);
        assertEquals(2, effect.args().getIntOr("count", 0));
    }
    @Test void invalidKnowledgeLengthsAndNegativeEffectDurationsAreRejected() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            buffer.writeVarInt(1025);
            assertThrows(IllegalArgumentException.class, () -> KnowledgePayload.CODEC.decode(buffer));
        } finally { buffer.release(); }
        assertThrows(IllegalArgumentException.class, () -> new EffectPayload("ui.test", new CompoundTag(), -1));
    }
}
