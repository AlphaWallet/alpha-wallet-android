package com.alphawallet.app.viewmodel;

import android.app.Activity;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.lifi.Chain;
import com.alphawallet.app.entity.lifi.Connection;
import com.alphawallet.app.entity.lifi.Quote;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.SwapService;
import com.alphawallet.app.service.TokensService;
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

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@HiltViewModel
public class SwapViewModel extends BaseViewModel
{
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;
    private final SwapService swapService;
    private final CreateTransactionInteract createTransactionInteract;
    private final KeyService keyService;

    private final MutableLiveData<List<Chain>> chains = new MutableLiveData<>();
    private final MutableLiveData<Chain> chain = new MutableLiveData<>();
    private final MutableLiveData<List<Connection>> connections = new MutableLiveData<>();
    private final MutableLiveData<Quote> quote = new MutableLiveData<>();
    private final MutableLiveData<Long> network = new MutableLiveData<>();
    private final MutableLiveData<Integer> progressInfo = new MutableLiveData<>();
    private final MutableLiveData<TransactionData> transactionFinalised = new MutableLiveData<>();
    private final MutableLiveData<Throwable> transactionError = new MutableLiveData<>();

    private Disposable chainsDisposable;
    private Disposable connectionsDisposable;
    private Disposable quoteDisposable;
    private Disposable transactionDisposable;

    @Inject
    public SwapViewModel(
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService,
            SwapService swapService,
            CreateTransactionInteract createTransactionInteract,
            KeyService keyService)
    {
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
        this.swapService = swapService;
        this.createTransactionInteract = createTransactionInteract;
        this.keyService = keyService;
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

    public LiveData<Integer> progressInfo()
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
        progressInfo.postValue(C.ProgressInfo.FETCHING_CHAINS);
        progress.postValue(true);

        chainsDisposable = swapService.getChains()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onChains, this::onError);
    }

    public void getConnections(long from, long to)
    {
        progressInfo.postValue(C.ProgressInfo.FETCHING_CONNECTIONS);
        progress.postValue(true);

        connectionsDisposable = swapService.getConnections(from, to)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onConnections, this::onError);
    }

    public void getQuote(Connection.LToken source, Connection.LToken dest, String address, String amount, String slippage)
    {
        if (hasEnoughBalance(address, source, amount))
        {
            progressInfo.postValue(C.ProgressInfo.FETCHING_QUOTE);
            progress.postValue(true);

            quoteDisposable = swapService.getQuote(source, dest, address, amount, slippage)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onQuote, this::onError);
        }
        else
        {
            error.postValue(new ErrorEnvelope(C.ErrorCode.INSUFFICIENT_BALANCE, ""));
        }
    }

    public boolean hasEnoughBalance(String address, Connection.LToken source, String amount)
    {
        BigDecimal bal = new BigDecimal(getBalance(address, source));
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
                error.postValue(new ErrorEnvelope(C.ErrorCode.SWAP_API_ERROR, result));
            }
        }
        catch (JSONException e)
        {
            error.postValue(new ErrorEnvelope(C.ErrorCode.SWAP_API_ERROR, e.getMessage()));
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
                error.postValue(new ErrorEnvelope(C.ErrorCode.SWAP_API_ERROR, result));
            }
        }
        catch (JSONException e)
        {
            error.postValue(new ErrorEnvelope(C.ErrorCode.SWAP_API_ERROR, e.getMessage()));
        }

        progress.postValue(false);
    }

    private void onQuote(String result)
    {
        if (!isValidQuote(result))
        {
            error.postValue(new ErrorEnvelope(C.ErrorCode.SWAP_API_ERROR, result));
        }
        else
        {
            Quote q = new Gson().fromJson(result, Quote.class);
            quote.postValue(q);
        }

        progress.postValue(false);
    }

    private boolean isValidQuote(String result)
    {
        return result.contains("id")
                && result.contains("action")
                && result.contains("tool");
    }

    public String getBalance(String walletAddress, Connection.LToken token)
    {
        String address = token.address;

        // Note: In the LIFI API, the native token has either of these two addresses.
        // In AlphaWallet, the wallet address is used.
        if (address.equalsIgnoreCase("0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee") ||
                address.equalsIgnoreCase("0x0000000000000000000000000000000000000000"))
        {
            address = walletAddress;
        }
        Token t = tokensService.getToken(token.chainId, address);
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
}
