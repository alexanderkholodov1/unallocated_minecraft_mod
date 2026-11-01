package org.unallocated.contract;

import java.time.Instant;

/** The original agreement, available to later narrative callbacks without reading other files. */
public record ContractReceipt(Instant acceptedAt, ContractSubmission submission) { }
