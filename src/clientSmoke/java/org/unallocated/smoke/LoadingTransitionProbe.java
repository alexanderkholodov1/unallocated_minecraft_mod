package org.unallocated.smoke;

/** Test-only: keep a genuine transition screen open long enough to capture its rendered caption. */
public final class LoadingTransitionProbe {
    private static long deadline;
    private LoadingTransitionProbe() { }
    public static void holdForCapture() { deadline = System.nanoTime() + 30_000_000_000L; }
    public static void release() { deadline = 0; }
    public static boolean holding() { return deadline != 0 && System.nanoTime() < deadline; }
}
