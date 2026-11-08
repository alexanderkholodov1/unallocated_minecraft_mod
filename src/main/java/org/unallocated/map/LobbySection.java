package org.unallocated.map;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** ValleCraft Network's old public hub, with a working road and three retired game gates. */
public final class LobbySection implements MapSection {
    @Override public String id() { return "lobby"; }
    @Override public void build(MapWriter w) {
        w.ground(-40, -24, 16, 26);
        for (int x = -15; x <= 15; x++) for (int z = -15; z <= 15; z++) {
            boolean trim = Math.abs(x) == 15 || Math.abs(z) == 15 || x % 8 == 0 || z % 8 == 0;
            w.put(x, -1, z, trim ? Blocks.QUARTZ_BLOCK : Math.floorMod(x * 13 + z * 7, 43) == 0
                ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS);
        }
        // Generous paths around the boards and out to the staff house and leaderboard.
        w.fill(-35, -1, -5, -16, -1, -1, Blocks.GRAVEL);
        w.fill(-32, -1, -16, -28, -1, 11, Blocks.GRAVEL);
        w.fill(14, -1, -2, 16, -1, 2, Blocks.GRAVEL);
        gate(w, 15, 0, true, "survival", false);
        gate(w, -15, 0, true, "minigames", true);
        gate(w, 0, -15, false, "plots", true);
        gate(w, 0, 15, false, "creative", true);
        w.fill(-7, 0, -8, -7, 4, -8, Blocks.OAK_LOG);
        w.fill(7, 0, -8, 7, 4, -8, Blocks.OAK_LOG);
        w.fill(-6, 1, -8, 6, 3, -8, Blocks.SPRUCE_PLANKS);
        w.fill(-8, 4, -9, 8, 4, -6, Blocks.SPRUCE_SLAB);
        board(w, "lobby.rules", "rules");
        board(w, "lobby.motd", "motd");
        board(w, "lobby.task", "task");
        w.fill(14, 0, -3, 14, 2, -3, Blocks.OAK_LOG);
        w.sign("lobby.route", Direction.WEST, "map.unallocated.lobby_route.0", "map.unallocated.lobby_route.1",
            "map.unallocated.lobby_route.2", "map.unallocated.lobby_route.3");
        letters(w);
        leaderboard(w);
        for (int x : new int[] {-11, 11}) for (int z : new int[] {-11, 11}) w.lamp(x, z);
        for (int x : new int[] {6, 10}) {
            w.fill(x, 0, 7, x + 2, 0, 7, Blocks.OAK_STAIRS);
        }
        for (int x : new int[] {-9, 9}) {
            w.fill(x - 1, 0, 4, x + 1, 0, 6, Blocks.GRASS_BLOCK);
            w.put(x, 1, 5, Blocks.POPPY);
        }
        // The old well landmark is now a dry planter, with no cavity to fall into.
        w.put("lobby.well", Blocks.GRASS_BLOCK.defaultBlockState());
        w.fill(-12, 0, -4, -12, 2, -4, Blocks.OAK_LOG);
        w.sign(-12, 1, -3, Direction.SOUTH, "map.unallocated.parkour.0", "map.unallocated.parkour.1", "", "");
        for (int z : new int[] {-1, 2, 5, 8}) w.put(-13, 0, z, Blocks.QUARTZ_BLOCK);
        adminHouse(w);
    }
    private static void board(MapWriter w, String landmark, String key) {
        w.sign(landmark, Direction.SOUTH, "map.unallocated." + key + ".0", "map.unallocated." + key + ".1",
            "map.unallocated." + key + ".2", "map.unallocated." + key + ".3");
    }
    private static void gate(MapWriter w, int x, int z, boolean alongZ, String mode, boolean closed) {
        for (int t = -3; t <= 3; t++) for (int y = -1; y <= 4; y++) {
            int dx = alongZ ? 0 : t, dz = alongZ ? t : 0;
            if (Math.abs(t) == 3 || y == -1 || y == 4) {
                w.put(x + dx, y, z + dz, y == 4 && t == 2 ? Blocks.CRYING_OBSIDIAN : Blocks.OBSIDIAN);
            } else if (closed && y == 0) {
                w.put(x + dx, y, z + dz, t == 0 ? Blocks.OAK_FENCE_GATE.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, alongZ ? Direction.EAST : Direction.NORTH)
                    : Blocks.OAK_FENCE.defaultBlockState());
            }
        }
        // Side-mounted signs leave the arch and all approaches clear. Closed gates can be opened,
        // and their backs remain open to the surrounding hub grounds.
        if (alongZ) w.sign(x, 2, z + 4, Direction.SOUTH, "map.unallocated.gate." + mode,
            closed ? "map.unallocated.gate.offline" : "map.unallocated.gate.road", "", "");
        else w.sign(x + 3, 2, z + 1, Direction.SOUTH, "map.unallocated.gate." + mode,
            closed ? "map.unallocated.gate.offline" : "map.unallocated.gate.road", "", "");
    }
    private static void letters(MapWriter w) {
        // 3x5 block glyphs facing north toward spawn; 39 blocks wide.
        String[] glyphs = {"101101101101010", "010101111101101", "100100100100111", "100100100100111",
            "111100110100111", "111100100100111", "110101110101101", "010101111101101",
            "111100110100100", "111010010010010"};
        for (int i = 0; i < glyphs.length; i++) for (int row = 0; row < 5; row++) for (int col = 0; col < 3; col++) {
            if (glyphs[i].charAt(row * 3 + col) == '1') w.put(12 - i * 4 - col, 10 - row, 24, Blocks.QUARTZ_BLOCK);
        }
        w.fill(-27, 0, 24, -27, 10, 24, Blocks.STONE_BRICKS);
        w.fill(13, 0, 24, 13, 10, 24, Blocks.STONE_BRICKS);
        w.fill(-27, 5, 24, 13, 5, 24, Blocks.STONE_BRICKS);
    }
    private static void leaderboard(MapWriter w) {
        w.fill(-38, 0, -17, -19, 3, -17, Blocks.STONE_BRICKS);
        w.sign(-29, 3, -16, Direction.SOUTH, "map.unallocated.top.title", "map.unallocated.top.season", "", "");
        String[] players = {"kai", "noor", "efe", "makena", "zuri", "sunny", "ari"};
        for (int i = 0; i < players.length; i++) {
            w.sign(-37 + i * 2, 1, -16, Direction.SOUTH, "map.unallocated.top." + players[i],
                "map.unallocated.top.score." + i, "", "");
        }
    }
    private static void adminHouse(MapWriter w) {
        w.fill(-35, -1, 12, -25, -1, 22, Blocks.STONE_BRICKS);
        w.fill(-35, 0, 12, -25, 3, 22, Blocks.SPRUCE_PLANKS);
        w.fill(-34, 0, 13, -26, 3, 21, Blocks.AIR);
        w.fill(-36, 4, 11, -24, 4, 23, Blocks.SPRUCE_SLAB);
        w.fill(-35, 1, 15, -35, 2, 18, Blocks.GLASS_PANE);
        w.fill(-25, 1, 15, -25, 2, 18, Blocks.GLASS_PANE);
        var door = Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        w.put("hub.admin_house", door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        w.put(-30, 1, 12, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        w.sign(-30, 2, 11, Direction.NORTH, "map.unallocated.admin.0", "map.unallocated.admin.1", "", "");
        w.fill(-33, 0, 19, -31, 0, 19, Blocks.OAK_SLAB);
        w.put(-33, 1, 19, Blocks.LANTERN);
        w.put(-27, 0, 20, Blocks.CHEST);
        w.put(-27, 0, 16, Blocks.LECTERN);
        // A second open doorway gives an independent exit to the back garden.
        w.fill(-31, 0, 22, -30, 1, 22, Blocks.AIR);
    }
}
