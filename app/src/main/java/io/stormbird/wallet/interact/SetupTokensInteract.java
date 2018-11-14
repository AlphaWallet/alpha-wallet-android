package io.stormbird.wallet.interact;

/**
 * Created by James on 16/01/2018.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenInfo;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.service.TokensService;

public class SetupTokensInteract {

    private final static String TAG = "STI";
    private final TokenRepositoryType tokenRepository;
    public static final String UNKNOWN_CONTRACT = "[Unknown Contract]";
    public static final String EXPIRED_CONTRACT = "[Expired Contract]";

    public SetupTokensInteract(TokenRepositoryType tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public Observable<TokenInfo> update(String address, boolean isERC875) {
        return tokenRepository.update(address, isERC875)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Parse all transactions not associated with known tokens and pick up unknown contracts
     * @param transactions
     * @param tokensService
     * @param txMap
     * @return
     */
    public Single<List<String>> getUnknownTokens(Transaction[] transactions, TokensService tokensService, Map<String, Transaction> txMap)
    {
        return Single.fromCallable(() -> {
            List<String> unknownTokens = new ArrayList<>();
            //process the remaining transactions
            for (Transaction t : transactions)
            {
                Token localToken = tokensService.getToken(t.to);
                if (t.input != null && t.input.length() > 2 && localToken == null && !unknownTokens.contains(t.to))
                {
                    unknownTokens.add(t.to);
                }
                if (localToken != null)
                {
                    txMap.remove(t.hash);
                }
            }

            return unknownTokens;
        });
    }

    public Observable<TokenInfo> addToken(String address, boolean isERC875)
    {
        return tokenRepository.update(address, isERC875);
    }

    public Token terminateToken(Token token, Wallet wallet, NetworkInfo network)
    {
        tokenRepository.terminateToken(token, wallet, network);
        return token;
    }
}
