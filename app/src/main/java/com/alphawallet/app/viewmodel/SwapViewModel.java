package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.lifi.Chain;
import com.alphawallet.app.entity.lifi.Connection;
import com.alphawallet.app.entity.lifi.Quote;
import com.alphawallet.app.entity.lifi.SwapProvider;
import com.alphawallet.app.entity.lifi.Token;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.SwapRepositoryType;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.SwapService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.SelectRouteActivity;
import com.alphawallet.app.ui.SelectSwapProvidersActivity;
import com.alphawallet.app.ui.widget.entity.ProgressInfo;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Hex;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@HiltViewModel
public class SwapViewModel extends BaseViewModel
{
    private final AssetDefinitionService assetDefinitionService;
    private final PreferenceRepositoryType preferenceRepository;
    private final SwapRepositoryType swapRepository;
    private final TokensService tokensService;
    private final SwapService swapService;
    private final CreateTransactionInteract createTransactionInteract;
    private final KeyService keyService;

    private final MutableLiveData<List<Chain>> chains = new MutableLiveData<>();
    private final MutableLiveData<Chain> chain = new MutableLiveData<>();
    private final MutableLiveData<List<Connection>> connections = new MutableLiveData<>();
    private final MutableLiveData<Quote> quote = new MutableLiveData<>();
    private final MutableLiveData<Long> network = new MutableLiveData<>();
    private final MutableLiveData<ProgressInfo> progressInfo = new MutableLiveData<>();
    private final MutableLiveData<TransactionData> transactionFinalised = new MutableLiveData<>();
    private final MutableLiveData<Throwable> transactionError = new MutableLiveData<>();

    private Disposable chainsDisposable;
    private Disposable connectionsDisposable;
    private Disposable quoteDisposable;
    private Disposable transactionDisposable;

    @Inject
    public SwapViewModel(
            AssetDefinitionService assetDefinitionService,
            PreferenceRepositoryType preferenceRepository,
            SwapRepositoryType swapRepository,
            TokensService tokensService,
            SwapService swapService,
            CreateTransactionInteract createTransactionInteract,
            KeyService keyService,
            AnalyticsServiceType analyticsService)
    {
        this.assetDefinitionService = assetDefinitionService;
        this.preferenceRepository = preferenceRepository;
        this.swapRepository = swapRepository;
        this.tokensService = tokensService;
        this.swapService = swapService;
        this.createTransactionInteract = createTransactionInteract;
        this.keyService = keyService;
        setAnalyticsService(analyticsService);
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    public TokensService getTokensService()
    {
        return tokensService;
    }

    public LiveData<List<Chain>> chains()
    {
        return chains;
    }

    public LiveData<Chain> chain()
    {
        return chain;
    }

    public LiveData<List<Connection>> connections()
    {
        return connections;
    }

    public LiveData<Quote> quote()
    {
        return quote;
    }

    public LiveData<Long> network()
    {
        return network;
    }

    public LiveData<ProgressInfo> progressInfo()
    {
        return progressInfo;
    }

    public LiveData<TransactionData> transactionFinalised()
    {
        return transactionFinalised;
    }

    public LiveData<Throwable> transactionError()
    {
        return transactionError;
    }

    public Chain getChain()
    {
        return chain.getValue();
    }

    public void setChain(Chain c)
    {
        chain.postValue(c);
    }

    public void getChains()
    {
        progressInfo.postValue(new ProgressInfo(true, R.string.message_fetching_chains));

        chainsDisposable = swapService.getChains()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onChains, this::onChainsError);
    }

    public String getSwapProviderUrl(String key)
    {
        List<SwapProvider> tools = getSwapProviders();
        for (SwapProvider td : tools)
        {
            if (key.startsWith(td.key))
            {
                return td.url;
            }
        }

        return "";
    }

    public void getConnections(long from, long to)
    {
        progressInfo.postValue(new ProgressInfo(true, R.string.message_fetching_connections));

        connectionsDisposable = swapService.getConnections(from, to)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onConnections, this::onConnectionsError);
    }

    public void getQuote(Token source, Token dest, String address, String amount, String slippage, String allowExchanges)
    {
        if (!isValidAmount(amount)) return;
        if (hasEnoughBalance(source, amount))
        {
            progressInfo.postValue(new ProgressInfo(true, R.string.message_fetching_quote));

            quoteDisposable = swapService.getQuote(source, dest, address, amount, slippage, allowExchanges)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onQuote, this::onQuoteError);
        }
        else
        {
            error.postValue(new ErrorEnvelope(C.ErrorCode.INSUFFICIENT_BALANCE, ""));
        }
    }

    private boolean isValidAmount(String amount)
    {
        try
        {
            BigDecimal d = new BigDecimal(amount);
        }
        catch (Exception e)
        {
            return false;
        }
        return true;
    }

    private void onChainsError(Throwable t)
    {
        postError(C.ErrorCode.SWAP_CHAIN_ERROR, Objects.requireNonNull(t.getMessage()));
    }

    private void onConnectionsError(Throwable t)
    {
        postError(C.ErrorCode.SWAP_CONNECTIONS_ERROR, Objects.requireNonNull(t.getMessage()));
    }

    private void onQuoteError(Throwable t)
    {
        postError(C.ErrorCode.SWAP_QUOTE_ERROR, Objects.requireNonNull(t.getMessage()));
    }

    public boolean hasEnoughBalance(Token source, String amount)
    {
        BigDecimal bal = new BigDecimal(getBalance(source));
        BigDecimal reqAmount = new BigDecimal(amount);
        return bal.compareTo(reqAmount) >= 0;
    }

    private void onChains(String result)
    {
        try
        {
            JSONObject obj = new JSONObject(result);
            if (obj.has("chains"))
            {
                JSONArray json = obj.getJSONArray("chains");

                List<Chain> chainList = new Gson().fromJson(json.toString(), new TypeToken<List<Chain>>()
                {
                }.getType());

                chains.postValue(chainList);
            }
            else
            {
                postError(C.ErrorCode.SWAP_CHAIN_ERROR, result);
            }
        }
        catch (JSONException e)
        {
            postError(C.ErrorCode.SWAP_CHAIN_ERROR, Objects.requireNonNull(e.getMessage()));
        }
    }

    private void onConnections(String result)
    {
        try
        {
            JSONObject obj = new JSONObject(result);
            if (obj.has("connections"))
            {
                JSONArray json = obj.getJSONArray("connections");

                List<Connection> connectionList = new Gson().fromJson(json.toString(), new TypeToken<List<Connection>>()
                {
                }.getType());

                connections.postValue(connectionList);
            }
            else
            {
                postError(C.ErrorCode.SWAP_CONNECTIONS_ERROR, result);
            }
        }
        catch (JSONException e)
        {
            postError(C.ErrorCode.SWAP_CONNECTIONS_ERROR, Objects.requireNonNull(e.getMessage()));
        }

        progressInfo.postValue(new ProgressInfo(false));
    }

    private void onQuote(String result)
    {
        if (!isValidQuote(result))
        {
            postError(C.ErrorCode.SWAP_QUOTE_ERROR, result);
        }
        else
        {
            Quote q = new Gson().fromJson(result, Quote.class);
            quote.postValue(q);
        }

        progressInfo.postValue(new ProgressInfo(false));
    }

    private void postError(int errorCode, String errorStr)
    {
        Timber.e(errorStr);
        if (errorStr.toLowerCase(Locale.ENGLISH).contains("timeout"))
        {
            this.error.postValue(new ErrorEnvelope(C.ErrorCode.SWAP_TIMEOUT_ERROR, errorStr));
            return;
        }
        this.error.postValue(new ErrorEnvelope(errorCode, checkMessage(errorStr)));
    }

    private String checkMessage(String errorStr)
    {
        try
        {
            JSONObject json = new JSONObject(errorStr);
            if (json.has("message"))
            {
                return json.getString("message");
            }
        }
        catch (JSONException e)
        {
            Timber.e(e);
        }
        return errorStr;
    }

    private boolean isValidQuote(String result)
    {
        return result.contains("id")
                && result.contains("action")
                && result.contains("tool");
    }

    public String getBalance(Token token)
    {
        com.alphawallet.app.entity.tokens.Token t;
        if (token.isNativeToken())
        {
            t = tokensService.getServiceToken(token.chainId);
        }
        else
        {
            t = tokensService.getToken(token.chainId, token.address);
        }

        if (t != null)
        {
            return BalanceUtils.getShortFormat(t.balance.toString(), t.tokenInfo.decimals);
        }
        else return "0";
    }

    public void getAuthentication(Activity activity, Wallet wallet, SignAuthenticationCallback callback)
    {
        keyService.getAuthenticationForSignature(wallet, activity, callback);
    }

    public void sendTransaction(Quote quote, Wallet wallet, long chainId)
    {
        sendTransaction(buildWeb3Transaction(quote), wallet, chainId);
    }

    public void sendTransaction(Web3Transaction finalTx, Wallet wallet, long chainId)
    {
        transactionDisposable = createTransactionInteract
                .createWithSig2(wallet, finalTx, chainId)
                .subscribe(transactionFinalised::postValue,
                        transactionError::postValue);
    }

    public Web3Transaction buildWeb3Transaction(Quote quote)
    {
        Quote.TransactionRequest request = quote.transactionRequest;

        return new Web3Transaction(
                new Address(request.from),
                new Address(request.to),
                Hex.hexToBigInteger(request.value, BigInteger.ZERO),
                Hex.hexToBigInteger(request.gasPrice, BigInteger.ZERO),
                Hex.hexToBigInteger(request.gasLimit, BigInteger.ZERO),
                -1,
                request.data
        );
    }

    public List<SwapProvider> getSwapProviders()
    {
        return swapRepository.getProviders();
    }

    public Set<String> getPreferredSwapProviders()
    {
        return preferenceRepository.getSelectedSwapProviders();
    }

    @Override
    protected void onCleared()
    {
        if (chainsDisposable != null && !chainsDisposable.isDisposed())
        {
            chainsDisposable.dispose();
        }
        if (connectionsDisposable != null && !connectionsDisposable.isDisposed())
        {
            connectionsDisposable.dispose();
        }
        if (quoteDisposable != null && !quoteDisposable.isDisposed())
        {
            quoteDisposable.dispose();
        }
        if (transactionDisposable != null && !transactionDisposable.isDisposed())
        {
            transactionDisposable.dispose();
        }
        super.onCleared();
    }

    public void getRoutes(Activity activity,
                          ActivityResultLauncher<Intent> launcher,
                          Token source,
                          Token dest,
                          String address,
                          String amount,
                          String slippage)
    {
        if (!isValidAmount(amount)) return;
        if (hasEnoughBalance(source, amount))
        {
            Intent intent = new Intent(activity, SelectRouteActivity.class);
            intent.putExtra("fromChainId", String.valueOf(source.chainId));
            intent.putExtra("toChainId", String.valueOf(dest.chainId));
            intent.putExtra("fromTokenAddress", String.valueOf(source.address));
            intent.putExtra("toTokenAddress", String.valueOf(dest.address));
            intent.putExtra("fromAddress", address);
            intent.putExtra("fromAmount", BalanceUtils.getRawFormat(amount, source.decimals));
            intent.putExtra("fromTokenDecimals", source.decimals);
            intent.putExtra("slippage", slippage);
            intent.putExtra("fromTokenSymbol", source.symbol);
            intent.putExtra("fromTokenIcon", source.symbol);
            intent.putExtra("fromTokenLogoUri", source.logoURI);
            launcher.launch(intent);
        }
        else
        {
            error.postValue(new ErrorEnvelope(C.ErrorCode.INSUFFICIENT_BALANCE, ""));
        }
    }

    public void prepare(Activity activity, ActivityResultLauncher<Intent> launcher)
    {
        if (getPreferredSwapProviders().isEmpty())
        {
            Intent intent = new Intent(activity, SelectSwapProvidersActivity.class);
            launcher.launch(intent);
        }
        else
        {
            getChains();
        }
    }
}
