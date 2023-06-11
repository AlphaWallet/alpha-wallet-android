package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.GasEstimate;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.TransactionReturn;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.service.TransactionSendHandlerInterface;
import com.alphawallet.app.ui.ImportTokenActivity;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.hardware.SignatureFromKey;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@HiltViewModel
public class SendViewModel extends BaseViewModel implements TransactionSendHandlerInterface
{
    private final MutableLiveData<Token> finalisedToken = new MutableLiveData<>();
    private final MutableLiveData<TransactionReturn> transactionFinalised = new MutableLiveData<>();
    private final MutableLiveData<TransactionReturn> transactionError = new MutableLiveData<>();
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<List<Token>> tokens = new MutableLiveData<>();

    private final MyAddressRouter myAddressRouter;
    private final EthereumNetworkRepositoryType networkRepository;
    private final TokensService tokensService;
    private final GasService gasService;
    private final AssetDefinitionService assetDefinitionService;
    private final KeyService keyService;
    private final CreateTransactionInteract createTransactionInteract;
    private final PreferenceRepositoryType preferenceRepository;
    private final GenericWalletInteract genericWalletInteract;
    private final FetchTokensInteract fetchTokensInteract;

    @Inject
    public SendViewModel(MyAddressRouter myAddressRouter,
                         EthereumNetworkRepositoryType ethereumNetworkRepositoryType,
                         TokensService tokensService,
                         CreateTransactionInteract createTransactionInteract,
                         GasService gasService,
                         AssetDefinitionService assetDefinitionService,
                         KeyService keyService,
                         AnalyticsServiceType analyticsService,
                         PreferenceRepositoryType preferenceRepository,
                         GenericWalletInteract genericWalletInteract,
                         FetchTokensInteract fetchTokensInteract)
    {
        this.myAddressRouter = myAddressRouter;
        this.networkRepository = ethereumNetworkRepositoryType;
        this.tokensService = tokensService;
        this.gasService = gasService;
        this.assetDefinitionService = assetDefinitionService;
        this.keyService = keyService;
        this.createTransactionInteract = createTransactionInteract;
        this.preferenceRepository = preferenceRepository;
        this.genericWalletInteract = genericWalletInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        setAnalyticsService(analyticsService);
    }

    public void prepare()
    {
        disposable = genericWalletInteract.find()
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .subscribe(wallet::postValue, this::onError);
    }

    public void fetchTokens()
    {
        disposable = fetchTokensInteract.fetchTokenMetas(wallet.getValue(), tokensService.getNetworkFilters(), assetDefinitionService)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::onTokenMetas, this::onError);
    }

    private void onTokenMetas(TokenCardMeta[] tokenCardMetas)
    {
        Arrays.sort(tokenCardMetas, Comparator.comparing(TokenCardMeta::getNameWeight));
        List<Token> tokenList = new ArrayList<>();
        int i = 0;
        for (TokenCardMeta meta : tokenCardMetas)
        {
            Pair<String, Long> lastSentToken = getLastSentToken();
            Token t = tokensService.getToken(meta.getChain(), meta.getAddress());
            if (t.balance.compareTo(BigDecimal.ZERO) > 0 && !t.isNonFungible())
            {
                boolean isLastSentToken = t.tokenInfo.address.equalsIgnoreCase(lastSentToken.first);
                if (i == 0 && isLastSentToken && t.tokenInfo.chainId == lastSentToken.second)
                {
                    tokenList.add(i++, t); // Insert at the top
                }
                else if (i > 0 && isLastSentToken)
                {
                    // Will only trigger if last sent token is a network token
                    // List other network tokens below last sent
                    tokenList.add(i++, t);
                }
                else
                {
                    tokenList.add(t);
                }
            }
        }

        tokens.postValue(tokenList);
    }

    public Pair<String, Long> getLastSentToken()
    {
        return preferenceRepository.getLastSentToken();
    }

    public void setLastSentToken(Token token)
    {
        preferenceRepository.setLastSentToken(token);
    }

    public LiveData<List<Token>> tokens()
    {
        return tokens;
    }

    public MutableLiveData<Wallet> wallet()
    {
        return wallet;
    }

    public MutableLiveData<Token> finalisedToken()
    {
        return finalisedToken;
    }

    public MutableLiveData<TransactionReturn> transactionFinalised()
    {
        return transactionFinalised;
    }

    public MutableLiveData<TransactionReturn> transactionError()
    {
        return transactionError;
    }

    public void showContractInfo(Context ctx, Wallet wallet, Token token)
    {
        myAddressRouter.open(ctx, wallet, token);
    }

    public NetworkInfo getNetworkInfo(long chainId)
    {
        return networkRepository.getNetworkByChain(chainId);
    }

    public boolean isValidChain(long chainId)
    {
        return networkRepository.getNetworkByChain(chainId) != null;
    }

    public boolean isNetworkEnabled(long chainId)
    {
        return networkRepository.getFilterNetworkList().contains(chainId);
    }

    public Token getToken(long chainId, String tokenAddress)
    {
        return tokensService.getToken(chainId, tokenAddress);
    }

    public void showImportLink(Context context, String importTxt)
    {
        Intent intent = new Intent(context, ImportTokenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(C.IMPORT_STRING, importTxt);
        context.startActivity(intent);
    }

    public void fetchToken(long chainId, String address, String walletAddress)
    {
        tokensService.update(address, chainId, ContractType.NOT_SET)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(tokenInfo -> gotTokenUpdate(tokenInfo, walletAddress), this::onError).isDisposed();
    }

    private void gotTokenUpdate(TokenInfo tokenInfo, String walletAddress)
    {
        disposable = tokensService.addToken(tokenInfo, walletAddress)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(finalisedToken::postValue, this::onError);
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    public TokensService getTokenService()
    {
        return tokensService;
    }

    public void startGasCycle(long chainId)
    {
        gasService.startGasPriceCycle(chainId);
    }

    public void onDestroy()
    {
        gasService.stopGasPriceCycle();
    }

    public byte[] getTransactionBytes(Token token, String sendAddress, BigDecimal sendAmount)
    {
        byte[] txBytes;
        if (token.isEthereum())
        {
            txBytes = new byte[0];
        }
        else
        {
            txBytes = TokenRepository.createTokenTransferData(sendAddress, sendAmount.toBigInteger());
        }

        return txBytes;
    }

    public Single<GasEstimate> calculateGasEstimate(Wallet wallet, byte[] transactionBytes, long chainId, String sendAddress, BigDecimal sendAmount)
    {
        return gasService.calculateGasEstimate(transactionBytes, chainId, sendAddress, sendAmount.toBigInteger(), wallet, BigInteger.ZERO);
    }

    public void getAuthentication(Activity activity, Wallet wallet, SignAuthenticationCallback callback)
    {
        keyService.getAuthenticationForSignature(wallet, activity, callback);
    }

    public void requestSignature(Web3Transaction finalTx, Wallet wallet, long chainId)
    {
        createTransactionInteract.requestSignature(finalTx, wallet, chainId, this);
    }

    public void sendTransaction(Wallet wallet, long chainId, Web3Transaction tx, SignatureFromKey signatureFromKey)
    {
        createTransactionInteract.sendTransaction(wallet, chainId, tx, signatureFromKey);
    }

    @Override
    public void transactionFinalised(TransactionReturn txData)
    {
        transactionFinalised.postValue(txData);
    }

    @Override
    public void transactionError(TransactionReturn txError)
    {
        transactionError.postValue(txError);
    }
}
