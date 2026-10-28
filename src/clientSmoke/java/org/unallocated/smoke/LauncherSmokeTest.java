package org.unallocated.smoke;

import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import org.unallocated.client.launcher.LauncherClient;

/** Stable regression worlds exercise layout, continue and new without depending on authored beats. */
final class LauncherSmokeTest {
    static void run(ClientGameTestContext context) {
        int original = context.computeOnClient(client -> client.options.guiScale().get());
        try {
            for (int scale : new int[] {1, 2, 3, 4, 0}) {
                context.runOnClient(client -> {
                    client.options.guiScale().set(scale); client.resizeGui();
                    client.gui.setScreen(new TitleScreen());
                    var screen = client.gui.screen();
                    List<AbstractWidget> widgets = Screens.getWidgets(screen).stream().filter(w -> w.visible).toList();
                    var play = widgets.stream().filter(w -> w.getMessage().getString().equals(
                        Component.translatable("menu.unallocated.play").getString())).findFirst().orElseThrow();
                    var single = widgets.stream().filter(w -> w.getMessage().getString().equals(
                        Component.translatable("menu.singleplayer").getString())).findFirst().orElseThrow();
                    if (play.getBottom() > single.getY()) throw new AssertionError("Launcher is not above Singleplayer");
                    for (var widget : widgets) {
                        if (widget.getX() < 0 || widget.getY() < 0 || widget.getRight() > screen.width
                            || widget.getBottom() > screen.height) throw new AssertionError("Title widget outside GUI at scale " + scale);
                        for (var other : widgets) {
                            if (widget != other && widget.getX() < other.getRight() && widget.getRight() > other.getX()
                                && widget.getY() < other.getBottom() && widget.getBottom() > other.getY()) {
                                throw new AssertionError("Title widgets overlap at scale " + scale + ": "
                                    + widget.getMessage().getString() + " / " + other.getMessage().getString());
                            }
                        }
                    }
                });
            }
        } finally {
            context.runOnClient(client -> { client.options.guiScale().set(original); client.resizeGui(); client.gui.setScreen(new TitleScreen()); });
        }
        List<Path> copies = new ArrayList<>();
        try {
            context.clickScreenButton("menu.unallocated.play");
            Path first = opened(context); copies.add(first);
            quit(context);
            context.clickScreenButton("menu.unallocated.play"); context.waitForScreen(ConfirmScreen.class);
            context.clickScreenButton("menu.unallocated.continue.yes");
            if (!opened(context).equals(first)) throw new AssertionError("Continue opened another save");
            quit(context);
            context.clickScreenButton("menu.unallocated.play"); context.waitForScreen(ConfirmScreen.class);
            context.clickScreenButton("menu.unallocated.continue.new");
            Path second = opened(context); copies.add(second);
            if (second.equals(first) || !second.getFileName().toString().equals("UNALLOCATED (2)")) {
                throw new AssertionError("New session must preserve the first copy and choose the next free folder");
            }
            quit(context);
            for (Path copy : copies) {
                var state = LauncherClient.readSession(copy);
                if (!state.getBooleanOr("armed", false) || !state.getBooleanOr("started", false)
                    || state.getBooleanOr("finished", true)) throw new AssertionError("Quit did not preserve an unfinished session");
            }
        } catch (java.io.IOException exception) { throw new AssertionError(exception); }
        finally {
            for (Path copy : copies) {
                try (var paths = Files.walk(copy)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
                } catch (java.io.IOException exception) { throw new AssertionError("Could not remove launcher regression copy", exception); }
            }
        }
        org.slf4j.LoggerFactory.getLogger("unallocated-smoke").info(
            "UNALLOCATED smoke: launcher GUI scales, continue and new copies verified");
    }
    private static Path opened(ClientGameTestContext context) {
        context.waitFor(client -> client.player != null && client.level != null && client.gui.screen() == null);
        return context.computeOnClient(client -> client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize());
    }
    private static void quit(ClientGameTestContext context) {
        context.runOnClient(client -> client.disconnect(new TitleScreen(), false));
        context.waitForScreen(TitleScreen.class);
        context.waitFor(client -> client.getSingleplayerServer() == null && client.level == null);
    }
}
