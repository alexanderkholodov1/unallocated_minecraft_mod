package org.unallocated.smoke;

import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import org.slf4j.LoggerFactory;

/** Capture the actual blocking screen before Fabric turns a failed wait into client shutdown. */
final class LoadingFailure {
    private LoadingFailure() {}
    static <T> T capture(ClientGameTestContext context, String name, Supplier<T> action) {
        long start = System.nanoTime();
        try {
            T result = action.get();
            LoggerFactory.getLogger("unallocated-smoke").info("UNALLOCATED smoke: load={} elapsed_ms={}", name, (System.nanoTime() - start) / 1_000_000);
            return result;
        } catch (RuntimeException | AssertionError failure) {
            try {
                context.runOnClient(client -> {
                    var screen = client.gui.screen();
                    LoggerFactory.getLogger("unallocated-smoke").error("UNALLOCATED smoke: load={} failed screen={} title={} player={} level={} server={}", name,
                        screen == null ? "null" : screen.getClass().getName(), screen == null ? "" : screen.getTitle().getString(),
                        client.player != null, client.level != null, client.getSingleplayerServer() != null);
                });
                context.takeScreenshot("unallocated-load-failed-" + name);
            } catch (Throwable captureFailure) { failure.addSuppressed(captureFailure); }
            throw failure;
        }
    }
}
