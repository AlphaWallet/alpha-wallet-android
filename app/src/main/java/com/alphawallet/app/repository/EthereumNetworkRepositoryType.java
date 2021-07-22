package com.alphawallet.app.repository;

import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.KnownContract;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.entity.RealmToken;

import org.web3j.protocol.Web3j;

import java.math.BigInteger;
import java.util.List;

import io.reactivex.Single;

public interface EthereumNetworkRepositoryType {
    NetworkInfo getActiveBrowserNetwork();

    void setActiveBrowserNetwork(NetworkInfo networkInfo);

    NetworkInfo getNetworkByChain(int chainId);

    Single<BigInteger> getLastTransactionNonce(Web3j web3j, String walletAddress);

    NetworkInfo[] getAvailableNetworkList();
    NetworkInfo[] getAllActiveNetworks();

    void addOnChangeDefaultNetwork(OnNetworkChangeListener onNetworkChanged);

    String getNameById(int id);

    List<Integer> getFilterNetworkList();
    List<Integer> getSelectedFilters(boolean isMainNet);

    void setFilterNetworkList(Integer[] networkList);

    List<ContractLocator> getAllKnownContracts(List<Integer> networkFilters);

    Single<Token[]> getBlankOverrideTokens(Wallet wallet);

    Token getBlankOverrideToken();

    Token getBlankOverrideToken(NetworkInfo networkInfo);

    KnownContract readContracts();

    boolean getIsPopularToken(int chainId, String address);

    String getCurrentWalletAddress();
    boolean hasSetNetworkFilters();
    boolean isMainNetSelected();

    boolean isChainContract(int chainId, String address);
}
