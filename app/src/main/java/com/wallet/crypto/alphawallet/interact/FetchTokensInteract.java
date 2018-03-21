package com.wallet.crypto.alphawallet.interact;

import com.wallet.crypto.alphawallet.entity.OrderContractAddressPair;
import com.wallet.crypto.alphawallet.entity.SalesOrder;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.repository.TokenRepositoryType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class FetchTokensInteract {

    private final TokenRepositoryType tokenRepository;

    public FetchTokensInteract(TokenRepositoryType tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public Observable<Token[]> fetch(Wallet wallet) {
        return tokenRepository.fetchActive(wallet.address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Map<String, Token>> fetchList(Wallet wallet) {
        return tokenRepository.fetchActive(wallet.address)
                .map(this::tokensToMap)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Map<String, Token> tokensToMap(Token[] tokenArray) {
        Map<String, Token> tokenMap = new HashMap<>();
        for (Token t : tokenArray) tokenMap.put(t.getAddress(), t);
        return tokenMap;
    }

    public Observable<Token[]> fetchCache(Wallet wallet) {
        return tokenRepository.fetchActiveCache(wallet.address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Token[]> fetchStored(Wallet wallet) {
        return tokenRepository.fetchActiveStored(wallet.address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Token[]> fetchStoredWithEth(Wallet wallet) {
        return tokenRepository.fetchActiveStoredPlusEth(wallet.address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Token> fetchSingle(Wallet wallet, Token token) {
        return tokenRepository.fetchActiveSingle(wallet.address, token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable updateBalance(Wallet wallet, Token token, List<Integer> burnList) {
        return tokenRepository
                        .setBurnList(wallet, token, burnList)
                        .observeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Token> updateBalance(Wallet wallet, Token token)
    {
        return tokenRepository.fetchActiveTokenBalance(wallet.address, token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<OrderContractAddressPair> updateBalancePair(Token token, SalesOrder so)
    {
        return tokenRepository.fetchActiveTokenBalance(so.ownerAddress, token)
                .map(updateToken -> mapToPair(updateToken, so))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private OrderContractAddressPair mapToPair(Token token, SalesOrder so)
    {
        OrderContractAddressPair pair = new OrderContractAddressPair();
        pair.order = so;
        if (token instanceof Ticket) {
            Ticket t = (Ticket) token;
            pair.balance = t.balanceArray;
        }
        return pair;
    }
}
