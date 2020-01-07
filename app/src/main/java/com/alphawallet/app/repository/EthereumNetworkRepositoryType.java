package com.alphawallet.app.repository;

import org.web3j.protocol.Web3j;

import java.math.BigInteger;
import java.util.List;

import com.alphawallet.app.entity.ContractResult;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Ticker;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenTicker;

import io.reactivex.Single;

public interface EthereumNetworkRepositoryType {

	NetworkInfo getDefaultNetwork();
	NetworkInfo getNetworkByChain(int chainId);

	Single<BigInteger> getLastTransactionNonce(Web3j web3j, String walletAddress);

	void setDefaultNetworkInfo(NetworkInfo networkInfo);

	NetworkInfo[] getAvailableNetworkList();

	void addOnChangeDefaultNetwork(OnNetworkChangeListener onNetworkChanged);

	Single<Ticker> getTicker(int chainId, TokenTicker tTicker);
	Single<Token> attachTokenTicker(Token token);
	Single<Token[]> attachTokenTickers(Token[] tokens);
	TokenTicker getTokenTicker(Token token);
	String getNameById(int id);

	Single<Token[]> getTokensOnNetwork(int chainId, String address);

    List<Integer> getFilterNetworkList();
    void setFilterNetworkList(int[] networkList);

	boolean checkTickers();

	List<ContractResult> getAllKnownContracts(List<Integer> networkFilters);
}
