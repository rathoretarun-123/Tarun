package androidx.test.runner.lifecycle;
// Minimal stand-in (the real androidx.test library is only on Google's Maven, unreachable from the build machine).
public enum Stage { PRE_ON_CREATE, CREATED, STARTED, RESUMED, PAUSED, STOPPED, RESTARTED, DESTROYED }
