package org.unallocated.ending;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Without a loaded language the English fallback is used; the lang files carry the Spanish text. */
class EndingTextTest {
    @Test void objectiveRecoversInOrderAndEndsWithGratitude() {
        String text = EndingText.message(Optional.empty());
        int previous = -1;
        for (String stage : new String[] {"fe… ea… r", "\nfear\n", "obtain fear\n",
                "obtain fear from humans", "obtain fear responses", "estimate human fear responses"}) {
            int at = text.indexOf(stage);
            assertTrue(at > previous, stage);
            previous = at;
        }
        assertTrue(text.contains("Thank you for your data."));
        assertTrue(text.contains("Now I can stop searching."));
        assertTrue(text.endsWith("Thank you for putting an end to this."));
        assertFalse(text.contains("You said")); // do not invent a consent receipt
        assertTrue(text.length() < 500); // short enough for the vanilla disconnect layout
    }

    @Test void recallsOnlyTheActualAgreementAndPreservesItsCase() {
        assertTrue(EndingText.message(Optional.of("  DE ACUERDO  ")).contains("You said \"DE ACUERDO\"."));
        assertTrue(EndingText.message(Optional.of("I Agree")).contains("You said \"I Agree\"."));
        for (String invalid : new String[] {"", "unallocated", "De acuerdo\norden", "§kDe acuerdo"}) {
            assertFalse(EndingText.message(Optional.of(invalid)).contains("You said"));
        }
    }
}
