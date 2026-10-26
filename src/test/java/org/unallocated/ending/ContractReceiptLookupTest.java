package org.unallocated.ending;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Public fixture shapes deliberately match alpha.1.3's API, without shipping a second ContractStore. */
public class ContractReceiptLookupTest {
    public record Submission(String typedText) {}
    public record Receipt(Submission submission) {}
    public static class Store {
        private final String fixture;
        public Store(Path path) { fixture = path.toString(); }
        public Optional<Receipt> read() throws IOException {
            return switch (fixture) {
                case "absent", "corrupt" -> Optional.empty();
                case "unreadable" -> throw new IOException("fixture");
                case "clicked" -> Optional.of(new Receipt(new Submission("")));
                case "invalid" -> Optional.of(new Receipt(new Submission("not agreement")));
                default -> Optional.of(new Receipt(new Submission("de ACUERDO")));
            };
        }
    }

    @Test void readsViaTheStoreApiWithoutOpeningItsFiles() throws Exception {
        assertEquals(Optional.of("de ACUERDO"), ContractReceiptLookup.read(Store.class, Path.of("typed")));
    }
    @Test void absentCorruptUnreadableOrClickedReceiptDegradesGracefully() throws Exception {
        for (String fixture : new String[] {"absent", "corrupt", "unreadable", "clicked", "invalid"}) {
            assertTrue(ContractReceiptLookup.read(Store.class, Path.of(fixture)).isEmpty(), fixture);
        }
    }
    @Test void unmergedContractPackageIsOptional() {
        assertTrue(ContractReceiptLookup.read(Path.of("nonexistent-game-directory")).isEmpty());
    }
}
