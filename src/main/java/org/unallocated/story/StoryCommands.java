package org.unallocated.story;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

final class StoryCommands {
    static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(
            Commands.literal("unallocated").then(Commands.literal("story").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("status").executes(c -> {
                    var r = StoryRuntime.get(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("Story: " + (r.currentChapter() == null ? "inactive" : r.currentChapter().get("id")) + " flags=" + r.state().flags()), false); return 1;
                }))
                .then(Commands.literal("fire").then(Commands.argument("beat", StringArgumentType.word()).executes(c -> StoryRuntime.get(c.getSource().getServer()).fire(StringArgumentType.getString(c, "beat")) ? 1 : 0)))
                .then(Commands.literal("check").executes(c -> {
                    var result = StoryLoader.load(StoryLoader.resources(), new StoryValidator(c.getSource().getServer()));
                    for (var f : result.findings()) if (!f.warning()) { org.unallocated.UnallocatedMod.LOGGER.error(f.marker()); c.getSource().sendFailure(Component.literal(f.marker())); }
                    if (result.valid()) { StoryRuntime.log("check ok"); c.getSource().sendSuccess(() -> Component.literal("Story check: OK"), false); } return result.valid() ? 1 : 0;
                }))
                .then(Commands.literal("build").executes(c -> { StoryBuild.run(c.getSource().getServer()); return 1; }))
            )));
    }
}
