package androidx.test.internal.runner.intent;
// Minimal stand-in (the real androidx.test library is only on Google's Maven, unreachable from the build machine).
public class IntentMonitorImpl implements androidx.test.runner.intent.IntentMonitor {
    public void signalIntent(android.content.Intent i) {}
}
