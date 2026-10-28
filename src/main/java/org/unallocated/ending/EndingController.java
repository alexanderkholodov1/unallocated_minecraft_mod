package org.unallocated.ending;

import java.util.List;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.unallocated.UnallocatedMod;
import org.unallocated.core.Act;
import org.unallocated.core.CoreRuntime;
import org.unallocated.core.ServerEvents;

/** Server-thread shutdown shared by the v0 ending and v0.3's future outcome strategies. */
public final class EndingController {
    public record Ending(String id, Component reason) {
        public Ending {
            if (!Objects.requireNonNull(id).matches("[A-Z][A-Z0-9_]{0,31}")) {
                throw new IllegalArgumentException("Invalid ending id");
            }
            Objects.requireNonNull(reason);
        }
    }
    private EndingController() {}

    /** Called from EPILOGUE after any outcome-specific scene/report has completed.
     * v0.3 may pass TERMINATED/CONSUMED and its own message through this same save/close path.
     * A finished session never closes again on reload; debug act jumps explicitly reactivate it.
     */
    public static void close(ServerPlayer player, Ending ending) {
        var server = player.level().getServer();
        var runtime = CoreRuntime.get(server);
        if (runtime.state().finished()) return;
        if (runtime.state().act() != Act.EPILOGUE) {
            throw new IllegalStateException("Close the session from EPILOGUE");
        }
        runtime.state().addFlag("ending." + ending.id());
        runtime.sequencer().advance(player);
        runtime.events().post(new ServerEvents.EndingReached(ending.id()));
        // Persist the terminal checkpoint and player data BEFORE the disconnect can tear down the world.
        // Vanilla's subsequent stop performs the final flush and releases the world lock.
        server.saveEverything(false, true, true);
        UnallocatedMod.LOGGER.info("UNALLOCATED ending={}", ending.id());
        for (var connected : List.copyOf(server.getPlayerList().getPlayers())) {
            connected.connection.disconnect(ending.reason());
        }
        // The integrated server belongs to Minecraft.disconnect(), which saves, stops and joins it.
        // A dedicated server has no client owner to perform that final stop.
        if (server.isDedicatedServer()) server.halt(false);
    }
}
