package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Ticker;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.router.SellDetailRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.MarketQueueService;
import com.alphawallet.app.ui.SellDetailActivity;
import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.tools.ParseMagicLink;

import java.math.BigInteger;

/**
 * Created by James on 21/02/2018.
 */

public class SellDetailViewModel extends BaseViewModel {
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Double> ethereumPrice = new MutableLiveData<>();
    private final MutableLiveData<String> universalLinkReady = new MutableLiveData<>();

    private Token token;
    private ParseMagicLink parser;

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final MarketQueueService marketQueueService;
    private final CreateTransactionInteract createTransactionInteract;
    private final SellDetailRouter sellDetailRouter;
    private final KeyService keyService;
    private final AssetDefinitionService assetDefinitionService;

    private byte[] linkMessage;

    SellDetailViewModel(FindDefaultNetworkInteract findDefaultNetworkInteract,
                        GenericWalletInteract genericWalletInteract,
                        MarketQueueService marketQueueService,
                        CreateTransactionInteract createTransactionInteract,
                        SellDetailRouter sellDetailRouter,
                        KeyService keyService,
                        AssetDefinitionService assetDefinitionService) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.marketQueueService = marketQueueService;
        this.createTransactionInteract = createTransactionInteract;
        this.sellDetailRouter = sellDetailRouter;
        this.keyService = keyService;
        this.assetDefinitionService = assetDefinitionService;
    }

    private void initParser()
    {
        if (parser == null)
        {
            parser = new ParseMagicLink(new CryptoFunctions(), EthereumNetworkRepository.extraChains());
        }
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }
    public LiveData<Double> ethereumPrice() { return ethereumPrice; }
    public LiveData<String> universalLinkReady() { return universalLinkReady; }

    public String getSymbol()
    {
        return findDefaultNetworkInteract.getNetworkInfo(token.tokenInfo.chainId).symbol;
    }

    public NetworkInfo getNetwork()
    {
        return findDefaultNetworkInteract.getNetworkInfo(token.tokenInfo.chainId);
    }

    public void prepare(Token token, Wallet wallet) {
        this.token = token;
        this.defaultWallet.setValue(wallet);
        //now get the ticker
        disposable = findDefaultNetworkInteract
                .getTicker(token.tokenInfo.chainId)
                .subscribe(this::onTicker, this::onError);
    }

    private void onTicker(Ticker ticker)
    {
        ethereumPrice.postValue(Double.parseDouble(ticker.price_usd));
    }

    public void generateSalesOrders(String contractAddr, BigInteger price, int[] ticketIndicies, BigInteger firstTicketId)
    {
        marketQueueService.createSalesOrders(defaultWallet.getValue(), price, ticketIndicies, contractAddr, firstTicketId, processMessages, token.tokenInfo.chainId);
    }

    public void generateUniversalLink(int[] ticketSendIndexList, String contractAddress, BigInteger price, long expiry)
    {
        initParser();
        if (ticketSendIndexList == null || ticketSendIndexList.length == 0) return; //TODO: Display error message

        byte[] tradeBytes = parser.getTradeBytes(ticketSendIndexList, contractAddress, price, expiry);
        try {
            linkMessage = ParseMagicLink.generateLeadingLinkBytes(ticketSendIndexList, contractAddress, price, expiry);
        } catch (SalesOrderMalformed e) {
            //TODO: Display appropriate error to user
        }

        //sign this link
        disposable = createTransactionInteract
                .sign(defaultWallet().getValue(), tradeBytes, token.tokenInfo.chainId)
                .subscribe(this::gotSignature, this::onError);
    }

    public void openUniversalLinkSetExpiry(Context context, String selection, double price)
    {
        sellDetailRouter.openUniversalLink(context, token, selection, defaultWallet.getValue(), SellDetailActivity.SET_EXPIRY, price);
    }

    private void gotSignature(byte[] signature)
    {
        initParser();
        String universalLink = parser.completeUniversalLink(token.tokenInfo.chainId, linkMessage, signature);
        //Now open the share icon
        universalLinkReady.postValue(universalLink);
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
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
