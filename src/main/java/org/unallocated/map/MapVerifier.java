package org.unallocated.map;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.Difficulty;
import net.minecraft.resources.Identifier;
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
                signContains(level, "lobby.rules", "Cerrar las puetas");
                signContains(level, "lobby.motd", "7 / 20 conectados");
                signContains(level, "lobby.task", "el catalejo.");
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
        var rules = level.getServer().getGameRules();
        if (rules.get(GameRules.SPAWN_MOBS) || rules.get(GameRules.ADVANCE_TIME) || rules.get(GameRules.ADVANCE_WEATHER)
                || !rules.get(GameRules.KEEP_INVENTORY) || rules.get(GameRules.RESPAWN_RADIUS) != 0
                || level.getServer().getDefaultGameType() != GameType.ADVENTURE
                || level.getServer().getWorldData().getDifficulty() != Difficulty.NORMAL
                || !level.getServer().getRespawnData().pos().equals(MapLayout.checkpoint("lobby.spawn").pos())) {
            throw new IllegalStateException("Map rules or world spawn differ from D-17");
        }
        UnallocatedMod.LOGGER.info("UNALLOCATED map verified section={}", sectionId);
    }
    /** CI authoring check: restoring one section must repair damage and preserve every other section. */
    public static void verifyRegeneration(ServerLevel level) {
        verify(level, "all");
        if (!level.getBiome(MapLayout.checkpoint("lobby.spawn").pos()).is(Identifier.parse(MapLayout.BIOME_ID))) {
            throw new IllegalStateException("CI/template world must use the lobby biome");
        }
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
    private static void signContains(ServerLevel level, String id, String expected) {
        var pos = MapLayout.landmark(id);
        var entity = level.getBlockEntity(pos);
        String expectation = "Wrong map sign at " + id + " " + pos + "; expected FRONT/unfiltered line=\"" + expected + "\", waxed=true";
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
        return text.getMessages(filtered).stream().map(line -> line.getString()).toList();
    }
}
