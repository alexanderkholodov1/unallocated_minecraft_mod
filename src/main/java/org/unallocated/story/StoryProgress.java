package org.unallocated.story;

import com.google.gson.*;
import java.util.List;
import java.util.function.DoubleToIntFunction;
import org.unallocated.core.SessionState;
import static org.unallocated.story.StoryData.*;

/** Pure eligibility rules shared by natural triggers, autoplay and debug fire. */
public final class StoryProgress {
    private StoryProgress() {}
    public static boolean eligible(SessionState state, JsonObject beat, DoubleToIntFunction ticks) {
        String id = string(beat, "id", ""); int count = state.counters().getOrDefault("story.repeat." + id, 0);
        if (count >= number(object(beat, "repeat"), "max", 1)) return false;
        if (count > 0 && state.elapsedSessionTicks() - state.counters().getOrDefault("story.fired_at." + id, 0) < ticks.applyAsInt(number(object(beat, "repeat"), "cooldown_seconds", 0))) return false;
        JsonObject guards = object(beat, "if");
        for (String key : List.of("flags", "not_flags", "beats", "not_beats")) for (JsonElement e : array(guards, key)) {
            String prefix = key.endsWith("beats") ? "story.fired." : "story.flag.";
            if (state.flags().contains(prefix + e.getAsString()) == key.startsWith("not_")) return false;
        }
        return true;
    }
}
