# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/marat/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-dontpreverify
-verbose
-printseeds seeds.txt
-printusage unused.txt
-printmapping mapping.txt
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

#Wallet core
-keep class wallet.core.jni.** {*;}

#Realm
-keepnames public class * extends io.realm.RealmObject
-keep @io.realm.annotations.RealmModule class *
-keep class io.realm.** { *; }
-dontwarn javax.**
-dontwarn io.realm.**
-dontwarn rx.**
-dontwarn okio.**
-dontwarn retrofit.**
-dontwarn retrofit.appengine.UrlFetchClient

#Analytics
-keep class com.google.firebase.** { *; }

-keepclassmembers class com.alphawallet.** { *; }

-keepclassmembers class io.stormbird.wallet.** { *; }

-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }

-keep class !com.alphawallet.** { *; }