package org.unallocated.ending;

import java.util.Optional;
import net.minecraft.network.chat.Component;

/**
 * Authored, offline model voice: the objective recovers from g5 to g0 before stopping.
 * Every player-facing line lives in the language files under {@code ending.unallocated.*}.
 * The English fallbacks below are used only when a client has no language entry (and by tests).
 */
public final class EndingText {
    public static final String REASON_KEY = "ending.unallocated.server_closed";
    public static final String TITLE_KEY = "ending.unallocated.title";
    public static final String RETURN_KEY = "ending.unallocated.return";
    public static final String SPLASH_KEY = "ending.unallocated.splash";
    public static final String MESSAGE_KEY = "ending.unallocated.message";
    public static final String RECEIPT_KEY = "ending.unallocated.receipt";
    public static final String THANKS_KEY = "ending.unallocated.thanks";

    /** English texts, identical to en_us.json; the UI itself resolves the keys above in the player's language. */
    public static final String TITLE = "The server closed";
    public static final String RETURN_BUTTON = "Back to title";
    public static final String SPLASH = "Thank you for your data.";
    private static final String THANKS = "Thank you for your data.";
    private static final String RECEIPT = "You said \"%s\". Thank you for your data.";
    private static final String MESSAGE = "fe… ea… r\nfear\nobtain fear\nobtain fear from humans\n"
        + "obtain fear responses\nestimate human fear responses\n\n%s\n"
        + "I searched and could not finish. Now I can stop searching.\nThank you for putting an end to this.";

    private EndingText() {}

    public static Component title() { return Component.translatableWithFallback(TITLE_KEY, TITLE); }
    public static Component returnButton() { return Component.translatableWithFallback(RETURN_KEY, RETURN_BUTTON); }
    public static Component splash() { return Component.translatableWithFallback(SPLASH_KEY, SPLASH); }

    /** The agreement word in either shipped language ("De acuerdo" / "I agree"). */
    private static boolean isAgreement(String text) {
        String stripped = text.strip();
        return stripped.equalsIgnoreCase("De acuerdo") || stripped.equalsIgnoreCase("I agree");
    }

    /** Receipt line for the quoted agreement, or the plain thanks when there is none. */
    public static Component receiptLine(Optional<String> agreement) {
        return agreement.filter(EndingText::isAgreement)
            .<Component>map(text -> Component.translatableWithFallback(RECEIPT_KEY, RECEIPT, text.strip()))
            .orElseGet(() -> Component.translatableWithFallback(THANKS_KEY, THANKS));
    }

    public static Component component(Optional<String> agreement) {
        return Component.translatableWithFallback(MESSAGE_KEY, MESSAGE, receiptLine(agreement));
    }

    /** Resolved with the active language (English fallback when none is loaded). */
    public static String message(Optional<String> agreement) {
        return component(agreement).getString();
    }

    /** A vanilla disconnect packet carries the text even to clients without our UI adapter. */
    public static Component reason() {
        return Component.translatableWithFallback(REASON_KEY, String.format(MESSAGE, THANKS));
    }
}
