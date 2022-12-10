package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.DisplayState;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.cryptokeys.SignatureFromKey;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.TransferTicketDetailActivity;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.entity.SignableBytes;
import com.alphawallet.token.tools.ParseMagicLink;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by James on 21/02/2018.
 */
@HiltViewModel
public class TransferTicketDetailViewModel extends BaseViewModel
{
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<String> newTransaction = new MutableLiveData<>();
    private final MutableLiveData<String> universalLinkReady = new MutableLiveData<>();
    private final MutableLiveData<TransactionData> transactionFinalised = new MutableLiveData<>();
    private final MutableLiveData<Throwable> transactionError = new MutableLiveData<>();

    private final GenericWalletInteract genericWalletInteract;
    private final KeyService keyService;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final GasService gasService;
    private final TokensService tokensService;

    private ParseMagicLink parser;
    private Token token;

    private byte[] linkMessage;

    @Inject
    TransferTicketDetailViewModel(GenericWalletInteract genericWalletInteract,
                                  KeyService keyService,
                                  CreateTransactionInteract createTransactionInteract,
                                  FetchTransactionsInteract fetchTransactionsInteract,
                                  AssetDefinitionService assetDefinitionService,
                                  GasService gasService,
                                  AnalyticsServiceType analyticsService,
                                  TokensService tokensService)
    {
        this.genericWalletInteract = genericWalletInteract;
        this.keyService = keyService;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.gasService = gasService;
        this.tokensService = tokensService;
        setAnalyticsService(analyticsService);
    }

    public MutableLiveData<TransactionData> transactionFinalised()
    {
        return transactionFinalised;
    }

    public MutableLiveData<Throwable> transactionError()
    {
        return transactionError;
    }

    public LiveData<Wallet> defaultWallet()
    {
        return defaultWallet;
    }

    public LiveData<String> newTransaction()
    {
        return newTransaction;
    }

    public LiveData<String> universalLinkReady()
    {
        return universalLinkReady;
    }

    private void initParser()
    {
        if (parser == null)
        {
            parser = new ParseMagicLink(new CryptoFunctions(), EthereumNetworkRepository.extraChains());
        }
    }

    public void prepare(Token token)
    {
        this.token = token;
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);

        gasService.startGasPriceCycle(token.tokenInfo.chainId);
    }

    private void onDefaultWallet(Wallet wallet)
    {
        defaultWallet.setValue(wallet);
    }

    public Wallet getWallet()
    {
        return defaultWallet.getValue();
    }

    public void setWallet(Wallet wallet)
    {
        defaultWallet.setValue(wallet);
    }

    public void generateUniversalLink(List<BigInteger> ticketSendIndexList, String contractAddress, long expiry)
    {
        initParser();
        if (ticketSendIndexList == null || ticketSendIndexList.size() == 0)
            return; //TODO: Display error message

        int[] indexList = Utils.bigIntegerListToIntList(ticketSendIndexList);

        //NB tradeBytes is the exact bytes the ERC875 contract builds to check the valid order.
        //This is what we must sign.
        SignableBytes tradeBytes = new SignableBytes(parser.getTradeBytes(indexList, contractAddress, BigInteger.ZERO, expiry));
        try
        {
            linkMessage = ParseMagicLink.generateLeadingLinkBytes(indexList, contractAddress, BigInteger.ZERO, expiry);
        }
        catch (SalesOrderMalformed e)
        {
            //TODO: Display appropriate error to user
        }

        //sign this link
        disposable = createTransactionInteract
                .sign(defaultWallet().getValue(), tradeBytes)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::gotSignature, this::onError);
    }

    public void generateSpawnLink(List<BigInteger> tokenIds, String contractAddress, long expiry)
    {
        initParser();
        SignableBytes tradeBytes = new SignableBytes(parser.getSpawnableBytes(tokenIds, contractAddress, BigInteger.ZERO, expiry));
        try
        {
            linkMessage = ParseMagicLink.generateSpawnableLeadingLinkBytes(tokenIds, contractAddress, BigInteger.ZERO, expiry);
        }
        catch (SalesOrderMalformed e)
        {
            //TODO: Display appropriate error to user
        }

        //sign this link
        disposable = createTransactionInteract
                .sign(defaultWallet().getValue(), tradeBytes)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::gotSignature, this::onError);
    }

    private void gotSignature(SignatureFromKey signature)
    {
        String universalLink = parser.completeUniversalLink(token.tokenInfo.chainId, linkMessage, signature.signature);
        //Now open the share icon
        universalLinkReady.postValue(universalLink);
    }

    public void createTokenTransfer(String to, Token token, List<BigInteger> transferList)
    {
        if (!token.contractTypeValid())
        {
            //need to determine the spec
            disposable = fetchTransactionsInteract.queryInterfaceSpec(token.tokenInfo)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(spec -> onInterfaceSpec(spec, to, token, transferList), this::onError);
        }
        else
        {
            final byte[] data = TokenRepository.createTicketTransferData(to, transferList, token);
            disposable = createTransactionInteract.create(defaultWallet.getValue(), token.getAddress(),
                            BigInteger.ZERO, gasService.getGasPrice(), new BigInteger(C.DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS), data, token.tokenInfo.chainId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(newTransaction::postValue, this::onError);
        }
    }

    public void createTokenTransfer(String to, Token token, ArrayList<Pair<BigInteger, NFTAsset>> transferList)
    {
        //need to determine the spec
        disposable = fetchTransactionsInteract.queryInterfaceSpec(token.tokenInfo)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(spec -> onInterfaceSpec(spec, to, token, transferList), this::onError);
    }

    private void onInterfaceSpec(ContractType spec, String to, Token token, ArrayList<Pair<BigInteger, NFTAsset>> transferList)
    {
        token.setInterfaceSpec(spec);
        createTokenTransfer(to, token, transferList);
    }

    private void onInterfaceSpec(ContractType spec, String to, Token token, List<BigInteger> transferList)
    {
        token.setInterfaceSpec(spec);
        createTokenTransfer(to, token, transferList);
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    public void stopGasSettingsFetch()
    {
        gasService.stopGasPriceCycle();
    }

    public void getAuthorisation(Activity activity, SignAuthenticationCallback callback)
    {
        if (defaultWallet.getValue() != null)
        {
            keyService.getAuthenticationForSignature(defaultWallet.getValue(), activity, callback);
        }
    }

    public void resetSignDialog()
    {
        keyService.resetSigningDialog();
    }

    public void completeAuthentication(Operation signData)
    {
        keyService.completeAuthentication(signData);
    }

    public Single<BigInteger> calculateGasEstimate(Wallet wallet, byte[] transactionBytes, long chainId, String sendAddress, BigDecimal sendAmount)
    {
        return gasService.calculateGasEstimate(transactionBytes, chainId, sendAddress, sendAmount.toBigInteger(), wallet, BigInteger.ZERO);
    }

    public void getAuthentication(Activity activity, Wallet wallet, SignAuthenticationCallback callback)
    {
        keyService.getAuthenticationForSignature(wallet, activity, callback);
    }

    public void failedAuthentication(Operation signData)
    {
        keyService.completeAuthentication(signData);
    }

    public void sendTransaction(Web3Transaction finalTx, Wallet wallet, long chainId)
    {
        disposable = createTransactionInteract
                .createWithSig(wallet, finalTx, chainId)
                .subscribe(transactionFinalised::postValue,
                        transactionError::postValue);
    }

    public byte[] getERC721TransferBytes(String to, String contractAddress, String tokenId, long chainId)
    {
        Token token = tokensService.getToken(chainId, contractAddress);
        List<BigInteger> tokenIds = token.stringHexToBigIntegerList(tokenId);
        return TokenRepository.createERC721TransferFunction(to, token, tokenIds);
    }

    public TokensService getTokenService()
    {
        return tokensService;
    }

    public void openTransferState(Context context, Token token, String ticketIds, DisplayState transferStatus)
    {
        if (transferStatus != DisplayState.NO_ACTION)
        {
            Intent intent = new Intent(context, TransferTicketDetailActivity.class);
            intent.putExtra(C.Key.WALLET, defaultWallet.getValue());
            intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
            intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
            intent.putExtra(C.EXTRA_TOKENID_LIST, ticketIds);
            intent.putExtra(C.EXTRA_STATE, transferStatus.ordinal());
            intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            context.startActivity(intent);
        }
    }
}
