package org.unallocated.story;

import java.util.*;
import java.util.function.BiFunction;
import org.unallocated.core.SessionState;

/** Active-tick scheduler. All durable queue data fits the core's existing flags/counters. */
public final class StoryScheduler {
    public record Step(int at, int index) {}
    private record Pending(int serial, String source) {}
    private final SessionState state;
    public StoryScheduler(SessionState state) { this.state = state; }
    private static String key(int id) { return "story.pending." + id; }
    public void schedule(String source, List<Step> steps) {
        if (steps.isEmpty()) return;
        int serial = state.counters().getOrDefault("story.serial", 0) + 1;
        state.setCounter("story.serial", serial);
        state.addFlag(key(serial) + "." + source);
        state.setCounter(key(serial) + ".index", 0);
        state.setCounter(key(serial) + ".remaining", steps.getFirst().at());
    }
    private List<Pending> pending() {
        List<Pending> result = new ArrayList<>();
        for (String flag : state.flags()) if (flag.startsWith("story.pending.")) {
            String rest = flag.substring("story.pending.".length()); int dot = rest.indexOf('.');
            if (dot > 0) result.add(new Pending(Integer.parseInt(rest.substring(0, dot)), rest.substring(dot + 1)));
        }
        result.sort(Comparator.comparingInt(Pending::serial)); return result;
    }
    public void cancel(String source) {
        for (Pending p : pending()) if (source == null || source.equals(p.source)) remove(p);
    }
    private void remove(Pending p) { state.removeFlag(key(p.serial) + "." + p.source); }
    public void tick(java.util.function.Function<String, List<Step>> lookup, BiFunction<String, Integer, Boolean> execute, boolean advanceClock) {
        for (Pending p : pending()) {
            String flag = key(p.serial) + "." + p.source;
            if (!state.flags().contains(flag)) continue;
            List<Step> steps = lookup.apply(p.source);
            int cursor = state.counters().getOrDefault(key(p.serial) + ".index", 0);
            int remaining = state.counters().getOrDefault(key(p.serial) + ".remaining", 0) - (advanceClock ? 1 : 0);
            while (cursor < steps.size() && remaining <= 0 && state.flags().contains(flag)) {
                Step step = steps.get(cursor++);
                // Save before calling external code; a capture listener may save immediately.
                state.setCounter(key(p.serial) + ".index", cursor);
                int next = cursor < steps.size() ? steps.get(cursor).at() - step.at() : 0;
                state.setCounter(key(p.serial) + ".remaining", remaining + next);
                if (!execute.apply(p.source, step.index())) return;
                remaining += next;
            }
            if (cursor >= steps.size()) remove(p);
            else state.setCounter(key(p.serial) + ".remaining", Math.max(0, remaining));
        }
    }
}
