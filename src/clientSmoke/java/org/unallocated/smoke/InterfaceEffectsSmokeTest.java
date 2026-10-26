package org.unallocated.smoke;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import org.slf4j.LoggerFactory;
import org.unallocated.client.interfacefx.InterfaceDebugCommand;
import org.unallocated.client.interfacefx.InterfaceEffects;
import org.unallocated.client.interfacefx.InterfaceEffects.Effect;
import org.unallocated.client.interfacefx.InterfaceEffects.Request;
import org.unallocated.client.interfacefx.InterfaceOptionsGuard;
import org.unallocated.client.interfacefx.InterfaceTexts;
import org.unallocated.client.interfacefx.InterfaceTexts.LeakLanguage;

/** Runs against the packaged player JAR, never development class directories. */
@SuppressWarnings("UnstableApiUsage")
final class InterfaceEffectsSmokeTest {
    private InterfaceEffectsSmokeTest() { }

    static void run(ClientGameTestContext context) {
        context.runOnClient(client -> {
            require(InterfaceEffects.debugTrigger(request(Effect.SPLASH, 600)), "Arm title splash");
            require(!InterfaceEffects.trigger(request(Effect.LOADING, 600)), "Production API must enforce its global cooldown");
            try {
                new Request(Effect.FOV, 101, 2, .5, LeakLanguage.ES, 17, null);
                throw new AssertionError("Unbounded settings duration accepted");
            } catch (IllegalArgumentException expected) { }
            try {
                new Request(Effect.FOV, 60, 2, Double.NaN, LeakLanguage.ES, 17, null);
                throw new AssertionError("Non-finite settings strength accepted");
            } catch (IllegalArgumentException expected) { }
            for (var language : LeakLanguage.values()) {
                for (int generation = 0; generation < 6; generation++) {
                    String leak = InterfaceTexts.line("goal", generation, language).getString();
                    require(!leak.startsWith("interface.unallocated"), "Missing language resource");
                }
            }
        });
        context.waitTicks(3);
        context.takeScreenshot("unallocated-interface-splash");
        observed(context, "SPLASH");

        context.runOnClient(client -> require(InterfaceEffects.debugTrigger(request(Effect.LOADING, 600)), "Arm loading text"));
        context.setScreen(() -> new LevelLoadingScreen(new LevelLoadTracker(), LevelLoadingScreen.Reason.OTHER));
        context.waitTicks(3);
        context.takeScreenshot("unallocated-interface-loading");
        observed(context, "LOADING");
        context.setScreen(TitleScreen::new);
        context.runOnClient(client -> InterfaceEffects.clear());

        var baselineBytes = new AtomicReference<byte[]>();
        var baselineValues = new AtomicReference<List<Number>>();
        try (var world = context.worldBuilder().create()) {
            context.waitFor(client -> client.level != null && client.player != null && client.gui.screen() == null);
            context.runOnClient(client -> {
                try {
                    baselineBytes.set(Files.readAllBytes(client.gameDirectory.toPath().resolve("options.txt")));
                } catch (Exception failure) {
                    throw new AssertionError("Cannot read options baseline", failure);
                }
                baselineValues.set(List.of(client.options.sensitivity().get(), client.options.fov().get(), client.options.gamma().get()));
                client.debugEntries.setOverlayVisible(true);
                var position = Identifier.withDefaultNamespace("player_position");
                if (!client.debugEntries.isCurrentlyEnabled(position)) client.debugEntries.toggleStatus(position);
                // Exercise the production ChatScreen dispatch path through the same local handler.
                require(InterfaceDebugCommand.handle("/uinterface f3 60 2 es .5 17"), "Debug command interception");
            });
            context.waitTicks(3);
            context.takeScreenshot("unallocated-interface-f3");
            observed(context, "F3");
            context.runOnClient(client -> {
                var real = List.of("XYZ: 0 / 1 / 2", "Block: 0 1 2", "Chunk: 0 0 0", "Facing: south");
                var changed = List.copyOf(InterfaceEffects.debugLines(real));
                require(!changed.getFirst().equals(real.getFirst()) && changed.subList(1, 4).equals(real.subList(1, 4)),
                        "Only the XYZ display may change");
                client.debugEntries.setOverlayVisible(false);
                require(InterfaceEffects.debugTrigger(request(Effect.ADVANCEMENT, 100)), "Unearned advancement toast");
            });
            context.waitTicks(12);
            context.takeScreenshot("unallocated-interface-advancement");
            observed(context, "ADVANCEMENT");
            world.getServer().runCommand("advancement grant @a only minecraft:story/root");
            context.waitFor(client -> client.getConnection().getAdvancements().get(Identifier.withDefaultNamespace("story/mine_stone")) != null);
            context.runOnClient(client -> {
                var advancements = client.getConnection().getAdvancements();
                var progressBefore = advancements.progress().entrySet().stream().collect(java.util.stream.Collectors.toMap(
                        entry -> entry.getKey(), entry -> entry.getValue().isDone()));
                require(InterfaceEffects.debugTrigger(new Request(Effect.ADVANCEMENT, 100, 2, .5,
                        LeakLanguage.ES, 17, null, Identifier.withDefaultNamespace("story/mine_stone"))), "Existing unearned advancement");
                require(advancements.progress().entrySet().stream().allMatch(entry ->
                        progressBefore.get(entry.getKey()) == entry.getValue().isDone()), "Toast must never award actual advancement progress");
            });

            BlockPos sign = context.computeOnClient(client -> client.player.blockPosition().offset(0, 0, 3));
            world.getServer().runCommand("setblock " + sign.getX() + " " + sign.getY() + " " + sign.getZ() + " minecraft:oak_sign[rotation=8]");
            context.waitFor(client -> client.level.getBlockEntity(sign) instanceof SignBlockEntity);
            context.runOnClient(client -> {
                client.player.setYRot(0);
                client.player.setXRot(15);
                require(InterfaceEffects.debugTrigger(new Request(Effect.SIGN, 100, 3, .5, LeakLanguage.MIXED, 17, sign)), "Sign rendering drift");
            });
            context.waitTicks(6);
            context.takeScreenshot("unallocated-interface-sign");
            observed(context, "SIGN");
            world.getServer().runOnServer(server -> {
                var entity = (SignBlockEntity) server.overworld().getBlockEntity(sign);
                require(entity.getText(SignTextSlot.FRONT).getMessages(false).stream().allMatch(line -> line.getString().isEmpty()),
                        "Sign data on the server must stay unchanged");
            });

            for (Effect effect : List.of(Effect.SENSITIVITY, Effect.FOV, Effect.GAMMA)) {
                context.runOnClient(client -> {
                    InterfaceEffects.clear();
                    require(InterfaceEffects.debugTrigger(new Request(effect, 10, 2, 1, LeakLanguage.ES, 17, null)), "Settings effect " + effect);
                    require(InterfaceOptionsGuard.blocksSave(), "Write-ahead journal must protect every vanilla save");
                    client.options.save();
                    assertBytes(client.gameDirectory.toPath(), baselineBytes.get());
                });
                context.waitFor(client -> switch (effect) {
                    case SENSITIVITY -> client.options.sensitivity().get() != baselineValues.get().get(0).doubleValue();
                    case FOV -> client.options.fov().get() != baselineValues.get().get(1).intValue();
                    case GAMMA -> client.options.gamma().get() != baselineValues.get().get(2).doubleValue();
                    default -> false;
                });
                // Wall-clock expiry is required even when the game is paused or runs accelerated test ticks.
                context.waitFor(client -> {
                    if (InterfaceEffects.active(effect)) return false;
                    InterfaceEffects.tick();
                    assertValues(client.options.sensitivity().get(), client.options.fov().get(), client.options.gamma().get(), baselineValues.get());
                    return true;
                }, 600);
            }

            context.runOnClient(client -> {
                require(InterfaceEffects.debugTrigger(request(Effect.FOV, 100)), "FOV before user settings");
                client.gui.setScreen(new OptionsScreen(null, client.options));
                require(!InterfaceEffects.active(Effect.FOV) && !InterfaceOptionsGuard.blocksSave(), "User settings must restore and retire the transaction first");
            });
            context.setScreen(() -> null);
            // Opening settings deliberately allows new user preferences; establish the next byte baseline.
            context.runOnClient(client -> {
                try {
                    baselineBytes.set(Files.readAllBytes(client.gameDirectory.toPath().resolve("options.txt")));
                } catch (Exception failure) { throw new AssertionError(failure); }
                require(InterfaceEffects.debugTrigger(request(Effect.DEATH, 600)), "Arm actual death screen");
            });
            world.getServer().runCommand("kill @a");
            context.waitForScreen(DeathScreen.class);
            context.waitTicks(3);
            context.takeScreenshot("unallocated-interface-death");
            observed(context, "DEATH");
            context.runOnClient(client -> {
                require(InterfaceEffects.debugTrigger(request(Effect.SENSITIVITY, 100)), "Sensitivity before disconnect");
            });
        }
        context.waitForScreen(TitleScreen.class);
        context.runOnClient(client -> {
            require(!InterfaceEffects.active(Effect.SENSITIVITY), "Disconnect must clear settings");
            assertValues(client.options.sensitivity().get(), client.options.fov().get(), client.options.gamma().get(), baselineValues.get());
            client.options.save(); // A late vanilla save must still leave the snapshot byte-for-byte intact.
            assertBytes(client.gameDirectory.toPath(), baselineBytes.get());
            InterfaceEffects.clear();
        });
        LoggerFactory.getLogger("unallocated-smoke").info(
                "UNALLOCATED interface smoke: effects rendered; options restored byte-for-byte after world exit.");
    }

    private static Request request(Effect effect, int ticks) { return new Request(effect, ticks, 2, .5, LeakLanguage.ES, 17, null); }
    private static void observed(ClientGameTestContext context, String effect) {
        context.runOnClient(client -> require(InterfaceEffects.observed(effect) > 0, "Real render hook not reached: " + effect));
    }
    private static void assertValues(double sensitivity, int fov, double gamma, List<Number> expected) {
        require(sensitivity == expected.get(0).doubleValue() && fov == expected.get(1).intValue()
                && gamma == expected.get(2).doubleValue(), "In-memory options did not restore exactly");
    }
    private static void assertBytes(Path directory, byte[] expected) {
        try {
            require(Arrays.equals(expected, Files.readAllBytes(directory.resolve("options.txt"))), "Options bytes changed");
        } catch (Exception failure) { throw new AssertionError("Cannot compare options", failure); }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
