package com.alphawallet.app.service;

import android.arch.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.util.BalanceUtils;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import com.alphawallet.app.entity.GasSettings;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

/**
 * Created by James on 4/07/2019.
 * Stormbird in Sydney
 *
 * This service provides a universal source for current gas price and gas limit.
 * It can be customised to provide specific gas limits for functions
 * It also provides the Web3j GasProvider interface so can be used in the new Web3j contract handling system
 *
 */
public class GasService implements ContractGasProvider
{
    private final EthereumNetworkRepositoryType networkRepository;
    private final static long FETCH_GAS_PRICE_INTERVAL = 30;
    private final MutableLiveData<BigInteger> gasPrice = new MutableLiveData<>();

    private BigInteger currentGasPrice;
    private BigInteger currentGasPriceOverride;
    private BigInteger currentGasLimitOverride;
    private int currentChainId;
    private Disposable gasFetchDisposable;
    private Web3j web3j;

    public GasService(EthereumNetworkRepositoryType networkRepository)
    {
        this.networkRepository = networkRepository;
        currentChainId = 0;
        currentGasPrice = new BigInteger(C.DEFAULT_GAS_PRICE);
        currentGasLimitOverride = BigInteger.ZERO;
        currentGasPriceOverride = BigInteger.ZERO;
    }

    public MutableLiveData<BigInteger> gasPriceUpdateListener()
    {
        return gasPrice;
    }

    public void startGasListener(int chainId)
    {
        //check if checker is already running and is on the correct chain
        if (chainId != currentChainId || gasFetchDisposable == null || gasFetchDisposable.isDisposed())
        {
            currentChainId = chainId;
            if (networkRepository.getNetworkByChain(currentChainId) == null)
            {
                System.out.println("Network error, no chain, trying to pick: " + chainId);
                return;
            }

            web3j = Web3j.build(new HttpService(networkRepository.getNetworkByChain(currentChainId).rpcServerUrl));
            setCurrentPrice(chainId);
            gasFetchDisposable = Observable.interval(0, FETCH_GAS_PRICE_INTERVAL, TimeUnit.SECONDS)
                    .doOnNext(l -> fetchCurrentGasPrice()).subscribe();
        }
    }

    public void stopGasListener()
    {
        if (gasFetchDisposable != null && !gasFetchDisposable.isDisposed()) gasFetchDisposable.dispose();
        currentChainId = 0;
        currentGasLimitOverride = BigInteger.ZERO;
        currentGasPriceOverride = BigInteger.ZERO;
        web3j = null;
    }

    private void fetchCurrentGasPrice()
    {
        try
        {
            EthGasPrice price = web3j
                    .ethGasPrice()
                    .send();
            if (price.getGasPrice().compareTo(BalanceUtils.gweiToWei(BigDecimal.ZERO)) > 0)
            {
                currentGasPrice = price.getGasPrice();
                gasPrice.postValue(currentGasPrice);
            }
            else
            {
                gasPrice.postValue(currentGasPrice);
            }
        }
        catch (Exception ex)
        {
            gasPrice.postValue(currentGasPrice);
        }
    }

    //TODO: change the function to hash identifier and use that to determine gas limit
    @Override
    public BigInteger getGasLimit(String contractFunc)
    {
        if (!currentGasLimitOverride.equals(BigInteger.ZERO)) return currentGasLimitOverride;

        switch (contractFunc)
        {
            case "transferFrom(address,address,uint256[])":
            case "transfer(address,uint256[])":
            case "transferFrom(address,address,uint16[])":
            case "transfer(address,uint16[])":
                return new BigInteger(C.DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS);

            case "transfer(address,uint)":
            case "transfer(address,uint256)":
            case "approve(address,uint)":
                return new BigInteger(C.DEFAULT_GAS_LIMIT_FOR_TOKENS);

            case "trade(uint256,uint256[],uint8,bytes32,bytes32)":
            case "passTo(uint256,uint256[],uint8,bytes32,bytes32,address)":
            case "trade(uint256,uint16[],uint8,bytes32,bytes32)":
            case "passTo(uint256,uint16[],uint8,bytes32,bytes32,address)":
                //all these functions use keccak and ecrecover, may have high gas requirement
                return new BigInteger(C.DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS).multiply(BigInteger.valueOf(3));

            case "loadNewTickets(uint256[])":
            case "loadNewTickets(bytes32[])":
                return new BigInteger(C.DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS).multiply(BigInteger.valueOf(2));

            case "allocateTo(address,uint256)":
            case "transferFrom(address,address,uint)":
            case "approveAndCall(address,uint,bytes)":
            case "transferAnyERC20Token(address,uint)":
                return new BigInteger(C.DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS);

            case "endContract()":
            case "selfdestruct()":
            case "kill()":
                return new BigInteger(C.DEFAULT_GAS_LIMIT_FOR_END_CONTRACT);

            case "safeTransferFrom(address,address,uint256,bytes)":
            case "safeTransferFrom(address,address,uint256)":
            case "transferFrom(address,address,uint256)":
            case "approve(address,uint256)":
                return new BigInteger(C.DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS);

            case "giveBirth(uint256,uint256)":
            case "breedWithAuto(uint256,uint256)":
                return new BigInteger(C.DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS).multiply(BigInteger.valueOf(2));

            default:
                return new BigInteger(C.DEFAULT_GAS_LIMIT_FOR_TOKENS);
        }
    }

    @Override
    public BigInteger getGasPrice()
    {
        if (!currentGasPriceOverride.equals(BigInteger.ZERO)) return currentGasPriceOverride;
        else return currentGasPrice;
    }

    @Override
    public BigInteger getGasPrice(String contractFunc)
    {
        if (!currentGasPriceOverride.equals(BigInteger.ZERO)) return currentGasPriceOverride;
        else return currentGasPrice;
    }

    @Override
    public BigInteger getGasLimit()
    {
        if (!currentGasLimitOverride.equals(BigInteger.ZERO)) return currentGasLimitOverride;
        else return new BigInteger(C.DEFAULT_GAS_LIMIT);
    }

    public void setOverrideGasLimit(BigInteger gasOverride)
    {
        currentGasLimitOverride = gasOverride;
    }

    public void setOverrideGasPrice(BigInteger gasPriceOverride)
    {
        currentGasPriceOverride = gasPriceOverride;
    }

    public BigInteger getGasLimit(boolean tokenSending)
    {
        if (!currentGasLimitOverride.equals(BigInteger.ZERO))
        {
            return currentGasLimitOverride;
        }
        else if (tokenSending)
        {
            return new BigInteger(C.DEFAULT_GAS_LIMIT_FOR_TOKENS);
        }
        else
        {
            return new BigInteger(C.DEFAULT_GAS_LIMIT);
        }
    }

    public GasSettings getGasSettings(byte[] transactionBytes, boolean isNonFungible, int chainId)
    {
        BigInteger gasLimit = getGasLimit();
        BigInteger gasPrice = getGasPrice();
        if (transactionBytes != null) {
            if (isNonFungible)
            {
                gasLimit = new BigInteger(C.DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS);
            }
            else
            {
                gasLimit = new BigInteger(C.DEFAULT_GAS_LIMIT_FOR_TOKENS);
            }
            BigInteger estimate = estimateGasLimit(transactionBytes);
            if (estimate.compareTo(gasLimit) > 0) gasLimit = estimate;
            if (currentGasLimitOverride.equals(BigInteger.ZERO)) currentGasLimitOverride = gasLimit; //more accurate override
        }
        return new GasSettings(gasPrice, gasLimit);
    }

    private BigInteger estimateGasLimit(byte[] data)
    {
        BigInteger roundingFactor = BigInteger.valueOf(10000);
        BigInteger txMin = BigInteger.valueOf(C.GAS_LIMIT_MIN).multiply(BigInteger.valueOf(5));
        BigInteger bytePrice = BigInteger.valueOf(C.GAS_PER_BYTE);
        BigInteger dataLength = BigInteger.valueOf(data.length);
        BigInteger estimate = bytePrice.multiply(dataLength).add(txMin);
        estimate = estimate.divide(roundingFactor).add(BigInteger.ONE).multiply(roundingFactor);
        return estimate;
    }

    private void setCurrentPrice(int chainId)
    {
        if (EthereumNetworkRepository.hasGasOverride(chainId))
        {
            currentGasPrice = EthereumNetworkRepository.gasOverrideValue(chainId);
        }
        else
        {
            switch (chainId)
            {
                case EthereumNetworkRepository.XDAI_ID:
                    currentGasPrice = new BigInteger(C.DEFAULT_XDAI_GAS_PRICE);
                    break;
                default:
                    currentGasPrice = new BigInteger(C.DEFAULT_GAS_PRICE);
                    break;
            }
        }
    }
}
