package org.unallocated.core.network;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.unallocated.core.Act;

/** Stable PLAN §4.3 wire contracts. Collections and NBT are copied at the boundary. */
public final class CorePayloads {
    private CorePayloads() {}
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String id) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("unallocated", id));
    }
    public record ActPayload(Act act) implements CustomPacketPayload {
        public static final Type<ActPayload> TYPE = CorePayloads.type("act");
        public static final StreamCodec<RegistryFriendlyByteBuf, ActPayload> CODEC = StreamCodec.of(
            (buf, value) -> buf.writeEnum(value.act), buf -> new ActPayload(buf.readEnum(Act.class)));
        public Type<ActPayload> type() { return TYPE; }
    }
    public record KnowledgePayload(Set<String> flags) implements CustomPacketPayload {
        public KnowledgePayload { flags = Set.copyOf(flags); }
        public static final Type<KnowledgePayload> TYPE = CorePayloads.type("knowledge");
        public static final StreamCodec<RegistryFriendlyByteBuf, KnowledgePayload> CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeVarInt(value.flags.size());
                value.flags.stream().sorted().forEach(flag -> buf.writeUtf(flag, 256));
            }, buf -> {
                int size = buf.readVarInt();
                if (size < 0 || size > 1024) throw new IllegalArgumentException("Invalid flag count");
                Set<String> flags = new HashSet<>();
                for (int i = 0; i < size; i++) flags.add(buf.readUtf(256));
                return new KnowledgePayload(flags);
            });
        public Type<KnowledgePayload> type() { return TYPE; }
    }
    public record EffectPayload(String effectId, CompoundTag args, int durationTicks) implements CustomPacketPayload {
        public EffectPayload {
            java.util.Objects.requireNonNull(effectId);
            args = args.copy();
            if (durationTicks < 0) throw new IllegalArgumentException("Negative effect duration");
        }
        @Override public CompoundTag args() { return args.copy(); }
        public static final Type<EffectPayload> TYPE = CorePayloads.type("effect");
        public static final StreamCodec<RegistryFriendlyByteBuf, EffectPayload> CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeUtf(value.effectId, 256); buf.writeNbt(value.args); buf.writeVarInt(value.durationTicks);
            }, buf -> new EffectPayload(buf.readUtf(256), java.util.Objects.requireNonNull(buf.readNbt()), buf.readVarInt()));
        public Type<EffectPayload> type() { return TYPE; }
    }
    public record ClientSignalsPayload(boolean debugOpen, boolean paused, boolean focused) implements CustomPacketPayload {
        public static final Type<ClientSignalsPayload> TYPE = CorePayloads.type("client_signals");
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientSignalsPayload> CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeBoolean(value.debugOpen); buf.writeBoolean(value.paused); buf.writeBoolean(value.focused); },
            buf -> new ClientSignalsPayload(buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));
        public Type<ClientSignalsPayload> type() { return TYPE; }
    }
}
