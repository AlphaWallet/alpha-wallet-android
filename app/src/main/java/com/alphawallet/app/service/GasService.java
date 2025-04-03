package com.alphawallet.app.service;

import static com.alphawallet.app.C.DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS;
import static com.alphawallet.app.C.GAS_LIMIT_CONTRACT;
import static com.alphawallet.app.C.GAS_LIMIT_DEFAULT;
import static com.alphawallet.app.C.GAS_LIMIT_MIN;
import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static com.alphawallet.app.repository.TokenRepository.getWeb3jService;
import static com.alphawallet.app.repository.TokensRealmSource.TICKER_DB;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.alphawallet.app.entity.EIP1559FeeOracleResult;
import com.alphawallet.app.entity.FeeHistory;
import com.alphawallet.app.entity.GasEstimate;
import com.alphawallet.app.entity.GasPriceSpread;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.SuggestEIP1559Kt;
import com.alphawallet.app.entity.TXSpeed;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.HttpServiceHelper;
import com.alphawallet.app.repository.KeyProvider;
import com.alphawallet.app.repository.KeyProviderFactory;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.entity.Realm1559Gas;
import com.alphawallet.app.repository.entity.RealmGasSpread;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.google.gson.Gson;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

/**
 * Created by JB on 18/11/2020.
 *
 * Starts a cycle to update the gas settings stored in the database
 */
public class GasService implements ContractGasProvider
{
    public final static long FETCH_GAS_PRICE_INTERVAL_SECONDS = 15;
    private static final String BLOCK_COUNT = "[BLOCK_COUNT]";
    private static final String NEWEST_BLOCK = "[NEWEST_BLOCK]";
    private static final String REWARD_PERCENTILES = "[REWARD_PERCENTILES]";
    private static final String FEE_HISTORY = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_feeHistory\",\"params\":[\""+ BLOCK_COUNT +"\", \""+ NEWEST_BLOCK +"\",["+ REWARD_PERCENTILES +"]],\"id\":1}";
    private final String WHALE_ACCOUNT = "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045"; //used for calculating gas estimate where a tx would exceed the limits with default gas settings
    private final EthereumNetworkRepositoryType networkRepository;
    private final OkHttpClient httpClient;
    private final RealmManager realmManager;
    private long currentChainId;
    private Web3j web3j;
    private BigInteger currentGasPrice;
    private long currentGasPriceTime;
    private BigInteger currentLowGasPrice = BigInteger.ZERO;
    private final String ETHERSCAN_API_KEY;
    private final String POLYGONSCAN_API_KEY;
    private boolean keyFail;

    @Nullable
    private Disposable gasFetchDisposable;

    public GasService(EthereumNetworkRepositoryType networkRepository, OkHttpClient httpClient, RealmManager realm)
    {
        this.networkRepository = networkRepository;
        this.httpClient = httpClient;
        this.realmManager = realm;
        gasFetchDisposable = null;
        this.currentChainId = MAINNET_ID;

        web3j = null;
        KeyProvider keyProvider = KeyProviderFactory.get();
        ETHERSCAN_API_KEY = "&apikey=" + keyProvider.getEtherscanKey();
        POLYGONSCAN_API_KEY = "&apikey=" + keyProvider.getPolygonScanKey();
        keyFail = false;
        currentGasPrice = BigInteger.ZERO;
        currentGasPriceTime = 0;
    }

    public void startGasPriceCycle(long chainId)
    {
        updateChainId(chainId);
        if (gasFetchDisposable != null && !gasFetchDisposable.isDisposed()) gasFetchDisposable.dispose();
        gasFetchDisposable = Observable.interval(0, FETCH_GAS_PRICE_INTERVAL_SECONDS, TimeUnit.SECONDS)
                .doOnNext(l -> fetchCurrentGasPrice()).subscribe();
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
        else if (web3j == null || web3j.ethChainId().getId() != chainId)
        {
            currentGasPrice = BigInteger.ZERO;
            currentGasPriceTime = 0;
            currentChainId = chainId;
            web3j = getWeb3jService(chainId);
        }
    }

    private void fetchCurrentGasPrice()
    {
        currentLowGasPrice = BigInteger.ZERO;
        updateCurrentGasPrices()
                .flatMap(this::useNodeEstimate)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(updated -> {
                    Timber.d("Updated gas prices: %s", updated);
                }, Throwable::printStackTrace)
                .isDisposed();

        //also update EIP1559 if required and we haven't previously determined there's no EIP1559 support
        getEIP1559FeeStructure(currentChainId)
                .map(result -> updateEIP1559Realm(result, currentChainId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(r -> {
                    if (!r) Timber.d("Fail to update fees");
                }, this::handleError).isDisposed();
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

    private boolean nodeFetchValid()
    {
        return (System.currentTimeMillis() + FETCH_GAS_PRICE_INTERVAL_SECONDS * 1000) <= currentGasPriceTime;
    }

    private Single<Boolean> updateCurrentGasPrices()
    {
        String gasOracleAPI = EthereumNetworkRepository.getEtherscanGasOracle(currentChainId);
        if (!TextUtils.isEmpty(gasOracleAPI))
        {
            if (!keyFail && gasOracleAPI.contains("etherscan")) gasOracleAPI += ETHERSCAN_API_KEY;
            if (!keyFail && gasOracleAPI.contains("polygonscan")) gasOracleAPI += POLYGONSCAN_API_KEY;
            return updateEtherscanGasPrices(gasOracleAPI);
        }
        else
        {
            //use node to get chain price
            return useNodeEstimate(false);
        }
    }

    private Single<Boolean> useNodeEstimate(boolean updated)
    {
        if (nodeFetchValid())
        {
            return Single.fromCallable(() -> true);
        }
        else if (EthereumNetworkRepository.hasGasOverride(currentChainId))
        {
            updateRealm(new GasPriceSpread(EthereumNetworkRepository.gasOverrideValue(currentChainId),
                    networkRepository.hasLockedGas(currentChainId)), currentChainId);
            currentGasPriceTime = System.currentTimeMillis();
            currentGasPrice = EthereumNetworkRepository.gasOverrideValue(currentChainId);
            return Single.fromCallable(() -> true);
        }
        else
        {
            return getNodeEstimate(currentChainId)
                    .map(price -> updateGasPrice(price, currentChainId, updated));
        }
    }

    private Single<EthGasPrice> getNodeEstimate(long chainId)
    {
        return Single.fromCallable(() -> TokenRepository.getWeb3jService(chainId).ethGasPrice().send());
    }

    private Boolean updateGasPrice(EthGasPrice ethGasPrice, long chainId, boolean databaseUpdated)
    {
        currentGasPrice = ethGasPrice.getGasPrice();
        currentGasPriceTime = System.currentTimeMillis();
        if (!databaseUpdated)
        {
            updateRealm(new GasPriceSpread(currentGasPrice, networkRepository.hasLockedGas(chainId)), chainId);
        }
        return true;
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
            try (Response response = httpClient.newCall(request).execute())
            {
                if (response.code() / 200 == 1)
                {
                    String result = response.body()
                            .string();
                    GasPriceSpread gps = new GasPriceSpread(result);
                    updateRealm(gps, chainId);

                    if (gps.isResultValid())
                    {
                        update = true;
                        currentLowGasPrice = gps.getBaseFee();
                    }
                    else
                    {
                        keyFail = true;
                    }
                }
            }
            catch (Exception e)
            {
                Timber.w(e);
            }

            return update;
        });
    }

    /**
     * Store latest gas prices in the database.
     * This decouples gas service from any activity
     *
     * @param oracleResult
     * @param chainId
     */
    private void updateRealm(final GasPriceSpread oracleResult, final long chainId)
    {
        try (Realm realm = realmManager.getRealmInstance(TICKER_DB))
        {
            realm.executeTransaction(r -> {
                RealmGasSpread rgs = r.where(RealmGasSpread.class)
                        .equalTo("chainId", chainId)
                        .findFirst();
                if (rgs == null)
                    rgs = r.createObject(RealmGasSpread.class, chainId);

                rgs.setGasSpread(oracleResult, System.currentTimeMillis());
                r.insertOrUpdate(rgs);
            });
        }
    }

    //If for whatever reason gasprice hasn't been fetched or is out of date, use a manual fetch to ensure process goes through.
    public Single<EIP1559FeeOracleResult> fetchGasPrice(long chainId, boolean use1559Gas)
    {
        //fetch relevant average setting
        if (use1559Gas)
        {
            return getEIP1559FeeStructure(chainId)
                    .map(result -> {
                        //select average
                        EIP1559FeeOracleResult standard = (result != null && result.containsKey(TXSpeed.STANDARD)) ? result.get(TXSpeed.STANDARD) : null;
                        if (standard != null)
                        {
                            return standard;
                        }
                        else
                        {
                            //return legacy calc
                            EthGasPrice gasPrice = getNodeEstimate(chainId).blockingGet();
                            return new EIP1559FeeOracleResult(BigInteger.ZERO, BigInteger.ZERO, gasPrice.getGasPrice());
                        }
                    });
        }
        else
        {
            //get legacy gas
            return getNodeEstimate(chainId)
                    .map(result -> new EIP1559FeeOracleResult(result.getGasPrice(), BigInteger.ZERO, BigInteger.ZERO));

        }
    }

    private boolean updateEIP1559Realm(final Map<Integer, EIP1559FeeOracleResult> result, final long chainId)
    {
        boolean succeeded = true;
        try (Realm realm = realmManager.getRealmInstance(TICKER_DB))
        {
            realm.executeTransaction(r -> {
                Realm1559Gas rgs = r.where(Realm1559Gas.class)
                        .equalTo("chainId", chainId)
                        .findFirst();

                if (rgs == null)
                {
                    rgs = r.createObject(Realm1559Gas.class, chainId);
                }

                rgs.setResultData(result, System.currentTimeMillis());
                //r.insertOrUpdate(rgs);
            });
        }
        catch (Exception e)
        {
            succeeded = false;
        }

        return succeeded;
    }

    public Single<GasEstimate> calculateGasEstimate(byte[] transactionBytes, long chainId, String toAddress,
                                                    BigInteger amount, Wallet wallet, final BigInteger defaultLimit)
    {
        updateChainId(chainId);
        return useNodeEstimate(true)
                .flatMap(com -> calculateGasEstimateInternal(transactionBytes, chainId, toAddress, amount, wallet, defaultLimit));
    }

    public Single<GasEstimate> calculateGasEstimateInternal(byte[] transactionBytes, long chainId, String toAddress,
                                                            BigInteger amount, Wallet wallet, final BigInteger defaultLimit)
    {
        String txData = "";
        if (transactionBytes != null && transactionBytes.length > 0)
        {
            txData = Numeric.toHexString(transactionBytes);
        }

        updateChainId(chainId);
        String finalTxData = txData;

        BigInteger useGasLimit = defaultLimit.equals(BigInteger.ZERO) ? EthereumNetworkBase.getMaxGasLimit(chainId) : defaultLimit;

        if ((toAddress.equals("") || toAddress.equals(ZERO_ADDRESS)) && txData.length() > 0) //Check gas for constructor
        {
            return networkRepository.getLastTransactionNonce(web3j, wallet.address)
                    .flatMap(nonce -> ethEstimateGas(wallet.address, nonce, getLowGasPrice(), EthereumNetworkBase.getMaxGasLimit(chainId), finalTxData))
                    .map(estimate -> convertToGasLimit(estimate, EthereumNetworkBase.getMaxGasLimit(chainId)));
        }
        else
        {
            return networkRepository.getLastTransactionNonce(web3j, wallet.address)
                    .flatMap(nonce -> ethEstimateGas(chainId, wallet.address, useGasLimit, nonce, toAddress, amount, finalTxData))
                    .flatMap(estimate -> handleOutOfGasError(estimate, chainId, toAddress, amount, finalTxData))
                    .map(estimate -> convertToGasLimit(estimate, defaultLimit));
        }
    }

    private GasEstimate convertToGasLimit(EthEstimateGas estimate, BigInteger defaultLimit)
    {
        if (estimate.hasError())
        {
            if (estimate.getError().getCode() == -32000) //out of gas
            {
                return new GasEstimate(defaultLimit, estimate.getError().getMessage());
            }
            else
            {
                return new GasEstimate(BigInteger.ZERO, estimate.getError().getMessage());
            }
        }
        else if (estimate.getAmountUsed().compareTo(BigInteger.ZERO) > 0)
        {
            return new GasEstimate(estimate.getAmountUsed());
        }
        else if (defaultLimit == null || defaultLimit.equals(BigInteger.ZERO))
        {
            return new GasEstimate(new BigInteger(DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS));
        }
        else
        {
            return new GasEstimate(defaultLimit);
        }
    }

    // If gas estimate failed due to insufficient gas, use whale account to estimate; we just want the tx estimate.
    private Single<EthEstimateGas> handleOutOfGasError(@NonNull EthEstimateGas estimate, long chainId, String toAddress, BigInteger amount, String finalTxData)
    {
        if (!estimate.hasError() || chainId != 1) return Single.fromCallable(() -> estimate);
        else return networkRepository.getLastTransactionNonce(web3j, WHALE_ACCOUNT)
                .flatMap(nonce -> ethEstimateGas(chainId, WHALE_ACCOUNT, EthereumNetworkBase.getMaxGasLimit(chainId), nonce, toAddress, amount, finalTxData));
    }

    private BigInteger getLowGasPrice()
    {
        return currentGasPrice;
    }

    // For Constructor only
    private Single<EthEstimateGas> ethEstimateGas(String fromAddress, BigInteger nonce, BigInteger gasPrice,
                                                  BigInteger gasLimit, String txData)
    {
        final Transaction transaction = new Transaction(fromAddress, nonce, gasPrice, gasLimit, null, BigInteger.ZERO, txData);
        return Single.fromCallable(() -> web3j.ethEstimateGas(transaction).send());
    }

    private Single<EthEstimateGas> ethEstimateGas(long chainId, String fromAddress, BigInteger limit, BigInteger nonce, String toAddress,
                                                  BigInteger amount, String txData)
    {
        final Transaction transaction = new Transaction (
                fromAddress,
                nonce,
                currentGasPrice,
                limit,
                toAddress,
                amount,
                txData);

        return Single.fromCallable(() -> web3j.ethEstimateGas(transaction).send());
    }

    private Single<Map<Integer, EIP1559FeeOracleResult>> getEIP1559FeeStructure(long chainId)
    {
        return InfuraGasAPI.get1559GasEstimates(chainId, httpClient)
                .flatMap(result -> BlockNativeGasAPI.get(httpClient).get1559GasEstimates(result, chainId))
                .flatMap(this::useCalculationIfRequired); //if interface doesn't have blocknative API then use calculation method
    }

    private Single<Map<Integer, EIP1559FeeOracleResult>> useCalculationIfRequired(Map<Integer, EIP1559FeeOracleResult> resultMap)
    {
        if (resultMap.size() > 0)
        {
            return Single.fromCallable(() -> resultMap);
        }
        else
        {
            return getEIP1559FeeStructureCalculation();
        }
    }

    private Single<Map<Integer, EIP1559FeeOracleResult>> getEIP1559FeeStructureCalculation()
    {
        return getChainFeeHistory(100, "latest", "")
                .flatMap(feeHistory -> SuggestEIP1559Kt.suggestEIP1559(this, feeHistory));
    }

    private void handleError(Throwable err)
    {
        Timber.w(err);
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
            case ERC721_ENUMERABLE:
                return new BigInteger(DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS);
            default:
                //unknown
                return BigInteger.valueOf(GAS_LIMIT_CONTRACT);
        }
    }

    public Single<FeeHistory> getChainFeeHistory(int blockCount, String lastBlock, String rewardPercentiles)
    {
        //TODO: Replace once Web3j fully supports EIP1559
        String requestJSON = FEE_HISTORY.replace(BLOCK_COUNT, (Numeric.prependHexPrefix(Long.toHexString(blockCount)))).replace(NEWEST_BLOCK, lastBlock)
                .replace(REWARD_PERCENTILES, rewardPercentiles);

        RequestBody requestBody = RequestBody.create(requestJSON, HttpService.JSON_MEDIA_TYPE);
        NetworkInfo info = networkRepository.getNetworkByChain(currentChainId);

        final Request.Builder rqBuilder = new Request.Builder()
                .url(info.rpcServerUrl)
                .post(requestBody);

        HttpServiceHelper.addRequiredCredentials(info.rpcServerUrl, rqBuilder,
                KeyProviderFactory.get().getInfuraSecret());

        return Single.fromCallable(() -> {
            Request request = rqBuilder.build();

            try (Response response = httpClient.newCall(request).execute())
            {
                if (response.code() / 200 == 1)
                {
                    JSONObject jsonData = new JSONObject(response.body().string());
                    return new Gson().fromJson(jsonData.getJSONObject("result").toString(), FeeHistory.class);
                }
            }
            catch (org.json.JSONException j)
            {
                Timber.e("Note: " + info.getShortName() + " does not appear to have EIP1559 support");
            }
            catch (Exception e)
            {
                Timber.e(e);
            }

            return new FeeHistory();
        });
    }
}
