package com.alphawallet.app.interact;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.TokenInfo;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.repository.TransactionRepositoryType;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import com.alphawallet.app.service.TokensService;

public class FetchTransactionsInteract {

    private final TransactionRepositoryType transactionRepository;
    private final TokenRepositoryType tokenRepository;

    public FetchTransactionsInteract(TransactionRepositoryType transactionRepository,
                                     TokenRepositoryType tokenRepositoryType) {
        this.transactionRepository = transactionRepository;
        this.tokenRepository = tokenRepositoryType;
    }

    public Observable<Transaction[]> fetchCached(Wallet wallet, int maxTransactions) {
        return transactionRepository
                .fetchCachedTransactions(wallet, maxTransactions)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Transaction[]> fetchNetworkTransactions(NetworkInfo networkInfo, String tokenAddress, long lastBlock, String userAddress) {
        return transactionRepository
                .fetchNetworkTransaction(networkInfo, tokenAddress, lastBlock, userAddress)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Transaction[]> storeTransactions(Wallet wallet, Transaction[] txList)
    {
        return transactionRepository.storeTransactions(wallet, txList);
    }

    public Single<Transaction[]> fetchTransactionsFromStorage(Wallet wallet, Token token, int count)
    {
        return transactionRepository.fetchTransactionsFromStorage(wallet, token, count);
    }

    public Single<ContractType> queryInterfaceSpec(TokenInfo tokenInfo)
    {
        //can resolve erc20, erc721 and erc875 from a getbalance check and look at decimals. Otherwise try more esoteric
        return tokenRepository.determineCommonType(tokenInfo)
                .flatMap(type -> additionalHandling(type, tokenInfo));
    }

    private Single<ContractType> additionalHandling(ContractType type, TokenInfo tokenInfo)
    {
        switch (type)
        {
            case ERC20:
            case ERC721:
                return Single.fromCallable(() -> type);
            case ERC875:
                //requires additional handling to determine if it's Legacy type, but safe to return ERC875 for now:
                transactionRepository.queryInterfaceSpec(tokenInfo.address, tokenInfo)
                        .subscribeOn(Schedulers.io())
                        .subscribe(actualType -> TokensService.setInterfaceSpec(tokenInfo.chainId, tokenInfo.address, actualType)).isDisposed();
                return Single.fromCallable(() -> type);
            default:
                //Token wasn't any of the easily determinable ones, use constructor to analyse
                return tokenRepository.resolveProxyAddress(tokenInfo) //resolve proxy address to find base constructor and analyse
                        .flatMap(address -> transactionRepository.queryInterfaceSpec(address, tokenInfo));
        }
    }

    public Transaction fetchCached(String walletAddress, String hash)
    {
        return transactionRepository.fetchCachedTransaction(walletAddress, hash);
    }

    public Observable<TokenInfo> queryInterfaceSpecForService(TokenInfo tokenInfo)
    {
        return queryInterfaceSpec(tokenInfo).toObservable()
                .map(spec -> addSpecToService(tokenInfo, spec));
    }

    private TokenInfo addSpecToService(TokenInfo info, ContractType contractType)
    {
        TokensService.setInterfaceSpec(info.chainId, info.address, contractType);
        return info;
    }
}
