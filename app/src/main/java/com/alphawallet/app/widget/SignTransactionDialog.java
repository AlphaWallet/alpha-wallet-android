package com.alphawallet.app.widget;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.AuthenticationCallback;
import com.alphawallet.app.entity.AuthenticationFailType;
import com.alphawallet.app.entity.Operation;

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
        // Create the Confirm Credentials screen. You can customize the title and description. Or
        // we will provide a generic one for you if you leave it null
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
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
            authCallback.authenticateFail("Device unlocked", AuthenticationFailType.DEVICE_NOT_SECURE, callBackId);
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
        KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(unlockTitle, unlockDetail);
        if (intent != null) {
            context.startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + callBackId.ordinal());
        }
    }

    private void authenticate(FingerprintManager fpManager, Context context, AuthenticationCallback authCallback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;
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
                        break;
                    case FINGERPRINT_ERROR_LOCKOUT_PERMANENT:
                        fingerprintError.setText(R.string.too_many_fails);
                        fingerprintError.setVisibility(View.VISIBLE);
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
                authCallback.authenticateFail("Authentication Failure", AuthenticationFailType.FINGERPRINT_NOT_VALIDATED, callBackId);
            }
        }, null);
    }

    private static FingerprintManager fingerprintUnlockSupported(Context ctx)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return null;
        }
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
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return (keyguardManager == null || keyguardManager.isDeviceSecure());
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
}