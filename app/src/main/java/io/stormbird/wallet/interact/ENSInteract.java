package io.stormbird.wallet.interact;

import io.reactivex.Single;
import io.stormbird.token.tools.Numeric;
import io.stormbird.wallet.repository.TokenRepositoryType;
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

    public Single<String> checkENSAddress(String name)
    {
        if (name == null || name.length() < 1) return Single.fromCallable(() -> "");
        return checkENSAddressFunc(name)
                .flatMap(resultHash -> tokenRepository.callAddressMethod("owner", resultHash, ENSCONTRACT))
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

    private Single<byte[]> checkENSAddressFunc(final String name)
    {
        return Single.fromCallable(() -> {
            //split name
            String[] components = name.split("\\.");

            byte[] resultHash = new byte[32];
            Arrays.fill(resultHash, (byte)0);

            for (int i = (components.length - 1); i >= 0; i--)
            {
                String nameComponent = components[i];
                resultHash = hashJoin(resultHash, nameComponent.getBytes());
            }

            return resultHash;
        });
    }

    private byte[] hashJoin(byte[] lastHash, byte[] input)
    {
        byte[] joined = new byte[lastHash.length*2];

        byte[] inputHash = Hash.sha3(input);
        System.arraycopy(lastHash, 0, joined, 0, lastHash.length);
        System.arraycopy(inputHash, 0, joined, lastHash.length, inputHash.length);
        return Hash.sha3(joined);
    }
}
