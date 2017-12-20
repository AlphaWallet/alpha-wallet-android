package com.wallet.crypto.trustapp.router;

import android.app.Activity;
import android.content.Intent;

import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.views.ExportAccountActivity;

public class ExportAccountRouter {

	public static final String WALLET = "wallet";

	public void openForResult(Activity activity, Account account, int requestCode) {
		Intent intent = new Intent(activity, ExportAccountActivity.class);
		intent.putExtra(WALLET, account);
		activity.startActivityForResult(intent, requestCode);
	}
}
