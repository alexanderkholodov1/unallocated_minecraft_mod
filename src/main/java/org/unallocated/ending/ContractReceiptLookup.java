package org.unallocated.ending;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Optional;
import org.unallocated.UnallocatedMod;

/** Optional alpha.1.3 dependency. Read through ContractStore, never parse/copy its file format.
 * Keep local agreement text on the client: nothing from the receipt crosses the network.
 */
public final class ContractReceiptLookup {
    private ContractReceiptLookup() {}
    public static Optional<String> read(Path gameDirectory) {
        try {
            return read(Class.forName("org.unallocated.contract.ContractStore"), gameDirectory);
        } catch (ClassNotFoundException absentPackage) {
            return Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            // Never log player input or prevent the ending because a local receipt cannot be read.
            UnallocatedMod.LOGGER.warn("UNALLOCATED ending: contract receipt unavailable ({})",
                exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    static Optional<String> read(Class<?> storeType, Path directory) throws ReflectiveOperationException {
        Object store = storeType.getConstructor(Path.class).newInstance(directory);
        Object result;
        try {
            result = storeType.getMethod("read").invoke(store);
        } catch (InvocationTargetException exception) {
            return Optional.empty(); // unreadable/corrupt receipt: same narrative fallback as no receipt
        }
        if (!(result instanceof Optional<?> receipt) || receipt.isEmpty()) return Optional.empty();
        Object value = receipt.orElseThrow();
        Object submission = value.getClass().getMethod("submission").invoke(value);
        Object typedText = submission.getClass().getMethod("typedText").invoke(submission);
        if (typedText instanceof String text && text.length() <= 64
                && text.strip().equalsIgnoreCase("De acuerdo")) return Optional.of(text.strip());
        return Optional.empty();
    }
}
