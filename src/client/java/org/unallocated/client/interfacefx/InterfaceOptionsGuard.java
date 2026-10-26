package org.unallocated.client.interfacefx;

import java.io.File;
import java.io.IOException;
import net.minecraft.client.Options;
import org.unallocated.UnallocatedMod;
import org.unallocated.interfacefx.InterfaceOptionsJournal;

/** Lives before Options.load, so a killed process is recovered before vanilla reads anything. */
public final class InterfaceOptionsGuard {
    private static InterfaceOptionsJournal journal;
    private static boolean initialized;
    private static boolean available;
    private static boolean saveBlocked;
    private static Options owner;
    private static double sensitivity;
    private static double gamma;
    private static int fov;

    private InterfaceOptionsGuard() { }

    public static synchronized void beforeLoad(File optionsFile) {
        if (initialized) return;
        initialized = true;
        try {
            journal = new InterfaceOptionsJournal(optionsFile.toPath().toAbsolutePath().getParent());
            if (journal.pending()) {
                saveBlocked = true;
                journal.restore(); // Keep protection against startup's own Options.save calls.
                UnallocatedMod.LOGGER.info("UNALLOCATED interface: recovered original options before load.");
            }
            available = true;
            Runtime.getRuntime().addShutdownHook(new Thread(InterfaceOptionsGuard::shutdown,
                    "unallocated-interface-options-restore"));
        } catch (IOException failure) {
            // No settings effect may run without a durable original. Do not destroy recovery evidence.
            saveBlocked = true;
            UnallocatedMod.LOGGER.error("UNALLOCATED interface: settings effects disabled; options journal unavailable.", failure);
        }
    }

    public static synchronized boolean begin(Options options) {
        if (!available) return false;
        try {
            journal.begin(); // Durable and atomic before ANY OptionInstance.set.
            saveBlocked = true;
            if (owner == null) {
                sensitivity = options.sensitivity().get();
                fov = options.fov().get();
                gamma = options.gamma().get();
                owner = options;
            }
            return true;
        } catch (IOException failure) {
            available = false;
            UnallocatedMod.LOGGER.error("UNALLOCATED interface: could not journal options; drift refused.", failure);
            return false;
        }
    }

    public static synchronized boolean blocksSave() { return saveBlocked; }

    public static synchronized void restoreMemoryAndFile() {
        if (owner != null) {
            owner.sensitivity().set(sensitivity);
            owner.fov().set(fov);
            owner.gamma().set(gamma);
            owner = null;
        }
        restoreFile();
    }

    private static void restoreFile() {
        if (journal == null) return;
        try {
            journal.restore();
        } catch (IOException failure) {
            available = false;
            UnallocatedMod.LOGGER.error("UNALLOCATED interface: restore deferred to next start; journal retained.", failure);
        }
    }

    /** Explicitly opening settings ends drift before the user can edit their own preferences. */
    public static synchronized void beforeUserOptions() {
        restoreMemoryAndFile();
        if (!available || journal == null) return;
        try {
            journal.finish();
            saveBlocked = false;
        } catch (IOException failure) {
            available = false;
            UnallocatedMod.LOGGER.error("UNALLOCATED interface: could not retire options journal.", failure);
        }
    }

    /** Called after vanilla closes, and again by the JVM hook if close was interrupted. */
    public static synchronized void shutdown() {
        // The hook touches only disk: OptionInstance callbacks belong to the render thread.
        if (journal == null) return;
        try {
            journal.finish();
        } catch (IOException failure) {
            UnallocatedMod.LOGGER.error("UNALLOCATED interface: journal retained for restart recovery.", failure);
        }
        // Keep lock until process exit: a shutdown hook may still need to replay recovery.
    }
}
