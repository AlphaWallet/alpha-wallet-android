package com.wallet.crypto.trustapp.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.trustapp.views.CreateAccountActivity;

public class CreateWalletRouter {
	public void open(Context context) {
		Intent intent = new Intent(context, CreateAccountActivity.class);
		context.startActivity(intent);
	}
}
