package org.unallocated.story;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

public final class StoryTriggers {
    @FunctionalInterface public interface TriggerParser {
        Trigger parse(JsonObject json, StoryValidator.Context context);
    }
    public interface Trigger {
        boolean test(ServerPlayer player);
        void simulate(ServerPlayer player);
        default boolean automatic() { return false; }
        default void reset() {}
    }
    private static final Map<String, TriggerParser> PARSERS = new LinkedHashMap<>();
    private StoryTriggers() {}
    public static void register(String name, TriggerParser parser) {
        if (PARSERS.putIfAbsent(name, java.util.Objects.requireNonNull(parser)) != null)
            throw new IllegalArgumentException("Duplicate story trigger " + name);
    }
    public static TriggerParser parser(String name) { StoryBuiltins.initialize(); return PARSERS.get(name); }
}
