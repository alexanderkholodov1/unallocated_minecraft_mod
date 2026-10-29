package org.unallocated.bait;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.unallocated.bait.SpyglassBaitLogic.Action;
import org.unallocated.bait.SpyglassBaitLogic.Decision;
import org.unallocated.bait.SpyglassBaitLogic.Observation;
import org.unallocated.bait.SpyglassBaitLogic.Phase;

class SpyglassBaitLogicTest {
    private static final SpyglassBaitSpec THREE = SpyglassBaitSpec.defaults()
            .withDistances(List.of(60.0, 40.0, 20.0)).withTiming(10, 5, 0).withFallbackAfterTicks(0);

    private static Observation hidden(double distance) {
        return new Observation(true, false, false, false, false, distance, false);
    }

    private static Observation looking(double distance) {
        return new Observation(true, false, false, true, true, distance, false);
    }

    private static Observation peripheral(double distance) {
        return new Observation(true, false, false, true, false, distance, false);
    }

    private static Observation scope(double distance) {
        return new Observation(true, true, true, true, true, distance, false);
    }

    private static Observation scopeElsewhere(double distance) {
        return new Observation(true, true, false, false, false, distance, false);
    }

    private static SpyglassBaitLogic placed(SpyglassBaitSpec spec) {
        SpyglassBaitLogic logic = new SpyglassBaitLogic(spec);
        for (int i = 0; i < spec.appearDelayTicks() + 5; i++) {
            Decision decision = logic.tick(hidden(spec.distances().getFirst()));
            if (decision.action() == Action.PLACE) {
                logic.placed(true);
                return logic;
            }
        }
        return fail("figure never placed");
    }

    /** Ticks with the same observation; fails if any decision other than NONE appears. */
    private static void quiet(SpyglassBaitLogic logic, Observation observation, int ticks) {
        for (int i = 0; i < ticks; i++) {
            Decision decision = logic.tick(observation);
            assertEquals(Action.NONE, decision.action(), "tick " + i + " " + observation + " -> " + decision);
        }
    }

    /** One full spyglass use on the figure: raise, hold, lower. Applies a step move if the zoom mask allows it. */
    private static void useSpyglass(SpyglassBaitLogic logic, double distance, int holdTicks) {
        for (int i = 0; i < holdTicks; i++) {
            Decision decision = logic.tick(scope(distance));
            if (decision.action() == Action.MOVE) {
                assertTrue(i < SpyglassBaitLogic.ZOOM_MASK_TICKS, "moved while visible in the scope at tick " + i);
                logic.moved(true);
            } else if (decision.action() != Action.NONE) {
                return;
            }
        }
    }

    @Test
    void appearsAfterTheDelayAtTheFirstDistance() {
        SpyglassBaitLogic logic = new SpyglassBaitLogic(THREE.withAppearDelayTicks(40));
        for (int i = 1; i < 40; i++) assertEquals(Action.NONE, logic.tick(hidden(0)).action());
        Decision decision = logic.tick(hidden(0));
        assertEquals(Action.PLACE, decision.action());
        assertEquals(60.0, decision.distance());
        assertEquals(Phase.WAITING, logic.phase());
        logic.placed(true);
        assertEquals(Phase.WATCHING, logic.phase());
    }

    @Test
    void oneSpyglassUseCountsOnceAndTheFigureWaitsUntilUnseen() {
        SpyglassBaitLogic logic = placed(THREE);
        quiet(logic, scope(60), 40);
        assertEquals(1, logic.sightings());
        assertTrue(logic.pendingStep());
        quiet(logic, looking(60), 30);
        quiet(logic, peripheral(60), 30);
        assertEquals(0, logic.step(), "never moves while seen with the naked eye");
        Decision decision = logic.tick(hidden(60));
        assertEquals(Action.MOVE, decision.action());
        assertEquals(40.0, decision.distance());
        logic.moved(true);
        assertEquals(1, logic.step());
        assertFalse(logic.pendingStep());
    }

    @Test
    void reRaisingTheSpyglassHidesTheStepInTheZoomAnimation() {
        SpyglassBaitLogic logic = placed(THREE);
        useSpyglass(logic, 60, 20);
        quiet(logic, looking(60), 10);
        Decision decision = logic.tick(scope(60));
        assertEquals(Action.MOVE, decision.action(), "first scope tick is masked by the zoom-in");
        logic.moved(true);
        assertEquals(1, logic.step());
        quiet(logic, scope(40), 20);
        assertEquals(2, logic.sightings(), "the same use that revealed it closer counts as the next sighting");
    }

    @Test
    void panningTheScopeAwayAlsoHidesIt() {
        SpyglassBaitLogic logic = placed(THREE);
        quiet(logic, scope(60), 20);
        Decision decision = logic.tick(scopeElsewhere(60));
        assertEquals(Action.MOVE, decision.action());
        logic.moved(true);
        quiet(logic, scope(40), 20);
        assertEquals(1, logic.sightings(), "same scope session: re-finding it does not count again");
    }

    /** The player looks away once: the pending step is applied. */
    private static void lookAway(SpyglassBaitLogic logic, double distance) {
        Decision decision = logic.tick(hidden(distance));
        assertEquals(Action.MOVE, decision.action(), "step applied while unseen");
        logic.moved(true);
    }

    @Test
    void lastLookFiresTheScreamerThenTheFigureIsGone() {
        SpyglassBaitLogic logic = placed(THREE);
        useSpyglass(logic, 60, 20);
        lookAway(logic, 60);
        useSpyglass(logic, 40, 20);
        lookAway(logic, 40);
        assertEquals(2, logic.step(), "at the too-close distance");
        assertEquals(2, logic.sightings());
        assertEquals(Phase.WATCHING, logic.phase());
        int ticks = 0;
        Decision decision;
        do {
            decision = logic.tick(scope(20));
            ticks++;
        } while (decision.action() == Action.NONE && ticks < 100);
        assertEquals(Action.SCARE, decision.action());
        assertEquals(SpyglassBaitLogic.ZOOM_MASK_TICKS + logic.scareDelay() + 1, ticks,
                "short dread after the zoom settles, then the scare");
        assertEquals(Phase.SCARING, logic.phase());
        for (int i = 1; i < SpyglassBaitLogic.SCARE_HOLD_TICKS; i++) {
            assertEquals(Action.NONE, logic.tick(scope(3)).action());
        }
        Decision end = logic.tick(scope(3));
        assertEquals(Action.REMOVE, end.action());
        assertEquals("scare-end", end.cause());
        assertEquals(Phase.GONE, logic.phase());
        assertEquals(Action.NONE, logic.tick(scope(3)).action());
    }

    @Test
    void withTheScareOffItSimplyIsNotThereNextTime() {
        SpyglassBaitLogic logic = placed(THREE.withScare(false).withDistances(List.of(30.0, 12.0)));
        useSpyglass(logic, 30, 20);
        lookAway(logic, 30);
        quiet(logic, scope(12), 40);
        assertEquals(Phase.WATCHING, logic.phase(), "stays while watched");
        Decision decision = logic.tick(hidden(12));
        assertEquals(Action.REMOVE, decision.action());
        assertEquals("vanish", decision.cause());
    }

    @Test
    void walkingTowardItWhileLookingAwayMakesItRecede() {
        SpyglassBaitLogic logic = placed(THREE);
        quiet(logic, looking(45), 100);
        Decision decision = Decision.none(true);
        for (int i = 0; i < 60 && decision.action() == Action.NONE; i++) decision = logic.tick(hidden(45));
        assertEquals(Action.MOVE, decision.action());
        assertEquals("recede", decision.cause());
        assertEquals(60.0, decision.distance(), "keeps its distance: a speck that invites following");
        logic.moved(true);
        assertEquals(0, logic.step(), "receding is not a step");
    }

    @Test
    void walkingAwayMakesItFollowUnseen() {
        SpyglassBaitLogic logic = placed(THREE);
        Decision decision = Decision.none(true);
        for (int i = 0; i < 60 && decision.action() == Action.NONE; i++) decision = logic.tick(hidden(110));
        assertEquals(Action.MOVE, decision.action());
        assertEquals("follow", decision.cause());
    }

    @Test
    void walkingUpToItWhileWatchingEndsInTheScreamer() {
        SpyglassBaitLogic logic = placed(THREE);
        logic.tick(looking(18));
        assertEquals(2, logic.step(), "closer than the last distance is too close");
        Decision decision = Decision.none(false);
        for (int i = 0; i < 40 && decision.action() == Action.NONE; i++) decision = logic.tick(looking(18));
        assertEquals(Action.SCARE, decision.action());
    }

    @Test
    void neverUsingTheSpyglassStillBringsItCloserAfterTheFallbackDelay() {
        SpyglassBaitLogic logic = placed(THREE.withFallbackAfterTicks(200));
        quiet(logic, looking(60), 60);
        quiet(logic, hidden(60), 10);
        assertEquals(0, logic.sightings(), "plain looks do nothing at first");
        quiet(logic, hidden(60), 140);
        quiet(logic, looking(60), SpyglassBaitLogic.NAKED_DWELL_TICKS + 5);
        assertEquals(1, logic.sightings());
        Decision decision = logic.tick(hidden(60));
        assertEquals(Action.MOVE, decision.action());
        assertEquals("fallback", decision.cause());
        logic.moved(true);
        quiet(logic, looking(40), SpyglassBaitLogic.NAKED_DWELL_TICKS + 5);
        assertEquals(2, logic.sightings(), "once active, every plain look counts");
    }

    @Test
    void leavesQuietlyAfterItsLifetimeOnlyWhenUnseen() {
        SpyglassBaitLogic logic = placed(THREE.withMaxLifetimeTicks(300));
        quiet(logic, looking(60), 320);
        Decision decision = logic.tick(hidden(60));
        assertEquals(Action.REMOVE, decision.action());
        assertEquals("lifetime", decision.cause());
    }

    @Test
    void shootingTheFigureBringsItIntoYourFace() {
        SpyglassBaitLogic logic = placed(THREE);
        Decision decision = logic.tick(new Observation(true, false, false, true, true, 60, true));
        assertEquals(Action.SCARE, decision.action());
        assertEquals("hurt", decision.cause());
    }

    @Test
    void aLostFigureIsPlacedAgainThenGivenUp() {
        SpyglassBaitLogic logic = placed(THREE);
        Observation missing = new Observation(false, false, false, false, false, 0, false);
        Decision decision = Decision.none(true);
        for (int i = 0; i < 30 && decision.action() == Action.NONE; i++) decision = logic.tick(missing);
        assertEquals(Action.PLACE, decision.action());
        assertEquals(60.0, decision.distance());
        for (int i = 0; i < SpyglassBaitLogic.MAX_LOST_TICKS + 5; i++) {
            decision = logic.tick(missing);
            if (decision.action() == Action.REMOVE) break;
        }
        assertEquals(Action.REMOVE, decision.action());
        assertEquals(Phase.GONE, logic.phase());
    }

    @Test
    void directorCanForceAStepAndTheScare() {
        SpyglassBaitLogic logic = placed(THREE);
        assertTrue(logic.forceStep());
        quiet(logic, looking(60), 5);
        assertEquals(Action.MOVE, logic.tick(hidden(60)).action());
        logic.moved(true);
        assertEquals(1, logic.step());
        assertEquals(0, logic.sightings(), "forced steps are not sightings");
        assertTrue(logic.forceScare());
        assertEquals(Action.SCARE, logic.tick(hidden(40)).action());
    }

    @Test
    void failedForcedMoveWaitsBeforeRetryingAndDoesNotAdvance() {
        SpyglassBaitLogic logic = placed(THREE);
        assertTrue(logic.forceStep());
        assertEquals(Action.MOVE, logic.tick(hidden(60)).action());
        logic.moved(false);
        assertEquals(0, logic.step());
        quiet(logic, hidden(60), SpyglassBaitLogic.RETRY_TICKS / 2 - 1);
        assertEquals(Action.MOVE, logic.tick(hidden(60)).action());
        logic.moved(true);
        assertEquals(1, logic.step());
    }

    @Test
    void neverPlacedOrMovedWhileVisible() {
        SplittableRandom random = new SplittableRandom(7);
        for (int run = 0; run < 200; run++) {
            SpyglassBaitLogic logic = new SpyglassBaitLogic(SpyglassBaitSpec.defaults().withSeed(run)
                    .withFallbackAfterTicks(random.nextInt(2) * 300));
            boolean wasScoping = false;
            int scopeTicks = 0;
            for (int tick = 0; tick < 3000 && logic.phase() != Phase.GONE; tick++) {
                boolean scoping = random.nextInt(4) == 0 ? !wasScoping : wasScoping;
                boolean inView = random.nextInt(3) > 0;
                boolean inScope = scoping && inView && random.nextBoolean();
                Observation observation = new Observation(true, scoping, inScope, inView,
                        inView && random.nextBoolean(), 5 + random.nextDouble() * 120, false);
                scopeTicks = scoping ? (wasScoping ? scopeTicks + 1 : 1) : 0;
                wasScoping = scoping;
                Decision decision = logic.tick(observation);
                boolean masked = scoping && scopeTicks <= SpyglassBaitLogic.ZOOM_MASK_TICKS;
                boolean ok = random.nextInt(5) > 0;
                switch (decision.action()) {
                    case MOVE -> {
                        assertTrue(!observation.seen() || masked, "run " + run + " tick " + tick + ": " + decision);
                        logic.moved(ok);
                    }
                    // Placement has no figure yet; the adapter must choose a spot out of view.
                    case PLACE -> {
                        assertTrue(decision.unseen());
                        logic.placed(ok);
                    }
                    case REMOVE -> {
                        if (decision.cause().equals("vanish") || decision.cause().equals("lifetime")) {
                            assertTrue(!observation.seen() || masked, "quiet removals happen unseen: " + decision);
                        }
                    }
                    default -> { }
                }
            }
        }
    }

    @Test
    void scareMomentIsSeededAndBounded() {
        SpyglassBaitSpec spec = SpyglassBaitSpec.defaults().withTiming(10, 14, 10);
        for (long seed = 0; seed < 50; seed++) {
            int delay = new SpyglassBaitLogic(spec.withSeed(seed)).scareDelay();
            assertEquals(delay, new SpyglassBaitLogic(spec.withSeed(seed)).scareDelay());
            assertTrue(delay >= 14 && delay <= 24, "delay " + delay);
        }
    }

    @Test
    void specRejectsDistancesThatDoNotApproach() {
        assertThrows(IllegalArgumentException.class, () -> THREE.withDistances(List.of(20.0, 40.0)));
        assertThrows(IllegalArgumentException.class, () -> THREE.withDistances(List.of(40.0, 40.0)));
        assertThrows(IllegalArgumentException.class, () -> THREE.withDistances(List.of()));
        assertThrows(IllegalArgumentException.class, () -> THREE.withDistances(List.of(400.0, 10.0)));
        assertThrows(IllegalArgumentException.class, () -> THREE.withTiming(0, 1, 1));
        List<Double> steps = SpyglassBaitSpec.geometric(5, 92, 14);
        assertEquals(5, steps.size());
        assertEquals(92.0, steps.getFirst());
        assertEquals(14.0, steps.getLast());
        THREE.withDistances(steps);
        assertEquals(List.of(10.0), SpyglassBaitSpec.geometric(1, 50, 10));
        assertEquals(4, SpyglassBaitSpec.defaults().lastStep());
    }

    @Test
    void shortRenderDistanceShrinksTheApproachProportionally() {
        SpyglassBaitSpec spec = SpyglassBaitSpec.defaults();
        assertEquals(spec, spec.fittedTo(150), "fits already");
        List<Double> fitted = spec.fittedTo(46).distances();
        assertEquals(List.of(46.0, 32.0, 22.0, 14.0, 7.0), fitted);
        List<Double> tiny = spec.fittedTo(10).distances();
        assertEquals(10.0, tiny.getFirst());
        assertEquals(SpyglassBaitSpec.MIN_DISTANCE, tiny.getLast());
        for (int i = 1; i < tiny.size(); i++) assertTrue(tiny.get(i) < tiny.get(i - 1));
    }
}
