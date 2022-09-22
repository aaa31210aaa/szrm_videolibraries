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
-dontskipnonpubliclibraryclassmembers
-printconfiguration
-keep,allowobfuscation @interface androidx.annotation.Keep

-keep @androidx.annotation.Keep class *
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

#-keep class com.tencent.** { *; }
#-keep class common.model.** {*;}
#-keep class common.callback.**{*;}
#-keep class common.utils.AppInit {*;}
#-keep class common.utils.OkGoUtils {*;}
#-keep class adpter.** { *; }
#-keep class ui.activity.VideoDetailActivity.**
#-keep class tencent.liteav.demo.superplayer.**{*;}
#-keep class widget.**{*;}
#-keep class common.callback.**{*;}
#-keep class common.constants.**{*;}
#-keep class common.http.**{*;}
#-keep class common.manager.**{*;}
#-keep class common.model.**{*;}
#-keep class common.utils.**{*;}
#-keep class common.widget.**{*;}
-keep class common.**{*;}
-keep class utils.**{*;}
-keep class model.bean.**{*;}
-keep class ui.activity.**{*;}
-keep class ui.fragment.**{*;}
-keep class res.**{*;}
