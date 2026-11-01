package org.unallocated.story;
import com.google.gson.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StoryTextsTest {
    @Test void clientLocalesFallbackFixedAndLocalPlaceholders() {
        var envelope = JsonParser.parseString("""
            {"languages":["es","en"],"default_language":"es","language_select":"client",
             "agreement_fallback":{"es":"clic","en":"click"},"elapsed_min":4,"deaths":2,"counters":{"door":3}}
            """).getAsJsonObject();
        assertEquals("es", StoryTexts.language(envelope,"es_mx")); assertEquals("es",StoryTexts.language(envelope,"fr_fr")); assertEquals("en",StoryTexts.language(envelope,"en_us"));
        var text = JsonParser.parseString("{\"es\":\"{player}|{agreement}|{local_time}|{world}|{elapsed_min}|{deaths}|{counter:door}|{counter:missing}\"}").getAsJsonObject();
        assertEquals("Alex|clic|12:34|save|4|2|3|0", StoryTexts.render(text,envelope,"es_mx",Map.of("player","Alex","local_time","12:34","world","save")));
        assertTrue(StoryTexts.render(text,envelope,"es",Map.of("agreement","dE AcUeRdO")).contains("dE AcUeRdO"));
        envelope.addProperty("language_select","fixed"); assertEquals("es",StoryTexts.language(envelope,"en_us"));
    }
}
