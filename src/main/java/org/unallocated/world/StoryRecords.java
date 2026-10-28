package org.unallocated.world;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import org.unallocated.core.SessionState;

/** Large internal JSON belongs in server-side counters, never in the 256-character knowledge flags. */
public final class StoryRecords {
    private StoryRecords() {}
    public static void write(SessionState state, String key, JsonObject data) {
        byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8);
        for (int start = 0; start < bytes.length; start += 4) {
            int word = 0;
            for (int i = 0; i < 4 && start + i < bytes.length; i++) word |= (bytes[start + i] & 255) << (i * 8);
            state.setCounter(key + ".data." + start / 4, word);
        }
        state.setCounter(key + ".length", bytes.length);
        state.addFlag(key);
    }
    public static JsonObject read(SessionState state, String key) {
        int length = state.counters().getOrDefault(key + ".length", 0);
        if (length <= 0) throw new IllegalStateException("Missing story record " + key);
        byte[] bytes = new byte[length];
        for (int start = 0; start < length; start += 4) {
            int word = state.counters().getOrDefault(key + ".data." + start / 4, 0);
            for (int i = 0; i < 4 && start + i < length; i++) bytes[start + i] = (byte) (word >>> (i * 8));
        }
        return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
    }
    public static void remove(SessionState state, String key) { state.removeFlag(key); state.setCounter(key + ".length", 0); }
}
