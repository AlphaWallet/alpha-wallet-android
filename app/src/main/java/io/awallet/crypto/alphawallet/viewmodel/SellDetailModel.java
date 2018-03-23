package io.awallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import org.web3j.crypto.Sign;

import io.awallet.crypto.alphawallet.entity.GasSettings;
import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.SalesOrder;
import io.awallet.crypto.alphawallet.entity.Ticker;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.interact.CreateTransactionInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.service.MarketQueueService;

import java.math.BigInteger;

import static io.awallet.crypto.alphawallet.service.MarketQueueService.sigFromByteArray;

/**
 * Created by James on 21/02/2018.
 */

public class SellDetailModel extends BaseViewModel {
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<GasSettings> gasSettings = new MutableLiveData<>();
    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Double> ethereumPrice = new MutableLiveData<>();
    private final MutableLiveData<String> universalLinkReady = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final MarketQueueService marketQueueService;
    private final CreateTransactionInteract createTransactionInteract;

    private byte[] linkMessage;

    SellDetailModel(FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
                          MarketQueueService marketQueueService,
                    CreateTransactionInteract createTransactionInteract) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.marketQueueService = marketQueueService;
        this.createTransactionInteract = createTransactionInteract;
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }
    public LiveData<Double> ethereumPrice() { return ethereumPrice; }
    public LiveData<String> universalLinkReady() { return universalLinkReady; }

    public void prepare(Ticket ticket) {
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

    public void generateSalesOrders(String contractAddr, BigInteger price, int[] ticketIndicies, int firstTicketId)
    {
        marketQueueService.createSalesOrders(defaultWallet.getValue(), price, ticketIndicies, contractAddr, firstTicketId, processMessages);
    }

    public void setWallet(Wallet wallet)
    {
        defaultWallet.setValue(wallet);
    }

    public void generateUniversalLink(int[] ticketSendIndexList, String contractAddress, BigInteger price)
    {
        if (ticketSendIndexList == null || ticketSendIndexList.length == 0) return; //TODO: Display error message

        linkMessage = SalesOrder.getTradeBytes(ticketSendIndexList, contractAddress, price, 0);

        //sign this link
        disposable = createTransactionInteract
                .sign(defaultWallet().getValue(), linkMessage)
                .subscribe(this::gotSignature, this::onError);
    }

    private void gotSignature(byte[] signature)
    {
        //convert signature to internal representation
        Sign.SignatureData linkSignature = sigFromByteArray(signature);
        String universalLink = SalesOrder.completeUniversalLink(linkMessage, linkSignature);

        //Now open the share icon
        universalLinkReady.postValue(universalLink);
    }
}
