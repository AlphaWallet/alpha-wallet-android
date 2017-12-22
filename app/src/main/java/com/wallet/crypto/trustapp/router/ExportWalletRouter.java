package com.wallet.crypto.trustapp.router;

import android.app.Activity;
import android.content.Intent;

import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.views.ExportAccountActivity;

public class ExportWalletRouter {

	public static final String WALLET = "wallet";

	public void openForResult(Activity activity, Wallet wallet, int requestCode) {
		Intent intent = new Intent(activity, ExportAccountActivity.class);
		intent.putExtra(WALLET, wallet);
		activity.startActivityForResult(intent, requestCode);
	}
}
