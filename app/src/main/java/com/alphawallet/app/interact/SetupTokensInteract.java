package com.alphawallet.app.interact;

/**
 * Created by James on 16/01/2018.
 */

import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.TokenInfo;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.UnknownToken;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.TokenRepositoryType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import com.alphawallet.app.service.TokensService;

public class SetupTokensInteract {

    private final static String TAG = "STI";
    private final TokenRepositoryType tokenRepository;
    public static final String UNKNOWN_CONTRACT = "[Unknown Contract]";
    public static final String EXPIRED_CONTRACT = "[Expired Contract]";

    public SetupTokensInteract(TokenRepositoryType tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public Observable<TokenInfo> update(String address, int chainId) {
        return tokenRepository.update(address, chainId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Parse all transactions not associated with known tokens and pick up unknown contracts
     * @param transactions
     * @param tokensService
     * @return
     */
    public Single<List<UnknownToken>> getUnknownTokens(Transaction[] transactions, TokensService tokensService)
    {
        return Single.fromCallable(() -> {
            Map<String, UnknownToken> unknownTokenMap = new HashMap<>();
            //List<UnknownToken> unknownTokens = new ArrayList<>();

            //process the remaining transactions
            for (Transaction t : transactions)
            {
                Token localToken = tokensService.getToken(t.chainId, t.to);

                if (localToken != null && localToken.tokenInfo.chainId != t.chainId)
                {
                    System.out.println("Collision!");
                    if (localToken.isBad()) localToken = null;
                }

                if (t.input != null && t.input.length() > 2 && localToken == null
                        && t.to != null && t.to.length() > 0 && !unknownTokenMap.containsKey(t.to) && t.error.equals("0"))
                {
                    unknownTokenMap.put(t.to, new UnknownToken(t.chainId, t.to));//only add token to scan if it wasn't an error transaction
                }

                if (unknownTokenMap.size() > 50) break;
            }

            return new ArrayList<>(unknownTokenMap.values());
        });
    }

    public Observable<TokenInfo> addToken(String address, int chainId)
    {
        return tokenRepository.update(address, chainId);
    }

    public Token terminateToken(Token token, Wallet wallet, NetworkInfo network)
    {
        tokenRepository.terminateToken(token, wallet, network);
        return token;
    }
}
