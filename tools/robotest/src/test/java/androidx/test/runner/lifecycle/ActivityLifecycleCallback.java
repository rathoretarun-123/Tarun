package androidx.test.runner.lifecycle;
// Minimal stand-in (the real androidx.test library is only on Google's Maven, unreachable from the build machine).
public interface ActivityLifecycleCallback { void onActivityLifecycleChanged(android.app.Activity a, Stage s); }
