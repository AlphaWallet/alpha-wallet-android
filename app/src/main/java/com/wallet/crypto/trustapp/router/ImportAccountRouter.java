package com.wallet.crypto.trustapp.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.trustapp.views.ImportAccountActivity;

public class ImportAccountRouter {

	public void open(Context context) {
		Intent intent = new Intent(context, ImportAccountActivity.class);
		context.startActivity(intent);
	}
}
