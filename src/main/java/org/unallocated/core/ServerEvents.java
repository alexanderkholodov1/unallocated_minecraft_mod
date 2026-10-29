package org.unallocated.core;

import net.minecraft.server.level.ServerPlayer;

/** Event names and fields from PLAN §4.3. Bus instance supplies the world/session context. */
public final class ServerEvents {
    private ServerEvents() {}
    public record ActChanged(Act from, Act to) {}
    public record TriggerEntered(ServerPlayer player, String triggerId) {}
    public record LoreRead(ServerPlayer player, String loreId) {}
    public record ScareFired(String primitiveId, int intensity) {}
    public record FearSampled(double f, double confidence) {}
    public record LoopLap(int n) {}
    public record PlayerTaken(ServerPlayer player) {}
    public record PlayerReturned(ServerPlayer player) {}
    public record ThreadCut(String id, boolean silent) {}
    public record EndingReached(String id) {}
}
