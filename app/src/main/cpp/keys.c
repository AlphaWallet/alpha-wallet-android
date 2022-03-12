
#include <string.h>
#include <alloca.h>
#include <jni.h>
#if __has_include ("..\..\..\..\..\keys.secret")
#   define HAS_KEYS 1
#   include "..\..\..\..\..\keys.secret"
#else
#   define HAS_KEYS 0
#endif

#ifdef IFKEY
#   define HAS_INFURA 1
#else
#   define HAS_INFURA 0
#endif

#ifdef OSKEY
#   define HAS_OS 1
#else
#   define HAS_OS 0
#endif

#ifdef PSKEY
#   define HAS_PS 1
#else
#   define HAS_PS 0
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
#elif (HAS_INFURA == 1)
    return (*env)->NewStringUTF(env, IFKEY);
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

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_EthereumNetworkBase_getSecondaryInfuraKey( JNIEnv* env, jobject thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, secondaryInfuraKey);
#elif (HAS_INFURA == 1)
    return (*env)->NewStringUTF(env, IFKEY);
#else
    const jstring key = "da3717f25f824cc1baa32d812386d93f";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_service_TransactionsNetworkClient_getBSCExplorerKey( JNIEnv* env, jobject thiz )
{
#if (HAS_KEYS == 1)
    return getBSCExplorerKey(env);
#else
    return (*env)->NewStringUTF(env, "");
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_service_TransactionsNetworkClient_getEtherscanKey( JNIEnv* env, jclass thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, etherscanKey);
#else
    const jstring key = "6U31FTHW3YYHKW6CYHKKGDPHI9HEJ9PU5F";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_service_GasService_getEtherscanKey( JNIEnv* env, jobject thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, etherscanKey);
#else
    const jstring key = "6U31FTHW3YYHKW6CYHKKGDPHI9HEJ9PU5F";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_widget_EmailPromptView_getMailchimpKey(JNIEnv *env, jclass clazz) {
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, mailchimpKey);
#else
    const jstring key = "--";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_service_GasService_getPolygonScanKey(JNIEnv *env, jobject thiz) {
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, polygonScanKey);
#elif (HAS_PS == 1)
    return (*env)->NewStringUTF(env, PSKEY);
#else
    const jstring key = "";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_service_TransactionsNetworkClient_getPolygonScanKey( JNIEnv* env, jclass thiz )
{
    return Java_com_alphawallet_app_service_GasService_getPolygonScanKey(env, thiz);
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_service_TransactionsNetworkClient_getCovalentKey( JNIEnv* env, jclass thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedCKey(env, 4, '_', covalentKey);
#else
    const jstring key = "ckey_9bfb5c8fe0f04c7491231e60ee8"; // <-- Add your covalent key here. This public one could be rate limited
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_service_OpenSeaService_getOpenSeaKey( JNIEnv* env, jclass thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, openSeaKey);
#elif (HAS_OS == 1)
    return (*env)->NewStringUTF(env, OSKEY);
#else
    const jstring key = "...";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_util_AWEnsResolver_getOpenSeaKey( JNIEnv* env, jclass thiz )
{
    return Java_com_alphawallet_app_service_OpenSeaService_getOpenSeaKey(env, thiz);
}