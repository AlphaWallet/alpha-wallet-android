package io.stormbird.wallet.repository;

import org.web3j.protocol.Web3j;

import java.math.BigInteger;

import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Ticker;

import io.reactivex.Single;

public interface EthereumNetworkRepositoryType {

	NetworkInfo getDefaultNetwork();

	Single<BigInteger> getLastTransactionNonce(Web3j web3j, String walletAddress);

	String getActiveRPC();
	void setActiveRPC(String rpcURL);

	void setDefaultNetworkInfo(NetworkInfo networkInfo);

	NetworkInfo[] getAvailableNetworkList();

	void addOnChangeDefaultNetwork(OnNetworkChangeListener onNetworkChanged);

	Single<Ticker> getTicker();
}
