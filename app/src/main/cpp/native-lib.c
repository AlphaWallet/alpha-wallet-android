
#include <string.h>
#include <jni.h>



JNIEXPORT jstring JNICALL
Java_com_wallet_crypto_trustapp_controller_PasswordManager_getKeyStringFromNative( JNIEnv* env, jobject thiz )
{
    // TODO: fill in your key - must be 32 bytes
    const jstring key = "35TheTru5tWa11ets3cr3tK3y377123!";
    return (*env)->NewStringUTF(env, key);
}

JNIEXPORT jbyteArray JNICALL
Java_com_wallet_crypto_trustapp_controller_PasswordManager_getIvStringFromNative( JNIEnv* env, jobject thiz )
{
    // TODO: fill in your iv - must be 16 bytes
    const jstring iv = "8201va0184a0md8i";
    return (*env)->NewStringUTF(env, iv);
}