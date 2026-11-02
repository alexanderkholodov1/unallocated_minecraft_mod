package org.unallocated.map;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Bounded, update-suppressed placement avoids order-dependent neighbour changes and item drops. */
public final class MapWriter {
    private static final int[][] NEIGHBORS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    private final ServerLevel level;
    private final MapLayout.Section section;
    private final List<Runnable> signWrites = new ArrayList<>();
    MapWriter(ServerLevel level, MapLayout.Section section) { this.level = level; this.section = section; }
    public void put(int x, int y, int z, Block block) { put(x, y, z, block.defaultBlockState()); }
    public void put(String landmark, BlockState state) {
        var pos = MapLayout.landmark(landmark);
        put(pos.getX() - section.x(), pos.getY() - section.y(), pos.getZ() - section.z(), state);
    }
    public void stock(String landmark, int slot, ItemStack stack) {
        if (!(level.getBlockEntity(MapLayout.landmark(landmark)) instanceof Container container)) {
            throw new IllegalStateException("Missing container at " + landmark);
        }
        container.setItem(slot, stack);
    }
    public void put(int x, int y, int z, BlockState state) {
        BlockPos pos = section.at(x, y, z);
        if (!section.bounds().contains(pos.getX(), pos.getY(), pos.getZ())) {
            throw new IllegalArgumentException("Placement outside " + section.id() + ": " + pos);
        }
        place(level, pos, state);
    }
    static void place(ServerLevel level, BlockPos pos, BlockState state) {
        final boolean changed;
        try {
            changed = level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS
                | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Map placement failed at " + pos + " for " + state, exception);
        }
        if (!changed && level.getBlockState(pos) != state) {
            throw new IllegalStateException("Map placement rejected at " + pos + ": requested " + state
                + ", actual " + level.getBlockState(pos));
        }
    }
    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        for (int x = x1; x <= x2; x++) for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) put(x, y, z, block);
    }
    /** Sections have already been graded; this helper only restores their grass surface. */
    public void ground(int x1, int z1, int x2, int z2) {
        fill(x1, -1, z1, x2, -1, z2, Blocks.GRASS_BLOCK);
    }
    private static int distance(MapLayout.BlockBox b, int x, int z) {
        return Math.max(Math.max(Math.max(b.minX() - x, x - b.maxX()), 0),
            Math.max(Math.max(b.minZ() - z, z - b.maxZ()), 0));
    }
    /** A margin belongs to its nearest section, with stable layout order resolving ties. */
    private static String owner(int x, int z) {
        String result = "";
        int nearest = Integer.MAX_VALUE;
        for (var candidate : MapLayout.sections().values()) {
            int d = distance(candidate.bounds(), x, z);
            if (d < nearest) { nearest = d; result = candidate.id(); }
        }
        return result;
    }
    private static int surface(ServerLevel level, int x, int z) {
        // Ignore trees and plants when finding the natural ground underneath them.
        for (int y = level.getMinY() + level.getHeight() - 1; y > level.getMinY(); y--) {
            var block = level.getBlockState(new BlockPos(x, y, z)).getBlock();
            if (block == Blocks.GRASS_BLOCK || block == Blocks.DIRT || block == Blocks.STONE
                    || block == Blocks.COARSE_DIRT || block == Blocks.GRAVEL || block == Blocks.SAND
                    || block == Blocks.DEEPSLATE || block == Blocks.SNOW_BLOCK) return y;
        }
        return MapLayout.GROUND_Y;
    }
    /** Four terrace rings normally suffice. Extend grading only where steep natural terrain
     * would otherwise leave a cliff at their outer edge, always inside the authored island. */
    static void prepare(ServerLevel level, MapLayout.Section section) {
        var island = MapLayout.ISLAND;
        int width = island.maxX() - island.minX() + 1;
        int depth = island.maxZ() - island.minZ() + 1;
        int[][] original = new int[width][depth];
        int[][] heights = new int[width][depth];
        String[][] owners = new String[width][depth];
        int[][] distances = new int[width][depth];
        for (int ix = 0; ix < width; ix++) for (int iz = 0; iz < depth; iz++) {
            int x = island.minX() + ix, z = island.minZ() + iz;
            String id = owner(x, z);
            int d = distance(MapLayout.section(id).bounds(), x, z);
            owners[ix][iz] = id;
            distances[ix][iz] = d;
            int h = d == 0 ? MapLayout.GROUND_Y : Math.min(surface(level, x, z), MapBuilder.sightlineCeiling(x, z));
            original[ix][iz] = h;
            heights[ix][iz] = Math.max(-d, Math.min(d, h - MapLayout.GROUND_Y));
        }
        // Propagate slope limits toward high/low terrain. Each pass only reduces the absolute
        // displacement from ground level, so this converges and re-authoring is idempotent.
        boolean changed;
        do {
            changed = false;
            for (int ix = 0; ix < width; ix++) for (int iz = 0; iz < depth; iz++) {
                int h = heights[ix][iz];
                for (int[] delta : NEIGHBORS) {
                    int nx = ix + delta[0], nz = iz + delta[1];
                    if (nx < 0 || nz < 0 || nx >= width || nz >= depth) continue;
                    int neighbor = heights[nx][nz];
                    int limit = h * neighbor < 0 ? 0 : Math.abs(neighbor) + 1;
                    int next = Integer.signum(h) * Math.min(Math.abs(h), limit);
                    if (next != h) { changed = true; h = next; }
                }
                heights[ix][iz] = h;
            }
        } while (changed);
        for (int ix = 0; ix < width; ix++) for (int iz = 0; iz < depth; iz++) {
            if (!owners[ix][iz].equals(section.id())) continue;
            int top = MapLayout.GROUND_Y + heights[ix][iz];
            if (distances[ix][iz] > 4 && top == original[ix][iz]) continue;
            int x = island.minX() + ix, z = island.minZ() + iz;
            for (int y = level.getMinY() + 1; y < level.getMinY() + level.getHeight(); y++) {
                var block = y > top ? Blocks.AIR : y == top ? Blocks.GRASS_BLOCK
                    : y >= top - 3 ? Blocks.DIRT : Blocks.STONE;
                var pos = new BlockPos(x, y, z);
                if (level.getBlockState(pos).getBlock() != block) place(level, pos, block.defaultBlockState());
            }
        }
    }
    public void sign(int x, int y, int z, Direction facing, String a, String b, String c, String d) {
        put(x, y, z, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
        var pos = section.at(x, y, z);
        List<Component> lines = java.util.stream.Stream.of(a, b, c, d).map(MapWriter::line).toList();
        var text = new SignText(lines, lines, DyeColor.BLACK, false);
        // Configure the final entity after bulk placement/shape updates, never an intermediate one.
        signWrites.add(() -> {
            if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) {
                throw new IllegalStateException("Missing sign block entity at " + pos + "; block=" + level.getBlockState(pos));
            }
            sign.setText(text, SignTextSlot.FRONT);
            sign.setText(text, SignTextSlot.BACK);
            sign.setWaxed(true);
            if (!sign.getText(SignTextSlot.FRONT).equals(text) || !sign.getText(SignTextSlot.BACK).equals(text) || !sign.isWaxed()) {
                throw new IllegalStateException("Sign write failed at " + pos + "; expected=" + List.of(a, b, c, d)
                    + ", waxed=true; found front=" + sign.getText(SignTextSlot.FRONT).getMessages(false)
                    + ", back=" + sign.getText(SignTextSlot.BACK).getMessages(false) + ", waxed=" + sign.isWaxed());
            }
        });
    }
    /** Sign lines are language keys (map.unallocated.*) so each client reads them in its own language. */
    private static Component line(String text) {
        return text.startsWith("map.unallocated.") ? Component.translatable(text) : Component.literal(text);
    }
    void finishSigns() { signWrites.forEach(Runnable::run); }
    public void sign(String landmark, Direction facing, String a, String b, String c, String d) {
        var pos = MapLayout.landmark(landmark);
        sign(pos.getX() - section.x(), pos.getY() - section.y(), pos.getZ() - section.z(), facing, a, b, c, d);
    }
    public void lamp(int x, int z) {
        fill(x, 0, z, x, 2, z, Blocks.OAK_FENCE);
        put(x, 3, z, Blocks.LANTERN);
    }
}
