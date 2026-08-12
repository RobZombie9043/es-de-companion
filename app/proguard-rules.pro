# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# RetroAchievements/api-kotlin (see CLAUDE.md's RetroAchievements section) - its response
# models are deserialized by Gson via reflection, so their fields must survive
# minification/obfuscation unchanged, and its RetroInterface is a Retrofit dynamic-proxy
# interface whose method/annotation shapes Retrofit needs at runtime. This is a best-effort
# starting point, not yet verified against a real minified build - see CLAUDE.md's open risk
# on this.
-keep class org.retroachivements.api.data.** { *; }
-keep interface org.retroachivements.api.RetroInterface { *; }
-keepattributes Signature
-keepattributes *Annotation*