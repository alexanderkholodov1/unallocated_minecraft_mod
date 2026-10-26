package org.unallocated.interfacefx;

import java.nio.file.Files;
import java.nio.file.Path;

/** CI subprocess exits without finally blocks, close(), or JVM shutdown hooks. */
public final class InterfaceJournalCrashFixture {
    public static void main(String[] args) throws Exception {
        Path directory = Path.of(args[0]);
        var journal = new InterfaceOptionsJournal(directory);
        journal.begin();
        Files.writeString(directory.resolve("options.txt"), "sensitivity:0.1\nfov:30\ngamma:0.0\n");
        Runtime.getRuntime().halt(23);
    }
}
