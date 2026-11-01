package org.unallocated.audio;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Separate from the future general effect payload so sibling packages can merge additively. */
public record AudioPayload(String cue, double x, double y, double z) implements CustomPacketPayload {
    public static final Type<AudioPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("unallocated", "audio"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AudioPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AudioPayload::cue,
            ByteBufCodecs.DOUBLE, AudioPayload::x,
            ByteBufCodecs.DOUBLE, AudioPayload::y,
            ByteBufCodecs.DOUBLE, AudioPayload::z, AudioPayload::new);

    @Override
    public Type<AudioPayload> type() { return TYPE; }
}
