package io.awallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.util.Log;

import io.awallet.crypto.alphawallet.entity.CryptoFunctions;
import io.awallet.crypto.alphawallet.entity.ErrorEnvelope;
import io.awallet.crypto.alphawallet.entity.GasSettings;
import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.entity.TokenInfo;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.interact.CreateTransactionInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.repository.TokenRepository;
import io.awallet.crypto.alphawallet.router.AssetDisplayRouter;
import io.awallet.crypto.alphawallet.router.TransferTicketDetailRouter;
import io.awallet.crypto.alphawallet.service.FeeMasterService;
import io.awallet.crypto.alphawallet.service.MarketQueueService;
import io.awallet.crypto.alphawallet.ui.TransferTicketDetailActivity;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.entity.SalesOrderMalformed;
import io.stormbird.token.tools.ParseMagicLink;

import java.math.BigInteger;
import java.util.List;

import static io.awallet.crypto.alphawallet.C.ErrorCode.EMPTY_COLLECTION;
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
    private final MutableLiveData<String> userTransaction = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final MarketQueueService marketQueueService;
    private final CreateTransactionInteract createTransactionInteract;
    private final TransferTicketDetailRouter transferTicketDetailRouter;
    private final FeeMasterService feeMasterService;
    private final AssetDisplayRouter assetDisplayRouter;

    private CryptoFunctions cryptoFunctions;
    private ParseMagicLink parser;

    private byte[] linkMessage;

    TransferTicketDetailViewModel(FindDefaultNetworkInteract findDefaultNetworkInteract,
                                  FindDefaultWalletInteract findDefaultWalletInteract,
                                  MarketQueueService marketQueueService,
                                  CreateTransactionInteract createTransactionInteract,
                                  TransferTicketDetailRouter transferTicketDetailRouter,
                                  FeeMasterService feeMasterService,
                                  AssetDisplayRouter assetDisplayRouter) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.marketQueueService = marketQueueService;
        this.createTransactionInteract = createTransactionInteract;
        this.transferTicketDetailRouter = transferTicketDetailRouter;
        this.feeMasterService = feeMasterService;
        this.assetDisplayRouter = assetDisplayRouter;
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }
    public LiveData<String> newTransaction() { return newTransaction; }
    public LiveData<String> universalLinkReady() { return universalLinkReady; }
    public LiveData<String> userTransaction() { return userTransaction; }

    private void initParser()
    {
        if (parser == null)
        {
            cryptoFunctions = new CryptoFunctions();
            parser = new ParseMagicLink(cryptoFunctions);
        }
    }

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

    public void generateSalesOrders(String contractAddr, BigInteger price, int[] ticketIndicies, BigInteger firstTicketId)
    {
        marketQueueService.createSalesOrders(defaultWallet.getValue(), price, ticketIndicies, contractAddr, firstTicketId, processMessages);
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
        if (ticketSendIndexList == null || ticketSendIndexList.length == 0) return; //TODO: Display error message

        //NB tradeBytes is the exact bytes the ERC875 contract builds to check the valid order.
        //This is what we must sign.
        byte[] tradeBytes = parser.getTradeBytes(ticketSendIndexList, contractAddress, BigInteger.ZERO, expiry);
        try {
            linkMessage = parser.generateLeadingLinkBytes(ticketSendIndexList, contractAddress, BigInteger.ZERO, expiry);
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
            String universalLink = parser.completeUniversalLink(linkMessage, signature);
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

    public void createTicketTransfer(String to, String contractAddress, String indexList, BigInteger gasPrice, BigInteger gasLimit)
    {
        final byte[] data = TokenRepository.createTicketTransferData(to, indexList);
        disposable = createTransactionInteract
                .create(defaultWallet.getValue(), contractAddress, BigInteger.valueOf(0), gasPrice, gasLimit, data)
                .subscribe(this::onCreateTransaction, this::onError);
    }

    public void feeMasterCall(String url, String to, Ticket t, String indices)
    {
        disposable = feeMasterService.generateAndSendFeemasterTransaction(url, defaultWallet.getValue(), to, t, 0, indices)
            .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::processResult, this::txError);
    }

    private void txError(Throwable throwable)
    {
        error.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "Network error."));
    }

    private void processResult(Integer result)
    {
        if ((result/100) == 2) newTransaction.postValue("Transaction accepted by server.");
        else
        {
            switch (result)
            {
                case 401:
                    error.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "Signature invalid."));
                    break;
                default:
                    error.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "Transfer failed."));
                    break;
            }
        }
    }

    public void showAssets(Context ctx, Ticket ticket, boolean isClearStack)
    {
        assetDisplayRouter.open(ctx, ticket, isClearStack);
    }
}
