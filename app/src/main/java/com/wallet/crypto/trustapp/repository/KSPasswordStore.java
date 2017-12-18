package com.wallet.crypto.trustapp.repository;

import android.content.Context;

import com.wallet.crypto.trustapp.controller.ServiceErrorException;
import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.util.KS;

public class KSPasswordStore implements PasswordStore {

	private final Context context;

	public KSPasswordStore(Context context) {
		this.context = context;
	}

	@Override
	public String getPassword(Account account) throws ServiceErrorException {
		return new String(KS.get(context, account.address));
	}

	@Override
	public boolean setPassword(Account account, String password) throws ServiceErrorException {
		return KS.put(context, account.address, password);
	}
}
