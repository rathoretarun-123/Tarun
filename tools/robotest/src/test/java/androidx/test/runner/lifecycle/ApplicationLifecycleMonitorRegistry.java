package androidx.test.runner.lifecycle;
// Minimal stand-in (the real androidx.test library is only on Google's Maven, unreachable from the build machine).
public final class ApplicationLifecycleMonitorRegistry {
    private static ApplicationLifecycleMonitor m;
    public static ApplicationLifecycleMonitor getInstance() { return m; }
    public static void registerInstance(ApplicationLifecycleMonitor x) { m = x; }
}
