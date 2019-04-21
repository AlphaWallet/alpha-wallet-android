package io.stormbird.wallet.interact;

import io.reactivex.Single;
import io.stormbird.token.tools.Numeric;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.ui.widget.entity.ENSHandler;
import org.web3j.crypto.Hash;

import java.math.BigInteger;
import java.util.Arrays;

import static io.stormbird.wallet.C.ENSCONTRACT;

/**
 * Created by James on 4/12/2018.
 * Stormbird in Singapore
 */
public class ENSInteract
{
    private final TokenRepositoryType tokenRepository;

    public ENSInteract(TokenRepositoryType tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public Single<String> checkENSAddress(int chainId, String name)
    {
        if (!ENSHandler.canBeENSName(name)) return Single.fromCallable(() -> "0");
        return tokenRepository.resolveENS(chainId, name)
                .map(this::checkAddress);
    }

    private String checkAddress(String returnedAddress)
    {
        BigInteger test = Numeric.toBigInt(returnedAddress);
        if (!test.equals(BigInteger.ZERO))
        {
            return returnedAddress;
        }
        else
        {
            return "0";
        }
    }
}
