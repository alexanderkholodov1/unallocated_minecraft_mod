package org.unallocated.creature;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.unallocated.story.*;
import static org.junit.jupiter.api.Assertions.*;

class CreatureActionsTest {
    static { CreatureActions.registerActions(); }
    private StoryValidator parse(String source, boolean trigger) {
        var validator = new StoryValidator(null, id -> false, false, false);
        var json = JsonParser.parseString(source).getAsJsonObject();
        if (trigger) StoryTriggers.parser("creature").parse(json, validator.context("world.json", "/when", new JsonObject()));
        else StoryActions.parser("creature").parse(json, validator.context("world.json", "/action", new JsonObject()));
        return validator;
    }
    @Test void mannequinBodyAndAllOperationsCompile() {
        assertTrue(parse("{\"do\":\"creature\",\"op\":\"spawn\",\"id\":\"figure\",\"body\":{\"mannequin\":{\"profile\":\"player\",\"scale\":1.2}},\"where\":{\"pos\":[0,64,0],\"dimension\":\"pocket\"},\"facing\":\"player\"}", false).findings().isEmpty());
        for (String op : new String[]{"stalk", "chase", "hold", "face_player", "despawn"}) assertTrue(parse("{\"do\":\"creature\",\"op\":\"" + op + "\",\"id\":\"figure\"}", false).findings().isEmpty(), op);
    }
    @Test void creatureTriggersCompileWithDwellAndDistance() {
        for (String event : new String[]{"near", "seen", "caught"}) assertTrue(parse("{\"type\":\"creature\",\"creature\":\"figure\",\"event\":\"" + event + "\",\"distance\":3,\"seconds\":0.5}", true).findings().isEmpty(), event);
    }
    @Test void invalidBodiesAndMissingTexturesFailAtLoad() {
        for (String body : new String[]{"{}", "{\"mannequin\":{\"profile\":\"invalid profile name\"}}", "{\"mannequin\":{\"profile\":\"player\",\"scale\":0}}", "{\"mannequin\":{\"profile\":\"player\",\"texture\":\"unallocated:missing.png\"}}", "{\"summon\":\"not an entity\"}"}) {
            assertFalse(parse("{\"do\":\"creature\",\"op\":\"spawn\",\"id\":\"figure\",\"body\":" + body + ",\"where\":{\"pos\":[0,64,0]}}", false).findings().isEmpty(), body);
        }
        assertFalse(parse("{\"type\":\"creature\",\"creature\":\"figure\",\"event\":\"unknown\"}", true).findings().isEmpty());
        assertFalse(parse("{\"do\":\"creature\",\"id\":\"figure\",\"op\":\"chase\",\"speed\":-1}", false).findings().isEmpty());
    }
}
