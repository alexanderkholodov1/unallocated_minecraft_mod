package org.unallocated.map;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class MapCommand {
    private MapCommand() {}
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(
            Commands.literal("unallocated")
                .then(Commands.literal("mapcheck").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .executes(ctx -> { MapVerifier.verify(ctx.getSource().getLevel(), "all"); return 1; })
                    .then(Commands.literal("rebuild").executes(ctx -> {
                        MapVerifier.verifyRegeneration(ctx.getSource().getLevel()); return 1;
                    })))
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
                            MapBuilder.build(source.getLevel(), section);
                            source.sendSuccess(() -> Component.literal("Mapa reconstruido."), true);
                            return 1;
                        })))
        ));
    }
}
