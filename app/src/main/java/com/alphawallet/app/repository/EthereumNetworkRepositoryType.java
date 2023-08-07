package com.alphawallet.app.repository;

import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.KnownContract;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;

import org.web3j.protocol.Web3j;

import java.math.BigInteger;
import java.util.List;

import io.reactivex.Single;

public interface EthereumNetworkRepositoryType {

    NetworkInfo getActiveBrowserNetwork();

    void setActiveBrowserNetwork(NetworkInfo networkInfo);

    NetworkInfo getNetworkByChain(long chainId);

    Single<BigInteger> getLastTransactionNonce(Web3j web3j, String walletAddress);

    NetworkInfo[] getAvailableNetworkList();
    NetworkInfo[] getAllActiveNetworks();

    void addOnChangeDefaultNetwork(OnNetworkChangeListener onNetworkChanged);

    String getNameById(long chainId);

    List<Long> getFilterNetworkList();
    List<Long> getSelectedFilters();
    Long getDefaultNetwork();

    void setFilterNetworkList(Long[] networkList);

    List<ContractLocator> getAllKnownContracts(List<Long> networkFilters);

    Single<Token[]> getBlankOverrideTokens(Wallet wallet);

    Token getBlankOverrideToken();

    Token getBlankOverrideToken(NetworkInfo networkInfo);

    KnownContract readContracts();

    boolean getIsPopularToken(long chainId, String address);

    String getCurrentWalletAddress();
    boolean hasSetNetworkFilters();
    void setHasSetNetworkFilters();
    String getDappBrowserRPC(long chainId);

    void saveCustomRPCNetwork(String networkName, String rpcUrl, long chainId, String symbol, String blockExplorerUrl, String explorerApiUrl, boolean isTestnet, Long oldChainId);
    void removeCustomRPCNetwork(long chainId);

    boolean isChainContract(long chainId, String address);
    boolean hasLockedGas(long chainId);
    boolean hasBlockNativeGasAPI(long chainId);

    NetworkInfo getBuiltInNetwork(long chainId);

    void commitPrefs();
}
