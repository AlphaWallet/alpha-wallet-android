package io.stormbird.wallet.interact;

/**
 * Created by James on 16/01/2018.
 */

import android.util.Log;

import org.web3j.utils.Numeric;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenInfo;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.TransactionDecoder;
import io.stormbird.wallet.entity.TransactionInput;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.service.TokensService;

public class SetupTokensInteract {

    private final static String TAG = "STI";
    private final TokenRepositoryType tokenRepository;
    private List<String> unknownContracts = new ArrayList<>();

    public static final String UNKNOWN_CONTRACT = "[Unknown Contract]";
    public static final String EXPIRED_CONTRACT = "[Expired Contract]";

    public SetupTokensInteract(TokenRepositoryType tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public Observable<TokenInfo> update(String address) {
        return tokenRepository.update(address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public void clearAll()
    {
        unknownContracts.clear();
    }

    //use this function to generate unit test string
    private void generateTestString(Transaction[] txList)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("String[] inputTestList = {");
        boolean first = true;
        for (Transaction t : txList) {
            if (!first) {
                sb.append("\n,");
            }
            first = false;
            sb.append("\"");
            sb.append(t.input);
            sb.append("\"");
        }

        sb.append("};");
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

    public Observable<TokenInfo> addToken(String address)
    {
        return tokenRepository.update(address);
    }

    public void setupUnknownList(TokensService tokensService, List<String> xmlContractAddresses)
    {
        unknownContracts.clear();
        if (xmlContractAddresses != null)
        {
            for (String address : xmlContractAddresses)
            {
                if (tokensService.getToken(address) == null) unknownContracts.add(address);
            }
        }
    }

    public Token terminateToken(Token token, Wallet wallet, NetworkInfo network)
    {
        tokenRepository.terminateToken(token, wallet, network);
        return token;
    }
}
