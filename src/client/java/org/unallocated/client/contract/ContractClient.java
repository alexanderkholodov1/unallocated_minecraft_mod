package org.unallocated.client.contract;

import java.io.IOException;
import java.time.Clock;
import java.util.Optional;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.unallocated.UnallocatedMod;
import org.unallocated.contract.ContractReceipt;
import org.unallocated.contract.ContractSession;
import org.unallocated.contract.ContractStore;
import org.unallocated.contract.ContractSubmission;

public final class ContractClient {
    private static ContractSession session;
    private static boolean acceptedWithoutStore;

    private ContractClient() { }

    /** Called before Gui initializes or displays a requested title screen. */
    public static Screen beforeScreen(Screen requested) {
        if (!(requested instanceof TitleScreen) || acceptedWithoutStore) return requested;
        if (session == null) {
            try {
                session = new ContractSession(new ContractStore(FabricLoader.getInstance().getGameDir()),
                        Clock.systemUTC(), Boolean.getBoolean("unallocated.contract.replay"));
            } catch (IOException | SecurityException exception) {
                UnallocatedMod.LOGGER.warn("UNALLOCATED contract: could not read local receipt.", exception);
                // Still offer the agreement; accept() below permits this session to continue.
            }
        }
        if (session != null && !session.shouldShow()) return requested;
        UnallocatedMod.LOGGER.info("UNALLOCATED contract: showing before title screen.");
        return new ContractScreen(requested);
    }

    public static Optional<ContractReceipt> receipt() {
        return session == null ? Optional.empty() : session.receipt();
    }

    /** Click, text and future voice adapters all end here. Never log the submitted text. */
    static void accept(ContractSubmission submission, Screen next) {
        acceptedWithoutStore = true;
        try {
            if (session != null) session.accept(submission);
        } catch (IOException | SecurityException exception) {
            UnallocatedMod.LOGGER.warn("UNALLOCATED contract: acceptance kept for this session; "
                    + "could not save unallocated/contract.properties. It will appear again on restart.", exception);
        }
        Minecraft.getInstance().gui.setScreen(next);
    }
}
