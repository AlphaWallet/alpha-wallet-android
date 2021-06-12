package com.alphawallet.app.service;

import android.text.format.DateUtils;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.entity.GasPriceSpread;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.entity.RealmGasSpread;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.token.tools.Numeric;

import org.jetbrains.annotations.Nullable;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import static com.alphawallet.app.C.DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS;
import static com.alphawallet.app.C.GAS_LIMIT_CONTRACT;
import static com.alphawallet.app.C.GAS_LIMIT_DEFAULT;
import static com.alphawallet.app.C.GAS_LIMIT_MIN;
import static com.alphawallet.app.repository.TokenRepository.getWeb3jService;
import static com.alphawallet.app.repository.TokensRealmSource.TICKER_DB;
import static com.alphawallet.ethereum.EthereumNetworkBase.ARTIS_TAU1_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

/**
 * Created by JB on 18/11/2020.
 *
 * Starts a cycle to update the gas settings stored in the database
 * Service automatically cleans up after itself - erasing readings older than 12 hours
 */
public class GasService2 implements ContractGasProvider
{
    private final static String GAS_NOW_API = "https://www.gasnow.org/api/v3/gas/price?utm_source=AlphaWallet";
    public final static long FETCH_GAS_PRICE_INTERVAL_SECONDS = 15;
    private final static long TWELVE_HOURS = 12 * DateUtils.HOUR_IN_MILLIS;

    private final EthereumNetworkRepositoryType networkRepository;
    private final OkHttpClient httpClient;
    private final RealmManager realmManager;
    private int currentChainId;
    private Web3j web3j;
    private BigInteger currentGasPrice;

    @Nullable
    private Disposable gasFetchDisposable;

    public GasService2(EthereumNetworkRepositoryType networkRepository, OkHttpClient httpClient, RealmManager realm)
    {
        this.networkRepository = networkRepository;
        this.httpClient = httpClient;
        this.realmManager = realm;
        gasFetchDisposable = null;
        currentChainId = MAINNET_ID;

        web3j = null;
    }

    public void startGasPriceCycle(int chainId)
    {
        updateChainId(chainId);
        if (gasFetchDisposable == null || gasFetchDisposable.isDisposed())
        {
            gasFetchDisposable = Observable.interval(0, FETCH_GAS_PRICE_INTERVAL_SECONDS, TimeUnit.SECONDS)
                    .doOnNext(l -> fetchCurrentGasPrice()).subscribe();
        }
    }

    public void stopGasPriceCycle()
    {
        if (gasFetchDisposable != null && !gasFetchDisposable.isDisposed())
        {
            gasFetchDisposable.dispose();
        }
    }

    public void updateChainId(int chainId)
    {
        if (networkRepository.getNetworkByChain(chainId) == null)
        {
            System.out.println("Network error, no chain, trying to pick: " + chainId);
        }
        else if (EthereumNetworkRepository.hasGasOverride(chainId))
        {
            currentChainId = chainId;
        }
        else if (web3j == null || web3j.ethChainId().getId() != chainId)
        {
            currentChainId = chainId;
            web3j = getWeb3jService(chainId);
        }
    }

    private void fetchCurrentGasPrice()
    {
        updateCurrentGasPrices()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(updated -> { if (BuildConfig.DEBUG) System.out.println("Updated gas prices: " + updated); },
                        Throwable::printStackTrace)
                .isDisposed();
    }

    @Override
    public BigInteger getGasPrice(String contractFunc)
    {
        return currentGasPrice;
    }

    @Override
    public BigInteger getGasPrice()
    {
        return currentGasPrice;
    }

    @Override
    public BigInteger getGasLimit(String contractFunc)
    {
        return null;
    }

    @Override
    public BigInteger getGasLimit()
    {
        return new BigInteger(C.DEFAULT_UNKNOWN_FUNCTION_GAS_LIMIT);
    }

    private Single<Boolean> updateCurrentGasPrices()
    {
        if (currentChainId == MAINNET_ID)
        {
            return updateGasNow();
        }
        else
        {
            //use node to get chain price
            return useNodeEstimate();
        }
    }

    private Single<Boolean> useNodeEstimate()
    {
        if (EthereumNetworkRepository.hasGasOverride(currentChainId))
        {
            updateRealm(new GasPriceSpread(EthereumNetworkRepository.gasOverrideValue(currentChainId)), currentChainId);
            currentGasPrice = EthereumNetworkRepository.gasOverrideValue(currentChainId);
            return Single.fromCallable(() -> true);
        }
        else
        {
            final int nodeId = currentChainId;
            return Single.fromCallable(() -> web3j
                    .ethGasPrice().send())
                    .map(price -> updateGasPrice(price, nodeId));
        }
    }

    private Boolean updateGasPrice(EthGasPrice ethGasPrice, int chainId)
    {
        currentGasPrice = fixGasPrice(ethGasPrice.getGasPrice(), chainId);
        updateRealm(new GasPriceSpread(currentGasPrice), chainId);
        return true;
    }

    private BigInteger fixGasPrice(BigInteger gasPrice, int chainId)
    {
        if (gasPrice.compareTo(BigInteger.ZERO) > 0)
        {
            return gasPrice;
        }
        else
        {
            //gas price from node is zero
            switch (chainId)
            {
                default:
                    return gasPrice;
                case ARTIS_TAU1_ID:
                    //this node incorrectly returns gas price zero, use 1 Gwei
                    return new BigInteger(C.DEFAULT_XDAI_GAS_PRICE);
            }
        }
    }

    private Single<Boolean> updateGasNow()
    {
        return Single.fromCallable(() -> {
            boolean update = false;
            try
            {
                Request request = new Request.Builder()
                        .url(GAS_NOW_API)
                        .get()
                        .build();
                okhttp3.Response response = httpClient.newCall(request)
                        .execute();

                if (response.code() / 200 == 1)
                {
                    String result = response.body()
                            .string();
                    GasPriceSpread gps = new GasPriceSpread(result);
                    updateRealm(gps, MAINNET_ID);
                    update = true;
                    currentGasPrice = gps.standard;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return update;
        });
    }

    /**
     * Store latest gas prices in the database.
     * This decouples gas service from any activity
     *
     * @param gasPriceSpread
     * @param chainId
     */
    private void updateRealm(final GasPriceSpread gasPriceSpread, final int chainId)
    {
        realmManager.getRealmInstance(TICKER_DB).executeTransactionAsync(r -> {
            RealmGasSpread rgs = r.where(RealmGasSpread.class)
                    .equalTo("timeStamp", gasPriceSpread.timeStamp)
                    .findFirst();
            if (rgs == null)
                rgs = r.createObject(RealmGasSpread.class, gasPriceSpread.timeStamp);

            rgs.setGasSpread(gasPriceSpread, chainId);

            //remove old results
            r.where(RealmGasSpread.class)
                    .lessThan("timeStamp", gasPriceSpread.timeStamp - TWELVE_HOURS)
                    .findAll().deleteAllFromRealm();
        });
    }

    public Single<EthEstimateGas> calculateGasEstimate(byte[] transactionBytes, int chainId, String toAddress, BigInteger amount, Wallet wallet)
    {
        String txData = "";
        if (transactionBytes != null && transactionBytes.length > 0)
        {
            txData = Numeric.toHexString(transactionBytes);
        }

        updateChainId(chainId);
        String finalTxData = txData;

        return networkRepository.getLastTransactionNonce(web3j, wallet.address)
                .flatMap(nonce -> ethEstimateGas(wallet.address, nonce, getGasPrice(), getGasLimit(), toAddress, amount, finalTxData));
    }

    private Single<EthEstimateGas> ethEstimateGas(String fromAddress, BigInteger nonce, BigInteger gasPrice,
                                                  BigInteger gasLimit, String toAddress,
                                                  BigInteger amount, String txData)
    {
        final Transaction transaction = new Transaction (
                fromAddress,
                nonce,
                gasPrice,
                gasLimit,
                toAddress,
                amount,
                txData);

        return Single.fromCallable(() -> web3j.ethEstimateGas(transaction).send());
    }

    public static BigInteger getDefaultGasLimit(Token token, Web3Transaction tx)
    {
        boolean hasPayload = tx.payload != null && tx.payload.length() >= 10;

        switch (token.getInterfaceSpec())
        {
            case ETHEREUM:
                return hasPayload ? BigInteger.valueOf(GAS_LIMIT_CONTRACT) : BigInteger.valueOf(GAS_LIMIT_MIN);
            case ERC20:
                return BigInteger.valueOf(GAS_LIMIT_DEFAULT);
            case ERC875_LEGACY:
            case ERC875:
            case ERC721:
            case ERC721_LEGACY:
            case ERC721_TICKET:
            case ERC721_UNDETERMINED:
                return new BigInteger(DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS);
            default:
                //unknown
                return BigInteger.valueOf(GAS_LIMIT_CONTRACT);
        }
    }
}
