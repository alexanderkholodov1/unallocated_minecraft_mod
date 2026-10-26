package org.unallocated.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Locale;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.unallocated.UnallocatedMod;
import org.unallocated.core.Act;
import org.unallocated.core.CoreRuntime;
import org.unallocated.core.network.CoreNetworking;

/** Other packages may merge scare/build children into the same Brigadier root via Fabric's callback. */
public final class UnallocatedCommand {
    private UnallocatedCommand() {}
    private static CoreRuntime core(CommandSourceStack source) { return CoreRuntime.get(source.getServer()); }
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(
            Commands.literal("unallocated")
                .then(Commands.literal("siguiente").executes(ctx -> {
                    var source = ctx.getSource();
                    core(source).sequencer().advance(source.getPlayerOrException());
                    source.sendSuccess(() -> Component.literal("Sesión avanzada."), false);
                    return 1;
                }))
                .then(Commands.literal("status").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("act").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (Act act : Act.values()) if (act.name().startsWith(builder.getRemaining().toUpperCase(Locale.ROOT))) builder.suggest(act.name());
                            return builder.buildFuture();
                        }).executes(ctx -> {
                            var source = ctx.getSource();
                            Act act;
                            try { act = Act.valueOf(StringArgumentType.getString(ctx, "id").toUpperCase(Locale.ROOT)); }
                            catch (IllegalArgumentException e) { source.sendFailure(Component.literal("Acto desconocido.")); return 0; }
                            if (source.getPlayer() == null) core(source).sequencer().skipTo(act);
                            else core(source).sequencer().skipTo(act, source.getPlayer());
                            source.sendSuccess(() -> Component.literal("Acto: " + act), false);
                            return 1;
                        })))
                .then(Commands.literal("flag").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> {
                        core(ctx.getSource()).state().addFlag(StringArgumentType.getString(ctx, "id"));
                        CoreNetworking.sync(ctx.getSource().getServer(), core(ctx.getSource()).state());
                        return 1;
                    })))
                .then(Commands.literal("fear").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .executes(ctx -> {
                        var state = core(ctx.getSource()).state();
                        ctx.getSource().sendSuccess(() -> Component.literal("Perfil: " + state.profile() + " · temperatura=" + state.temperature()), false);
                        return 1;
                    }))
        ));
    }
    private static int status(CommandSourceStack source) {
        var runtime = core(source);
        var state = runtime.state();
        String report = "UNALLOCATED status act=" + state.act() + " started=" + state.started()
            + " finished=" + state.finished() + " elapsedTicks=" + state.elapsedActTicks()
            + " budgetTicks=" + runtime.sequencer().budgetTicks() + " checkpoint=" + runtime.sequencer().checkpointId()
            + " flags=" + state.flags() + " counters=" + state.counters();
        UnallocatedMod.LOGGER.info(report);
        source.sendSuccess(() -> Component.literal("Estado de sesión: " + state.act() + " · "
            + state.elapsedActTicks() / 20 + " s · punto de control: " + runtime.sequencer().checkpointId()), false);
        return 1;
    }
}
