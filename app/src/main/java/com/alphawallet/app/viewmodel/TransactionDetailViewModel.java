package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokenscript.EventUtils;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.repository.TransactionsRealmCache;
import com.alphawallet.app.repository.entity.RealmTransaction;
import com.alphawallet.app.router.ExternalBrowserRouter;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.web3.entity.Web3Transaction;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthTransaction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

import static com.alphawallet.app.repository.TokenRepository.getWeb3jService;

import javax.inject.Inject;

@HiltViewModel
public class TransactionDetailViewModel extends BaseViewModel {
    private final ExternalBrowserRouter externalBrowserRouter;

    private final FindDefaultNetworkInteract networkInteract;
    private final TokenRepositoryType tokenRepository;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final CreateTransactionInteract createTransactionInteract;

    private final KeyService keyService;
    private final TokensService tokenService;
    private final GasService gasService;
    private final AnalyticsServiceType analyticsService;

    private final MutableLiveData<BigInteger> latestBlock = new MutableLiveData<>();
    private final MutableLiveData<Transaction> latestTx = new MutableLiveData<>();
    private final MutableLiveData<Transaction> transaction = new MutableLiveData<>();
    private final MutableLiveData<TransactionData> transactionFinalised = new MutableLiveData<>();
    private final MutableLiveData<Throwable> transactionError = new MutableLiveData<>();

    public LiveData<BigInteger> latestBlock() {
        return latestBlock;
    }
    public LiveData<Transaction> latestTx() { return latestTx; }
    public LiveData<Transaction> onTransaction() { return transaction; }
    private String walletAddress;

    @Nullable
    private Disposable pendingUpdateDisposable;

    @Nullable
    private Disposable currentBlockUpdateDisposable;

    @Inject
    TransactionDetailViewModel(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            ExternalBrowserRouter externalBrowserRouter,
            TokenRepositoryType tokenRepository,
            TokensService tokenService,
            FetchTransactionsInteract fetchTransactionsInteract,
            KeyService keyService,
            GasService gasService,
            CreateTransactionInteract createTransactionInteract,
            AnalyticsServiceType analyticsService)
    {
        this.networkInteract = findDefaultNetworkInteract;
        this.externalBrowserRouter = externalBrowserRouter;
        this.tokenService = tokenService;
        this.tokenRepository = tokenRepository;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.keyService = keyService;
        this.gasService = gasService;
        this.createTransactionInteract = createTransactionInteract;
        this.analyticsService = analyticsService;
    }

    public MutableLiveData<TransactionData> transactionFinalised()
    {
        return transactionFinalised;
    }
    public MutableLiveData<Throwable> transactionError() { return transactionError; }

    public void prepare(final long chainId, final String walletAddr)
    {
        walletAddress = walletAddr;
        currentBlockUpdateDisposable = Observable.interval(0, 6, TimeUnit.SECONDS)
                .doOnNext(l -> {
                    disposable = tokenRepository.fetchLatestBlockNumber(chainId)
                            .subscribeOn(Schedulers.io())
                            .subscribeOn(AndroidSchedulers.mainThread())
                            .subscribe(latestBlock::postValue, t -> { this.latestBlock.postValue(BigInteger.ZERO); });
                }).subscribe();
    }

    public void showMoreDetails(Context context, Transaction transaction) {
        Uri uri = buildEtherscanUri(transaction);
        if (uri != null && Utils.isValidUrl(uri.toString())) {
            externalBrowserRouter.open(context, uri);
        }
    }

    public void startPendingTimeDisplay(final String txHash)
    {
        pendingUpdateDisposable = Observable.interval(0, 1, TimeUnit.SECONDS)
            .doOnNext(l -> displayCurrentPendingTime(txHash)).subscribe();
    }

    //TODO: move to display new transaction
    private void displayCurrentPendingTime(final String txHash)
    {
        //check if TX has been written
        Transaction tx = fetchTransactionsInteract.fetchCached(walletAddress, txHash);
        if (tx != null)
        {
            latestTx.postValue(tx);
            if (!tx.isPending())
            {
                if (pendingUpdateDisposable != null && !pendingUpdateDisposable.isDisposed())
                    pendingUpdateDisposable.dispose();
            }
        }
    }

    public void shareTransactionDetail(Context context, Transaction transaction)
    {
        Uri shareUri = buildEtherscanUri(transaction);
        if (shareUri != null) {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.subject_transaction_detail));
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareUri.toString());
            context.startActivity(Intent.createChooser(sharingIntent, "Share via"));
        }
    }

    public Token getToken(long chainId, String address)
    {
        return tokenService.getTokenOrBase(chainId, address);
    }

    @Nullable
    private Uri buildEtherscanUri(Transaction transaction)
    {
        NetworkInfo networkInfo = networkInteract.getNetworkInfo(transaction.chainId);
        if (networkInfo != null) {
            return networkInfo.getEtherscanUri(transaction.hash);
        }
        return null;
    }

    public boolean hasEtherscanDetail(Transaction tx)
    {
        NetworkInfo networkInfo = networkInteract.getNetworkInfo(tx.chainId);
        return networkInfo != null && !networkInfo.getEtherscanUri(tx.hash).equals(Uri.EMPTY);
    }

    public String getNetworkName(long chainId)
    {
        return networkInteract.getNetworkName(chainId);
    }

    public String getNetworkSymbol(long chainId)
    {
        return networkInteract.getNetworkInfo(chainId).symbol;
    }

    public void onDispose()
    {
        if (pendingUpdateDisposable != null && !pendingUpdateDisposable.isDisposed()) pendingUpdateDisposable.dispose();
        if (currentBlockUpdateDisposable != null && !currentBlockUpdateDisposable.isDisposed()) currentBlockUpdateDisposable.dispose();
    }

    public void fetchTransaction(Wallet wallet, String txHash, long chainId)
    {
        Transaction tx = fetchTransactionsInteract.fetchCached(wallet.address, txHash);
        if (tx == null || tx.gas.startsWith("0x"))
        {
            //fetch Transaction from chain
            Web3j web3j = getWeb3jService(chainId);
            disposable = EventUtils.getTransactionDetails(txHash, web3j)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(ethTx -> storeTx(ethTx, wallet, chainId, web3j), this::onError);
        }
        else
        {
            transaction.postValue(tx);
        }
    }

    private void storeTx(EthTransaction rawTx, Wallet wallet, long chainId, Web3j web3j)
    {
        //need to fetch the tx block time
        if (rawTx == null)
        {
            error.postValue(new ErrorEnvelope("no transaction"));
            return;
        }

        org.web3j.protocol.core.methods.response.Transaction ethTx = rawTx.getTransaction().get();
        disposable = EventUtils.getBlockDetails(ethTx.getBlockHash(), web3j)
                .map(ethBlock -> new Transaction(ethTx, chainId, true, ethBlock.getBlock().getTimestamp().longValue()))
                .map(tx -> writeTransaction(wallet, tx))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(transaction::postValue, this::onError);
    }

    private Transaction writeTransaction(Wallet wallet, Transaction tx)
    {
        try (Realm instance = fetchTransactionsInteract.getRealmInstance(wallet))
        {
            instance.beginTransaction();
            RealmTransaction realmTx = instance.where(RealmTransaction.class)
                    .equalTo("hash", tx.hash)
                    .findFirst();

            if (realmTx == null) realmTx = instance.createObject(RealmTransaction.class, tx.hash);
            TransactionsRealmCache.fill(realmTx, tx);
            instance.commitTransaction();
        }
        catch (Exception e)
        {
            //
        }

        return tx;
    }

    public void startGasCycle(long chainId)
    {
        gasService.startGasPriceCycle(chainId);
    }

    public void onDestroy()
    {
        gasService.stopGasPriceCycle();
    }

    public void restartServices()
    {
        fetchTransactionsInteract.restartTransactionService();
    }

    public TokensService getTokenService()
    {
        return tokenService;
    }

    public BigInteger calculateMinGasPrice(BigInteger oldGasPrice)
    {
        BigInteger candidateGasOverridePrice = new BigDecimal(oldGasPrice).multiply(BigDecimal.valueOf(1.1)).setScale(0, RoundingMode.CEILING).toBigInteger();
        BigInteger checkGasPrice = oldGasPrice.add(BalanceUtils.gweiToWei(BigDecimal.valueOf(2)));

        return checkGasPrice.max(candidateGasOverridePrice); //highest price between adding 2 gwei or 10%
    }

    public void getAuthentication(Activity activity, Wallet wallet, SignAuthenticationCallback callback)
    {
        keyService.getAuthenticationForSignature(wallet, activity, callback);
    }

    public void sendTransaction(Web3Transaction finalTx, Wallet wallet, long chainId, String overridenTxHash)
    {
        disposable = createTransactionInteract
                .createWithSig(wallet, finalTx, chainId)
                .subscribe(txData -> processTransaction(txData, wallet, overridenTxHash),
                        transactionError::postValue);
    }

    private void processTransaction(TransactionData txData, Wallet wallet, String overridenTxHash)
    {
        //remove old tx from database
        fetchTransactionsInteract.removeOverridenTransaction(wallet, overridenTxHash);
        //update Activity
        transactionFinalised.postValue(txData);
    }

    public void actionSheetConfirm(String mode)
    {
        AnalyticsProperties analyticsProperties = new AnalyticsProperties();
        analyticsProperties.setData(mode);

        analyticsService.track(C.AN_CALL_ACTIONSHEET, analyticsProperties);
    }
}
