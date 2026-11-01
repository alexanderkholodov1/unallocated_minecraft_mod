package org.unallocated.contract;

import java.util.Objects;

/** Input adapter boundary: a future spoken adapter can submit through the same flow. */
public record ContractSubmission(Method method, String typedText) {
    public enum Method { CLICKED, TYPED }

    public ContractSubmission {
        Objects.requireNonNull(method);
        Objects.requireNonNull(typedText);
        if (typedText.length() > 64 || (method == Method.TYPED && !isAgreement(typedText))) {
            throw new IllegalArgumentException("Expected De acuerdo, at most 64 characters");
        }
    }

    public static boolean isAgreement(String text) {
        return text.strip().equalsIgnoreCase("De acuerdo");
    }
}
