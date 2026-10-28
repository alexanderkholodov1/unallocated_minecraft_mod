package org.unallocated.world;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.unallocated.story.*;
import static org.junit.jupiter.api.Assertions.*;

class WorldActionsTest {
    static { WorldActions.registerActions(); }
    @Test void publicParsersAcceptCrossDimensionCopyAndSeededSpreadingCorruption() {
        for (String json : new String[]{
            "{\"do\":\"copy\",\"from\":{\"section\":\"base\"},\"to\":{\"pos\":[100,60,100],\"dimension\":\"pocket\"}}",
            "{\"do\":\"corrupt\",\"area\":{\"box\":[0,60,0,15,70,15],\"dimension\":\"pocket\"},\"rules\":[{\"from\":[\"minecraft:stone\"],\"to\":[\"minecraft:cobblestone\"],\"ratio\":0.25}],\"seed\":17,\"over_seconds\":4}"
        }) {
            var validator = new StoryValidator(null, id -> false, false, false);
            var action = JsonParser.parseString(json).getAsJsonObject();
            assertNotNull(StoryActions.parser(action.get("do").getAsString()).parse(action, validator.context("world.json", "/action", new JsonObject())));
            assertTrue(validator.findings().isEmpty(), () -> validator.findings().toString());
        }
    }
    @Test void corruptionRejectsInvalidSeedRulesAndTimingBeforeExecution() {
        for (String invalid : new String[]{"\"seed\":1.5", "\"seed\":9223372036854775808", "\"over_seconds\":-1", "\"rules\":[{\"from\":[\"bad id\"],\"to\":[],\"ratio\":2}]"}) {
            var json = JsonParser.parseString("{\"do\":\"corrupt\",\"area\":{\"box\":[0,0,0,1,1,1]},\"rules\":[]}").getAsJsonObject();
            var replacement = JsonParser.parseString("{" + invalid + "}").getAsJsonObject();
            replacement.entrySet().forEach(entry -> json.add(entry.getKey(), entry.getValue()));
            var validator = new StoryValidator(null, id -> false, false, false);
            StoryActions.parser("corrupt").parse(json, validator.context("world.json", "/action", new JsonObject()));
            assertFalse(validator.findings().isEmpty(), invalid);
        }
    }
}
