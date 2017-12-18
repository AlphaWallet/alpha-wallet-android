package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.controller.ServiceErrorException;
import com.wallet.crypto.trustapp.entity.Account;

public interface PasswordStore {
	String getPassword(Account account) throws ServiceErrorException;
	boolean setPassword(Account account, String password) throws ServiceErrorException;
}
