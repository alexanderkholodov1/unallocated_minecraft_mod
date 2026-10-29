package org.unallocated.map;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;

/** Timber-framed mouth and a short, safe adit. The loop/pocket packages own anything beyond it. */
public final class MineSection implements MapSection {
    @Override public String id() { return "mine"; }
    @Override public void build(MapWriter w) {
        w.ground(0, 0, 32, 35);
        for (int x = 13; x <= 32; x++) for (int z = 0; z <= 35; z++) {
            int h = Math.max(0, Math.min(11, (int) (18 - Math.sqrt((x - 24) * (x - 24) + (z - 15) * (z - 15)))));
            if (h < 1) continue;
            w.fill(x, 0, z, x, h - 2, z, Blocks.STONE);
            w.put(x, h - 1, z, (x + z) % 7 == 0 ? Blocks.ANDESITE : Blocks.GRASS_BLOCK);
        }
        w.fill(0, -1, 13, 12, -1, 17, Blocks.COARSE_DIRT);
        w.fill(13, -1, 13, 26, -1, 17, Blocks.STONE);
        w.fill(13, 0, 13, 26, 3, 17, Blocks.AIR);
        for (int x : new int[] {13, 19, 25}) {
            w.fill(x, 0, 12, x, 3, 12, Blocks.STRIPPED_OAK_LOG);
            w.fill(x, 0, 18, x, 3, 18, Blocks.STRIPPED_OAK_LOG);
            for (int z = 12; z <= 18; z++) w.put(x, 4, z, Blocks.STRIPPED_OAK_LOG.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Z));
        }
        w.put(12, 2, 12, Blocks.WALL_TORCH.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        w.put(19, 2, 13, Blocks.WALL_TORCH.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        for (int x = 14; x <= 24; x++) w.put(x, 0, 14, Blocks.RAIL.defaultBlockState()
            .setValue(BlockStateProperties.RAIL_SHAPE, RailShape.EAST_WEST));
        w.put(23, 1, 12, Blocks.IRON_ORE);
        w.put(21, 0, 18, Blocks.COPPER_ORE);
        // A mundane dead end for v0.1, with a solid floor. Later acts replace this seam.
        w.fill(27, 0, 13, 27, 4, 17, Blocks.STONE);
        w.put(27, 1, 15, Blocks.ANDESITE);
        w.fill(11, 0, 11, 11, 2, 11, Blocks.OAK_LOG);
        w.sign("mine.route", Direction.WEST, "LA MINA", "Deja las vagonetas", "junto a la entrada", "Base ←");
        w.put(5, 0, 21, Blocks.BARREL);
        w.fill(7, 0, 23, 10, 0, 23, Blocks.OAK_LOG);
        w.put(11, 0, 8, Blocks.COBBLESTONE);
        w.put(12, 0, 9, Blocks.ANDESITE);
        w.put(12, 1, 9, Blocks.COBBLESTONE);
        w.put(11, 0, 22, Blocks.ANDESITE);
        w.put(9, 0, 18, Blocks.COBBLESTONE);
    }
}
