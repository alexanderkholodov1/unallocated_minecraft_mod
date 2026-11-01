package org.unallocated.creature;

import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CreatureMotionTest {
    private static final CreatureMotion.Point PLAYER = new CreatureMotion.Point(0, 64, 0);
    @Test void stalkingMovesOnlyWhileUnseenAndNeverCrossesMinimumDistance() {
        var random = new Random(17);
        for (int i = 0; i < 1000; i++) {
            var from = new CreatureMotion.Point(4 + random.nextDouble() * 40, 64, random.nextDouble() * 40);
            double step = random.nextDouble() * 50;
            assertEquals(from, CreatureMotion.toward(from, PLAYER, step, 3, true, at -> true));
            var next = CreatureMotion.toward(from, PLAYER, step, 3, false, at -> true);
            assertTrue(next.distance(PLAYER) >= 3 - 1e-7);
            assertTrue(next.distance(from) <= step + 1e-7);
            assertTrue(next.distance(PLAYER) <= from.distance(PLAYER));
        }
    }
    @Test void chaseCannotJumpThroughAWallEvenWithALargeStep() {
        var from = new CreatureMotion.Point(10, 64, 0);
        var next = CreatureMotion.toward(from, PLAYER, 100, 0, false, at -> at.x() > 5.1 || at.x() < 4.9);
        assertTrue(next.x() > 5.1, next.toString());
        assertEquals(from, CreatureMotion.toward(from, PLAYER, 1, 0, false, at -> false));
    }
    @Test void steeringDoesNotOvershootPlayerOrMoveAnAlreadyCloseStalker() {
        var from = new CreatureMotion.Point(1, 64, 0);
        assertEquals(from, CreatureMotion.toward(from, PLAYER, 10, 3, false, at -> true));
        assertEquals(PLAYER, CreatureMotion.toward(from, PLAYER, 10, 0, false, at -> true));
        assertEquals(from, CreatureMotion.toward(from, PLAYER, 0, 0, false, at -> true));
    }
    @Test void visibilityUsesTheBaitsConservativeAnglesAndRequiresLineOfSight() {
        assertTrue(CreatureMotion.inView(Math.cos(Math.toRadians(70)), 10, false, true));
        assertFalse(CreatureMotion.inView(Math.cos(Math.toRadians(81)), 10, false, true));
        assertFalse(CreatureMotion.inView(1, 10, false, false));
        assertFalse(CreatureMotion.inView(Math.cos(Math.toRadians(6)), 10, true, true));
        assertTrue(CreatureMotion.inView(1, 10, true, true));
    }
}
