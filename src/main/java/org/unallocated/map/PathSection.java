package org.unallocated.map;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** A player-made track, with a short northern branch to the hill and a bend to the base. */
public final class PathSection implements MapSection {
    @Override public String id() { return "path"; }
    @Override public void build(MapWriter w) {
        w.ground(0, -10, 33, 27);
        for (int x = 0; x <= 33; x++) {
            int centerZ = x < 14 ? 0 : Math.min(12, x - 14);
            for (int dz = -2; dz <= 2; dz++) {
                int z = centerZ + dz;
                var block = Math.floorMod(x * 13 + z * 7, 11) == 0 ? Blocks.COBBLESTONE : Blocks.COARSE_DIRT;
                w.put(x, -1, z, block);
            }
        }
        for (int z = -10; z <= 0; z++) for (int x = 16; x <= 18; x++) w.put(x, -1, z, Blocks.GRAVEL);
        // Stable torches, named by local route order for the later Act 1 guidance effect.
        for (int i = 0; i < 5; i++) {
            var pos = MapLayout.landmark("path.torch." + i);
            var origin = MapLayout.section(id());
            w.put(pos.getX() - origin.x(), 0, pos.getZ() - origin.z(), Blocks.COBBLESTONE);
            w.put("path.torch." + i, Blocks.TORCH.defaultBlockState());
        }
        w.fill(26, 0, 7, 26, 2, 7, Blocks.OAK_LOG);
        w.sign("path.route", Direction.WEST, "LA BASE  →", "LA MINA detrás", "Cierra la puerta", "al llegar");
        // A small rest stop, used and left untidily; no diaries or horror set dressing.
        w.fill(6, -1, 12, 11, -1, 17, Blocks.COARSE_DIRT);
        w.put("path.campfire", Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false));
        w.fill(6, 0, 16, 10, 0, 16, Blocks.OAK_LOG);
        w.put(11, 0, 13, Blocks.BARREL);
        w.put(5, 0, 12, Blocks.COBBLESTONE);
        w.put(7, 0, 12, Blocks.COBBLESTONE);
        w.put(22, 0, 24, Blocks.OAK_LOG);
        w.put(23, 0, 24, Blocks.OAK_LOG);
    }
}
