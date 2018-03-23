package io.awallet.crypto.alphawallet.repository;

import io.awallet.crypto.alphawallet.entity.NetworkInfo;

public interface OnNetworkChangeListener {
	void onNetworkChanged(NetworkInfo networkInfo);
}
