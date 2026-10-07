package androidx.test.platform.ui;
// Minimal stand-in (the real androidx.test library is only on Google's Maven, unreachable from the build machine).
public class InjectEventSecurityException extends Exception { public InjectEventSecurityException(String m) { super(m); } public InjectEventSecurityException(Throwable t) { super(t); } }
