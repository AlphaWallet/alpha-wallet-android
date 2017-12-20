package com.wallet.crypto.trustapp.router;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.trustapp.views.ImportAccountActivity;

public class ImportAccountRouter {

	public void open(Context context) {
		Intent intent = new Intent(context, ImportAccountActivity.class);
		context.startActivity(intent);
	}

	public void openForResult(Activity activity, int requestCode) {
		Intent intent = new Intent(activity, ImportAccountActivity.class);
		activity.startActivityForResult(intent, requestCode);
	}
}
