package io.awallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import io.awallet.crypto.alphawallet.entity.GasSettings;
import io.awallet.crypto.alphawallet.entity.MagicLinkParcel;
import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.interact.CreateTransactionInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.repository.TokenRepository;
import io.awallet.crypto.alphawallet.service.MarketQueueService;

import static io.awallet.crypto.alphawallet.entity.MagicLinkParcel.generateReverseTradeData;

import org.web3j.tx.Contract;
import org.web3j.utils.Convert;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Created by James on 23/02/2018.
 */
public class PurchaseTicketsViewModel extends BaseViewModel
{
    private final MutableLiveData<String> newTransaction = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<GasSettings> gasSettings = new MutableLiveData<>();
    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final MarketQueueService marketQueueService;

    PurchaseTicketsViewModel(FindDefaultNetworkInteract findDefaultNetworkInteract,
                             FindDefaultWalletInteract findDefaultWalletInteract,
                             CreateTransactionInteract createTransactionInteract,
                             MarketQueueService marketQueueService) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.marketQueueService = marketQueueService;
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }

    public LiveData<String> sendTransaction() {
        return newTransaction;
    }

    public void prepare() {
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

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
    }

    public void setWallet(Wallet wallet)
    {
        defaultWallet.setValue(wallet);
    }

    public void buyRange(MagicLinkParcel marketInstance)
    {
        //ok let's try to drive this guy through
        final byte[] tradeData = generateReverseTradeData(marketInstance.magicLink);
        //quick sanity check, dump price
        BigInteger milliWei = Convert.fromWei(marketInstance.magicLink.priceWei.toString(), Convert.Unit.FINNEY).toBigInteger();
        double recreatePrice = milliWei.doubleValue() / 1000.0;
        System.out.println("Approx value of trade: " + recreatePrice);
        //now push the transaction
        progress.postValue(true);
        disposable = createTransactionInteract
                .create(new Wallet(defaultWallet().getValue().address), marketInstance.magicLink.contractAddress, marketInstance.magicLink.priceWei,
                        Contract.GAS_PRICE, Contract.GAS_LIMIT, tradeData)
                .subscribe(this::onCreateTransaction, this::onError);
    }

    private void onCreateTransaction(String transaction)
    {
        progress.postValue(false);
        newTransaction.postValue(transaction);
    }

    //TODO: get the current ETH price, update the fiat price, use CoinmarketcapTicker

}
