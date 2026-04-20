# TubeTone proguard rules

# NewPipeExtractor transitive deps (Rhino, jsoup, re2j) reference classes
# not present on Android. Silence R8 warnings — the referencing code paths
# are not exercised at runtime on Android.
-dontwarn com.google.re2j.Matcher
-dontwarn com.google.re2j.Pattern
-dontwarn java.beans.BeanDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
-dontwarn javax.script.ScriptEngineFactory

# Rhino JavaScript engine (used by NewPipe for signature decryption)
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.** { *; }

# NewPipeExtractor
-keep class org.schabi.newpipe.extractor.** { *; }

# Room generated classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
