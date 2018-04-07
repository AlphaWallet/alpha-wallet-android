package io.awallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import io.awallet.crypto.alphawallet.entity.GasSettings;
import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.SalesOrder;
import io.awallet.crypto.alphawallet.entity.SalesOrderMalformed;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.entity.TokenInfo;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.interact.CreateTransactionInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.repository.TokenRepository;
import io.awallet.crypto.alphawallet.router.TransferTicketDetailRouter;
import io.awallet.crypto.alphawallet.service.MarketQueueService;
import io.awallet.crypto.alphawallet.ui.TransferTicketDetailActivity;

import org.web3j.crypto.Sign;
import org.web3j.tx.Contract;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static io.awallet.crypto.alphawallet.service.MarketQueueService.sigFromByteArray;

/**
 * Created by James on 21/02/2018.
 */
public class TransferTicketDetailViewModel extends BaseViewModel {
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<GasSettings> gasSettings = new MutableLiveData<>();
    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<String> newTransaction = new MutableLiveData<>();
    private final MutableLiveData<String> universalLinkReady = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final MarketQueueService marketQueueService;
    private final CreateTransactionInteract createTransactionInteract;
    private final TransferTicketDetailRouter transferTicketDetailRouter;

    private byte[] linkMessage;

    TransferTicketDetailViewModel(FindDefaultNetworkInteract findDefaultNetworkInteract,
                                  FindDefaultWalletInteract findDefaultWalletInteract,
                                  MarketQueueService marketQueueService,
                                  CreateTransactionInteract createTransactionInteract,
                                  TransferTicketDetailRouter transferTicketDetailRouter) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.marketQueueService = marketQueueService;
        this.createTransactionInteract = createTransactionInteract;
        this.transferTicketDetailRouter = transferTicketDetailRouter;
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }
    public LiveData<String> newTransaction() { return newTransaction; }
    public LiveData<String> universalLinkReady() { return universalLinkReady; }

    public void prepare(Ticket ticket)
    {
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo)
    {
        defaultNetwork.postValue(networkInfo);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
    }

    public void generateSalesOrders(String contractAddr, BigInteger price, int[] ticketIndicies, int firstTicketId)
    {
        marketQueueService.createSalesOrders(defaultWallet.getValue(), price, ticketIndicies, contractAddr, firstTicketId, processMessages);
    }

    public void setWallet(Wallet wallet)
    {
        defaultWallet.setValue(wallet);
    }

    public void createTicketTransfer(String to, String contractAddress, String indexList, BigInteger gasPrice, BigInteger gasLimit)
    {
        final byte[] data = TokenRepository.createTicketTransferData(to, indexList);
        disposable = createTransactionInteract
                .create(defaultWallet.getValue(), contractAddress, BigInteger.valueOf(0), gasPrice, gasLimit, data)
                .subscribe(this::onCreateTransaction, this::onError);
    }

    private void onCreateTransaction(String transaction)
    {
        newTransaction.postValue(transaction);
    }

    public void generateUniversalLink(int[] ticketSendIndexList, String contractAddress, long expiry)
    {
        if (ticketSendIndexList == null || ticketSendIndexList.length == 0) return; //TODO: Display error message

        //NB tradeBytes is the exact bytes the ERC875 contract builds to check the valid order.
        //This is what we must sign.
        byte[] tradeBytes = SalesOrder.getTradeBytes(ticketSendIndexList, contractAddress, BigInteger.ZERO, expiry);
        try {
            linkMessage = SalesOrder.generateLeadingLinkBytes(ticketSendIndexList, contractAddress, BigInteger.ZERO, expiry);
        } catch (SalesOrderMalformed e) {
            //TODO: Display appropriate error to user
        }

        //sign this link
        disposable = createTransactionInteract
                .sign(defaultWallet().getValue(), tradeBytes)
                .subscribe(this::gotSignature, this::onError);
    }

    private void gotSignature(byte[] signature)
    {
        try
        {
            String universalLink = SalesOrder.completeUniversalLink(linkMessage, signature);
            //Now open the share icon
            universalLinkReady.postValue(universalLink);
        }
        catch (SalesOrderMalformed sm) {
            //TODO: Display appropriate error to user
            sm.printStackTrace();
        }
    }

    public void openTransferState(Context context, Ticket ticket, String ticketIds, int transferStatus)
    {
        transferTicketDetailRouter.openTransfer(context, ticket, ticketIds, defaultWallet.getValue(), transferStatus);
    }
}
