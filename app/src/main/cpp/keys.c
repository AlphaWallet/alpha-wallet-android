
#include <string.h>
#include <alloca.h>
#include <jni.h>
#if __has_include ("..\..\..\..\..\keys.secret")
#   define HAS_KEYS 1
#   include "..\..\..\..\..\keys.secret"
#else
#   define HAS_KEYS 0
#endif

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_EthereumNetworkBase_getAmberDataKey( JNIEnv* env, jobject thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, amberdataKey);
#else
    const jstring key = "obtain-api-key-from-amberdata-io";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_EthereumNetworkBase_getInfuraKey( JNIEnv* env, jobject thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, infuraKey);
#else
    const jstring key = "da3717f25f824cc1baa32d812386d93f";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_service_TickerService_getAmberDataKey( JNIEnv* env, jobject thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, amberdataKey);
#else
    const jstring key = "obtain-api-key-from-amberdata-io";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_service_TickerService_getCMCKey( JNIEnv* env, jobject thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, cmcKey);
#else
    const jstring key = "ea2d0a6b-7e77-4015-bccf-4877e5c5b882";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_service_AnalyticsService_getAnalyticsKey( JNIEnv* env, jobject thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, mixpanelKey);
#else
    const jstring key = "d4c1140e21f6204184bb1ea02eb84412";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_viewmodel_Erc20DetailViewModel_getRampKey( JNIEnv* env, jobject thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, rampKey);
#else
    const jstring key = "asfjkdhvcmbnekjfhskjdhfskjdhfskjdhfsdkjf"; // <-- replace with your Ramp key
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_OnRampRepository_getRampKey( JNIEnv* env, jobject thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, rampKey);
#else
    const jstring key = "asfjkdhvcmbnekjfhskjdhfskjdhfskjdhfsdkjf"; // <-- replace with your Ramp key
    return (*env)->NewStringUTF(env, key);
#endif
}