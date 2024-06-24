package com.langitwallet.app.router;

import android.app.Activity;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.langitwallet.app.C;
import com.langitwallet.app.ui.ImportWalletActivity;

public class ImportWalletRouter
{

    public void openForResult(Activity activity, int requestCode, boolean fromSplash)
    {
        Intent intent = new Intent(activity, ImportWalletActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra(C.EXTRA_FROM_SPLASH, fromSplash);
        activity.startActivityForResult(intent, requestCode);
    }

    public void openWatchCreate(Activity activity, int requestCode)
    {
        Intent intent = new Intent(activity, ImportWalletActivity.class);
        intent.putExtra(C.EXTRA_STATE, "watch");
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        activity.startActivityForResult(intent, requestCode);
    }

    public void loginWithGoogle(Activity activity, GoogleSignInClient mGoogleSignInClient) {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, C.RC_SIGN_IN);
    }
}
