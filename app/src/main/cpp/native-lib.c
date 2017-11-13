
#include <string.h>
#include <jni.h>



JNIEXPORT jstring JNICALL
Java_com_wallet_crypto_trustapp_controller_PasswordManager_getKeyStringFromNative( JNIEnv* env, jobject thiz )
{
    // TODO: fill in your key - must be 32 bytes
    const jstring key = "ThisIsNotTheKeyYoureLookingFor!!";
    return (*env)->NewStringUTF(env, key);
}

JNIEXPORT jbyteArray JNICALL
Java_com_wallet_crypto_trustapp_controller_PasswordManager_getIvStringFromNative( JNIEnv* env, jobject thiz )
{
    // TODO: fill in your iv - must be 16 bytes
    const jstring iv = "NorTheInitVector";
    return (*env)->NewStringUTF(env, iv);
}