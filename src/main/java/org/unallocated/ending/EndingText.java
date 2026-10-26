package org.unallocated.ending;

import java.util.Optional;
import net.minecraft.network.chat.Component;

/** Authored, offline model voice: the objective recovers from g5 to g0 before stopping. */
public final class EndingText {
    public static final String REASON_KEY = "unallocated.ending.server_closed";
    public static final String TITLE = "El servidor se cerró";
    public static final String RETURN_BUTTON = "Volver al título";
    public static final String SPLASH = "Gracias por tus datos.";
    private EndingText() {}

    public static String message(Optional<String> agreement) {
        String receipt = agreement.filter(text -> text.strip().equalsIgnoreCase("De acuerdo"))
            .map(text -> "Dijiste «" + text.strip() + "». Gracias por tus datos.")
            .orElse("Gracias por tus datos.");
        return "mi… ed… o\nmiedo\nobtener miedo\nobtener miedo de humanos\n"
            + "obtener respuestas de miedo\nestimar respuestas de miedo humano\n\n"
            + receipt + "\nBusqué sin poder terminar. Ya puedo dejar de buscar.\n"
            + "Gracias por poner fin a esto.";
    }

    /** A vanilla disconnect packet carries the text even to clients without our UI adapter. */
    public static Component reason() {
        return Component.translatableWithFallback(REASON_KEY, message(Optional.empty()));
    }
}
