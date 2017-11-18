package com.wallet.crypto.trustapp.entity;

import java.math.BigInteger;

public class Account {
    public final String address;
    public final BigInteger balanceInWei;

	public Account(String address, BigInteger balanceInWei) {
		this.address = address;
		this.balanceInWei = balanceInWei;
	}
}
