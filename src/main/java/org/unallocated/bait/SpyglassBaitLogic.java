package org.unallocated.bait;

import java.util.SplittableRandom;

/**
 * The sighting/approach rules of the spyglass bait, without Minecraft: one call to {@link #tick} per server tick with
 * what the player can see, returning what the world adapter should do. The adapter reports back whether a placement
 * or move succeeded.
 *
 * <p>The rule that makes it frightening rather than silly: the figure is <b>never moved while the player can see
 * it</b>. Every move happens while it is out of view, behind terrain, outside the spyglass circle, or during the first
 * ticks of raising the spyglass, when the zoom animation itself hides the jump. So it is never seen moving; it is just
 * closer each time you look (the owner's "sin moverse entre intentos").
 */
public final class SpyglassBaitLogic {
    /** Ticks after the scope goes up during which the zoom-in animation hides a jump of the figure. */
    public static final int ZOOM_MASK_TICKS = 3;
    /** Ticks the screamer image and figure stay before the figure is gone. */
    public static final int SCARE_HOLD_TICKS = 16;
    /** Ticks a plain (no spyglass) look must last to count once the fallback is active. */
    public static final int NAKED_DWELL_TICKS = 24;
    static final int RETRY_TICKS = 20;
    static final int LURE_COOLDOWN_TICKS = 40;
    static final int APPEAR_TIMEOUT_TICKS = 2400;
    static final int MAX_LOST_TICKS = 600;

    public enum Phase { WAITING, WATCHING, SCARING, GONE }

    public enum Action { NONE, PLACE, MOVE, SCARE, REMOVE }

    /**
     * What the player perceives this tick.
     *
     * @param figurePresent the figure entity exists and is loaded
     * @param scoping       the player is looking through a spyglass
     * @param inScope       the figure is inside the spyglass circle with a clear line of sight
     * @param inView        the figure is inside the plain field of view with a clear line of sight
     * @param lookedAt      the figure is near the centre of the plain view with a clear line of sight
     * @param distance      blocks from the player's eyes to the figure
     * @param figureHurt    the figure was damaged (shot, hit)
     */
    public record Observation(boolean figurePresent, boolean scoping, boolean inScope, boolean inView,
            boolean lookedAt, double distance, boolean figureHurt) {
        public boolean seen() {
            return scoping ? inScope : inView;
        }
    }

    /**
     * @param action     what to do
     * @param distance   target distance for PLACE and MOVE
     * @param unseen     the figure may be adjusted (turned to face the player) without being seen
     * @param zoomMasked only the zoom-in hides this MOVE: keep the same bearing so it reads as part of the zoom
     * @param cause      short reason, for the log
     */
    public record Decision(Action action, double distance, boolean unseen, boolean zoomMasked, String cause) {
        static Decision none(boolean unseen) {
            return new Decision(Action.NONE, 0, unseen, false, "");
        }
    }

    private final SpyglassBaitSpec spec;
    private final int scareDelay;
    private Phase phase = Phase.WAITING;
    private int age;
    private int step;
    private int sightings;
    private int scopeUses;
    private boolean pendingStep;
    private boolean pendingIsStep;
    private boolean vanishWhenUnseen;
    private boolean wasScoping;
    private boolean sessionCounted;
    private boolean nakedCounted;
    private int scopeTicks;
    private int dwell;
    private int nakedDwell;
    private int finalLook;
    private int lastMoveAttempt = Integer.MIN_VALUE / 2;
    private int lastMove = Integer.MIN_VALUE / 2;
    private int lastSpyglassSighting;
    private int phaseTicks;
    private int lostTicks;
    private boolean forceScare;
    private String lastCause = "appear";

    public SpyglassBaitLogic(SpyglassBaitSpec spec) {
        this.spec = spec;
        this.scareDelay = spec.scareDelayTicks()
                + (spec.scareJitterTicks() == 0 ? 0 : new SplittableRandom(spec.seed()).nextInt(spec.scareJitterTicks() + 1));
    }

    public Decision tick(Observation o) {
        age++;
        if (o.scoping() && !wasScoping) {
            scopeTicks = 0;
            sessionCounted = false;
            scopeUses++;
        }
        wasScoping = o.scoping();
        scopeTicks = o.scoping() ? scopeTicks + 1 : 0;
        return switch (phase) {
            case GONE -> Decision.none(true);
            case WAITING -> waiting();
            case SCARING -> {
                if (++phaseTicks >= SCARE_HOLD_TICKS) yield end("scare-end");
                yield Decision.none(false);
            }
            case WATCHING -> watching(o);
        };
    }

    private Decision waiting() {
        int waited = age - spec.appearDelayTicks();
        if (waited < 0) return Decision.none(true);
        if (waited > APPEAR_TIMEOUT_TICKS) return end("appear-timeout");
        return age - lastMoveAttempt >= RETRY_TICKS ? place(distanceOf(0), "appear") : Decision.none(true);
    }

    private Decision watching(Observation o) {
        boolean seen = o.seen();
        boolean masked = o.scoping() && scopeTicks <= ZOOM_MASK_TICKS;
        boolean unseen = !seen || masked;

        if (!o.figurePresent()) {
            if (++lostTicks > MAX_LOST_TICKS) return end("lost");
            return age - lastMoveAttempt >= RETRY_TICKS ? place(distanceOf(step), "lost") : Decision.none(true);
        }
        lostTicks = 0;
        if (forceScare || o.figureHurt()) {
            forceScare = false;
            if (spec.scare()) return scare(o.figureHurt() ? "hurt" : "forced");
            vanishWhenUnseen = true;
        }
        if (unseen && (vanishWhenUnseen || age > spec.maxLifetimeTicks())) {
            return end(vanishWhenUnseen ? "vanish" : "lifetime");
        }
        if (vanishWhenUnseen) return Decision.none(false);

        // The player walked up to it while watching: it is already too close.
        if (step < spec.lastStep() && seen && o.distance() <= distanceOf(spec.lastStep())) {
            step = spec.lastStep();
            pendingStep = false;
            lastCause = "walked";
        }

        if (step == spec.lastStep()) {
            boolean looking = o.scoping() ? o.inScope() && !masked : o.lookedAt();
            finalLook = looking ? finalLook + 1 : 0;
            if (finalLook > scareDelay) {
                if (spec.scare()) return scare("too-close");
                vanishWhenUnseen = true;
                return Decision.none(false);
            }
        }

        boolean zoomOnly = seen && masked;
        if (pendingStep && unseen && age - lastMoveAttempt >= RETRY_TICKS / 2) {
            pendingIsStep = true;
            return move(distanceOf(step + 1), lastCause, zoomOnly);
        }

        if (!pendingStep && step < spec.lastStep()) {
            countSightings(o, masked);
            if (pendingStep && unseen) {
                pendingIsStep = true;
                return move(distanceOf(step + 1), lastCause, zoomOnly);
            }
        }

        double wanted = distanceOf(step);
        if (!seen && !pendingStep && age - lastMove >= LURE_COOLDOWN_TICKS
                && age - lastMoveAttempt >= RETRY_TICKS) {
            if (o.distance() < wanted - Math.max(6.0, wanted * 0.15)) {
                pendingIsStep = false;
                return move(wanted, "recede", false);
            }
            if (o.distance() > wanted + Math.max(16.0, wanted * 0.4)) {
                pendingIsStep = false;
                return move(wanted, "follow", false);
            }
        }
        return Decision.none(unseen);
    }

    private void countSightings(Observation o, boolean masked) {
        if (o.scoping() && o.inScope() && !masked) {
            dwell++;
        } else {
            dwell = 0;
        }
        if (dwell >= spec.dwellTicks() && !sessionCounted) {
            sessionCounted = true;
            lastSpyglassSighting = age;
            sightings++;
            requestStep("spyglass");
            return;
        }
        // Never (or no longer) using the spyglass: plain looks bring it closer too, one step per look.
        boolean fallback = spec.fallbackAfterTicks() > 0 && age - lastSpyglassSighting >= spec.fallbackAfterTicks();
        if (!o.scoping() && o.lookedAt()) {
            nakedDwell++;
        } else if (!o.seen()) {
            nakedDwell = 0;
            nakedCounted = false;
        }
        if (fallback && nakedDwell >= NAKED_DWELL_TICKS && !nakedCounted) {
            nakedCounted = true;
            sightings++;
            requestStep("fallback");
        }
    }

    private void requestStep(String cause) {
        pendingStep = true;
        lastCause = cause;
    }

    /** The adapter placed (or failed to place) the figure after a PLACE decision. */
    public void placed(boolean ok) {
        if (ok) {
            lastMove = age;
            if (phase == Phase.WAITING) {
                phase = Phase.WATCHING;
                lastSpyglassSighting = age;
            }
        }
    }

    /** The adapter moved (or failed to move) the figure after a MOVE decision. */
    public void moved(boolean ok) {
        if (!ok) return;
        lastMove = age;
        if (pendingIsStep && pendingStep) {
            step++;
            pendingStep = false;
            dwell = 0;
            nakedDwell = 0;
            nakedCounted = false;
            finalLook = 0;
        }
        pendingIsStep = false;
    }

    /** Director/act budget: bring the figure one step closer at the next unseen moment. */
    public boolean forceStep() {
        if (phase != Phase.WATCHING || pendingStep || step >= spec.lastStep()) return false;
        requestStep("forced");
        return true;
    }

    /** Director/act budget: fire the screamer now (or vanish if the spec has the scare off). */
    public boolean forceScare() {
        if (phase != Phase.WATCHING) return false;
        forceScare = true;
        return true;
    }

    /** The bait was cleared from outside (command, beat, player left). */
    public void cancel() {
        phase = Phase.GONE;
    }

    private Decision place(double distance, String cause) {
        lastMoveAttempt = age;
        return new Decision(Action.PLACE, distance, true, false, cause);
    }

    private Decision move(double distance, String cause, boolean zoomMasked) {
        lastMoveAttempt = age;
        return new Decision(Action.MOVE, distance, true, zoomMasked, cause);
    }

    private Decision scare(String cause) {
        phase = Phase.SCARING;
        phaseTicks = 0;
        return new Decision(Action.SCARE, 0, false, false, cause);
    }

    private Decision end(String cause) {
        phase = Phase.GONE;
        return new Decision(Action.REMOVE, 0, true, false, cause);
    }

    private double distanceOf(int index) {
        return spec.distances().get(Math.min(index, spec.lastStep()));
    }

    public SpyglassBaitSpec spec() { return spec; }
    public Phase phase() { return phase; }
    public int step() { return step; }
    public int sightings() { return sightings; }
    public int scopeUses() { return scopeUses; }
    public boolean pendingStep() { return pendingStep; }
    public int age() { return age; }
    public int scareDelay() { return scareDelay; }
    /** Distance the figure keeps at the current step. */
    public double currentDistance() { return distanceOf(step); }
}
