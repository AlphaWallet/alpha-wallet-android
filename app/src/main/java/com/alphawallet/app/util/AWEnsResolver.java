package com.alphawallet.app.util;

import android.text.TextUtils;

import org.web3j.crypto.WalletUtils;
import org.web3j.ens.Contracts;
import org.web3j.ens.EnsResolutionException;
import org.web3j.ens.EnsResolver;
import org.web3j.ens.NameHash;
import org.web3j.ens.contracts.generated.ENS;
import org.web3j.ens.contracts.generated.PublicResolver;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthSyncing;
import org.web3j.protocol.core.methods.response.NetVersion;
import org.web3j.tx.ClientTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.utils.Numeric;

import io.reactivex.Single;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.service.GasService;

/**
 * Created by James on 29/05/2019.
 * Stormbird in Sydney
 */
public class AWEnsResolver extends EnsResolver
{
    static final long DEFAULT_SYNC_THRESHOLD = 1000 * 60 * 3;

    public AWEnsResolver(Web3j web3j, long syncThreshold) {
        super(web3j, syncThreshold);
    }

    public AWEnsResolver(Web3j web3j) {
        this(web3j, DEFAULT_SYNC_THRESHOLD);
    }

    public Single<Wallet> resolveWalletEns(Wallet wallet)
    {
        return Single.fromCallable(() -> {
            try
            {
                wallet.ENSname = reverseResolve(wallet.address);
                if (wallet.ENSname != null && wallet.ENSname.length() > 0)
                {
                    //check ENS name integrity - it must point to the wallet address
                    String resolveAddress = resolve(wallet.ENSname);
                    if (!resolveAddress.equalsIgnoreCase(wallet.address))
                    {
                        wallet.ENSname = null;
                    }
                }
                else
                {
                    wallet.ENSname = null;
                }
            }
            catch (Exception e)
            {
                wallet.ENSname = null;
            }
            return wallet;
        });
    }

    public Single<String> resolveEnsName(String address)
    {
        return Single.fromCallable(() -> {
            String ensName = "";
            try
            {
                ensName = reverseResolve(address);
                if (!TextUtils.isEmpty(ensName))
                {
                    //check ENS name integrity - it must point to the wallet address
                    String resolveAddress = resolve(ensName);
                    if (!resolveAddress.equalsIgnoreCase(address))
                    {
                        ensName = "";
                    }
                }
                else
                {
                    ensName = "";
                }
            }
            catch (Exception e)
            {
                // no action
            }
            return ensName;
        });
    }

    public Single<String> resolveENSAddress(String ensName)
    {
        return Single.fromCallable(() -> {
            String address = "";
            if (!isValidEnsName(ensName)) return "";
            try
            {
                address = resolve(ensName);
            }
            catch (Exception e)
            {
                // no action
            }
            return address;
        });
    }

    public static boolean isValidEnsName(String input) {
        return input != null // will be set to null on new Contract creation
                && (input.contains(".") || !WalletUtils.isValidAddress(input));
    }
}
