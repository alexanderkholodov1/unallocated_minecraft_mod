package org.unallocated.contract;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.DateTimeException;
import java.util.Optional;
import java.util.Properties;

/** Local to the game directory, never a world save or a network payload. */
public final class ContractStore {
    public static final String RELATIVE_PATH = "unallocated/contract.properties";
    public static final String TEXT_VERSION = "1";
    private final Path file;

    public ContractStore(Path gameDirectory) {
        file = gameDirectory.resolve(RELATIVE_PATH);
    }

    public Optional<ContractReceipt> read() throws IOException {
        if (!Files.exists(file)) return Optional.empty();
        if (!Files.isRegularFile(file) || Files.size(file) > 4096) return Optional.empty();
        Properties data = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            data.load(reader);
            if (!"1".equals(data.getProperty("schemaVersion"))
                    || !TEXT_VERSION.equals(data.getProperty("contractVersion"))) return Optional.empty();
            return Optional.of(new ContractReceipt(Instant.parse(data.getProperty("acceptedAt")),
                    new ContractSubmission(ContractSubmission.Method.valueOf(data.getProperty("method")),
                            data.getProperty("typedText"))));
        } catch (IllegalArgumentException | NullPointerException | DateTimeException exception) {
            // A partial/corrupt receipt is not evidence of agreement.
            return Optional.empty();
        }
    }

    public void write(ContractReceipt receipt) throws IOException {
        Files.createDirectories(file.getParent());
        Properties data = new Properties();
        data.setProperty("schemaVersion", "1");
        data.setProperty("contractVersion", TEXT_VERSION);
        data.setProperty("acceptedAt", receipt.acceptedAt().toString());
        data.setProperty("method", receipt.submission().method().name());
        data.setProperty("typedText", receipt.submission().typedText());
        Path pending = Files.createTempFile(file.getParent(), "contract-", ".pending");
        try {
            try (Writer writer = Files.newBufferedWriter(pending, StandardCharsets.UTF_8)) {
                data.store(writer, "UNALLOCATED - aceptación local; ver docs/plan/CONTRACT-TEXT.md");
            }
            try {
                Files.move(pending, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(pending, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(pending);
        }
    }
}
