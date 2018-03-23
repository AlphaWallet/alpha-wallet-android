package io.awallet.crypto.alphawallet.interact;

import io.awallet.crypto.alphawallet.entity.OrderContractAddressPair;
import io.awallet.crypto.alphawallet.entity.SalesOrder;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.repository.TokenRepositoryType;

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

    public Observable<Token[]> fetchList(Wallet wallet) {
        return tokenRepository.fetchActive(wallet.address)
                //.map(this::tokensToSingle)
                //.map(this::tokensToMap)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Token tokensToSingle(Token[] tokens)
    {
        if (tokens.length > 0) return tokens[0];
        else return null;
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
                        .subscribeOn(Schedulers.io())
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

    public Observable<Token> updateBalance(String address, Token token)
    {
        return tokenRepository.fetchActiveTokenBalance(address, token)
                //.map(updateToken -> mapToPair(updateToken, so))
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
