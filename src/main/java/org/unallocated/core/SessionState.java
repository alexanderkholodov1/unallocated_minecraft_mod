package org.unallocated.core;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** One single-player-first session per overworld. Mutators always mark SavedData dirty.
 * Timestamps use the persisted active-tick clock, not process ticks or wall time: no time elapses
 * while the server is empty/stopped or the integrated server is paused. profile is a numeric
 * feature map until the telemetry package defines a richer profile. No client options are stored.
 */
public final class SessionState extends SavedData {
    public static final String ID = "unallocated_session";
    public static final Codec<SessionState> CODEC = CompoundTag.CODEC.xmap(SessionState::fromTag, SessionState::toTag);
    // Command-storage DFU has no rules for our custom fields; own schema version is checked below.
    // A non-null type avoids requiring Fabric's extra object-builder SavedDataStorage mixin.
    public static final SavedDataType<SessionState> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("unallocated", ID), () -> new SessionState(0), CODEC,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private Act act = Act.CONTRACT;
    private long actStartTick, sessionStartTick, ticks, seed;
    private boolean started, finished;
    private final Set<String> flags = new HashSet<>();
    private final Map<String, Integer> counters = new HashMap<>();
    private final Map<String, Double> profile = new HashMap<>();
    private double temperature = 1.0;

    public SessionState(long seed) { this.seed = seed; }
    public static SessionState get(MinecraftServer server) {
        var storage = server.overworld().getDataStorage();
        SessionState state = storage.get(TYPE);
        if (state == null) {
            state = new SessionState(server.overworld().getSeed());
            storage.set(TYPE, state);
            state.setDirty();
        }
        return state;
    }
    public Act act() { return act; }
    public long actStartTick() { return actStartTick; }
    public long sessionStartTick() { return sessionStartTick; }
    public long ticks() { return ticks; }
    public long seed() { return seed; }
    public long elapsedActTicks() { return ticks - actStartTick; }
    public long elapsedSessionTicks() { return ticks - sessionStartTick; }
    public boolean started() { return started; }
    public boolean finished() { return finished; }
    public Set<String> flags() { return Collections.unmodifiableSet(flags); }
    public Map<String, Integer> counters() { return Collections.unmodifiableMap(counters); }
    public Map<String, Double> profile() { return Collections.unmodifiableMap(profile); }
    public double temperature() { return temperature; }
    public boolean addFlag(String flag) {
        boolean added = flags.add(flag);
        if (added) setDirty();
        return added;
    }
    public boolean removeFlag(String flag) {
        boolean removed = flags.remove(flag);
        if (removed) setDirty();
        return removed;
    }
    public void setCounter(String id, int value) { counters.put(id, value); setDirty(); }
    public void setProfile(String id, double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite profile value");
        profile.put(id, value); setDirty();
    }
    public void setTemperature(double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid temperature");
        temperature = value; setDirty();
    }
    void tickClock() { if (started && !finished) { ticks++; setDirty(); } }
    void moveTo(Act next) {
        if (!started) { started = true; sessionStartTick = ticks; }
        act = next; actStartTick = ticks; finished = false; setDirty();
    }
    void finish() { finished = true; setDirty(); }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema", 1);
        tag.putString("act", act.name());
        tag.putLong("actStartTick", actStartTick);
        tag.putLong("sessionStartTick", sessionStartTick);
        tag.putLong("ticks", ticks);
        tag.putLong("seed", seed);
        tag.putBoolean("started", started);
        tag.putBoolean("finished", finished);
        tag.putDouble("temperature", temperature);
        CompoundTag savedFlags = new CompoundTag();
        flags.forEach(flag -> savedFlags.putBoolean(flag, true));
        tag.put("flags", savedFlags);
        CompoundTag savedCounters = new CompoundTag();
        counters.forEach(savedCounters::putInt);
        tag.put("counters", savedCounters);
        CompoundTag savedProfile = new CompoundTag();
        profile.forEach(savedProfile::putDouble);
        tag.put("profile", savedProfile);
        return tag;
    }

    public static SessionState fromTag(CompoundTag tag) {
        if (tag.getIntOr("schema", 1) != 1) throw new IllegalArgumentException("Unsupported session schema");
        SessionState state = new SessionState(tag.getLongOr("seed", 0));
        state.act = Act.valueOf(tag.getStringOr("act", "CONTRACT"));
        state.ticks = Math.max(0, tag.getLongOr("ticks", 0));
        state.sessionStartTick = Math.clamp(tag.getLongOr("sessionStartTick", 0), 0, state.ticks);
        state.actStartTick = Math.clamp(tag.getLongOr("actStartTick", 0), state.sessionStartTick, state.ticks);
        state.started = tag.getBooleanOr("started", false);
        state.finished = tag.getBooleanOr("finished", false);
        state.setTemperature(tag.getDoubleOr("temperature", 1.0));
        CompoundTag savedFlags = tag.getCompoundOrEmpty("flags");
        for (String key : savedFlags.keySet()) if (savedFlags.getBooleanOr(key, false)) state.flags.add(key);
        CompoundTag savedCounters = tag.getCompoundOrEmpty("counters");
        for (String key : savedCounters.keySet()) state.counters.put(key, savedCounters.getIntOr(key, 0));
        CompoundTag savedProfile = tag.getCompoundOrEmpty("profile");
        for (String key : savedProfile.keySet()) state.setProfile(key, savedProfile.getDoubleOr(key, 0));
        state.setDirty(false);
        return state;
    }
}
