package org.unallocated.bait;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /ubait}: the development command that places the bait for testing and for the video, until the core's
 * {@code /unallocated} and the act sequencer exist (plan §4.3). Needs cheats (game-master permission).
 * <pre>
 * /ubait start [steps first closest scare]   out of view, as a story beat would
 * /ubait ahead [steps first closest scare]   straight ahead in plain view (video, CI)
 * /ubait step | scare | clear | status
 * /ubait selftest                            dedicated-server self-test (console)
 * </pre>
 */
public final class SpyglassBaitCommand {
    private SpyglassBaitCommand() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ubait")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> status(context.getSource()))
                .then(withParameters("start", false))
                .then(withParameters("ahead", true))
                .then(Commands.literal("step").executes(context -> reply(context.getSource(),
                        SpyglassBait.forceStep(context.getSource().getPlayerOrException()), "step")))
                .then(Commands.literal("scare").executes(context -> reply(context.getSource(),
                        SpyglassBait.forceScare(context.getSource().getPlayerOrException()), "scare")))
                .then(Commands.literal("clear").executes(context -> reply(context.getSource(),
                        SpyglassBait.clear(context.getSource().getPlayerOrException(), "command"), "clear")))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("selftest").executes(context -> {
                    SpyglassBaitSelfTest.start(context.getSource().getLevel(),
                            BlockPos.containing(context.getSource().getPosition()));
                    context.getSource().sendSuccess(() -> text("selftest", "Autoprueba del cebo en marcha."), false);
                    return 1;
                })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> withParameters(String name, boolean ahead) {
        return Commands.literal(name)
                .executes(context -> start(context, SpyglassBaitSpec.defaults(), ahead))
                .then(Commands.argument("steps", IntegerArgumentType.integer(1, SpyglassBaitSpec.MAX_STEPS))
                        .then(Commands.argument("first", DoubleArgumentType.doubleArg(SpyglassBaitSpec.MIN_DISTANCE,
                                        SpyglassBaitSpec.MAX_DISTANCE))
                                .then(Commands.argument("closest", DoubleArgumentType.doubleArg(
                                                SpyglassBaitSpec.MIN_DISTANCE, SpyglassBaitSpec.MAX_DISTANCE))
                                        .then(Commands.argument("scare", BoolArgumentType.bool())
                                                .executes(context -> custom(context, ahead))))));
    }

    private static int custom(CommandContext<CommandSourceStack> context, boolean ahead) throws CommandSyntaxException {
        try {
            SpyglassBaitSpec spec = SpyglassBaitSpec.defaults()
                    .withDistances(SpyglassBaitSpec.geometric(IntegerArgumentType.getInteger(context, "steps"),
                            DoubleArgumentType.getDouble(context, "first"),
                            DoubleArgumentType.getDouble(context, "closest")))
                    .withScare(BoolArgumentType.getBool(context, "scare"));
            return start(context, spec, ahead);
        } catch (IllegalArgumentException invalid) {
            context.getSource().sendFailure(text("invalid",
                    "Distancias no válidas: la primera debe ser mayor que la más cercana."));
            return 0;
        }
    }

    private static int start(CommandContext<CommandSourceStack> context, SpyglassBaitSpec spec, boolean ahead)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SpyglassBait.place(player, spec, null, ahead);
        context.getSource().sendSuccess(() -> text("started", "Cebo del catalejo colocado: %s",
                spec.distances().toString()), false);
        return 1;
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        String status = SpyglassBait.status(source.getPlayerOrException());
        source.sendSuccess(() -> Component.literal("ubait: " + status), false);
        return 1;
    }

    private static int reply(CommandSourceStack source, boolean done, String key) {
        if (done) {
            source.sendSuccess(() -> text(key, "Hecho."), false);
            return 1;
        }
        source.sendFailure(text("none", "No hay un cebo activo para eso."));
        return 0;
    }

    /** Spanish text; the key lets a language file override it later without code changes. */
    private static Component text(String key, String spanish, Object... args) {
        return Component.translatableWithFallback("bait.unallocated.command." + key, spanish, args);
    }
}
