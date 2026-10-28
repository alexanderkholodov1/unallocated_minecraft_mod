package org.unallocated.story;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.unallocated.map.MapLayout;
import static org.junit.jupiter.api.Assertions.*;

class StoryLoaderTest {
    private StoryLoader.Result load(Path path) {
        return StoryLoader.load(StoryLoader.directory(path), new StoryValidator(null,
            id -> Files.isRegularFile(Path.of("src/main/resources/data", id.split(":", 2)[0], "function", id.split(":", 2)[1] + ".mcfunction")), false, false));
    }
    @Test void fixturesAgreeWithOfflineRuleAndPointerContract() throws Exception {
        Path base = Path.of("src/test/resources/story-fixtures");
        try (var valid = Files.list(base.resolve("valid"))) {
            for (Path dir : valid.filter(Files::isDirectory).toList()) {
                var result = load(dir);
                assertTrue(result.valid(), () -> dir + ": " + result.findings());
            }
        }
        try (var invalid = Files.list(base.resolve("invalid"))) {
            for (Path dir : invalid.filter(Files::isDirectory).toList()) {
                String[] expected = Files.readString(dir.resolve("EXPECT.txt")).strip().split(" +");
                var result = load(dir);
                assertFalse(result.valid(), dir.toString());
                assertTrue(result.findings().stream().anyMatch(f -> (f.file() + "#" + f.pointer()).equals(expected[0]) && f.rule().equals(expected[1])), () -> dir + ": " + result.findings());
            }
        }
    }
    @Test void duplicateKeysAndTrailingJsonAreRejected() {
        assertThrows(Exception.class, () -> StoryLoader.parse("{\"format\":1,\"format\":2}"));
        assertThrows(Exception.class, () -> StoryLoader.parse("{} {}"));
    }
    @Test void shippedStoryIsNeutralAndValid() {
        var result = load(Path.of("src/main/resources/data/unallocated/story"));
        assertTrue(result.valid(), () -> result.findings().toString());
        assertEquals(1, result.data().chapters().size());
        assertEquals(420, result.data().chapters().getFirst().get("budget_seconds").getAsInt());
    }
    @Test void anchorNamesExactlyMatchMapLayout() throws Exception {
        JsonObject anchors = JsonParser.parseString(Files.readString(Path.of("scripts/map_anchors.json"))).getAsJsonObject();
        Map<String, Set<String>> expected = Map.of("checkpoints", MapLayout.checkpoints().keySet(), "landmarks", MapLayout.landmarks().keySet(), "sections", MapLayout.sections().keySet(), "triggers", MapLayout.triggers().keySet());
        assertEquals(expected.keySet(), anchors.keySet());
        expected.forEach((key, names) -> assertEquals(names, new HashSet<>(anchors.getAsJsonArray(key).asList().stream().map(JsonElement::getAsString).toList())));
    }
    @Test void packagesFourActionsAndUnverifiedPostChainsAreUnknown() {
        for (String action : List.of("copy", "corrupt", "creature", "post_effect")) assertNull(StoryActions.parser(action));
        assertNull(StoryTriggers.parser("creature"));
    }
}
