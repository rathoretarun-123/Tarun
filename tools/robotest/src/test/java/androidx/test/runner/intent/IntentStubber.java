package androidx.test.runner.intent;
// Minimal stand-in (the real androidx.test library is only on Google's Maven, unreachable from the build machine).
public interface IntentStubber { android.app.Instrumentation.ActivityResult getActivityResultForIntent(android.content.Intent i); }
