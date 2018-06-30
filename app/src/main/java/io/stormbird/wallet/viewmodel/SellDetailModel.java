package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import io.stormbird.wallet.entity.CryptoFunctions;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Ticker;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.router.AssetDisplayRouter;
import io.stormbird.wallet.router.SellDetailRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.MarketQueueService;
import io.stormbird.token.entity.SalesOrderMalformed;
import io.stormbird.token.tools.ParseMagicLink;

import java.math.BigInteger;

import static io.stormbird.wallet.ui.SellDetailActivity.SET_EXPIRY;

/**
 * Created by James on 21/02/2018.
 */

public class SellDetailModel extends BaseViewModel {
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Double> ethereumPrice = new MutableLiveData<>();
    private final MutableLiveData<String> universalLinkReady = new MutableLiveData<>();

    private Ticket ticket;
    private CryptoFunctions cryptoFunctions;
    private ParseMagicLink parser;

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final MarketQueueService marketQueueService;
    private final CreateTransactionInteract createTransactionInteract;
    private final SellDetailRouter sellDetailRouter;
    private final AssetDisplayRouter assetDisplayRouter;
    private final AssetDefinitionService assetDefinitionService;

    private byte[] linkMessage;

    SellDetailModel(FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
                          MarketQueueService marketQueueService,
                    CreateTransactionInteract createTransactionInteract,
                    SellDetailRouter sellDetailRouter,
                    AssetDisplayRouter assetDisplayRouter,
                    AssetDefinitionService assetDefinitionService) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.marketQueueService = marketQueueService;
        this.createTransactionInteract = createTransactionInteract;
        this.sellDetailRouter = sellDetailRouter;
        this.assetDisplayRouter = assetDisplayRouter;
        this.assetDefinitionService = assetDefinitionService;
    }

    private void initParser()
    {
        if (parser == null)
        {
            cryptoFunctions = new CryptoFunctions();
            parser = new ParseMagicLink(cryptoFunctions);
        }
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }
    public LiveData<Double> ethereumPrice() { return ethereumPrice; }
    public LiveData<String> universalLinkReady() { return universalLinkReady; }

    public void prepare(Ticket ticket) {
        this.ticket = ticket;
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet)
    {
        defaultWallet.setValue(wallet);

        //now get the ticker
        disposable = findDefaultNetworkInteract
                .getTicker()
                .subscribe(this::onTicker, this::onError);
    }

    private void onTicker(Ticker ticker)
    {
        ethereumPrice.postValue(Double.parseDouble(ticker.price_usd));
    }

    public void generateSalesOrders(String contractAddr, BigInteger price, int[] ticketIndicies, BigInteger firstTicketId)
    {
        marketQueueService.createSalesOrders(defaultWallet.getValue(), price, ticketIndicies, contractAddr, firstTicketId, processMessages);
    }

    public void setWallet(Wallet wallet)
    {
        defaultWallet.setValue(wallet);
    }

    public void generateUniversalLink(int[] ticketSendIndexList, String contractAddress, BigInteger price, long expiry)
    {
        initParser();
        if (ticketSendIndexList == null || ticketSendIndexList.length == 0) return; //TODO: Display error message

        byte[] tradeBytes = parser.getTradeBytes(ticketSendIndexList, contractAddress, price, expiry);
        try {
            linkMessage = parser.generateLeadingLinkBytes(ticketSendIndexList, contractAddress, price, expiry);
        } catch (SalesOrderMalformed e) {
            //TODO: Display appropriate error to user
        }

        //sign this link
        disposable = createTransactionInteract
                .sign(defaultWallet().getValue(), tradeBytes)
                .subscribe(this::gotSignature, this::onError);
    }

    public void openUniversalLinkSetExpiry(Context context, String selection, double price)
    {
        sellDetailRouter.openUniversalLink(context, ticket, selection, defaultWallet.getValue(), SET_EXPIRY, price);
    }

    private void gotSignature(byte[] signature)
    {
        initParser();
        try {
            String universalLink = parser.completeUniversalLink(linkMessage, signature);
            //Now open the share icon
            universalLinkReady.postValue(universalLink);
        }
        catch (SalesOrderMalformed sm)
        {
            //TODO: Display appropriate error to user
            sm.printStackTrace();
        }
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    public void showAssets(Context ctx, Ticket ticket, boolean isClearStack)
    {
        assetDisplayRouter.open(ctx, ticket, isClearStack);
    }
}
