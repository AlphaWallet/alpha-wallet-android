package com.wallet.crypto.trustapp.router;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.trustapp.ui.ImportWalletActivity;

public class ImportWalletRouter {

	public void open(Context context) {
		Intent intent = new Intent(context, ImportWalletActivity.class);
		context.startActivity(intent);
	}

	public void openForResult(Activity activity, int requestCode) {
		Intent intent = new Intent(activity, ImportWalletActivity.class);
		activity.startActivityForResult(intent, requestCode);
	}
}
