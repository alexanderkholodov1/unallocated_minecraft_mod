package org.unallocated.map;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelData;
import org.unallocated.UnallocatedMod;

/** Explicit authoring operation only: ordinary worlds are never built or reconfigured on startup. */
public final class MapBuilder {
    private static final Map<String, MapSection> SECTIONS = new LinkedHashMap<>();
    static {
        register(new LobbySection()); register(new HillSection()); register(new PathSection());
        register(new BaseSection()); register(new MineSection());
    }
    private MapBuilder() {}
    private static void register(MapSection section) { SECTIONS.put(section.id(), section); }
    public static List<String> sectionIds() { return List.copyOf(SECTIONS.keySet()); }
    public static void build(ServerLevel level, String sectionId) {
        if (!sectionId.equals("all") && !SECTIONS.containsKey(sectionId)) {
            throw new IllegalArgumentException("Unknown map section: " + sectionId);
        }
        var writers = new ArrayList<MapWriter>();
        if (sectionId.equals("all")) {
            for (var section : SECTIONS.values()) writers.add(buildSection(level, section));
        } else writers.add(buildSection(level, SECTIONS.get(sectionId)));
        UnallocatedMod.LOGGER.info("UNALLOCATED map build section={} phase=configure", sectionId);
        clearSightline(level);
        configure(level);
        writers.forEach(MapWriter::finishSigns);
        UnallocatedMod.LOGGER.info("UNALLOCATED map build section={} phase=verify", sectionId);
        MapVerifier.verify(level, sectionId);
        UnallocatedMod.LOGGER.info("UNALLOCATED map built section={}", sectionId);
    }
    private static MapWriter buildSection(ServerLevel level, MapSection section) {
        UnallocatedMod.LOGGER.info("UNALLOCATED map section={} phase=clear", section.id());
        MapWriter.prepare(level, MapLayout.section(section.id()));
        UnallocatedMod.LOGGER.info("UNALLOCATED map section={} phase=place", section.id());
        var writer = new MapWriter(level, MapLayout.section(section.id()));
        section.build(writer);
        UnallocatedMod.LOGGER.info("UNALLOCATED map section={} phase=shapes", section.id());
        var b = MapLayout.section(section.id()).bounds();
        for (var pos : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
            var state = level.getBlockState(pos);
            if (!state.isAir()) MapWriter.place(level, pos, Block.updateFromNeighbourShapes(state, level, pos));
        }
        UnallocatedMod.LOGGER.info("UNALLOCATED map section={} phase=complete", section.id());
        return writer;
    }
    static int sightlineCeiling(int x, int z) {
        var start = MapLayout.checkpoint("lobby.spawn").pos();
        var end = MapLayout.landmark("hill.bait");
        int ceiling = Integer.MAX_VALUE;
        for (int step = 0; step <= 84; step++) {
            double t = step / 84.0;
            int rayX = (int) Math.round(start.getX() + (end.getX() - start.getX()) * t);
            int rayZ = (int) Math.round(start.getZ() + (end.getZ() - start.getZ()) * t);
            if (Math.abs(x - rayX) <= 1 && Math.abs(z - rayZ) <= 1) {
                ceiling = Math.min(ceiling, (int) Math.ceil(65.6 + (73.6 - 65.6) * t) - 1);
            }
        }
        return ceiling;
    }
    /** Clear only the viewing corridor above the rising eye-to-bait ray, preserving the hill. */
    private static void clearSightline(ServerLevel level) {
        var start = MapLayout.checkpoint("lobby.spawn").pos();
        var end = MapLayout.landmark("hill.bait");
        for (int step = 0; step <= 84; step++) {
            double t = step / 84.0;
            int x = (int) Math.round(start.getX() + (end.getX() - start.getX()) * t);
            int z = (int) Math.round(start.getZ() + (end.getZ() - start.getZ()) * t);
            int bottom = (int) Math.ceil(65.6 + (73.6 - 65.6) * t);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                for (int y = bottom; y < level.getMinY() + level.getHeight(); y++) {
                    var pos = new BlockPos(x + dx, y, z + dz);
                    if (!level.getBlockState(pos).isAir()) MapWriter.place(level, pos, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }
    private static void configure(ServerLevel level) {
        var server = level.getServer();
        var rules = server.getGameRules();
        rules.set(GameRules.SPAWN_MOBS, false, server);
        rules.set(GameRules.ADVANCE_TIME, false, server);
        rules.set(GameRules.ADVANCE_WEATHER, false, server);
        rules.set(GameRules.KEEP_INVENTORY, true, server);
        rules.set(GameRules.RESPAWN_RADIUS, 0, server);
        if (server instanceof DedicatedServer dedicated) {
            // Dedicated startup overrides level.dat from server.properties; persist both sources.
            dedicated.setDifficulty(Difficulty.NORMAL);
            dedicated.setGameMode(GameType.ADVENTURE);
        } else {
            server.setDifficulty(Difficulty.NORMAL, true);
            server.setDefaultGameType(GameType.ADVENTURE);
        }
        server.enforceGameTypeForPlayers(GameType.ADVENTURE);
        var spawn = MapLayout.checkpoint("lobby.spawn");
        server.setRespawnData(LevelData.RespawnData.of(level.dimension(), spawn.pos(), spawn.yaw(), spawn.pitch()));
        // Commands use the 26.3 world-clock/weather implementation rather than deprecated dayTime APIs.
        var source = server.createCommandSourceStack();
        server.getCommands().performPrefixedCommand(source, "time set 11000");
        server.getCommands().performPrefixedCommand(source, "weather clear");
    }
}
