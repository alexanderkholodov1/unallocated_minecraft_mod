package org.unallocated.map;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
        if (sectionId.equals("all")) {
            UnallocatedMod.LOGGER.info("UNALLOCATED map build section=all phase=clear");
            clear(level, MapLayout.ISLAND);
            UnallocatedMod.LOGGER.info("UNALLOCATED map build section=all phase=terrain");
            terrain(level);
            for (var section : SECTIONS.values()) buildSection(level, section);
        } else buildSection(level, SECTIONS.get(sectionId));
        UnallocatedMod.LOGGER.info("UNALLOCATED map build section={} phase=configure", sectionId);
        configure(level);
        UnallocatedMod.LOGGER.info("UNALLOCATED map build section={} phase=verify", sectionId);
        MapVerifier.verify(level, sectionId);
        UnallocatedMod.LOGGER.info("UNALLOCATED map built section={}", sectionId);
    }
    private static void buildSection(ServerLevel level, MapSection section) {
        UnallocatedMod.LOGGER.info("UNALLOCATED map section={} phase=clear", section.id());
        clear(level, MapLayout.section(section.id()).bounds());
        UnallocatedMod.LOGGER.info("UNALLOCATED map section={} phase=place", section.id());
        section.build(new MapWriter(level, MapLayout.section(section.id())));
        UnallocatedMod.LOGGER.info("UNALLOCATED map section={} phase=shapes", section.id());
        var b = MapLayout.section(section.id()).bounds();
        for (var pos : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
            var state = level.getBlockState(pos);
            if (!state.isAir()) MapWriter.place(level, pos, Block.updateFromNeighbourShapes(state, level, pos));
        }
        UnallocatedMod.LOGGER.info("UNALLOCATED map section={} phase=complete", section.id());
    }
    private static void clear(ServerLevel level, MapLayout.BlockBox box) {
        if (!level.isInsideBuildHeight(box.minY()) || !level.isInsideBuildHeight(box.maxY())) {
            throw new IllegalArgumentException("Map box exceeds dimension build height: " + box
                + ", minY=" + level.getMinY() + ", height=" + level.getHeight());
        }
        for (var pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (!level.getBlockState(pos).isAir()) MapWriter.place(level, pos, Blocks.AIR.defaultBlockState());
        }
    }
    private static void terrain(ServerLevel level) {
        var b = MapLayout.ISLAND;
        double centerX = (b.minX() + b.maxX()) / 2.0;
        double radiusX = (b.maxX() - b.minX()) / 2.0;
        double radiusZ = (b.maxZ() - b.minZ()) / 2.0;
        for (int x = b.minX(); x <= b.maxX(); x++) for (int z = b.minZ(); z <= b.maxZ(); z++) {
            double edge = Math.pow((x - centerX) / radiusX, 2) + Math.pow(z / radiusZ, 2);
            if (edge > 0.95 + 0.025 * Math.sin(x * 0.4) * Math.cos(z * 0.3)) continue;
            int bottom = b.minY() + (int) (edge * 4);
            for (int y = bottom; y <= MapLayout.GROUND_Y; y++) {
                var block = y == MapLayout.GROUND_Y ? Blocks.GRASS_BLOCK : y >= MapLayout.GROUND_Y - 2 ? Blocks.DIRT : Blocks.STONE;
                MapWriter.place(level, new BlockPos(x, y, z), block.defaultBlockState());
            }
        }
        for (var root : MapLayout.TREES) {
            for (int y = 3; y <= 5; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 && Math.abs(z) == 2) continue;
                MapWriter.place(level, root.offset(x, y, z), Blocks.OAK_LEAVES.defaultBlockState()
                    .setValue(BlockStateProperties.PERSISTENT, true));
            }
            for (int y = 0; y <= 4; y++) MapWriter.place(level, root.above(y), Blocks.OAK_LOG.defaultBlockState());
            MapWriter.place(level, root.above(6), Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true));
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
        server.setDifficulty(Difficulty.NORMAL, true);
        server.setDefaultGameType(GameType.ADVENTURE);
        server.enforceGameTypeForPlayers(GameType.ADVENTURE);
        var spawn = MapLayout.checkpoint("lobby.spawn");
        server.setRespawnData(LevelData.RespawnData.of(level.dimension(), spawn.pos(), spawn.yaw(), spawn.pitch()));
        // Commands use the 26.3 world-clock/weather implementation rather than deprecated dayTime APIs.
        var source = server.createCommandSourceStack();
        server.getCommands().performPrefixedCommand(source, "time set 11000");
        server.getCommands().performPrefixedCommand(source, "weather clear");
    }
}
