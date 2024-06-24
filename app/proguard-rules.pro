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
-keepattributes AnnotationDefault,RuntimeVisibleAnnotations

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-dontpreverify
-allowaccessmodification
-verbose
-printseeds seeds.txt
-printusage unused.txt
-printmapping mapping.txt
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

-dontwarn jnr.posix.**
-dontwarn org.slf4j.**

-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

#---------------Begin: proguard configuration for support library  ----------
-keepclassmembers class * extends android.content.Context {
    public void *(android.view.View);
    public void *(android.view.MenuItem);
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

#trust library
-keep class wallet.core.jni.HDWallet { *; }
-keep class wallet.core.jni.Hash { *; }
-keep class wallet.core.jni.Curve { *; }
-keep class wallet.core.jni.CoinType { *; }
-keep class wallet.core.jni.Mnemonic { *; }
-keep class wallet.core.jni.PrivateKey { *; }
-keep class wallet.core.jni.proto.** { *; }

#entities, jsInterface & listeners
-keep class com.alphawallet.token.** { *; }
-keep class com.langitwallet.app.walletconnect.** { *; }
-keep class com.langitwallet.app.web3.** { *; }
-keep class com.langitwallet.app.web3j.** { *; }
-keep class com.langitwallet.app.entity.** { *; }
-keep class io.stormbird.wallet.model.api.** { *; }

-keep public class java.beans.* { *; }
-keep class jnr.unixsocket.* { *; }
-keep class org.java_websocket.client.*
-keep class org.java_websocket.handshake.*

-keepclassmembers class org.web3j.protocol.** { *; }
-keepclassmembers class org.web3j.crypto.* { *; }

-keep class * extends org.web3j.abi.TypeReference
-keep class * extends org.web3j.abi.datatypes.Type

-dontwarn org.bouncycastle.jce.provider.X509LDAPCertStoreSpi
-dontwarn org.bouncycastle.x509.util.LDAPStoreHelper

-keep class java.beans.Transient.** {*;}
-keep class java.beans.ConstructorProperties.** {*;}
-keep class java.nio.file.Path.** {*;}

-repackageclasses
#-dontobfuscate
#-printconfiguration ../full-r8-config.txt
