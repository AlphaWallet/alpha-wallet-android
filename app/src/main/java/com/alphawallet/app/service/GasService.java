package com.alphawallet.app.service;

import static com.alphawallet.app.C.DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS;
import static com.alphawallet.app.C.GAS_LIMIT_CONTRACT;
import static com.alphawallet.app.C.GAS_LIMIT_DEFAULT;
import static com.alphawallet.app.C.GAS_LIMIT_MAX;
import static com.alphawallet.app.C.GAS_LIMIT_MIN;
import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static com.alphawallet.app.repository.TokenRepository.getWeb3jService;
import static com.alphawallet.app.repository.TokensRealmSource.TICKER_DB;
import static com.alphawallet.ethereum.EthereumNetworkBase.ARTIS_TAU1_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.text.TextUtils;

import androidx.annotation.NonNull;

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
import io.realm.Realm;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import timber.log.Timber;

/**
 * Created by JB on 18/11/2020.
 *
 * Starts a cycle to update the gas settings stored in the database
 */
public class GasService implements ContractGasProvider
{
    public final static long FETCH_GAS_PRICE_INTERVAL_SECONDS = 15;
    private final String WHALE_ACCOUNT = "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045"; //used for calculating gas estimate where a tx would exceed the limits with default gas settings

    private final EthereumNetworkRepositoryType networkRepository;
    private final OkHttpClient httpClient;
    private final RealmManager realmManager;
    private long currentChainId;
    private Web3j web3j;
    private BigInteger currentGasPrice;
    private BigInteger currentLowGasPrice = BigInteger.ZERO;
    private final String ETHERSCAN_API_KEY;
    private final String POLYGONSCAN_API_KEY;
    private boolean keyFail;

    @Nullable
    private Disposable gasFetchDisposable;

    static {
        System.loadLibrary("keys");
    }

    public static native String getEtherscanKey();
    public static native String getPolygonScanKey();

    public GasService(EthereumNetworkRepositoryType networkRepository, OkHttpClient httpClient, RealmManager realm)
    {
        this.networkRepository = networkRepository;
        this.httpClient = httpClient;
        this.realmManager = realm;
        gasFetchDisposable = null;
        this.currentChainId = MAINNET_ID;

        web3j = null;
        ETHERSCAN_API_KEY = "&apikey=" + getEtherscanKey();
        POLYGONSCAN_API_KEY = "&apikey=" + getPolygonScanKey();
        keyFail = false;
    }

    public void startGasPriceCycle(long chainId)
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

    public void updateChainId(long chainId)
    {
        if (networkRepository.getNetworkByChain(chainId) == null)
        {
            Timber.d("Network error, no chain, trying to pick: %s", chainId);
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
        currentLowGasPrice = BigInteger.ZERO;
        currentGasPrice = BigInteger.ZERO;
        updateCurrentGasPrices()
                .flatMap(this::useNodeFallback)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(updated -> {
                    Timber.d("Updated gas prices: %s", updated);
                    }, Throwable::printStackTrace)
                .isDisposed();
    }

    private Single<Boolean> useNodeFallback(Boolean updated)
    {
        if (updated) return Single.fromCallable(() -> true);
        else
        {
            return useNodeEstimate();
        }
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
        return new BigInteger(DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS);
    }

    private Single<Boolean> updateCurrentGasPrices()
    {
        String gasOracleAPI = EthereumNetworkRepository.getGasOracle(currentChainId);
        if (!TextUtils.isEmpty(gasOracleAPI))
        {
            if (!keyFail && gasOracleAPI.contains("etherscan")) gasOracleAPI += ETHERSCAN_API_KEY;
            if (!keyFail && gasOracleAPI.contains("polygonscan")) gasOracleAPI += POLYGONSCAN_API_KEY;
            return updateEtherscanGasPrices(gasOracleAPI);
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
            updateRealm(new GasPriceSpread(EthereumNetworkRepository.gasOverrideValue(currentChainId),
                    networkRepository.hasLockedGas(currentChainId)), currentChainId);
            currentGasPrice = EthereumNetworkRepository.gasOverrideValue(currentChainId);
            return Single.fromCallable(() -> true);
        }
        else
        {
            final long nodeId = currentChainId;
            return Single.fromCallable(() -> web3j
                    .ethGasPrice().send())
                    .map(price -> updateGasPrice(price, nodeId));
        }
    }

    private Boolean updateGasPrice(EthGasPrice ethGasPrice, long chainId)
    {
        currentGasPrice = fixGasPrice(ethGasPrice.getGasPrice(), chainId);
        updateRealm(new GasPriceSpread(currentGasPrice, networkRepository.hasLockedGas(chainId)), chainId);
        return true;
    }

    private BigInteger fixGasPrice(BigInteger gasPrice, long chainId)
    {
        if (gasPrice.compareTo(BigInteger.ZERO) > 0)
        {
            return gasPrice;
        }
        else
        {
            //gas price from node is zero
            switch ((int)chainId)
            {
                default:
                    return gasPrice;
                case (int)ARTIS_TAU1_ID:
                    //this node incorrectly returns gas price zero, use 1 Gwei
                    return new BigInteger(C.DEFAULT_XDAI_GAS_PRICE);
            }
        }
    }

    private Single<Boolean> updateEtherscanGasPrices(String gasOracleAPI)
    {
        final long chainId = currentChainId;
        return Single.fromCallable(() -> {
            boolean update = false;
            Request request = new Request.Builder()
                    .url(gasOracleAPI)
                    .get()
                    .build();
            try (okhttp3.Response response = httpClient.newCall(request).execute())
            {
                if (response.code() / 200 == 1)
                {
                    String result = response.body()
                            .string();
                    GasPriceSpread gps = new GasPriceSpread(result);
                    if (gps.isResultValid())
                    {
                        updateRealm(gps, chainId);
                        update = true;
                        currentGasPrice = gps.standard;
                        currentLowGasPrice = gps.baseFee;
                    }
                    else
                    {
                        keyFail = true;
                    }
                }
            }
            catch (Exception e)
            {
                Timber.e(e);
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
    private void updateRealm(final GasPriceSpread gasPriceSpread, final long chainId)
    {
        try (Realm realm = realmManager.getRealmInstance(TICKER_DB))
        {
            realm.executeTransaction(r -> {
                RealmGasSpread rgs = r.where(RealmGasSpread.class)
                        .equalTo("chainId", chainId)
                        .findFirst();
                if (rgs == null)
                    rgs = r.createObject(RealmGasSpread.class, chainId);

                rgs.setGasSpread(gasPriceSpread, gasPriceSpread.timeStamp);
            });
        }
    }

    public Single<BigInteger> calculateGasEstimate(byte[] transactionBytes, long chainId, String toAddress, BigInteger amount, Wallet wallet, final BigInteger defaultLimit)
    {
        String txData = "";
        if (transactionBytes != null && transactionBytes.length > 0)
        {
            txData = Numeric.toHexString(transactionBytes);
        }

        updateChainId(chainId);
        String finalTxData = txData;

        if ((toAddress.equals("") || toAddress.equals(ZERO_ADDRESS)) && txData.length() > 0) //Check gas for constructor
        {
            return networkRepository.getLastTransactionNonce(web3j, wallet.address)
                    .flatMap(nonce -> ethEstimateGas(wallet.address, nonce, getLowGasPrice(), BigInteger.valueOf(GAS_LIMIT_MAX), finalTxData))
                    .map(estimate -> convertToGasLimit(estimate, BigInteger.valueOf(GAS_LIMIT_CONTRACT)));
        }
        else
        {
            return networkRepository.getLastTransactionNonce(web3j, wallet.address)
                    .flatMap(nonce -> ethEstimateGas(wallet.address, nonce, getLowGasPrice(), BigInteger.valueOf(GAS_LIMIT_MAX), toAddress, amount, finalTxData))
                    .flatMap(estimate -> handleOutOfGasError(estimate, chainId, toAddress, amount, finalTxData))
                    .map(estimate -> convertToGasLimit(estimate, defaultLimit));
        }
    }

    private BigInteger convertToGasLimit(EthEstimateGas estimate, BigInteger defaultLimit)
    {
        if (estimate.hasError())
        {
            if (estimate.getError().getCode() == -32000) //out of gas
            {
                return defaultLimit;
            }
            else
            {
                return BigInteger.ZERO;
            }
        }
        else if (estimate.getAmountUsed().compareTo(BigInteger.ZERO) > 0)
        {
            return estimate.getAmountUsed();
        }
        else if (defaultLimit == null || defaultLimit.equals(BigInteger.ZERO))
        {
            return new BigInteger(DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS); //cautious gas limit
        }
        else
        {
            return defaultLimit;
        }
    }

    // If gas estimate failed due to insufficient gas, use whale account to estimate; we just want the tx estimate.
    private Single<EthEstimateGas> handleOutOfGasError(@NonNull EthEstimateGas estimate, long chainId, String toAddress, BigInteger amount, String finalTxData)
    {
        if (!estimate.hasError() || chainId != 1) return Single.fromCallable(() -> estimate);
        else return networkRepository.getLastTransactionNonce(web3j, WHALE_ACCOUNT)
            .flatMap(nonce -> ethEstimateGas(WHALE_ACCOUNT, nonce, getLowGasPrice(), getGasLimit(), toAddress, amount, finalTxData));
    }

    private BigInteger getLowGasPrice()
    {
        if (currentLowGasPrice.compareTo(BigInteger.ZERO) > 0)
        {
            return currentLowGasPrice;
        }
        else
        {
            return currentGasPrice;
        }
    }

    private Single<EthEstimateGas> ethEstimateGas(String fromAddress, BigInteger nonce, BigInteger gasPrice,
                                                  BigInteger gasLimit, String txData)
    {
        final Transaction transaction = new Transaction(fromAddress, nonce, gasPrice, gasLimit, null, BigInteger.ZERO, txData);
        return Single.fromCallable(() -> web3j.ethEstimateGas(transaction).send());
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
