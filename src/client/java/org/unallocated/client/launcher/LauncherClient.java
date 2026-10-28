package org.unallocated.client.launcher;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.*;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import org.unallocated.UnallocatedMod;

/** The same bundled, pre-armed save is opened by judges and CI. No world authoring on the client. */
public final class LauncherClient {
    private LauncherClient() { }
    public static void initialize() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof TitleScreen)) return;
            var widgets = Screens.getWidgets(screen);
            String singleplayer = Component.translatable("menu.singleplayer").getString();
            var single = widgets.stream().filter(w -> w.getMessage().getString().equals(singleplayer)).findFirst();
            if (single.isEmpty()) return;
            var anchor = single.get(); int top = anchor.getY();
            // Reserve a row above Singleplayer. Fit the existing central buttons within the footer.
            for (var widget : widgets) {
                if (widget.getY() >= top && widget.getX() >= width / 2 - 110 && widget.getX() <= width / 2 + 110) {
                    widget.setY(Math.min(widget.getY() + 12, height - 30 - widget.getHeight()));
                }
            }
            widgets.add(Button.builder(Component.translatable("menu.unallocated.play"), button -> begin(client))
                .bounds(anchor.getX(), top - 12, anchor.getWidth(), 20).build());
        });
    }

    public static Path sessionFile(Path world) throws IOException {
        Path data = world.resolve("data");
        if (!Files.isDirectory(data)) throw new IOException("Missing world data");
        try (var paths = Files.walk(data, 3)) {
            var sessions = paths.filter(p -> p.getFileName().toString().equals("unallocated_session.dat")).toList();
            if (sessions.size() != 1) throw new IOException("Expected one session marker");
            return sessions.getFirst();
        }
    }

    public static CompoundTag readSession(Path world) throws IOException {
        return NbtIo.readCompressed(sessionFile(world), NbtAccounter.create(16 * 1024 * 1024))
            .getCompoundOrEmpty("data");
    }

    private static Path unfinished(Path saves) throws IOException {
        if (!Files.isDirectory(saves)) return null;
        List<Path> worlds;
        try (var paths = Files.list(saves)) {
            worlds = paths.filter(p -> Files.isDirectory(p) && !Files.isSymbolicLink(p)
                && p.getFileName().toString().matches("UNALLOCATED(?: \\(\\d+\\))?"))
                .sorted(Comparator.comparingLong(LauncherClient::modified).reversed()).toList();
        }
        for (Path world : worlds) {
            try {
                var state = readSession(world);
                if (state.getBooleanOr("armed", false) && !state.getBooleanOr("finished", false)) return world;
            } catch (IOException exception) {
                UnallocatedMod.LOGGER.warn("UNALLOCATED launcher: unreadable save marker in {}", world, exception);
            }
        }
        return null;
    }
    private static long modified(Path path) {
        try { return Files.getLastModifiedTime(sessionFile(path)).toMillis(); }
        catch (IOException exception) { return 0; }
    }

    private static void begin(Minecraft client) {
        client.gui.setScreen(new GenericMessageScreen(Component.translatable("menu.unallocated.connecting")));
        CompletableFuture.supplyAsync(() -> {
            try { return unfinished(client.gameDirectory.toPath().resolve("saves")); }
            catch (Exception exception) { throw new java.util.concurrent.CompletionException(exception); }
        }).whenComplete((world, error) -> client.execute(() -> {
            if (error != null) { fail(client, error); return; }
            if (world == null) { copyAndOpen(client); return; }
            client.gui.setScreen(new ConfirmScreen(keep -> {
                if (keep) open(client, world); else copyAndOpen(client);
            }, Component.translatable("menu.unallocated.continue.title"),
               Component.translatable("menu.unallocated.continue.message"),
               Component.translatable("menu.unallocated.continue.yes"),
               Component.translatable("menu.unallocated.continue.new")));
        }));
    }

    private static void copyAndOpen(Minecraft client) {
        client.gui.setScreen(new GenericMessageScreen(Component.translatable("menu.unallocated.connecting")));
        var visible = CompletableFuture.runAsync(() -> { }, CompletableFuture.delayedExecutor(1, java.util.concurrent.TimeUnit.SECONDS));
        CompletableFuture.supplyAsync(() -> {
            try { return copyTemplate(client.gameDirectory.toPath().resolve("saves")); }
            catch (Exception exception) { throw new java.util.concurrent.CompletionException(exception); }
        }).thenCombine(visible, (world, ignored) -> world).whenComplete((world, error) -> client.execute(() -> {
            if (error != null) fail(client, error); else open(client, world);
        }));
    }

    private static Path copyTemplate(Path saves) throws IOException {
        Files.createDirectories(saves);
        Path world = null;
        for (int i = 1; world == null; i++) {
            Path candidate = saves.resolve(i == 1 ? "UNALLOCATED" : "UNALLOCATED (" + i + ")");
            try { Files.createDirectory(candidate); world = candidate; }
            catch (FileAlreadyExistsException ignored) { }
        }
        try (InputStream resource = LauncherClient.class.getResourceAsStream("/unallocated/template/world.zip")) {
            if (resource == null) throw new IOException("Missing bundled world template");
            try (ZipInputStream zip = new ZipInputStream(resource)) {
                Set<Path> seen = new HashSet<>(); long total = 0;
                for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                    String name = entry.getName();
                    if (!name.startsWith("UNALLOCATED/") || name.contains("\\")) throw new IOException("Invalid template path");
                    String relative = name.substring("UNALLOCATED/".length());
                    Path target = world.resolve(relative).normalize();
                    if (!target.startsWith(world) || !seen.add(target)) throw new IOException("Unsafe or duplicate template path");
                    if (entry.isDirectory()) { Files.createDirectories(target); continue; }
                    Files.createDirectories(target.getParent());
                    try (OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                        byte[] buffer = new byte[8192]; int count;
                        while ((count = zip.read(buffer)) != -1) {
                            total += count;
                            if (total > 256L * 1024 * 1024) throw new IOException("Template exceeds extraction limit");
                            output.write(buffer, 0, count);
                        }
                    }
                }
            }
            var state = readSession(world);
            var level = NbtIo.readCompressed(world.resolve("level.dat"), NbtAccounter.create(16 * 1024 * 1024)).getCompoundOrEmpty("Data");
            if (!state.getBooleanOr("armed", false) || state.getBooleanOr("started", false)
                || state.getBooleanOr("finished", false) || level.getIntOr("GameType", -1) != 2
                || level.getBooleanOr("allowCommands", true)) throw new IOException("Invalid release template state");
            return world;
        } catch (Exception exception) {
            try (var paths = Files.walk(world)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            } catch (IOException cleanup) { exception.addSuppressed(cleanup); }
            if (exception instanceof IOException io) throw io;
            throw new IOException("Could not copy template", exception);
        }
    }

    private static void open(Minecraft client, Path world) {
        try {
            client.createWorldOpenFlows().openWorld(world.getFileName().toString(), () -> client.gui.setScreen(new TitleScreen()));
        } catch (Exception exception) { fail(client, exception); }
    }
    private static void fail(Minecraft client, Throwable error) {
        UnallocatedMod.LOGGER.error("UNALLOCATED launcher failed", error);
        client.gui.setScreen(new DisconnectedScreen(new TitleScreen(), Component.translatable("menu.unallocated.play"),
            Component.translatable("menu.unallocated.error")));
    }
}
