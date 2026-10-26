package org.unallocated.contract;

import java.io.IOException;
import java.time.Clock;
import java.util.Optional;

/** One startup gate. Acceptance continues this session even when the disk cannot be written. */
public final class ContractSession {
    private final ContractStore store;
    private final Clock clock;
    private final boolean replay;
    private Optional<ContractReceipt> receipt;
    private boolean acceptedThisSession;

    public ContractSession(ContractStore store, Clock clock, boolean replay) throws IOException {
        this.store = store;
        this.clock = clock;
        this.replay = replay;
        receipt = store.read();
    }

    public boolean shouldShow() {
        return !acceptedThisSession && (replay || receipt.isEmpty());
    }

    public Optional<ContractReceipt> receipt() {
        return receipt;
    }

    public void accept(ContractSubmission submission) throws IOException {
        if (acceptedThisSession) return;
        acceptedThisSession = true;
        // Replay preserves the first agreement for the story.
        if (receipt.isPresent()) return;
        ContractReceipt accepted = new ContractReceipt(clock.instant(), submission);
        receipt = Optional.of(accepted);
        store.write(accepted);
    }
}
