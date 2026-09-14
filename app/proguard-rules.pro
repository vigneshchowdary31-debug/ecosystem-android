# Release build rules.

# Tink references compile-time-only annotation and optional libraries that are not on the
# Android runtime classpath. Only com.google.crypto.tink.subtle.* is used.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.api.client.**
-dontwarn org.joda.time.**

# Remove verbose, debug and info logging from release builds. AppLog is also disabled at
# runtime in non-debuggable builds; this makes sure no call site survives shrinking.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
