package org.unallocated.map;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import net.minecraft.core.BlockPos;

/** Stable authoring contract. Coordinates are Overworld block coordinates; boxes are inclusive. */
public final class MapLayout {
    public static final int GROUND_Y = 63;
    public static final String BIOME_ID = "unallocated:lobby";
    public static final BlockBox ISLAND = new BlockBox(-58, 57, -84, 122, 91, 84);
    /** Scenery stays outside section ownership and outside the spawn-to-bait sightline. */
    public static final List<BlockPos> TREES = List.of(new BlockPos(-28, 64, -18), new BlockPos(-24, 64, 25),
        new BlockPos(-8, 64, 32), new BlockPos(15, 64, -27), new BlockPos(61, 64, -19),
        new BlockPos(77, 64, -26), new BlockPos(90, 64, 50), new BlockPos(35, 64, 46), new BlockPos(65, 64, 43));
    public record BlockBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public BlockBox {
            if (minX > maxX || minY > maxY || minZ > maxZ) throw new IllegalArgumentException("Inverted box");
        }
        public boolean contains(double x, double y, double z) {
            return x >= minX && x < maxX + 1.0 && y >= minY && y < maxY + 1.0 && z >= minZ && z < maxZ + 1.0;
        }
    }
    public record Section(String id, int x, int y, int z, BlockBox bounds) {
        public BlockPos at(int dx, int dy, int dz) { return new BlockPos(x + dx, y + dy, z + dz); }
    }
    public record Checkpoint(String section, int x, int y, int z, float yaw, float pitch) {
        public BlockPos pos() { return new BlockPos(x, y, z); }
    }
    private static final Map<String, Section> SECTIONS;
    private static final Map<String, Checkpoint> CHECKPOINTS;
    private static final Map<String, BlockBox> TRIGGERS;
    private static final Map<String, BlockPos> LANDMARKS;
    static {
        var sections = new LinkedHashMap<String, Section>();
        sections.put("lobby", new Section("lobby", 0, 64, 0, new BlockBox(-16, 60, -15, 16, 90, 17)));
        sections.put("hill", new Section("hill", 38, 64, -24, new BlockBox(23, 60, -39, 52, 90, -11)));
        sections.put("path", new Section("path", 17, 64, 0, new BlockBox(17, 60, -10, 50, 90, 27)));
        sections.put("base", new Section("base", 51, 64, 2, new BlockBox(51, 60, 2, 72, 90, 27)));
        sections.put("mine", new Section("mine", 73, 64, 5, new BlockBox(73, 57, 5, 105, 90, 40)));
        SECTIONS = Collections.unmodifiableMap(sections);
        var checkpoints = new LinkedHashMap<String, Checkpoint>();
        checkpoints.put("lobby.spawn", new Checkpoint("lobby", 0, 64, 8, 180, 0));
        checkpoints.put("lobby.board", new Checkpoint("lobby", 0, 64, -3, 180, 0));
        checkpoints.put("hill.bait_path", new Checkpoint("hill", 34, 72, -24, -90, 3));
        checkpoints.put("hill.view", new Checkpoint("hill", 24, 64, -12, -130, -8));
        checkpoints.put("path.midpoint", new Checkpoint("path", 34, 64, 3, -90, 0));
        checkpoints.put("base.entrance", new Checkpoint("base", 53, 64, 12, -90, 0));
        checkpoints.put("base.theft_zone", new Checkpoint("base", 61, 64, 12, 135, 8));
        checkpoints.put("mine.entrance", new Checkpoint("mine", 80, 64, 20, -90, 0));
        checkpoints.put("mine.threshold", new Checkpoint("mine", 89, 64, 20, -90, 4));
        CHECKPOINTS = Collections.unmodifiableMap(checkpoints);
        var triggers = new LinkedHashMap<String, BlockBox>();
        triggers.put("lobby.exit", new BlockBox(14, 64, -2, 17, 67, 2));
        triggers.put("hill.bait_path", new BlockBox(32, 72, -28, 44, 76, -20));
        triggers.put("base.theft_zone", new BlockBox(57, 64, 9, 65, 67, 16));
        triggers.put("mine.entrance", new BlockBox(85, 64, 18, 89, 67, 22));
        TRIGGERS = Collections.unmodifiableMap(triggers);
        var landmarks = new LinkedHashMap<String, BlockPos>();
        landmarks.put("lobby.rules", section("lobby").at(-5, 2, -7));
        landmarks.put("lobby.motd", section("lobby").at(0, 2, -7));
        landmarks.put("lobby.task", section("lobby").at(5, 2, -7));
        landmarks.put("lobby.route", section("lobby").at(13, 1, -3));
        landmarks.put("lobby.well", section("lobby").at(-9, 0, 5));
        landmarks.put("hill.bait", section("hill").at(4, 8, 0));
        landmarks.put("base.chest", section("base").at(7, 0, 6));
        landmarks.put("base.bed", section("base").at(13, 0, 13));
        landmarks.put("base.door", section("base").at(4, 0, 10));
        landmarks.put("base.back_door", section("base").at(17, 0, 18));
        landmarks.put("path.campfire", section("path").at(8, 0, 14));
        landmarks.put("path.route", section("path").at(25, 1, 7));
        int[][] lights = {{1, -4}, {8, 4}, {16, -2}, {23, 12}, {31, 16}};
        for (int i = 0; i < lights.length; i++) landmarks.put("path.torch." + i, section("path").at(lights[i][0], 1, lights[i][1]));
        landmarks.put("mine.route", section("mine").at(10, 1, 11));
        landmarks.put("mine.lintel", section("mine").at(13, 4, 15));
        landmarks.put("mine.end", section("mine").at(27, 0, 15));
        LANDMARKS = Collections.unmodifiableMap(landmarks);
    }
    private MapLayout() {}
    public static Map<String, Section> sections() { return SECTIONS; }
    public static Map<String, Checkpoint> checkpoints() { return CHECKPOINTS; }
    public static Map<String, BlockBox> triggers() { return TRIGGERS; }
    public static Map<String, BlockPos> landmarks() { return LANDMARKS; }
    public static Section section(String id) { return required(SECTIONS, id); }
    public static Checkpoint checkpoint(String id) { return required(CHECKPOINTS, id); }
    public static BlockBox trigger(String id) { return required(TRIGGERS, id); }
    public static BlockPos landmark(String id) { return required(LANDMARKS, id); }
    private static <T> T required(Map<String, T> values, String id) {
        T result = values.get(id);
        if (result == null) throw new IllegalArgumentException("Unknown map key: " + id);
        return result;
    }
}
