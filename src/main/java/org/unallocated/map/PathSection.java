package org.unallocated.map;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** A player-made track, with a short northern branch to the hill and a bend to the base. */
public final class PathSection implements MapSection {
    @Override public String id() { return "path"; }
    @Override public void build(MapWriter w) {
        w.ground(0, -10, 33, 60);
        for (int x = 0; x <= 33; x++) {
            int centerZ = x < 14 ? 0 : Math.min(12, x - 14);
            for (int dz = -2; dz <= 2; dz++) {
                int z = centerZ + dz;
                var block = Math.floorMod(x * 13 + z * 7, 11) == 0 ? Blocks.DIRT_PATH : Blocks.GRAVEL;
                w.put(x, -1, z, block);
            }
        }
        for (int z = -10; z <= 0; z++) for (int x = 16; x <= 18; x++) w.put(x, -1, z, Blocks.GRAVEL);
        // The checkpoint keeps a full-height solid foothold even where dirt paths are recessed.
        w.put(17, -1, 3, Blocks.GRAVEL);
        // Stable torches, named by local route order for the later Act 1 guidance effect.
        for (int i = 0; i < 5; i++) {
            var pos = MapLayout.landmark("path.torch." + i);
            var origin = MapLayout.section(id());
            w.put(pos.getX() - origin.x(), 0, pos.getZ() - origin.z(), Blocks.COBBLESTONE);
            w.put("path.torch." + i, Blocks.TORCH.defaultBlockState());
        }
        w.fill(26, 0, 7, 26, 2, 7, Blocks.OAK_LOG);
        w.sign("path.route", Direction.WEST, "map.unallocated.route.0", "map.unallocated.route.1", "map.unallocated.route.2", "map.unallocated.route.3");
        // A small rest stop, used and left untidily; no diaries or horror set dressing.
        w.fill(6, -1, 12, 11, -1, 17, Blocks.COARSE_DIRT);
        w.put("path.campfire", Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false));
        w.fill(6, 0, 16, 10, 0, 16, Blocks.OAK_LOG);
        w.put(11, 0, 13, Blocks.BARREL);
        w.put(5, 0, 12, Blocks.COBBLESTONE);
        w.put(7, 0, 12, Blocks.COBBLESTONE);
        // A narrow lane branches from the old survival road through individually claimed builds.
        w.fill(13, -1, 12, 15, -1, 59, Blocks.GRAVEL);
        w.fill(6, -1, 19, 27, -1, 21, Blocks.DIRT_PATH);
        w.fill(7, -1, 35, 27, -1, 37, Blocks.GRAVEL);
        w.fill(14, -1, 48, 31, -1, 50, Blocks.DIRT_PATH);
        house(w, 2, 22, 10, 30, Blocks.OAK_PLANKS, Blocks.SPRUCE_STAIRS, "noor", false);
        house(w, 20, 23, 29, 32, Blocks.STONE_BRICKS, Blocks.QUARTZ_BLOCK, "zuri", false);
        house(w, 3, 38, 12, 47, Blocks.BIRCH_PLANKS, Blocks.OAK_PLANKS, "efe", true);
        farm(w);
        arena(w);
    }
    private static void house(MapWriter w, int x1, int z1, int x2, int z2, Block wall, Block roof,
                              String player, boolean unfinished) {
        w.fill(x1, -1, z1, x2, -1, z2, Blocks.COBBLESTONE);
        w.fill(x1, 0, z1, x2, 2, z1, wall);
        w.fill(x1, 0, z1, x1, 2, z2, wall);
        if (!unfinished) {
            w.fill(x2, 0, z1, x2, 2, z2, wall);
            w.fill(x1, 0, z2, x2, 2, z2, wall);
            w.fill(x1 - 1, 3, z1 - 1, x2 + 1, 3, z2 + 1, roof);
            w.fill(x1, 1, z1 + 2, x1, 1, z1 + 4, Blocks.GLASS_PANE);
            w.fill(x2, 1, z1 + 2, x2, 1, z1 + 4, Blocks.GLASS_PANE);
            w.fill(x1 + 2, 0, z2, x1 + 3, 1, z2, Blocks.AIR);
            w.put(x2 - 1, 0, z2 - 1, Blocks.CRAFTING_TABLE);
            w.put(x1 + 1, 0, z2 - 1, Blocks.BARREL);
        } else {
            w.fill(x2, 0, z1, x2, 0, z2 - 2, wall);
            w.fill(x1 + 1, 0, z2 - 1, x1 + 3, 0, z2 - 1, Blocks.OAK_LOG);
            w.put(x1 + 2, 1, z2 - 1, Blocks.OAK_LOG);
        }
        int doorX = (x1 + x2) / 2;
        var door = Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        w.put(doorX, 0, z1, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        w.put(doorX, 1, z1, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        w.sign(x1 + 1, 1, z1 - 1, Direction.NORTH, "map.unallocated.claim." + player,
            "map.unallocated.claim." + player + ".detail", "", "");
        w.fill(doorX, -1, z1 - 2, doorX, -1, z1 - 1, Blocks.DIRT_PATH);
    }
    private static void farm(MapWriter w) {
        // Dry rain-fed wheat: no irrigation holes or water, and open ends between the rows.
        w.fill(2, -1, 51, 11, -1, 58, Blocks.COARSE_DIRT);
        for (int x : new int[] {3, 5, 7, 9}) for (int z = 52; z <= 57; z++) {
            w.put(x, -1, z, Blocks.FARMLAND);
            w.put(x, 0, z, Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7));
        }
        w.fill(11, 0, 51, 11, 2, 51, Blocks.OAK_LOG);
        w.sign(11, 1, 50, Direction.NORTH, "map.unallocated.farm.0", "map.unallocated.farm.1", "", "");
        w.put(2, 0, 58, Blocks.HAY_BLOCK);
    }
    private static void arena(MapWriter w) {
        w.fill(18, -1, 41, 31, -1, 55, Blocks.COARSE_DIRT);
        w.fill(18, 0, 41, 31, 0, 41, Blocks.OAK_FENCE);
        w.fill(18, 0, 55, 31, 0, 55, Blocks.OAK_FENCE);
        w.fill(18, 0, 42, 18, 0, 54, Blocks.OAK_FENCE);
        w.fill(31, 0, 42, 31, 0, 54, Blocks.OAK_FENCE);
        // Wide permanent gaps on three sides: the fence cannot become a player trap.
        w.fill(23, 0, 41, 26, 1, 41, Blocks.AIR);
        w.fill(18, 0, 47, 18, 1, 49, Blocks.AIR);
        w.fill(23, 0, 55, 26, 1, 55, Blocks.AIR);
        for (int z = 44; z <= 52; z += 4) {
            w.put(21, 0, z, Blocks.WOOL.red());
            w.put(22, 0, z, Blocks.WOOL.red());
            // The copied team's L points the same way instead of rotating to face its opponent.
            w.put(27, 0, z, Blocks.WOOL.blue());
            w.put(28, 0, z, Blocks.WOOL.blue());
            w.put(28, 0, z + 1, Blocks.WOOL.blue());
        }
        w.fill(29, 0, 54, 30, 3, 54, Blocks.SPRUCE_PLANKS);
        w.put(30, 3, 54, Blocks.AIR);
        w.sign(29, 2, 53, Direction.NORTH, "map.unallocated.arena.0", "map.unallocated.arena.1",
            "map.unallocated.arena.2", "map.unallocated.arena.3");
    }
}
