package org.unallocated.story;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

public final class StoryActions {
    @FunctionalInterface public interface ActionParser {
        Action parse(JsonObject json, StoryValidator.Context context);
    }
    @FunctionalInterface public interface Action {
        String execute(StoryRuntime runtime, ServerPlayer player);
    }
    private static final Map<String, ActionParser> PARSERS = new LinkedHashMap<>();
    private StoryActions() {}
    public static void register(String name, ActionParser parser) {
        if (PARSERS.putIfAbsent(name, java.util.Objects.requireNonNull(parser)) != null)
            throw new IllegalArgumentException("Duplicate story action " + name);
    }
    public static ActionParser parser(String name) { StoryBuiltins.initialize(); return PARSERS.get(name); }
}
