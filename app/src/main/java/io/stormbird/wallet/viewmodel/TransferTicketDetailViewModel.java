package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.entity.SalesOrderMalformed;
import io.stormbird.token.tools.Convert;
import io.stormbird.token.tools.Numeric;
import io.stormbird.token.tools.ParseMagicLink;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.entity.opensea.Asset;
import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.repository.TokenRepository;
import io.stormbird.wallet.router.AssetDisplayRouter;
import io.stormbird.wallet.router.ConfirmationRouter;
import io.stormbird.wallet.router.TransferTicketDetailRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.MarketQueueService;
import io.stormbird.wallet.service.TokensService;

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

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final MarketQueueService marketQueueService;
    private final CreateTransactionInteract createTransactionInteract;
    private final TransferTicketDetailRouter transferTicketDetailRouter;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AssetDisplayRouter assetDisplayRouter;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;
    private final ConfirmationRouter confirmationRouter;
    private final ENSInteract ensInteract;

    private CryptoFunctions cryptoFunctions;
    private ParseMagicLink parser;
    private Token token;

    private byte[] linkMessage;

    TransferTicketDetailViewModel(FindDefaultNetworkInteract findDefaultNetworkInteract,
                                  FindDefaultWalletInteract findDefaultWalletInteract,
                                  MarketQueueService marketQueueService,
                                  CreateTransactionInteract createTransactionInteract,
                                  TransferTicketDetailRouter transferTicketDetailRouter,
                                  FetchTransactionsInteract fetchTransactionsInteract,
                                  AssetDisplayRouter assetDisplayRouter,
                                  AssetDefinitionService assetDefinitionService,
                                  TokensService tokensService,
                                  ConfirmationRouter confirmationRouter,
                                  ENSInteract ensInteract) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.marketQueueService = marketQueueService;
        this.createTransactionInteract = createTransactionInteract;
        this.transferTicketDetailRouter = transferTicketDetailRouter;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.assetDisplayRouter = assetDisplayRouter;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
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
            parser = new ParseMagicLink(new CryptoFunctions());
        }
    }

    public void prepare(Token token)
    {
        this.token = token;
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
    }

    public void generateSalesOrders(String contractAddr, BigInteger price, int[] ticketIndicies, BigInteger firstTicketId)
    {
        marketQueueService.createSalesOrders(defaultWallet.getValue(), price, ticketIndicies, contractAddr, firstTicketId, processMessages, token.tokenInfo.chainId);
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

    public void createTicketTransfer(String to, Token token, String indexList, BigInteger gasPrice, BigInteger gasLimit)
    {
        if (token.unspecifiedSpec())
        {
            //need to determine the spec
            disposable = fetchTransactionsInteract.queryInterfaceSpec(token.tokenInfo)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(spec -> onInterfaceSpec(spec, to, token, indexList, gasPrice, gasLimit), this::onError);
        }
        else
        {
            final byte[] data = TokenRepository.createTicketTransferData(to, indexList, token);
            disposable = createTransactionInteract
                    .create(defaultWallet.getValue(), token.getAddress(), BigInteger.valueOf(0), gasPrice, gasLimit, data, token.tokenInfo.chainId)
                    .subscribe(this::onCreateTransaction, this::onError);
        }
    }

    private void onInterfaceSpec(ContractType spec, String to, Token token, String indexList, BigInteger gasPrice, BigInteger gasLimit)
    {
        token.setInterfaceSpec(spec);
        TokensService.setInterfaceSpec(token.tokenInfo.chainId, token.getAddress(), spec);
        createTicketTransfer(to, token, indexList, gasPrice, gasLimit);
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
}
