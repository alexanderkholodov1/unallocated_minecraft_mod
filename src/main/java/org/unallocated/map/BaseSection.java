package org.unallocated.map;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** The last players' practical shared house, with a workshop, two beds and an untended yard. */
public final class BaseSection implements MapSection {
    @Override public String id() { return "base"; }
    @Override public void build(MapWriter w) {
        w.ground(0, 0, 21, 25);
        w.fill(0, -1, 8, 5, -1, 12, Blocks.COARSE_DIRT);
        w.fill(17, -1, 16, 21, -1, 20, Blocks.COARSE_DIRT);
        w.fill(3, -1, 3, 18, -1, 20, Blocks.COBBLESTONE);
        w.fill(4, -1, 4, 17, -1, 19, Blocks.OAK_PLANKS);
        w.fill(4, 0, 4, 17, 0, 19, Blocks.COBBLESTONE);
        w.fill(4, 1, 4, 17, 3, 19, Blocks.OAK_PLANKS);
        w.fill(5, 0, 5, 16, 3, 18, Blocks.AIR);
        for (int x : new int[] {4, 17}) for (int z : new int[] {4, 19}) w.fill(x, 0, z, x, 3, z, Blocks.OAK_LOG);
        for (int z : new int[] {6, 14}) {
            w.fill(4, 1, z, 4, 2, z + 2, Blocks.GLASS_PANE);
            w.fill(17, 1, z, 17, 2, z + 2, Blocks.GLASS_PANE);
        }
        w.fill(8, 1, 4, 11, 2, 4, Blocks.GLASS_PANE);
        w.fill(8, 1, 19, 11, 2, 19, Blocks.GLASS_PANE);
        // One replacement pane is opaque, as though somebody ran out of glass.
        w.put(4, 1, 15, Blocks.OAK_PLANKS);
        door(w, "base.door", Direction.EAST);
        door(w, "base.back_door", Direction.EAST);
        for (int x = 3; x <= 18; x++) {
            int rise = Math.min(x - 3, 18 - x) / 2;
            for (int z = 3; z <= 20; z++) {
                w.put(x, 4 + rise, z, Blocks.SPRUCE_STAIRS.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, x <= 10 ? Direction.EAST : Direction.WEST));
            }
            if (x >= 4 && x <= 17) for (int y = 4; y < 4 + rise; y++) {
                w.put(x, y, 4, Blocks.OAK_PLANKS); w.put(x, y, 19, Blocks.OAK_PLANKS);
            }
        }
        w.fill(15, 4, 5, 15, 8, 5, Blocks.COBBLESTONE);
        w.put(15, 8, 5, Blocks.COBBLESTONE_WALL);
        w.put("base.chest", Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        w.stock("base.chest", 2, new ItemStack(Items.COAL, 11));
        w.stock("base.chest", 12, new ItemStack(Items.COBBLESTONE, 23));
        w.stock("base.chest", 19, new ItemStack(Items.BREAD, 3));
        w.put(14, 0, 6, Blocks.CRAFTING_TABLE);
        w.put(15, 0, 6, Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        w.put(16, 0, 12, Blocks.BARREL);
        w.fill(9, -1, 9, 11, -1, 14, Blocks.GRAY_WOOL);
        // This soft floor also serves the later theft scene's unnaturally quiet footsteps.
        bed(w, 13, 13, Blocks.RED_BED);
        bed(w, 6, 13, Blocks.WHITE_BED);
        w.put(12, 0, 14, Blocks.BARREL);
        w.put(12, 1, 14, Blocks.LANTERN);
        w.put(8, 0, 5, Blocks.LANTERN);
        w.fill(2, 0, 21, 9, 0, 21, Blocks.OAK_FENCE);
        w.fill(2, 0, 24, 9, 0, 24, Blocks.OAK_FENCE);
        w.fill(2, 0, 22, 2, 0, 23, Blocks.OAK_FENCE);
        w.fill(9, 0, 22, 9, 0, 23, Blocks.OAK_FENCE);
        w.fill(3, -1, 22, 8, -1, 23, Blocks.COARSE_DIRT);
        w.put(4, 0, 22, Blocks.DEAD_BUSH); w.put(7, 0, 23, Blocks.DEAD_BUSH);
        w.fill(19, 0, 5, 20, 1, 8, Blocks.OAK_LOG);
        w.fill(3, 0, 7, 3, 2, 7, Blocks.OAK_LOG);
        w.sign(2, 1, 7, Direction.WEST, "LA BASE", "Deja carbón", "para los demás", "Mina detrás →");
        w.lamp(1, 14); w.lamp(20, 21);
    }
    private static void door(MapWriter w, String landmark, Direction direction) {
        var state = Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
        var pos = MapLayout.landmark(landmark); var origin = MapLayout.section("base");
        w.put(landmark, state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        w.put(pos.getX() - origin.x(), 1, pos.getZ() - origin.z(), state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
    }
    private static void bed(MapWriter w, int x, int z, net.minecraft.world.level.block.Block block) {
        var state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        w.put(x, 0, z, state.setValue(BlockStateProperties.BED_PART, BedPart.FOOT));
        w.put(x, 0, z + 1, state.setValue(BlockStateProperties.BED_PART, BedPart.HEAD));
    }
}
