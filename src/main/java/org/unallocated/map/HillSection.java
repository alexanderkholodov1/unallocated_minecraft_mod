package org.unallocated.map;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** An ordinary low hill with a clear silhouette and an unobstructed view back toward spawn. */
public final class HillSection implements MapSection {
    @Override public String id() { return "hill"; }
    static int height(int x, int z) {
        return Math.max(0, Math.min(8, (int) Math.floor((14.5 - Math.sqrt(x * x + z * z)) * 0.8)));
    }
    @Override public void build(MapWriter w) {
        w.ground(-15, -15, 14, 13);
        for (int x = -15; x <= 14; x++) for (int z = -15; z <= 13; z++) {
            int h = height(x, z);
            if (h == 0) continue;
            w.fill(x, 0, z, x, h - 2, z, Blocks.STONE);
            w.put(x, h - 1, z, Math.floorMod(x * 19 + z * 7, 29) == 0 ? Blocks.COARSE_DIRT : Blocks.GRASS_BLOCK);
        }
        // A worn, climbable route on the southern slope. No parkour is required in Adventure.
        for (int z = 13; z >= 2; z--) for (int x = -5; x <= -3; x++) {
            int h = height(x, z);
            w.put(x, h - 1, z, Blocks.COBBLESTONE_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        }
        w.fill(-5, 7, -2, 5, 7, 2, Blocks.GRASS_BLOCK);
        w.fill(-5, 8, -2, 5, 10, 2, Blocks.AIR);
        // The far bait foothold is deliberately empty: the spyglass package owns its entity.
        w.put(4, 7, 0, Blocks.COARSE_DIRT);
        w.fill(7, 0, 8, 7, 3, 8, Blocks.OAK_LOG);
        w.put(6, 0, 8, Blocks.OAK_LOG);
        w.put(8, 0, 8, Blocks.OAK_LOG);
        w.put(-9, height(-9, 5), 5, Blocks.DEAD_BUSH);
    }
}
