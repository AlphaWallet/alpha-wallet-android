package io.awallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import org.web3j.abi.datatypes.generated.Bytes32;
import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.SalesOrder;
import io.awallet.crypto.alphawallet.entity.SalesOrderMalformed;
import io.awallet.crypto.alphawallet.entity.Ticker;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.interact.CreateTransactionInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.router.SellDetailRouter;
import io.awallet.crypto.alphawallet.service.MarketQueueService;

import java.math.BigInteger;

import static io.awallet.crypto.alphawallet.ui.SellDetailActivity.SET_EXPIRY;

/**
 * Created by James on 21/02/2018.
 */

public class SellDetailModel extends BaseViewModel {
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Double> ethereumPrice = new MutableLiveData<>();
    private final MutableLiveData<String> universalLinkReady = new MutableLiveData<>();

    private Ticket ticket;

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final MarketQueueService marketQueueService;
    private final CreateTransactionInteract createTransactionInteract;
    private final SellDetailRouter sellDetailRouter;

    private byte[] linkMessage;

    SellDetailModel(FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
                          MarketQueueService marketQueueService,
                    CreateTransactionInteract createTransactionInteract,
                    SellDetailRouter sellDetailRouter) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.marketQueueService = marketQueueService;
        this.createTransactionInteract = createTransactionInteract;
        this.sellDetailRouter = sellDetailRouter;
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
        if (ticketSendIndexList == null || ticketSendIndexList.length == 0) return; //TODO: Display error message

        byte[] tradeBytes = SalesOrder.getTradeBytes(ticketSendIndexList, contractAddress, price, expiry);
        try {
            linkMessage = SalesOrder.generateLeadingLinkBytes(ticketSendIndexList, contractAddress, price, expiry);
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
        try {
            String universalLink = SalesOrder.completeUniversalLink(linkMessage, signature);
            //Now open the share icon
            universalLinkReady.postValue(universalLink);
        }
        catch (SalesOrderMalformed sm)
        {
            //TODO: Display appropriate error to user
            sm.printStackTrace();
        }
    }
}
