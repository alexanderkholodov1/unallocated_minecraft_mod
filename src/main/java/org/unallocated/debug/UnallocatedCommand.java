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
                    var player = source.getPlayerOrException();
                    if (!core(source).sequencer().rescue(player)) {
                        source.sendFailure(Component.literal("Rescate disponible sólo durante una sesión activa, tras cinco segundos en el acto."));
                        return 0;
                    }
                    source.sendSuccess(() -> Component.literal("Sesión avanzada."), false);
                    return 1;
                }))
                .then(Commands.literal("arm").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .executes(ctx -> {
                        CoreRuntime.arm(ctx.getSource().getServer());
                        ctx.getSource().sendSuccess(() -> Component.literal("Mundo preparado; la sesión comenzará al entrar un jugador."), false);
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
                            if (!requireArmed(source)) return 0;
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
                        if (!requireArmed(ctx.getSource())) return 0;
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
    private static boolean requireArmed(CommandSourceStack source) {
        if (core(source).state().armed()) return true;
        source.sendFailure(Component.literal("Este mundo no está preparado para UNALLOCATED."));
        return false;
    }
    private static int status(CommandSourceStack source) {
        var runtime = core(source);
        var state = runtime.state();
        String report = "UNALLOCATED status act=" + state.act() + " started=" + state.started()
            + " finished=" + state.finished() + " elapsedTicks=" + state.elapsedActTicks()
            + " budgetTicks=" + runtime.sequencer().budgetTicks() + " checkpoint=" + runtime.sequencer().checkpointId()
            + " armed=" + state.armed() + " flags=" + state.flags() + " counters=" + state.counters();
        UnallocatedMod.LOGGER.info(report);
        source.sendSuccess(() -> Component.literal("Estado de sesión: " + state.act() + " · "
            + state.elapsedActTicks() / 20 + " s · punto de control: " + runtime.sequencer().checkpointId()), false);
        return 1;
    }
}
