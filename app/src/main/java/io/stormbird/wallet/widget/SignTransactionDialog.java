package io.stormbird.wallet.widget;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.AuthenticationCallback;


/**
 * Created by James on 7/06/2019.
 * Stormbird in Sydney
 */
public class SignTransactionDialog extends AWalletConfirmationDialog
{
    private int callBackId;
    public SignTransactionDialog(@NonNull Activity activity, int callBackId)
    {
        super(activity);
        //display fingerprint
        ImageView fingerPrint = findViewById(R.id.image_fingerprint);
        fingerPrint.setVisibility(View.VISIBLE);
        findViewById(R.id.dialog_button1_container).setVisibility(View.GONE);
        this.callBackId = callBackId;
    }

    //TODO: Needs to be 'use PIN'
    @Override
    public void setPrimaryButtonText(int resId)
    {
        btnPrimary.setText(context.getResources().getString(resId));
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
            //remove fingerprint
            ImageView fingerPrint = findViewById(R.id.image_fingerprint);
            fingerPrint.setVisibility(View.GONE);
            Button pin = findViewById(R.id.dialog_button1_container);
            pin.setVisibility(View.VISIBLE);
        }
        //        Intent intent = km.createConfirmDeviceCredentialIntent(null, null);
        //        if (intent != null) {
        //            Activity app = (Activity) context;
        //            app.startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
        //        }
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
                Toast.makeText(context, errString, Toast.LENGTH_SHORT).show();
                authCallback.authenticateFail(errString.toString());
            }

            @Override
            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                super.onAuthenticationHelp(helpCode, helpString);
                //Toast.makeText(context, helpString.toString(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                authCallback.authenticatePass(callBackId);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                authCallback.authenticateFail("Authentication Failure");
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

}
