package org.unallocated.map;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.entity.SignText;
import org.unallocated.UnallocatedMod;

/** Real-world checks used after authoring and by CI again after saving/reloading. */
public final class MapVerifier {
    private MapVerifier() {}
    public static void verify(ServerLevel level, String sectionId) {
        var ids = sectionId.equals("all") ? MapBuilder.sectionIds() : java.util.List.of(sectionId);
        for (String id : ids) {
            MapLayout.section(id);
            if (id.equals("lobby")) {
                expect(level, "lobby.rules", Blocks.OAK_WALL_SIGN);
                expect(level, "lobby.motd", Blocks.OAK_WALL_SIGN);
                expect(level, "lobby.route", Blocks.OAK_WALL_SIGN);
                expect(level, "hub.admin_house", Blocks.OAK_DOOR);
                expectAt(level, -27, 64, 20, Blocks.CHEST);
                expectAt(level, -27, 64, 16, Blocks.LECTERN);
                expectAt(level, 12, 74, 24, Blocks.QUARTZ_BLOCK);
                expectAt(level, -13, 64, 2, Blocks.QUARTZ_BLOCK);
                expectAt(level, 15, 68, 0, Blocks.OBSIDIAN);
                expectAt(level, -15, 64, 0, Blocks.OAK_FENCE_GATE);
                expectAt(level, 0, 64, -15, Blocks.OAK_FENCE_GATE);
                expectAt(level, 0, 64, 15, Blocks.OAK_FENCE_GATE);
                signContains(level, new BlockPos(15, 66, 4), "map.unallocated.gate.survival");
                signContains(level, new BlockPos(-29, 67, -16), "map.unallocated.top.title");
                signContains(level, new BlockPos(-30, 66, 11), "map.unallocated.admin.0");
                signContains(level, "lobby.rules", "map.unallocated.rules.3");
                signContains(level, "lobby.motd", "map.unallocated.motd.2");
                signContains(level, "lobby.task", "map.unallocated.task.3");
            }
            if (id.equals("hill")) {
                if (!level.getBlockState(MapLayout.landmark("hill.bait")).isAir()
                        || level.getBlockState(MapLayout.landmark("hill.bait").below()).getBlock() != Blocks.COARSE_DIRT) {
                    throw new IllegalStateException("Missing clear hill bait foothold");
                }
            }
            if (id.equals("path") && level.getBlockState(MapLayout.landmark("path.campfire")).getBlock() != Blocks.CAMPFIRE) {
                throw new IllegalStateException("Missing abandoned path rest stop");
            }
            if (id.equals("path")) {
                expectAt(level, 23, 64, 22, Blocks.OAK_DOOR);
                expectAt(level, 41, 64, 23, Blocks.OAK_DOOR);
                expectAt(level, 24, 64, 38, Blocks.OAK_DOOR);
                expectAt(level, 20, 63, 52, Blocks.FARMLAND);
                expectAt(level, 45, 64, 45, Blocks.WOOL.blue());
                signContains(level, new BlockPos(20, 65, 21), "map.unallocated.claim.noor");
                signContains(level, new BlockPos(38, 65, 22), "map.unallocated.claim.zuri");
                signContains(level, new BlockPos(21, 65, 37), "map.unallocated.claim.efe");
                signContains(level, new BlockPos(46, 66, 53), "map.unallocated.arena.0");
            }
            if (id.equals("base")) {
                expect(level, "base.chest", Blocks.CHEST);
                expect(level, "base.bed", Blocks.BED.red());
                expect(level, "base.door", Blocks.OAK_DOOR);
                if (!(level.getBlockEntity(MapLayout.landmark("base.chest")) instanceof net.minecraft.world.Container container)
                        || container.getItem(2).getItem() != net.minecraft.world.item.Items.COAL || container.getItem(2).getCount() != 11) {
                    throw new IllegalStateException("Missing base supplies");
                }
            }
            if (id.equals("mine")) {
                expect(level, "mine.lintel", Blocks.STRIPPED_OAK_LOG);
                expect(level, "mine.end", Blocks.STONE);
            }
            for (var entry : MapLayout.checkpoints().entrySet()) {
                var cp = entry.getValue();
                if (!cp.section().equals(id)) continue;
                if (!level.getBlockState(cp.pos()).isAir() || !level.getBlockState(cp.pos().above()).isAir()
                        || !level.getBlockState(cp.pos().below()).isSolid()) {
                    throw new IllegalStateException("Unsafe checkpoint: " + entry.getKey());
                }
                UnallocatedMod.LOGGER.info("UNALLOCATED map checkpoint={} verified", entry.getKey());
            }
        }
        var server = level.getServer();
        var rules = server.getGameRules();
        var mismatches = new java.util.ArrayList<String>();
        expectSetting(mismatches, "spawn_mobs", false, rules.get(GameRules.SPAWN_MOBS));
        expectSetting(mismatches, "advance_time", false, rules.get(GameRules.ADVANCE_TIME));
        expectSetting(mismatches, "advance_weather", false, rules.get(GameRules.ADVANCE_WEATHER));
        expectSetting(mismatches, "keep_inventory", true, rules.get(GameRules.KEEP_INVENTORY));
        expectSetting(mismatches, "respawn_radius", 0, rules.get(GameRules.RESPAWN_RADIUS));
        expectSetting(mismatches, "gamemode", GameType.ADVENTURE, server.getDefaultGameType());
        expectSetting(mismatches, "difficulty", Difficulty.NORMAL, server.getWorldData().getDifficulty());
        var spawn = MapLayout.checkpoint("lobby.spawn");
        expectSetting(mismatches, "world_spawn", LevelData.RespawnData.of(level.dimension(), spawn.pos(), spawn.yaw(), spawn.pitch()),
            server.getRespawnData());
        if (!mismatches.isEmpty()) {
            String report = "Map settings differ from D-17: " + String.join("; ", mismatches);
            UnallocatedMod.LOGGER.error("UNALLOCATED {}", report);
            throw new IllegalStateException(report);
        }
        UnallocatedMod.LOGGER.info("UNALLOCATED map verified section={}", sectionId);
    }
    private static void expectSetting(java.util.List<String> mismatches, String name, Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) mismatches.add(name + " expected=" + expected + " found=" + actual);
    }
    /** CI authoring check: restoring one section must repair damage and preserve every other section. */
    public static void verifyRegeneration(ServerLevel level) {
        verify(level, "all");
        long expected = fingerprint(level);
        long itemsBefore = droppedItems(level);
        for (String id : MapBuilder.sectionIds()) {
            var cp = MapLayout.checkpoints().values().stream().filter(point -> point.section().equals(id)).findFirst().orElseThrow();
            MapWriter.place(level, cp.pos(), Blocks.STONE.defaultBlockState());
            MapBuilder.build(level, id);
            if (fingerprint(level) != expected) throw new IllegalStateException("Section regeneration changed the map: " + id);
        }
        MapBuilder.build(level, "all");
        if (fingerprint(level) != expected) throw new IllegalStateException("Full regeneration changed the map");
        if (droppedItems(level) != itemsBefore) throw new IllegalStateException("Regeneration dropped container contents");
        UnallocatedMod.LOGGER.info("UNALLOCATED map regeneration verified sections={}", MapBuilder.sectionIds().size());
    }
    private static long droppedItems(ServerLevel level) {
        long count = 0;
        for (var entity : level.getAllEntities()) if (entity instanceof net.minecraft.world.entity.item.ItemEntity) count++;
        return count;
    }
    private static long fingerprint(ServerLevel level) {
        long hash = 0xcbf29ce484222325L;
        var b = MapLayout.ISLAND;
        for (var pos : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
            var state = level.getBlockState(pos);
            hash = (hash ^ Block.getId(state)) * 0x100000001b3L;
            if (!state.hasBlockEntity()) continue;
            var entity = level.getBlockEntity(pos);
            if (entity instanceof SignBlockEntity sign) {
                for (var slot : SignTextSlot.values()) for (var line : sign.getText(slot).getMessages(false)) {
                    hash = (hash ^ line.getString().hashCode()) * 0x100000001b3L;
                }
                hash = (hash ^ (sign.isWaxed() ? 1 : 0)) * 0x100000001b3L;
            }
            if (entity instanceof net.minecraft.world.Container container) for (int slot = 0; slot < container.getContainerSize(); slot++) {
                var stack = container.getItem(slot);
                hash = (hash ^ stack.getItem().getDescriptionId().hashCode()) * 0x100000001b3L;
                hash = (hash ^ stack.getCount()) * 0x100000001b3L;
            }
        }
        return hash;
    }
    private static void expect(ServerLevel level, String id, Block expected) {
        if (level.getBlockState(MapLayout.landmark(id)).getBlock() != expected) {
            throw new IllegalStateException("Wrong map block at " + id);
        }
    }
    private static void expectAt(ServerLevel level, int x, int y, int z, Block expected) {
        var pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).getBlock() != expected) throw new IllegalStateException("Wrong map block at " + pos);
    }
    private static void signContains(ServerLevel level, String id, String expected) {
        signContains(level, MapLayout.landmark(id), expected);
    }
    private static void signContains(ServerLevel level, BlockPos pos, String expected) {
        var entity = level.getBlockEntity(pos);
        String expectation = "Wrong map sign at " + pos + "; expected FRONT/unfiltered line=\"" + expected + "\", waxed=true";
        if (!(entity instanceof SignBlockEntity sign)) {
            throw new IllegalStateException(expectation + "; found block=" + level.getBlockState(pos) + ", entity=" + entity);
        }
        var front = sign.getText(SignTextSlot.FRONT);
        var back = sign.getText(SignTextSlot.BACK);
        if (!sign.isWaxed() || !signLines(front, false).contains(expected)) {
            throw new IllegalStateException(expectation + "; found waxed=" + sign.isWaxed()
                + ", front/unfiltered=" + signLines(front, false) + ", front/filtered=" + signLines(front, true)
                + ", back/unfiltered=" + signLines(back, false) + ", back/filtered=" + signLines(back, true));
        }
    }
    private static java.util.List<String> signLines(SignText text, boolean filtered) {
        // Lines are language keys; compare the key itself so the result never depends on a loaded language.
        return text.getMessages(filtered).stream().map(line -> line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents key
            ? key.getKey() : line.getString()).toList();
    }
}
