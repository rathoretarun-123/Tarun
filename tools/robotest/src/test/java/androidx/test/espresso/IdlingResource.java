package androidx.test.espresso;
// Minimal stand-in (the real androidx.test library is only on Google's Maven, unreachable from the build machine).
public interface IdlingResource {
    String getName();
    boolean isIdleNow();
    void registerIdleTransitionCallback(ResourceCallback c);
    interface ResourceCallback { void onTransitionToIdle(); }
}
