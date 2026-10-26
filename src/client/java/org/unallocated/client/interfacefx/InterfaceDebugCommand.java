package org.unallocated.client.interfacefx;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import org.unallocated.client.interfacefx.InterfaceEffects.Effect;
import org.unallocated.client.interfacefx.InterfaceEffects.Request;
import org.unallocated.client.interfacefx.InterfaceTexts.LeakLanguage;

/** Deliberately local: no chat text or command is forwarded to a server. */
public final class InterfaceDebugCommand {
    private InterfaceDebugCommand() { }

    public static boolean handle(String message) {
        String input = message.strip();
        if (!input.equals("/uinterface") && !input.startsWith("/uinterface ")) return false;
        Minecraft client = Minecraft.getInstance();
        if (!Boolean.getBoolean("unallocated.interface.debug")) {
            reply("disabled");
            return true;
        }
        String[] args = input.split("\\s+");
        if (args.length == 1 || args[1].equals("help")) {
            reply("help");
            return true;
        }
        if (args[1].equals("restore")) {
            InterfaceEffects.clear();
            reply("restored");
            return true;
        }
        try {
            if (args.length > 8) throw new IllegalArgumentException();
            Effect effect = Effect.valueOf(args[1].toUpperCase(Locale.ROOT));
            int ticks = args.length > 2 ? Integer.parseInt(args[2]) : Math.min(60, InterfaceEffects.maxTicks(effect));
            int generation = args.length > 3 ? Integer.parseInt(args[3]) : 2;
            LeakLanguage language = args.length > 4 ? LeakLanguage.parse(args[4]) : LeakLanguage.ES;
            double strength = args.length > 5 ? Double.parseDouble(args[5]) : .5;
            long seed = args.length > 6 ? Long.parseLong(args[6]) : 17;
            Identifier advancement = args.length > 7 ? Identifier.parse(args[7]) : null;
            BlockPos sign = client.hitResult instanceof BlockHitResult hit ? hit.getBlockPos() : null;
            if (effect == Effect.SIGN && (sign == null || client.level == null
                    || !(client.level.getBlockEntity(sign) instanceof net.minecraft.world.level.block.entity.SignBlockEntity))) {
                reply("sign");
                return true;
            }
            boolean accepted = InterfaceEffects.debugTrigger(new Request(effect, ticks, generation,
                    strength, language, seed, sign, advancement));
            reply(accepted ? "fired" : "refused");
        } catch (IllegalArgumentException failure) {
            reply("help");
        }
        return true;
    }

    private static void reply(String key) {
        Minecraft.getInstance().gui.hud.getChat().addMessage(Component.translatable("interface.unallocated.debug." + key));
    }
}
