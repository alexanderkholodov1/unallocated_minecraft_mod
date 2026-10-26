package org.unallocated.map;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.unallocated.UnallocatedMod;

public final class MapCommand {
    private MapCommand() {}
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(
            Commands.literal("unallocated")
                .then(Commands.literal("mapcheck").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .executes(ctx -> run(ctx.getSource(), "mapcheck",
                        () -> MapVerifier.verify(ctx.getSource().getLevel(), "all")))
                    .then(Commands.literal("rebuild").executes(ctx -> run(ctx.getSource(), "mapcheck rebuild",
                        () -> MapVerifier.verifyRegeneration(ctx.getSource().getLevel())))))
                .then(Commands.literal("build").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.argument("section", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (String id : MapBuilder.sectionIds()) if (id.startsWith(builder.getRemaining())) builder.suggest(id);
                            if ("all".startsWith(builder.getRemaining())) builder.suggest("all");
                            return builder.buildFuture();
                        }).executes(ctx -> {
                            var source = ctx.getSource();
                            String section = StringArgumentType.getString(ctx, "section");
                            if (!section.equals("all") && !MapBuilder.sectionIds().contains(section)) {
                                source.sendFailure(Component.literal("Sección desconocida. Usa: " + MapBuilder.sectionIds() + ", all"));
                                return 0;
                            }
                            return run(source, "build " + section, () -> {
                                MapBuilder.build(source.getLevel(), section);
                                source.sendSuccess(() -> Component.literal("Mapa reconstruido."), true);
                            });
                        })))
        ));
    }
    private static int run(CommandSourceStack source, String operation, Runnable action) {
        try {
            action.run();
            return 1;
        } catch (Exception exception) {
            // Commands otherwise replace the exception with a generic system-chat error.
            UnallocatedMod.LOGGER.error("UNALLOCATED map command failed operation=" + operation, exception);
            source.sendFailure(Component.literal("No se pudo completar la operación del mapa. Revisa el registro: "
                + exception.getClass().getSimpleName() + ": " + exception.getMessage()));
            return 0;
        }
    }
}
