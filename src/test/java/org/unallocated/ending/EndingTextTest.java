package org.unallocated.ending;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EndingTextTest {
    @Test void objectiveRecoversInOrderAndEndsWithGratitude() {
        String text = EndingText.message(Optional.empty());
        int previous = -1;
        for (String stage : new String[] {"mi… ed… o", "\nmiedo\n", "obtener miedo\n",
                "obtener miedo de humanos", "obtener respuestas de miedo", "estimar respuestas de miedo humano"}) {
            int at = text.indexOf(stage);
            assertTrue(at > previous, stage);
            previous = at;
        }
        assertTrue(text.contains("Gracias por tus datos."));
        assertTrue(text.contains("Ya puedo dejar de buscar."));
        assertTrue(text.endsWith("Gracias por poner fin a esto."));
        assertFalse(text.contains("Dijiste")); // do not invent a consent receipt
        assertTrue(text.length() < 500); // short enough for the vanilla disconnect layout
    }

    @Test void recallsOnlyTheActualAgreementAndPreservesItsCase() {
        assertTrue(EndingText.message(Optional.of("  DE ACUERDO  ")).contains("Dijiste «DE ACUERDO»."));
        for (String invalid : new String[] {"", "unallocated", "De acuerdo\norden", "§kDe acuerdo"}) {
            assertFalse(EndingText.message(Optional.of(invalid)).contains("Dijiste"));
        }
    }
}
