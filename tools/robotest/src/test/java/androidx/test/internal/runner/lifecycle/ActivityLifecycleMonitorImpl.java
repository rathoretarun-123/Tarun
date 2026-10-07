package androidx.test.internal.runner.lifecycle;
// Minimal stand-in (the real androidx.test library is only on Google's Maven, unreachable from the build machine).
import androidx.test.runner.lifecycle.*;
import java.util.*;
public class ActivityLifecycleMonitorImpl implements ActivityLifecycleMonitor {
    private final List<ActivityLifecycleCallback> cbs = new ArrayList<ActivityLifecycleCallback>();
    private final Map<android.app.Activity, Stage> stages = new IdentityHashMap<android.app.Activity, Stage>();
    public ActivityLifecycleMonitorImpl() {}
    public ActivityLifecycleMonitorImpl(boolean b) {}
    public void addLifecycleCallback(ActivityLifecycleCallback c) { cbs.add(c); }
    public void removeLifecycleCallback(ActivityLifecycleCallback c) { cbs.remove(c); }
    public Collection<android.app.Activity> getActivitiesInStage(Stage s) {
        List<android.app.Activity> o = new ArrayList<android.app.Activity>();
        for (Map.Entry<android.app.Activity, Stage> e : stages.entrySet()) if (e.getValue() == s) o.add(e.getKey());
        return o;
    }
    public Stage getLifecycleStageOf(android.app.Activity a) { return stages.get(a); }
    public void signalLifecycleChange(Stage s, android.app.Activity a) {
        stages.put(a, s);
        for (ActivityLifecycleCallback c : new ArrayList<ActivityLifecycleCallback>(cbs)) c.onActivityLifecycleChanged(a, s);
    }
}
