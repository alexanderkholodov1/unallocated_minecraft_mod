package org.unallocated.map;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Bounded, update-suppressed placement avoids order-dependent neighbour changes and item drops. */
public final class MapWriter {
    private final ServerLevel level;
    private final MapLayout.Section section;
    MapWriter(ServerLevel level, MapLayout.Section section) { this.level = level; this.section = section; }
    public void put(int x, int y, int z, Block block) { put(x, y, z, block.defaultBlockState()); }
    public void put(int x, int y, int z, BlockState state) {
        BlockPos pos = section.at(x, y, z);
        if (!section.bounds().contains(pos.getX(), pos.getY(), pos.getZ())) {
            throw new IllegalArgumentException("Placement outside " + section.id() + ": " + pos);
        }
        place(level, pos, state);
    }
    static void place(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
    }
    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        for (int x = x1; x <= x2; x++) for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) put(x, y, z, block);
    }
    public void ground(int x1, int z1, int x2, int z2) {
        fill(x1, -4, z1, x2, -2, z2, Blocks.DIRT);
        fill(x1, -1, z1, x2, -1, z2, Blocks.GRASS_BLOCK);
    }
    public void sign(int x, int y, int z, Direction facing, String a, String b, String c, String d) {
        put(x, y, z, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
        var sign = (SignBlockEntity) level.getBlockEntity(section.at(x, y, z));
        if (sign == null) throw new IllegalStateException("Missing sign block entity");
        List<Component> lines = List.of(Component.literal(a), Component.literal(b), Component.literal(c), Component.literal(d));
        var text = new SignText(lines, lines, DyeColor.BLACK, false);
        sign.setText(text, SignTextSlot.FRONT);
        sign.setText(text, SignTextSlot.BACK);
        sign.setWaxed(true);
    }
    public void lamp(int x, int z) {
        fill(x, 0, z, x, 2, z, Blocks.OAK_FENCE);
        put(x, 3, z, Blocks.LANTERN);
    }
}
