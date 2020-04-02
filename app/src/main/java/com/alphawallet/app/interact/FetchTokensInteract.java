package com.alphawallet.app.interact;

import com.alphawallet.app.entity.ContractResult;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.OrderContractAddressPair;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.ERC721Ticket;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.token.entity.MagicLinkData;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class FetchTokensInteract {

    private final TokenRepositoryType tokenRepository;

    public FetchTokensInteract(TokenRepositoryType tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public Observable<Token> fetchStoredToken(NetworkInfo network, Wallet wallet, String tokenAddress) {
        return tokenRepository.fetchCachedSingleToken(network, wallet.address, tokenAddress)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Token[]> fetchStoredWithEth(Wallet wallet) {
        return tokenRepository.fetchActiveStoredPlusEth(wallet.address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Token> fetchSingle(Wallet wallet, Token token) {
        return tokenRepository.fetchActiveSingle(wallet.address, token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Token[]> storeTickers(Wallet wallet, Token[] tokens) {
        return tokenRepository.storeTickers(wallet, tokens)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Token> fetchEth(NetworkInfo network, Wallet wallet)
    {
        return tokenRepository.getEthBalance(network, wallet).toObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<BigInteger> getLatestBlock(int chainId)
    {
        return tokenRepository.fetchLatestBlockNumber(chainId);
    }

    public Observable<ContractResult> getContractResponse(String address, int chainId, String method)
    {
        return tokenRepository.getTokenResponse(address, chainId, method).toObservable();
    }

    public Observable<Token> updateDefaultBalance(Token token, Wallet wallet)
    {
        return tokenRepository.fetchActiveTokenBalance(wallet.address, token)
                .subscribeOn(Schedulers.io());
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
        if (token == null) return Observable.fromCallable(() -> {
            return new Token(null, BigDecimal.ZERO, 0, "", ContractType.NOT_SET);
        });
        return tokenRepository.fetchActiveTokenBalance(address, token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<TokenTicker> getEthereumTicker(int chainId)
    {
        return tokenRepository.getEthTicker(chainId);
    }

    private OrderContractAddressPair mapToPair(Token token, MagicLinkData so)
    {
        OrderContractAddressPair pair = new OrderContractAddressPair();
        pair.order = so;
        pair.balance = token.getArrayBalance();
        return pair;
    }

    public Observable<Token> fetchBaseCurrencyBalance(NetworkInfo info, Token overrideToken, Wallet wallet)
    {
        if (overrideToken == null || overrideToken.getInterfaceSpec() == ContractType.ETHEREUM)
        {
            return fetchEth(info, wallet);
        }
        else
        {
            return tokenRepository.fetchActiveTokenBalance(wallet.address, overrideToken)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
        }
    }

    public Observable<Token> fetchBaseCurrencyBalance(NetworkInfo info, ContractResult overrideToken, Wallet wallet, TokensService service)
    {
        Token token;
        if (overrideToken.type == ContractType.ETHEREUM)
        {
            return fetchEth(info, wallet);
        }
        else
        {
            //fetch an erc20 balance, first get a template token (note that we don't read this token's balance,
            // but use it to query the balance of the token elsewhere).
            token = service.getToken(overrideToken.chainId, overrideToken.name);
        }

        if (token != null)
        {
            return tokenRepository.fetchActiveTokenBalance(wallet.address, token).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        }
        else
        {
            return Observable.fromCallable(() -> new Token(null, BigDecimal.ZERO, System.currentTimeMillis(), "eth", ContractType.ETHEREUM));
        }
    }

    // Only for sensing ERC721 Ticket
    public Single<Token[]> checkInterface(Token[] tokens, Wallet wallet)
    {
        return Single.fromCallable(() -> {
            //check if the token interface has been checked
            for (int i = 0; i < tokens.length; i++)
            {
                Token t = tokens[i];
                if (t.isERC721Ticket()) continue;
                ContractType type = TokensService.checkInterfaceSpec(t.tokenInfo.chainId, t.tokenInfo.address);

                if (type != ContractType.ERC721 && type != ContractType.ERC721_LEGACY)
                {
                    type = tokenRepository.determineCommonType(t.tokenInfo).blockingGet();
                    if (type != ContractType.ERC721_TICKET) type = t.getInterfaceSpec();
                    if (type == ContractType.ERC721_TICKET && t.isERC721())
                    {
                        //upgrade type:
                        List<BigInteger> balanceFromOpenSea = t.getArrayBalance();
                        tokens[i] = new ERC721Ticket(t.tokenInfo, balanceFromOpenSea, System.currentTimeMillis(), t.getNetworkName(), ContractType.ERC721_TICKET);
                        //update in database
                        tokenRepository.updateTokenType(t, wallet, type);
                    }
                    else
                    {
                        t.setInterfaceSpec(type);
                    }

                    TokensService.setInterfaceSpec(t.tokenInfo.chainId, t.tokenInfo.address, type);
                }
            }

            return tokens;
        });
    }

    public Single<Boolean> checkRedeemed(Token token, List<BigInteger> tickets)
    {
        if (token == null || tickets == null || tickets.size() == 0) return Single.fromCallable(() -> true ); //early return for invalid input
        BigInteger tokenId = tickets.get(0);
        return tokenRepository.fetchIsRedeemed(token, tokenId);
    }
}
