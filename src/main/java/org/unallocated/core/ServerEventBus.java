package org.unallocated.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Per-server, synchronous typed bus. Register/post on the server thread; close subscriptions
 * to release listeners. A failed content listener must not strand the session. */
public final class ServerEventBus {
    private final Map<Class<?>, List<Consumer<?>>> listeners = new HashMap<>();
    private final Consumer<RuntimeException> errors;
    public ServerEventBus(Consumer<RuntimeException> errors) { this.errors = errors; }
    public <T> AutoCloseable subscribe(Class<T> type, Consumer<T> listener) {
        listeners.computeIfAbsent(type, ignored -> new ArrayList<>()).add(listener);
        return () -> listeners.getOrDefault(type, List.of()).remove(listener);
    }
    @SuppressWarnings("unchecked")
    public <T> void post(T event) {
        for (Consumer<?> listener : List.copyOf(listeners.getOrDefault(event.getClass(), List.of()))) {
            try { ((Consumer<T>) listener).accept(event); }
            catch (RuntimeException e) { errors.accept(e); }
        }
    }
}
