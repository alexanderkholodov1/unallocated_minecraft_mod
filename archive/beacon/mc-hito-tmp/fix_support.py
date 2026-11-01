from pathlib import Path
import json
p=Path('fabric-api-modules.json');data=json.loads(p.read_text());data['modules']['fabric-registry-sync-v0']['reason']='Allows custom sound registration before registry freeze and synchronizes mod registry IDs.';p.write_text(json.dumps(data,indent=2)+'\n')
p=Path('scripts/install_headless_graphics.sh');s=p.read_text().replace('    sudo apt-get install -y --no-download --no-install-recommends "${debs[@]}"\nelse', '''    if sudo apt-get install -y --no-download --no-install-recommends "${debs[@]}"; then
        echo 'Installed cached headless graphics packages.'
    else
        echo 'Cached package dependency closure is incomplete; resolving from Ubuntu.'
        debs=()
    fi
fi
if (( ${#debs[@]} == 0 )); then''');p.write_text(s)
p=Path('src/main/java/org/unallocated/bait/SpyglassBaitSelfTest.java');s=p.read_text().replace('    private record Run(ServerLevel level, BlockPos origin, int[] waited) { }','''    private static final class Run {
        final ServerLevel level;
        final BlockPos origin;
        final List<String> problems = new ArrayList<>();
        int waited;
        @Nullable Mannequin figure;
        @Nullable Vec3 spot;
        @Nullable Vec3 next;
        @Nullable Vec3 viewer;
        Run(ServerLevel level, BlockPos origin) { this.level = level; this.origin = origin; }
        ServerLevel level() { return level; }
        BlockPos origin() { return origin; }
    }''').replace('new Run(level, origin, new int[1])','new Run(level, origin)')
a=s.index('        if (++current.waited()[0]');b=s.index('    private static void finish',a)
s=s[:a]+'''        if (++current.waited > TIMEOUT_TICKS) {
            finish(current, current.figure == null ? "chunk never loaded" : "not found in level after entity activation");
            return;
        }
        try {
            if (current.figure == null) {
                if (current.waited < 5 || !current.level.hasChunkAt(current.origin)) return;
                String failure = prepare(current);
                if (failure != null) finish(current, failure);
                return; // Entity visibility is applied by the chunk/entity manager on a subsequent tick.
            }
            if (current.level.getEntity(current.figure.getUUID()) != current.figure) return;
            finish(current, checkActive(current));
        } catch (RuntimeException exception) {
            UnallocatedMod.LOGGER.error("UNALLOCATED bait selftest threw", exception);
            finish(current, exception.toString());
        }
    }

'''+s[b:]
s=s.replace('        run = null;\n        figureId = null;', '        if (current.figure != null && !current.figure.isRemoved()) current.figure.discard();\n        run = null;\n        figureId = null;')
s=s.replace('    private static @Nullable String check(ServerLevel level, BlockPos origin) {', '''    private static @Nullable String prepare(Run current) {
        ServerLevel level = current.level;
        BlockPos origin = current.origin;''')
s=s.replace('        List<String> problems = new ArrayList<>();','        List<String> problems = current.problems;')
a=s.index('        figureId = figure.getUUID();');b=s.index('        if (!figure.isRemoved())',a)
s=s[:a]+'''        figureId = figure.getUUID();
        current.figure = figure;
        current.spot = spot;
        current.next = next;
        current.viewer = viewer;
        return null;
    }

    private static @Nullable String checkActive(Run current) {
        Mannequin figure = java.util.Objects.requireNonNull(current.figure);
        Vec3 spot = java.util.Objects.requireNonNull(current.spot);
        Vec3 next = java.util.Objects.requireNonNull(current.next);
        Vec3 viewer = java.util.Objects.requireNonNull(current.viewer);
        List<String> problems = current.problems;
        // tick() requires the exact spawned entity in the level before these checks can pass.
        if (!figure.isUsingItem()) problems.add("spyglass not raised");
        if (figure.shouldBeSaved()) problems.add("would be saved (save mixin not applied)");
        double turn = (SpyglassBaitFigure.yawToward(spot, viewer) - figure.getYHeadRot()) % 360.0;
        if (Math.abs((turn + 540.0) % 360.0 - 180.0) > 1.0) problems.add("not facing");
        SpyglassBaitFigure.moveTo(figure, next, viewer);
        if (figure.position().distanceTo(next) > 1.0E-6) problems.add("move");
        figure.discard();
'''+s[b:];p.write_text(s)
