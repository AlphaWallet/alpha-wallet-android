package io.stormbird.wallet.interact;

import io.stormbird.wallet.entity.MagicLinkParcel;
import io.stormbird.wallet.entity.OrderContractAddressPair;
import io.stormbird.wallet.entity.Ticker;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.TokenRepositoryType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.entity.MagicLinkData;

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

    public Observable<Token> fetchStoredToken(Wallet wallet, String tokenAddress) {
        return tokenRepository.fetchCachedSingleToken(wallet.address, tokenAddress)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Token[]> fetchStoredWithEth(Wallet wallet) {
        return tokenRepository.fetchActiveStoredPlusEth(wallet.address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Token> fetchSequential(Wallet wallet) {
        return tokenRepository.fetchActiveStoredSequential(wallet.address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Token> fetchSequentialNoEth(Wallet wallet) {
        return tokenRepository.fetchActiveStoredSequentialNoEth(wallet.address)
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

    public Observable<OrderContractAddressPair> updateBalancePair(Token token, MagicLinkData order)
    {
        return tokenRepository.fetchActiveTokenBalance(order.ownerAddress, token)
                .map(updateToken -> mapToPair(updateToken, order))
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

    public Single<Ticker> getEthereumTicker()
    {
        return tokenRepository.getEthTicker();
    }

    private OrderContractAddressPair mapToPair(Token token, MagicLinkData so)
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
