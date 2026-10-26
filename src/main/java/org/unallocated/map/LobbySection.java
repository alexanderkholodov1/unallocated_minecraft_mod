package org.unallocated.map;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** A modest spawn plaza: repaired cobble, communal well, noticeboard, empty benches. */
public final class LobbySection implements MapSection {
    @Override public String id() { return "lobby"; }
    @Override public void build(MapWriter w) {
        w.ground(-16, -15, 16, 17);
        for (int x = -12; x <= 12; x++) for (int z = -10; z <= 12; z++) {
            var block = Math.floorMod(x * 31 + z * 17, 23) == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE;
            if (Math.floorMod(x * 7 - z * 3, 41) == 0) block = Blocks.ANDESITE;
            w.put(x, -1, z, block);
        }
        w.fill(-12, -1, -10, 12, -1, -10, Blocks.STONE_BRICKS);
        w.fill(-12, -1, 12, 12, -1, 12, Blocks.STONE_BRICKS);
        w.fill(-12, -1, -10, -12, -1, 12, Blocks.STONE_BRICKS);
        w.fill(12, -1, -10, 12, -1, 12, Blocks.STONE_BRICKS);
        // Low perimeter, with a generous eastern exit toward the base.
        w.fill(-13, 0, -11, 13, 0, -11, Blocks.COBBLESTONE_WALL);
        w.fill(-13, 0, 13, 13, 0, 13, Blocks.COBBLESTONE_WALL);
        w.fill(-13, 0, -10, -13, 0, 12, Blocks.COBBLESTONE_WALL);
        w.fill(13, 0, -11, 13, 0, -4, Blocks.COBBLESTONE_WALL);
        w.fill(13, 0, 4, 13, 0, 13, Blocks.COBBLESTONE_WALL);
        // Old server noticeboard under a shallow plank canopy.
        w.fill(-7, 0, -8, -7, 4, -8, Blocks.OAK_LOG);
        w.fill(7, 0, -8, 7, 4, -8, Blocks.OAK_LOG);
        w.fill(-6, 1, -8, 6, 3, -8, Blocks.SPRUCE_PLANKS);
        w.fill(-8, 4, -9, 8, 4, -6, Blocks.SPRUCE_SLAB);
        w.sign("lobby.rules", Direction.SOUTH, "REGLAS", "No romper spawn", "No robar", "Cerrar las puetas");
        w.sign("lobby.motd", Direction.SOUTH, "Servidor del valle", "Bienvenidos", "7 / 20 conectados", "Última visita: ayer");
        w.sign("lobby.task", Direction.SOUTH, "Los demás están", "en la base.", "Llévales", "el catalejo.");
        w.put(-4, 3, -6, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        w.put(4, 3, -6, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        w.fill(14, -1, -2, 16, -1, 2, Blocks.COBBLESTONE);
        w.fill(14, 0, -3, 14, 2, -3, Blocks.OAK_LOG);
        w.sign("lobby.route", Direction.WEST, "LA BASE  →", "LA MINA  →", "Sigue el camino", "de antorchas");
        // Covered well: water is below the lip, so the spawn stays safe.
        w.fill(-11, 0, 3, -7, 0, 7, Blocks.COBBLESTONE);
        w.fill(-10, 0, 4, -8, 0, 6, Blocks.AIR);
        w.fill(-10, -1, 4, -8, -1, 6, Blocks.WATER);
        w.fill(-11, 1, 5, -11, 3, 5, Blocks.OAK_FENCE);
        w.fill(-7, 1, 5, -7, 3, 5, Blocks.OAK_FENCE);
        w.fill(-12, 4, 2, -6, 4, 8, Blocks.SPRUCE_SLAB);
        for (int z : new int[] {4, 9}) {
            for (int x = 5; x <= 9; x++) w.put(x, 0, z, Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
            w.put(4, 0, z, Blocks.OAK_LOG);
            w.put(10, 0, z, Blocks.OAK_LOG);
        }
        w.lamp(-11, -9); w.lamp(11, -9); w.lamp(11, 11); w.lamp(-11, 11);
        // One mismatched repair, not an overt horror prop.
        w.put(3, -1, 5, Blocks.CRACKED_STONE_BRICKS);
    }
}
