package org.unallocated.map;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
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
            if (id.equals("path") && level.getBlockState(MapLayout.section("path").at(8, 0, 14)).getBlock() != Blocks.CAMPFIRE) {
                throw new IllegalStateException("Missing abandoned path rest stop");
            }
            if (id.equals("base")) {
                expect(level, "base.chest", Blocks.CHEST);
                expect(level, "base.bed", Blocks.RED_BED);
                expect(level, "base.door", Blocks.OAK_DOOR);
                if (!(level.getBlockEntity(MapLayout.landmark("base.chest")) instanceof net.minecraft.world.Container container)
                        || container.getItem(2).getItem() != net.minecraft.world.item.Items.COAL || container.getItem(2).getCount() != 11) {
                    throw new IllegalStateException("Missing base supplies");
                }
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
        UnallocatedMod.LOGGER.info("UNALLOCATED map verified section={}", sectionId);
    }
    private static void expect(ServerLevel level, String id, Block expected) {
        if (level.getBlockState(MapLayout.landmark(id)).getBlock() != expected) {
            throw new IllegalStateException("Wrong map block at " + id);
        }
    }
    private static void signContains(ServerLevel level, String id, String expected) {
        if (!(level.getBlockEntity(MapLayout.landmark(id)) instanceof SignBlockEntity sign)
                || !sign.isWaxed() || sign.getText(SignTextSlot.FRONT).getMessages(false).stream()
                    .anyMatch(line -> line.getString().equals(expected))) {
            throw new IllegalStateException("Wrong map sign at " + id);
        }
    }
}
