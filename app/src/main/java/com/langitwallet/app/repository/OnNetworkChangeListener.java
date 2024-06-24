package com.langitwallet.app.repository;

import com.langitwallet.app.entity.NetworkInfo;

public interface OnNetworkChangeListener {
	void onNetworkChanged(NetworkInfo networkInfo);
}
