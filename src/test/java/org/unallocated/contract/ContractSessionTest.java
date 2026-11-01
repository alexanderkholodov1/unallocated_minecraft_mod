package org.unallocated.contract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ContractSessionTest {
    @TempDir Path game;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-26T05:00:00Z"), ZoneOffset.UTC);

    private ContractSession start(boolean replay) throws IOException {
        return new ContractSession(new ContractStore(game), clock, replay);
    }

    @Test void firstStartThenClickThenSecondStart() throws IOException {
        var first = start(false);
        assertTrue(first.shouldShow());
        assertTrue(first.receipt().isEmpty());
        assertFalse(Files.exists(game.resolve(ContractStore.RELATIVE_PATH)));
        first.accept(new ContractSubmission(ContractSubmission.Method.CLICKED, ""));
        assertFalse(first.shouldShow());
        var second = start(false);
        assertFalse(second.shouldShow());
        assertEquals(clock.instant(), second.receipt().orElseThrow().acceptedAt());
        assertEquals(ContractSubmission.Method.CLICKED, second.receipt().orElseThrow().submission().method());
        assertEquals("", second.receipt().orElseThrow().submission().typedText());
    }

    @Test void typedTextIsPreservedExactlyAndNotNormalizedInTheReceipt() throws IOException {
        var session = start(false);
        String original = "  dE AcUeRdO  ";
        session.accept(new ContractSubmission(ContractSubmission.Method.TYPED, original));
        assertEquals(new ContractReceipt(clock.instant(), new ContractSubmission(
                ContractSubmission.Method.TYPED, original)), new ContractStore(game).read().orElseThrow());
    }

    @Test void clickRecordsEvenUnfinishedTextWithoutRequiringItToMatch() throws IOException {
        start(false).accept(new ContractSubmission(ContractSubmission.Method.CLICKED, "de acuér…"));
        assertEquals("de acuér…", new ContractStore(game).read().orElseThrow().submission().typedText());
    }

    @Test void replayAndDuplicateAcceptancePreserveTheOriginalReceipt() throws IOException {
        var first = start(false);
        first.accept(new ContractSubmission(ContractSubmission.Method.CLICKED, ""));
        byte[] original = Files.readAllBytes(game.resolve(ContractStore.RELATIVE_PATH));
        var replay = start(true);
        assertTrue(replay.shouldShow());
        replay.accept(new ContractSubmission(ContractSubmission.Method.TYPED, "De acuerdo"));
        replay.accept(new ContractSubmission(ContractSubmission.Method.CLICKED, "other"));
        assertFalse(replay.shouldShow());
        assertArrayEquals(original, Files.readAllBytes(game.resolve(ContractStore.RELATIVE_PATH)));
        assertFalse(start(false).shouldShow());
    }

    @Test void deletingTheReceiptReopensTheAgreementOnNextStartup() throws IOException {
        start(false).accept(new ContractSubmission(ContractSubmission.Method.CLICKED, ""));
        Files.delete(game.resolve(ContractStore.RELATIVE_PATH));
        assertTrue(start(false).shouldShow());
    }

    @Test void invalidReceiptsCannotSkipTheAgreement() throws IOException {
        Path file = game.resolve(ContractStore.RELATIVE_PATH);
        Files.createDirectories(file.getParent());
        String valid = "schemaVersion=1\ncontractVersion=1\nacceptedAt=2026-10-26T05:00:00Z\n"
                + "method=TYPED\ntypedText=De acuerdo\n";
        for (String invalid : new String[] {"", "accepted=true", valid.replace("schemaVersion=1", "schemaVersion=2"),
                valid.replace("contractVersion=1", "contractVersion=2"), valid.replace("TYPED", "SPOKEN"),
                valid.replace("2026-10-26T05:00:00Z", "never"), valid.replace("De acuerdo", "no"),
                valid.replace("typedText=De acuerdo\n", ""), "x".repeat(4097), "typedText=\\uZZZZ"}) {
            Files.writeString(file, invalid);
            assertTrue(start(false).shouldShow(), invalid);
        }
    }

    @Test void invalidTypedSubmissionAndOversizeInputAreRejected() {
        for (String text : new String[] {"", "No", "De acuerdo mañana", "De  acuerdo"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new ContractSubmission(ContractSubmission.Method.TYPED, text));
        }
        assertThrows(IllegalArgumentException.class,
                () -> new ContractSubmission(ContractSubmission.Method.CLICKED, "x".repeat(65)));
    }

    @Test void nonUtf8ReceiptIsReplacedByAcceptanceAndSkippedOnNextStart() throws IOException {
        Path file = game.resolve(ContractStore.RELATIVE_PATH);
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[] {'#', (byte) 0xF3, '\n'});
        var first = start(false);
        assertTrue(first.shouldShow());
        assertTrue(first.receipt().isEmpty());
        first.accept(new ContractSubmission(ContractSubmission.Method.CLICKED, ""));
        var second = start(false);
        assertFalse(second.shouldShow());
        assertEquals(first.receipt(), second.receipt());
        assertTrue(second.receipt().isPresent());
    }

    @Test void failedWriteStillAcceptsThisSessionAndDoesNotInventPersistence() throws IOException {
        var first = start(false);
        // A regular file obstructing the directory is deterministic even when CI has elevated privileges.
        Files.writeString(game.resolve("unallocated"), "blocked");
        assertThrows(IOException.class,
                () -> first.accept(new ContractSubmission(ContractSubmission.Method.CLICKED, "")));
        assertFalse(first.shouldShow());
        assertTrue(first.receipt().isPresent());
        Files.delete(game.resolve("unallocated"));
        assertTrue(start(false).shouldShow());
    }
}
