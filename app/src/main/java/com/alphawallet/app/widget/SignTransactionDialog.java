package com.alphawallet.app.widget;

import static android.content.Context.KEYGUARD_SERVICE;
import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK;
import static androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.AuthenticationCallback;
import com.alphawallet.app.entity.AuthenticationFailType;
import com.alphawallet.app.entity.Operation;

import java.security.ProviderException;
import java.util.concurrent.Executor;

/**
 * Created by James on 7/06/2019.
 * Stormbird in Sydney
 */

/* Outstanding TODOs:
    1. Implement weak biometrics key unlock for lower security keys
       a. UI for this
       b. Designate the key as weak locked
 */
public class SignTransactionDialog
{
    public static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 123;

    private final boolean hasStrongBiometric;  //fingerprint, iris
    private final boolean hasDeviceCredential; //PIN, Swipe, Password
    private final boolean hasWeakCredential;   //TODO: Face unlock

    private BiometricPrompt biometricPrompt;

    public SignTransactionDialog(Context context)
    {
        BiometricManager biometricManager = BiometricManager.from(context);
        hasStrongBiometric = biometricManager.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS;
        hasDeviceCredential = biometricManager.canAuthenticate(DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS;
        hasWeakCredential = biometricManager.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public void getAuthentication(AuthenticationCallback authCallback, @NonNull Activity activity, Operation callbackId)
    {
        Executor executor = ContextCompat.getMainExecutor(activity);
        biometricPrompt = new BiometricPrompt((FragmentActivity) activity,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                switch (errorCode)
                {
                    case BiometricPrompt.ERROR_NEGATIVE_BUTTON:
                        //this could be a 'use pin'
                        if (!TextUtils.isEmpty(errString) && errString.equals(activity.getString(R.string.use_pin)))
                        {
                            showAuthenticationScreen(activity, authCallback, callbackId);
                            break;
                        }
                        //drop through  |
                        //              v
                    case BiometricPrompt.ERROR_CANCELED:
                        authCallback.authenticateFail("Cancelled", AuthenticationFailType.FINGERPRINT_ERROR_CANCELED, callbackId);
                        break;
                    case BiometricPrompt.ERROR_LOCKOUT:
                    case BiometricPrompt.ERROR_LOCKOUT_PERMANENT:
                        authCallback.authenticateFail(activity.getString(R.string.too_many_fails), AuthenticationFailType.FINGERPRINT_NOT_VALIDATED, callbackId);
                        break;
                    case BiometricPrompt.ERROR_USER_CANCELED:
                        authCallback.authenticateFail(activity.getString(R.string.fingerprint_error_user_canceled), AuthenticationFailType.AUTHENTICATION_DIALOG_CANCELLED, callbackId);
                        break;
                    case BiometricPrompt.ERROR_HW_NOT_PRESENT:
                    case BiometricPrompt.ERROR_HW_UNAVAILABLE:
                    case BiometricPrompt.ERROR_NO_BIOMETRICS:
                    case BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL:
                    case BiometricPrompt.ERROR_NO_SPACE:
                    case BiometricPrompt.ERROR_TIMEOUT:
                    case BiometricPrompt.ERROR_UNABLE_TO_PROCESS:
                    case BiometricPrompt.ERROR_VENDOR:
                        authCallback.authenticateFail(activity.getString(R.string.fingerprint_authentication_failed), AuthenticationFailType.FINGERPRINT_NOT_VALIDATED, callbackId);
                        break;
                }
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                authCallback.authenticatePass(callbackId);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                authCallback.authenticateFail(activity.getString(R.string.fingerprint_authentication_failed), AuthenticationFailType.FINGERPRINT_NOT_VALIDATED, callbackId);
            }
        });

        //determine how user can authenticate
        //This is a bit of a mess at the moment due to the API# transition.
        // Android 30+ dialog handles both BIOMETRIC_STRONG and DEVICE_CREDENTIAL
        //   - setAllowedAuthenticators as appropriate
        // Android -> 29 dialog only can handle BIOMETRIC_STRONG
        //   - if device has BIOMETRIC_STRONG setAllowedAuthenticators BIOMETRIC_STRONG, if also has DEVICE_CREDENTIAL then setup 'use pin' to call showAuthenticationScreen
        //   - if device doesn't have BIOMETRIC_STRONG then go straight to showAuthenticationScreen

        final BiometricPrompt.PromptInfo.Builder promptBuilder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.unlock_private_key));

        if (!hasStrongBiometric && !hasDeviceCredential)
        {
            //device should be unlocked, drop through
            showAuthenticationScreen(activity, authCallback, callbackId);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) // 30+
        {
            promptBuilder.setAllowedAuthenticators((hasStrongBiometric ? BIOMETRIC_STRONG : 0) | (hasDeviceCredential ? DEVICE_CREDENTIAL : 0));
        }
        else
        {
            if (!hasStrongBiometric) //go straight to authentication screen
            {
                showAuthenticationScreen(activity, authCallback, callbackId);
                return;
            }
            else
            {
                promptBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG)
                        .setNegativeButtonText(activity.getString(R.string.use_pin));
            }
        }

        if (!hasDeviceCredential)
        {
            promptBuilder.setNegativeButtonText(activity.getString(R.string.action_cancel));
        }

        try
        {
            BiometricPrompt.PromptInfo promptInfo = promptBuilder.build();
            biometricPrompt.authenticate(promptInfo);
        }
        catch (ProviderException e)
        {
            authCallback.authenticateFail(activity.getString(R.string.authentication_error), AuthenticationFailType.BIOMETRIC_AUTHENTICATION_NOT_AVAILABLE, callbackId);
        }
    }

    private void showAuthenticationScreen(Activity activity, AuthenticationCallback authCallback, Operation callBackId)
    {
        KeyguardManager km = (KeyguardManager) activity.getSystemService(KEYGUARD_SERVICE);
        if (km != null && !km.isDeviceSecure())
        {
            //device is unlocked. No need to authenticate
            authCallback.authenticatePass(callBackId);
        }
        else if (km != null)
        {
            Intent intent = km.createConfirmDeviceCredentialIntent(activity.getString(R.string.unlock_private_key), "");
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + callBackId.ordinal());
        }
        else
        {
            authCallback.authenticateFail("Device unlocked", AuthenticationFailType.DEVICE_NOT_SECURE, callBackId);
        }
    }

    public void close()
    {
        if (biometricPrompt != null)
        {
            try
            {
                biometricPrompt.cancelAuthentication();
            }
            catch (Exception e)
            {
                //
            }
        }
    }
}