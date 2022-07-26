package com.alphawallet.app.service;

import android.text.TextUtils;

import com.alphawallet.app.repository.TokenRepository;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class NodeService
{
    private static ConcurrentHashMap<Long, BigInteger> currentBlocks;

    public static void updateCurrentBlock(final long chainId)
    {
        if (currentBlocks == null) initBlocks();
        fetchCurrentBlock(chainId).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(blockValue -> currentBlocks.put(chainId, blockValue), onError -> currentBlocks.put(chainId, BigInteger.ZERO)).isDisposed();
    }

    public static BigInteger getCurrentBlock(long chainId)
    {
        if (currentBlocks == null) initBlocks();
        if (!currentBlocks.containsKey(chainId))
        {
            currentBlocks.put(chainId, fetchCurrentBlock(chainId).blockingGet());
        }

        return currentBlocks.get(chainId);
    }

    private static Single<BigInteger> fetchCurrentBlock(final long chainId)
    {
        return Single.fromCallable(() -> {
            Web3j web3j = TokenRepository.getWeb3jService(chainId);
            EthBlock ethBlock =
                    web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send();
            String blockValStr = ethBlock.getBlock().getNumberRaw();
            if (!TextUtils.isEmpty(blockValStr) && blockValStr.length() > 2)
            {
                return Numeric.toBigInt(blockValStr);
            }
            else if (currentBlocks.containsKey(chainId))
            {
                return currentBlocks.get(chainId);
            }
            else
            {
                return BigInteger.ZERO;
            }
        });
    }

    private static void initBlocks()
    {
        currentBlocks = new ConcurrentHashMap<>();
    }
}
