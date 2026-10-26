package org.unallocated.interfacefx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class InterfaceOptionsJournalTest {
    @TempDir Path directory;

    @Test void crashThenNewInstanceRestoresExactBytes() throws Exception {
        byte[] original = "# opciones ajenas\r\nsensitivity:0.743\r\nfov:93\nunknown:ñ\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(directory.resolve("options.txt"), original);
        try (var beforeCrash = new InterfaceOptionsJournal(directory)) {
            beforeCrash.begin();
            Files.writeString(directory.resolve("options.txt"), "sensitivity:0.1\nfov:60\ngamma:0.1");
            beforeCrash.begin(); // Repeated effects must not overwrite the first snapshot.
        }
        try (var restart = new InterfaceOptionsJournal(directory)) {
            assertTrue(restart.pending());
            restart.restore();
            assertArrayEquals(original, Files.readAllBytes(directory.resolve("options.txt")));
            assertTrue(restart.pending(), "Recovery remains replayable until clean finish");
            restart.finish();
            assertFalse(restart.pending());
        }
    }

    @Test void killedDuringRestoreCanReplayAndVanillaShutdownSaveCannotWin() throws Exception {
        byte[] original = {0, 13, 10, (byte) 255};
        Files.write(directory.resolve("options.txt"), original);
        try (var journal = new InterfaceOptionsJournal(directory)) {
            journal.begin();
            Files.writeString(directory.resolve("options.txt"), "mutated");
            journal.restore();
            Files.writeString(directory.resolve("options.txt"), "late vanilla save");
        }
        try (var restart = new InterfaceOptionsJournal(directory)) {
            restart.finish();
            assertArrayEquals(original, Files.readAllBytes(directory.resolve("options.txt")));
            restart.finish();
        }
    }

    @Test void missingOriginalIsRestoredAsMissing() throws Exception {
        try (var journal = new InterfaceOptionsJournal(directory)) {
            journal.begin();
            Files.writeString(directory.resolve("options.txt"), "created by save");
        }
        try (var restart = new InterfaceOptionsJournal(directory)) {
            restart.finish();
            assertFalse(Files.exists(directory.resolve("options.txt")));
        }
    }

    @Test void damagedJournalIsRetainedAndNeverRestoredOrReplaced() throws Exception {
        Files.writeString(directory.resolve("options.txt"), "original");
        Path file = directory.resolve(".unallocated-interface-options.journal");
        try (var journal = new InterfaceOptionsJournal(directory)) {
            journal.begin();
            byte[] bytes = Files.readAllBytes(file);
            bytes[10] ^= 1;
            Files.write(file, bytes);
            Files.writeString(directory.resolve("options.txt"), "current");
            assertThrows(IOException.class, journal::restore);
            assertThrows(IOException.class, journal::begin);
            assertThrows(IOException.class, journal::finish);
            assertArrayEquals(bytes, Files.readAllBytes(file));
            assertEquals("current", Files.readString(directory.resolve("options.txt")));
        }
    }

    @Test void secondClientCannotOwnOrChangeJournal() throws Exception {
        try (var first = new InterfaceOptionsJournal(directory)) {
            assertThrows(IOException.class, () -> new InterfaceOptionsJournal(directory));
            first.begin();
            first.finish();
        }
    }

    @Test void abandonedStagingFileIsNotAPublishedJournal() throws Exception {
        Files.writeString(directory.resolve("options.txt"), "original");
        Files.writeString(directory.resolve(".unallocated-interface-options.journal.staging"), "torn");
        try (var journal = new InterfaceOptionsJournal(directory)) {
            assertFalse(journal.pending());
            journal.begin();
            Files.writeString(directory.resolve("options.txt"), "mutated");
            journal.finish();
            assertEquals("original", Files.readString(directory.resolve("options.txt")));
        }
    }
}
