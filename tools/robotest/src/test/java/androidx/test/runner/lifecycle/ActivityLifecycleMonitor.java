package androidx.test.runner.lifecycle;
// Minimal stand-in (the real androidx.test library is only on Google's Maven, unreachable from the build machine).
public interface ActivityLifecycleMonitor {
    void addLifecycleCallback(ActivityLifecycleCallback c);
    void removeLifecycleCallback(ActivityLifecycleCallback c);
    java.util.Collection<android.app.Activity> getActivitiesInStage(Stage s);
    Stage getLifecycleStageOf(android.app.Activity a);
}
