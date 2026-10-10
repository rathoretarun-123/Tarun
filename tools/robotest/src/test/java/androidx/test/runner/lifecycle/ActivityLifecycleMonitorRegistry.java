package androidx.test.runner.lifecycle;
// Minimal stand-in (the real androidx.test library is only on Google's Maven, unreachable from the build machine).
public final class ActivityLifecycleMonitorRegistry {
    private static ActivityLifecycleMonitor m;
    public static ActivityLifecycleMonitor getInstance() { return m; }
    public static void registerInstance(ActivityLifecycleMonitor x) { m = x; }
}
