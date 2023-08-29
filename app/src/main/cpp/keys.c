
#include <string.h>
#include <alloca.h>
#include <jni.h>
#if __has_include ("../../../../../keys.secret")
#   define HAS_KEYS 1
#   include "../../../../../keys.secret"
#else
#   define HAS_KEYS 0
#endif

#define QUOTE(str) #str
#define EXPAND_AND_QUOTE(str) QUOTE(str)

#ifdef IFKEY
#   define HAS_INFURA 1
#   define INFURA_Q EXPAND_AND_QUOTE(IFKEY)
#else
#   define HAS_INFURA 0
#endif

#ifdef OSKEY
#   define HAS_OS 1
#   define OSKEY_Q EXPAND_AND_QUOTE(OSKEY)
#else
#   define HAS_OS 0
#endif

#ifdef PSKEY
#   define HAS_PS 1
#   define PSKEY_Q EXPAND_AND_QUOTE(PSKEY)
#else
#   define HAS_PS 0
#endif

#ifdef ASKEY
#   define HAS_AS 1
#   define ASKEY_Q EXPAND_AND_QUOTE(ASKEY)
#else
#   define HAS_AS 0
#endif

#ifdef WCKEY
#   define HAS_WC 1
#   define WCKEY_Q EXPAND_AND_QUOTE(WCKEY)
#else
#   define HAS_WC 0
#endif

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getInfuraKey( JNIEnv* env, jobject thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, infuraKey);
#elif (HAS_INFURA == 1)
    return (*env)->NewStringUTF(env, INFURA_Q);
#else
    const jstring key = "da3717f25f824cc1baa32d812386d93f";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getAnalyticsKey( JNIEnv* env, jobject thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, mixpanelKey);
#else
    const jstring key = "d4c1140e21f6204184bb1ea02eb84412";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getRampKey( JNIEnv* env, jobject thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, rampKey);
#else
    const jstring key = "asfjkdhvcmbnekjfhskjdhfskjdhfskjdhfsdkjf"; // <-- replace with your Ramp key
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getCoinbasePayAppId( JNIEnv* env, jobject thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, coinbasePayAppId);
#else
    const jstring key = ""; // <-- replace with your Coinbase Pay app id
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getSecondaryInfuraKey( JNIEnv* env, jobject thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, secondaryInfuraKey);
#elif (HAS_INFURA == 1)
    return (*env)->NewStringUTF(env, INFURA_Q);
#else
    const jstring key = "da3717f25f824cc1baa32d812386d93f";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getTertiaryInfuraKey( JNIEnv* env, jobject thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, tertiaryInfuraKey);
#elif (HAS_INFURA == 1)
    return (*env)->NewStringUTF(env, INFURA_Q);
#else
    const jstring key = "da3717f25f824cc1baa32d812386d93f";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getKlaytnKey( JNIEnv* env, jobject thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, klaytnKey);
#else
    return (*env)->NewStringUTF(env, "");
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getBSCExplorerKey( JNIEnv* env, jobject thiz )
{
#if (HAS_KEYS == 1)
    return getBSCExplorerKey(env);
#else
    return (*env)->NewStringUTF(env, "");
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getEtherscanKey( JNIEnv* env, jclass thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, etherscanKey);
#else
    const jstring key = "6U31FTHW3YYHKW6CYHKKGDPHI9HEJ9PU5F";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getMailchimpKey(JNIEnv *env, jclass clazz)
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, mailchimpKey);
#else
    const jstring key = "--";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getPolygonScanKey(JNIEnv *env, jobject thiz) {
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, polygonScanKey);
#elif (HAS_PS == 1)
    return (*env)->NewStringUTF(env, PSKEY_Q);
#else
    const jstring key = "";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getCovalentKey( JNIEnv* env, jclass thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedCKey(env, 4, '_', covalentKey);
#else
    const jstring key = "ckey_9bfb5c8fe0f04c7491231e60ee8"; // <-- Add your covalent key here. This public one could be rate limited
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getAuroraScanKey( JNIEnv* env, jclass thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, auroraKey);
#elif (HAS_AURORA == 1)
    return (*env)->NewStringUTF(env, ASKEY_Q);
#else
    const jstring key = "...";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getOpenSeaKey( JNIEnv* env, jclass thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, openSeaKey);
#elif (HAS_OS == 1)
    return (*env)->NewStringUTF(env, OSKEY_Q);
#else
    const jstring key = "...";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getWalletConnectProjectId( JNIEnv* env, jclass thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, walletConnectProjectId);
#elif (HAS_WC == 1)
    return (*env)->NewStringUTF(env, WCKEY_Q);
#else
    return (*env)->NewStringUTF(env, ""); // Doesn't have walletconnect key
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getInfuraSecret(JNIEnv *env, jobject thiz) {
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, infuraSecret);
#else
    const jstring key = "";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getUnstoppableDomainsKey( JNIEnv* env, jclass thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, unstoppableDomainsKey);
#else
    const jstring key = "";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getOkLinkKey( JNIEnv* env, jclass thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, oklinkKey);
#else
    const jstring key = "";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getBlockPiBaobabKey( JNIEnv* env, jclass thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, blockpiBaobab);
#else
    const jstring key = "";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getBlockPiCypressKey( JNIEnv* env, jclass thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, blockpiCypress);
#else
    const jstring key = "";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getBlockNativeKey( JNIEnv* env, jclass thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, blockNative);
#else
    const jstring key = "";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getSmartPassKey( JNIEnv* env, jclass thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, smartpass);
#else
    const jstring key = "";
    return (*env)->NewStringUTF(env, key);
#endif
}

JNIEXPORT jstring JNICALL
Java_com_alphawallet_app_repository_KeyProviderJNIImpl_getSmartPassDevKey( JNIEnv* env, jclass thiz )
{
#if (HAS_KEYS == 1)
    return getDecryptedKey(env, smartpassDev);
#else
    const jstring key = "";
    return (*env)->NewStringUTF(env, key);
#endif
}
