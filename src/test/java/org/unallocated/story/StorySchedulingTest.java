package org.unallocated.story;

import com.google.gson.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.unallocated.core.SessionState;
import static org.junit.jupiter.api.Assertions.*;

class StorySchedulingTest {
    @Test void fakeTicksKeepAtTiesFileOrderAndResumeRemainingDelay() {
        var state = new SessionState(0); var scheduler = new StoryScheduler(state);
        var steps = List.of(new StoryScheduler.Step(0, 2), new StoryScheduler.Step(2, 0), new StoryScheduler.Step(2, 1), new StoryScheduler.Step(5, 3));
        List<Integer> ran = new ArrayList<>(); scheduler.schedule("first.beat", steps);
        scheduler.tick(id -> steps, (id, index) -> { ran.add(index); return true; }, false);
        assertEquals(List.of(2), ran);
        scheduler.tick(id -> steps, (id, index) -> { ran.add(index); return true; }, true);
        var restored = SessionState.fromTag(state.toTag()); var resumed = new StoryScheduler(restored);
        resumed.tick(id -> steps, (id, index) -> { ran.add(index); return true; }, true);
        assertEquals(List.of(2, 0, 1), ran);
        resumed.tick(id -> steps, (id, index) -> { ran.add(index); return true; }, true);
        resumed.tick(id -> steps, (id, index) -> { ran.add(index); return true; }, true);
        assertEquals(List.of(2, 0, 1), ran);
        resumed.tick(id -> steps, (id, index) -> { ran.add(index); return true; }, true);
        assertEquals(List.of(2, 0, 1, 3), ran);
        assertTrue(restored.flags().isEmpty());
    }
    @Test void nextAndEndCancelEveryOldChapterActionIncludingTies() {
        for (String terminal : List.of("next_chapter", "end")) {
            var state = new SessionState(0); var scheduler = new StoryScheduler(state);
            var steps = List.of(new StoryScheduler.Step(0, 0), new StoryScheduler.Step(0, 1));
            scheduler.schedule("first.a", steps); scheduler.schedule("first.b", steps);
            List<Integer> ran = new ArrayList<>();
            scheduler.tick(id -> steps, (id, index) -> { ran.add(index); scheduler.cancel(null); return false; }, false);
            assertEquals(List.of(0), ran, terminal); assertTrue(state.flags().isEmpty());
        }
    }
    @Test void cancellationOnlyRemovesNamedBeatAndRepeatedSchedulesAreIndependent() {
        var state = new SessionState(0); var scheduler = new StoryScheduler(state);
        var steps = List.of(new StoryScheduler.Step(1, 0));
        scheduler.schedule("first.a", steps); scheduler.schedule("first.a", steps); scheduler.schedule("first.b", steps); scheduler.cancel("first.b");
        List<String> ran = new ArrayList<>();
        scheduler.tick(id -> steps, (id, index) -> { ran.add(id); return true; }, true);
        assertEquals(List.of("first.a", "first.a"), ran);
    }
    @Test void repeatCooldownAndEveryGuardAreSaved() {
        SessionState state = new SessionState(0);
        JsonObject beat = JsonParser.parseString("""
            {"id":"first.a","repeat":{"max":2,"cooldown_seconds":5},
             "if":{"flags":["ready"],"not_flags":["blocked"],"beats":["first.b"],"not_beats":["first.c"]}}
            """).getAsJsonObject();
        assertFalse(StoryProgress.eligible(state, beat, s -> (int) s * 20));
        state.addFlag("story.flag.ready"); state.addFlag("story.fired.first.b");
        assertTrue(StoryProgress.eligible(state, beat, s -> (int) s * 20));
        state.addFlag("story.flag.blocked"); assertFalse(StoryProgress.eligible(state, beat, s -> (int) s * 20)); state.removeFlag("story.flag.blocked");
        state.setCounter("story.repeat.first.a", 1); state.setCounter("story.fired_at.first.a", 0);
        assertFalse(StoryProgress.eligible(state, beat, s -> (int) s * 20));
        state.setCounter("story.fired_at.first.a", -100);
        state = SessionState.fromTag(state.toTag()); assertTrue(StoryProgress.eligible(state, beat, s -> (int) s * 20));
        state.setCounter("story.repeat.first.a", 2); assertFalse(StoryProgress.eligible(state, beat, s -> (int) s * 20));
    }
}
