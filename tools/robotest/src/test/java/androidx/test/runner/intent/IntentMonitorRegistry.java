package androidx.test.runner.intent;
// Minimal stand-in (the real androidx.test library is only on Google's Maven, unreachable from the build machine).
public final class IntentMonitorRegistry {
    private static IntentMonitor m;
    public static IntentMonitor getInstance() { return m; }
    public static void registerInstance(IntentMonitor x) { m = x; }
}
