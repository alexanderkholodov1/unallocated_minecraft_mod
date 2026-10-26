package org.unallocated.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Immutable JAR configuration; missing switches are off, malformed configuration fails at startup. */
public final class Features {
    private final Map<String, Boolean> switches;

    public Features(Map<String, Boolean> switches) { this.switches = Map.copyOf(switches); }
    public boolean enabled(String id) { return switches.getOrDefault(id, false); }
    public boolean actEnabled(Act act) { return enabled("acts." + act.name().toLowerCase(java.util.Locale.ROOT)); }

    public static Features load() {
        var stream = Features.class.getResourceAsStream("/assets/unallocated/features.json");
        if (stream == null) throw new IllegalStateException("Missing UNALLOCATED features.json");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return read(reader);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot read UNALLOCATED features.json", e);
        }
    }

    public static Features read(Reader reader) {
        Map<String, Boolean> result = new HashMap<>();
        flatten("", JsonParser.parseReader(reader).getAsJsonObject(), result);
        return new Features(result);
    }

    private static void flatten(String prefix, JsonObject object, Map<String, Boolean> result) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = prefix + entry.getKey();
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) flatten(key + ".", value.getAsJsonObject(), result);
            else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) result.put(key, value.getAsBoolean());
            else throw new IllegalArgumentException("Feature must be a boolean: " + key);
        }
    }
}
