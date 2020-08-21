package com.alphawallet.app.interact;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.repository.TransactionRepositoryType;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import com.alphawallet.app.service.TokensService;

import java.util.List;

public class FetchTransactionsInteract {

    private final TransactionRepositoryType transactionRepository;
    private final TokenRepositoryType tokenRepository;

    public FetchTransactionsInteract(TransactionRepositoryType transactionRepository,
                                     TokenRepositoryType tokenRepositoryType) {
        this.transactionRepository = transactionRepository;
        this.tokenRepository = tokenRepositoryType;
    }

    public Observable<Transaction[]> fetchCached(Wallet wallet, int maxTransactions, List<Integer> networkFilters) {
        return transactionRepository
                .fetchCachedTransactions(wallet, maxTransactions, networkFilters)
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
            case ERC721_LEGACY:
            case ERC721_TICKET:
                return Single.fromCallable(() -> type);
            case ERC875:
                //requires additional handling to determine if it's Legacy type, but safe to return ERC875 for now:
                transactionRepository.queryInterfaceSpec(tokenInfo.address, tokenInfo)
                        .subscribeOn(Schedulers.io())
                        .subscribe(actualType -> TokensService.setInterfaceSpec(tokenInfo.chainId, tokenInfo.address, actualType)).isDisposed();
                return Single.fromCallable(() -> type);
            default:
                return Single.fromCallable(() -> type); //take no further action: possible that this is not a valid token
        }
    }

    public Transaction fetchCached(String walletAddress, String hash)
    {
        return transactionRepository.fetchCachedTransaction(walletAddress, hash);
    }

    public Single<Transaction[]> markTransactionDropped(Wallet wallet, String hash)
    {
        return transactionRepository.markTransactionDropped(wallet, hash);
    }
}
