package androidx.test.internal.runner.lifecycle;
// Minimal stand-in (the real androidx.test library is only on Google's Maven, unreachable from the build machine).
public class ApplicationLifecycleMonitorImpl implements androidx.test.runner.lifecycle.ApplicationLifecycleMonitor {
    public void signalLifecycleChange(android.app.Application a, androidx.test.runner.lifecycle.ApplicationStage s) {}
}
