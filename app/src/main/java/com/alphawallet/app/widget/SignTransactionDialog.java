package com.alphawallet.app.widget;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
<<<<<<< HEAD
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
=======
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetDialog;
>>>>>>> e3074436a... Attempt to upgrade to AndroidX
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.AuthenticationCallback;
import com.alphawallet.app.entity.AuthenticationFailType;
import com.alphawallet.app.entity.Operation;

import static android.content.Context.KEYGUARD_SERVICE;
import static android.hardware.fingerprint.FingerprintManager.*;

/**
 * Created by James on 7/06/2019.
 * Stormbird in Sydney
 */
public class SignTransactionDialog extends BottomSheetDialog
{
    public static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 123;

    protected Activity context;
    private Operation callBackId;
    private final ImageView fingerprint;
    private final TextView cancel;
    private final TextView usePin;
    private final TextView fingerprintError;
    private final String unlockTitle;
    private final String unlockDetail;
    private AuthenticationCallback authCallback;
    private BiometricPrompt biometricPrompt;
    private CancellationSignal cancellationSignal;

    public SignTransactionDialog(@NonNull Activity activity, Operation callBackId, String msg, String desc)
    {
        super(activity);
        context = activity;
        setContentView(R.layout.dialog_unlock_private_key);
        fingerprint = findViewById(R.id.image_fingerprint);
        cancel = findViewById(R.id.text_cancel);
        usePin = findViewById(R.id.text_use_pin);
        TextView dialogTitle = findViewById(R.id.dialog_main_text);
        fingerprintError = findViewById(R.id.text_fingerprint_error);
        fingerprint.setVisibility(View.VISIBLE);
        unlockTitle = msg;
        unlockDetail = desc;

        if (msg != null) dialogTitle.setText(msg);

        this.callBackId = callBackId;
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setCanceledOnTouchOutside(true);

        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        usePin.setOnClickListener(v -> { showAuthenticationScreen(); });
    }

    //get fingerprint or PIN
    public void getFingerprintAuthorisation(AuthenticationCallback authCallback) {
        this.authCallback = authCallback;
        // Create the Confirm Credentials screen. You can customize the title and description. Or
        // we will provide a generic one for you if you leave it null
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P)
        {
            /*
            Dismiss current dialog and let OS prompt for its own
             */
            dismiss();

            /*
            Check if Biometric authentication is possible for the device or not
             */
            if (!hasPINLockSetup() && !checkBiometricSupport())
            {
                authCallback.authenticateFail(context.getString(R.string.device_not_secure_warning), AuthenticationFailType.BIOMETRIC_AUTHENTICATION_NOT_AVAILABLE, callBackId);
                return;
            }

            biometricPrompt = new BiometricPrompt.Builder(context)
                    .setTitle(unlockTitle)
                    .setSubtitle(unlockDetail)
                    .setNegativeButton(context.getString(R.string.action_cancel), context.getMainExecutor(),
                            (dialogInterface, i) ->
                                    authCallback.authenticateFail(context.getString(R.string.authentication_cancelled),
                                            AuthenticationFailType.AUTHENTICATION_DIALOG_CANCELLED, callBackId))
                    .build();
            biometricPrompt.authenticate(getCancellationSignal(), context.getMainExecutor(), getAuthenticationCallback());
        }
        else
        {
            getLegacyAuthentication(authCallback);
        }
    }

    public void getLegacyAuthentication(AuthenticationCallback authCallback)
    {
        this.authCallback = authCallback;
        KeyguardManager km = (KeyguardManager) context.getSystemService(KEYGUARD_SERVICE);
        FingerprintManager fpManager = fingerprintUnlockSupported(context);

        if (fpManager != null)
        {
            authenticate(fpManager, context, authCallback);
        }
        else
        {
            removeFingerprintGraphic();
        }

        if (!hasPINLockSetup())
        {
            authCallback.authenticateFail(context.getString(R.string.device_insecure), AuthenticationFailType.DEVICE_NOT_SECURE, callBackId);
        }
    }

    private void removeFingerprintGraphic()
    {
        //remove fingerprint
        fingerprint.setVisibility(View.GONE);
        TextView fingerPrintText = findViewById(R.id.fingerprint_text);
        fingerPrintText.setVisibility(View.GONE);
    }

    private void showAuthenticationScreen()
    {
        KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService(KEYGUARD_SERVICE);
        Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(unlockTitle, unlockDetail);
        if (intent != null) {
            context.startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + callBackId.ordinal());
        }
    }

    private void authenticate(FingerprintManager fpManager, Context context, AuthenticationCallback authCallback) {
        CancellationSignal cancellationSignal;
        cancellationSignal = new CancellationSignal();
        fpManager.authenticate(null, cancellationSignal, 0, new FingerprintManager.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                switch (errorCode)
                {
                    case FINGERPRINT_ERROR_CANCELED:
                        //No action, safe to ignore this return code
                        break;
                    case FINGERPRINT_ERROR_HW_NOT_PRESENT:
                    case FINGERPRINT_ERROR_HW_UNAVAILABLE:
                        //remove the fingerprint graphic
                        removeFingerprintGraphic();
                        break;
                    case FINGERPRINT_ERROR_LOCKOUT:
                        fingerprintError.setText(R.string.too_many_fails);
                        fingerprintError.setVisibility(View.VISIBLE);
                        showAuthenticationScreen();
                        break;
                    case FINGERPRINT_ERROR_LOCKOUT_PERMANENT:
                        fingerprintError.setText(R.string.too_many_fails);
                        fingerprintError.setVisibility(View.VISIBLE);
                        showAuthenticationScreen();
                        break;
                    case FINGERPRINT_ERROR_NO_FINGERPRINTS:
                        fingerprintError.setText(R.string.no_fingerprint_enrolled);
                        fingerprintError.setVisibility(View.VISIBLE);
                        break;
                    case FINGERPRINT_ERROR_TIMEOUT:
                        //safe to ignore
                        break;
                    case FINGERPRINT_ERROR_UNABLE_TO_PROCESS:
                        fingerprintError.setText(R.string.cannot_process_fingerprint);
                        fingerprintError.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                super.onAuthenticationHelp(helpCode, helpString);
            }

            @Override
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                authCallback.authenticatePass(callBackId);
            }

            //This is called when fingerprint is invalid
            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                authCallback.authenticateFail(context.getString(R.string.authentication_failed), AuthenticationFailType.FINGERPRINT_NOT_VALIDATED, callBackId);
            }
        }, null);
    }

    private static FingerprintManager fingerprintUnlockSupported(Context ctx)
    {
        if (ctx.checkSelfPermission(Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        FingerprintManager fpManager = (FingerprintManager) ctx.getSystemService(Context.FINGERPRINT_SERVICE);
        if (fpManager == null || !fpManager.isHardwareDetected() || !fpManager.hasEnrolledFingerprints()) {
            return null;
        }

        return fpManager;
    }

    private boolean hasPINLockSetup()
    {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(KEYGUARD_SERVICE);
        return (keyguardManager == null || keyguardManager.isDeviceSecure());
    }

    /**
     * Indicate whether this device can authenticate the user with biometrics
     * @return true if there are any available biometric sensors and biometrics are enrolled on the device, if not, return false
     */
    private Boolean checkBiometricSupport() {
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.USE_BIOMETRIC) != PackageManager.PERMISSION_GRANTED)
        {
            authCallback.authenticateFail(context.getString(R.string.authentication_failed), AuthenticationFailType.DEVICE_NOT_SECURE, callBackId);
            return false;
        }

        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
    }

    public void setCancelListener(View.OnClickListener listener) {
        cancel.setOnClickListener(listener);
    }

    @Override
    public void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        if (isShowing()) dismiss();
    }

    private CancellationSignal getCancellationSignal() {
        cancellationSignal = new CancellationSignal();
        cancellationSignal.setOnCancelListener(() ->
                authCallback.authenticateFail(context.getString(R.string.authentication_cancelled),
                        AuthenticationFailType.AUTHENTICATION_DIALOG_CANCELLED, callBackId));
        return cancellationSignal;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private BiometricPrompt.AuthenticationCallback getAuthenticationCallback() {
        return new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                switch (errorCode)
                {
                    case BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT:
                    case BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT_PERMANENT:
                    case BiometricPrompt.BIOMETRIC_ERROR_UNABLE_TO_PROCESS:
                    case BiometricPrompt.BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL:
                        cancellationSignal.cancel();
                        showAuthenticationScreen();
                        break;
                    case BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS:
                        //display legacy screen
                        authCallback.legacyAuthRequired(callBackId, unlockTitle, unlockDetail);
                        break;
                    default:
                        authCallback.authenticateFail(context.getString(R.string.authentication_failed), AuthenticationFailType.FINGERPRINT_NOT_VALIDATED, callBackId);
                        break;
                }
            }

            @Override
            public void onAuthenticationHelp(int helpCode,
                                             CharSequence helpString) {
                super.onAuthenticationHelp(helpCode, helpString);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                authCallback.authenticateFail(context.getString(R.string.authentication_failed), AuthenticationFailType.FINGERPRINT_NOT_VALIDATED, callBackId);
            }

            @Override
            public void onAuthenticationSucceeded(
                    BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                authCallback.authenticatePass(callBackId);
            }
        };
    }
}