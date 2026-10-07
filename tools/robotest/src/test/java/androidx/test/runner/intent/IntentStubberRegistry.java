package androidx.test.runner.intent;
// Minimal stand-in (the real androidx.test library is only on Google's Maven, unreachable from the build machine).
public final class IntentStubberRegistry {
    public static IntentStubber getInstance() { return null; }
    public static boolean isLoaded() { return false; }
}
