package org.unallocated.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BuildArmingTest {
    @Test void onlySuccessfulAllBuildsArmAndOriginalPermissionsAndSuggestionsSurvive() throws Exception {
        var dispatcher = new CommandDispatcher<String>();
        var operations = new ArrayList<String>();
        var armed = new ArrayList<String>();
        dispatcher.register(LiteralArgumentBuilder.<String>literal("unallocated")
            .then(LiteralArgumentBuilder.<String>literal("build").requires(source -> source.equals("admin"))
                .then(RequiredArgumentBuilder.<String, String>argument("section", StringArgumentType.word())
                    .suggests((context, builder) -> builder.suggest("all").buildFuture())
                    .executes(context -> {
                        String section = StringArgumentType.getString(context, "section");
                        if (section.equals("throws")) throw new IllegalStateException("build failed");
                        operations.add(section);
                        return section.equals("failed") ? 0 : 1;
                    }))));
        BuildArming.install(dispatcher, source -> { assertEquals("all", operations.getLast()); armed.add(source); });
        assertEquals(1, dispatcher.execute("unallocated build lobby", "admin")); assertTrue(armed.isEmpty());
        assertEquals(0, dispatcher.execute("unallocated build failed", "admin")); assertTrue(armed.isEmpty());
        assertThrows(IllegalStateException.class, () -> dispatcher.execute("unallocated build throws", "admin"));
        assertTrue(armed.isEmpty());
        assertThrows(com.mojang.brigadier.exceptions.CommandSyntaxException.class,
            () -> dispatcher.execute("unallocated build all", "player"));
        BuildArming.install(dispatcher, source -> fail("Command must not be wrapped twice"));
        assertEquals(1, dispatcher.execute("unallocated build all", "admin"));
        assertEquals(java.util.List.of("admin"), armed);
        var suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse("unallocated build a", "admin")).get();
        assertEquals("all", suggestions.getList().getFirst().getText());
    }
    @Test void absentMapPackageAddsNoFakeBuildCommand() {
        var dispatcher = new CommandDispatcher<String>();
        BuildArming.install(dispatcher, source -> fail("Unexpected arming"));
        assertTrue(dispatcher.getRoot().getChildren().isEmpty());
    }
}
