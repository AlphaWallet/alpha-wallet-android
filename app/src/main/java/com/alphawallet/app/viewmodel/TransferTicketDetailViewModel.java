package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.ERC721Token;
import com.alphawallet.app.entity.GasSettings;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Ticket;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokenRepository;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.tools.Convert;
import com.alphawallet.token.tools.Numeric;
import com.alphawallet.token.tools.ParseMagicLink;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.ENSInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.router.AssetDisplayRouter;
import com.alphawallet.app.router.ConfirmationRouter;
import com.alphawallet.app.router.TransferTicketDetailRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TokensService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * Created by James on 21/02/2018.
 */
public class TransferTicketDetailViewModel extends BaseViewModel {
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<String> newTransaction = new MutableLiveData<>();
    private final MutableLiveData<String> universalLinkReady = new MutableLiveData<>();
    private final MutableLiveData<String> userTransaction = new MutableLiveData<>();
    private final MutableLiveData<String> ensResolve = new MutableLiveData<>();
    private final MutableLiveData<String> ensFail = new MutableLiveData<>();

    private final GenericWalletInteract genericWalletInteract;
    private final KeyService keyService;
    private final CreateTransactionInteract createTransactionInteract;
    private final TransferTicketDetailRouter transferTicketDetailRouter;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AssetDisplayRouter assetDisplayRouter;
    private final AssetDefinitionService assetDefinitionService;
    private final GasService gasService;
    private final ConfirmationRouter confirmationRouter;
    private final ENSInteract ensInteract;

    private ParseMagicLink parser;
    private Token token;

    private byte[] linkMessage;

    TransferTicketDetailViewModel(GenericWalletInteract genericWalletInteract,
                                  KeyService keyService,
                                  CreateTransactionInteract createTransactionInteract,
                                  TransferTicketDetailRouter transferTicketDetailRouter,
                                  FetchTransactionsInteract fetchTransactionsInteract,
                                  AssetDisplayRouter assetDisplayRouter,
                                  AssetDefinitionService assetDefinitionService,
                                  GasService gasService,
                                  ConfirmationRouter confirmationRouter,
                                  ENSInteract ensInteract) {
        this.genericWalletInteract = genericWalletInteract;
        this.keyService = keyService;
        this.createTransactionInteract = createTransactionInteract;
        this.transferTicketDetailRouter = transferTicketDetailRouter;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.assetDisplayRouter = assetDisplayRouter;
        this.assetDefinitionService = assetDefinitionService;
        this.gasService = gasService;
        this.confirmationRouter = confirmationRouter;
        this.ensInteract = ensInteract;
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }
    public LiveData<String> newTransaction() { return newTransaction; }
    public LiveData<String> universalLinkReady() { return universalLinkReady; }
    public LiveData<String> userTransaction() { return userTransaction; }
    public LiveData<String> ensResolve() { return ensResolve; }
    public LiveData<String> ensFail() { return ensFail; }

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

        gasService.startGasListener(token.tokenInfo.chainId);
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
    }

    public void setWallet(Wallet wallet)
    {
        defaultWallet.setValue(wallet);
    }

    private void onCreateTransaction(String transaction)
    {
        userTransaction.postValue(transaction);
    }

    public void generateUniversalLink(int[] ticketSendIndexList, String contractAddress, long expiry)
    {
        initParser();
        if (ticketSendIndexList == null || ticketSendIndexList.length == 0)
            return; //TODO: Display error message

        //For testing:
        //GenerateSpawnLink(new ArrayList<BigInteger>(), contractAddress, expiry);
        //GenerateDispensoryLink(expiry);

        //NB tradeBytes is the exact bytes the ERC875 contract builds to check the valid order.
        //This is what we must sign.
        byte[] tradeBytes = parser.getTradeBytes(ticketSendIndexList, contractAddress, BigInteger.ZERO, expiry);
        try
        {
            linkMessage = ParseMagicLink.generateLeadingLinkBytes(ticketSendIndexList, contractAddress, BigInteger.ZERO, expiry);
        }
        catch (SalesOrderMalformed e)
        {
            //TODO: Display appropriate error to user
        }

        //sign this link
        disposable = createTransactionInteract
                .sign(defaultWallet().getValue(), tradeBytes, token.tokenInfo.chainId)
                .subscribe(this::gotSignature, this::onError);
    }

    //Generates a test dispenser link.
    // This will only work if the account being used is the same as the dispenser account,
    // or you remove the check 'require(msg.sender == approvedPaymaster);' in the dispensory contract
    private void GenerateDispensoryLink(long expiry)
    {
        String contractAddress = "0x4e4a970a03d0b24877244ac0b233575c201d3f44";
        BigDecimal weiVal = Convert.toWei(new BigDecimal("0.01"), Convert.Unit.ETHER).abs();
        BigInteger szaboAmount = Convert.fromWei(weiVal, Convert.Unit.SZABO).abs().toBigInteger();

        byte[] tradeBytes = parser.getCurrencyBytes(contractAddress, szaboAmount, expiry, 10);
        linkMessage = ParseMagicLink.generateCurrencyLink(tradeBytes);

        System.out.println(Numeric.toHexString(tradeBytes));

        //sign this link
        disposable = createTransactionInteract
                .sign(defaultWallet().getValue(), tradeBytes, token.tokenInfo.chainId)
                .subscribe(this::gotSignature, this::onError);
    }

    //TODO: implement UI for spawnables if this is ever to be done from the app
    private void GenerateSpawnLink(List<BigInteger> tokenIdSpawn, String contractAddress, long expiry)
    {
        BigInteger newToken = new BigInteger("0100", 16); //Simple ID for test spawnable
        tokenIdSpawn.add(newToken);

        byte[] tradeBytes = parser.getSpawnableBytes(tokenIdSpawn, contractAddress, BigInteger.ZERO, expiry);
        try
        {
            linkMessage = ParseMagicLink.generateSpawnableLeadingLinkBytes(tokenIdSpawn, contractAddress, BigInteger.ZERO, expiry);
        }
        catch (SalesOrderMalformed e)
        {
            //TODO: Display appropriate error to user
        }

        //sign this link
        disposable = createTransactionInteract
                .sign(defaultWallet().getValue(), tradeBytes, token.tokenInfo.chainId)
                .subscribe(this::gotSignature, this::onError);
    }

    private void gotSignature(byte[] signature)
    {
        String universalLink = parser.completeUniversalLink(token.tokenInfo.chainId, linkMessage, signature);
        //Now open the share icon
        universalLinkReady.postValue(universalLink);
    }

    public void openTransferState(Context context, Token token, String ticketIds, int transferStatus)
    {
        transferTicketDetailRouter.openTransfer(context, token, ticketIds, defaultWallet.getValue(), transferStatus);
    }

    public void createTicketTransfer(String to, Token token, String indexList)
    {
        if (token.unspecifiedSpec())
        {
            //need to determine the spec
            disposable = fetchTransactionsInteract.queryInterfaceSpec(token.tokenInfo)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(spec -> onInterfaceSpec(spec, to, token, indexList), this::onError);
        }
        else
        {
            final byte[] data = TokenRepository.createTicketTransferData(to, indexList, token);
            GasSettings settings = gasService.getGasSettings(data, true, token.tokenInfo.chainId);
            disposable = createTransactionInteract
                    .create(defaultWallet.getValue(), token.getAddress(), BigInteger.valueOf(0), settings.gasPrice, settings.gasLimit, data, token.tokenInfo.chainId)
                    .subscribe(this::onCreateTransaction, this::onError);
        }
    }

    private void onInterfaceSpec(ContractType spec, String to, Token token, String indexList)
    {
        token.setInterfaceSpec(spec);
        TokensService.setInterfaceSpec(token.tokenInfo.chainId, token.getAddress(), spec);
        createTicketTransfer(to, token, indexList);
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    public void showAssets(Context ctx, Ticket ticket, boolean isClearStack)
    {
        assetDisplayRouter.open(ctx, ticket, isClearStack);
    }

    public void openConfirm(Context ctx, String to, Token token, String tokenId, String ensDetails)
    {
        //first find the asset within the token
        Asset asset = null;
        for (Asset a : ((ERC721Token) token).tokenBalance)
        {
            if (a.getTokenId().equals(tokenId))
            {
                asset = a;
                break;
            }
        }

        if (asset != null)
        {
            confirmationRouter.openERC721Transfer(ctx, to, tokenId, token.getAddress(), token.getFullName(), asset.getName(), ensDetails, token.tokenInfo.chainId);
        }
    }

    public void checkENSAddress(int chainId, String name)
    {
        disposable = ensInteract.checkENSAddress (chainId, name)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ensResolve::postValue, throwable -> ensFail.postValue(""));
    }

    public void stopGasSettingsFetch()
    {
        gasService.stopGasListener();
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
}
