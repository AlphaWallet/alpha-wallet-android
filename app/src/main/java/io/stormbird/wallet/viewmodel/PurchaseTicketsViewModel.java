package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import io.stormbird.wallet.entity.GasSettings;
import io.stormbird.wallet.entity.MagicLinkParcel;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.service.MarketQueueService;
import io.stormbird.wallet.service.TokensService;

import static io.stormbird.wallet.entity.MagicLinkParcel.generateReverseTradeData;

import org.web3j.tx.Contract;
import org.web3j.utils.Convert;

import java.math.BigInteger;

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
    private final TokensService tokensService;

    PurchaseTicketsViewModel(FindDefaultNetworkInteract findDefaultNetworkInteract,
                             FindDefaultWalletInteract findDefaultWalletInteract,
                             CreateTransactionInteract createTransactionInteract,
                             MarketQueueService marketQueueService,
                             TokensService tokensService) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.marketQueueService = marketQueueService;
        this.tokensService = tokensService;
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

    public void buyRange(MagicLinkParcel marketInstance, int chainId)
    {
        Token token = tokensService.getToken(1, marketInstance.magicLink.contractAddress);
        //ok let's try to drive this guy through
        final byte[] tradeData = generateReverseTradeData(marketInstance.magicLink, token, null);
        //quick sanity check, dump price
        BigInteger milliWei = Convert.fromWei(marketInstance.magicLink.priceWei.toString(), Convert.Unit.FINNEY).toBigInteger();
        double recreatePrice = milliWei.doubleValue() / 1000.0;
        System.out.println("Approx value of trade: " + recreatePrice);
        //now push the transaction
        progress.postValue(true);
        disposable = createTransactionInteract
                .create(new Wallet(defaultWallet().getValue().address), marketInstance.magicLink.contractAddress, marketInstance.magicLink.priceWei,
                        Contract.GAS_PRICE, Contract.GAS_LIMIT, tradeData, chainId)
                .subscribe(this::onCreateTransaction, this::onError);
    }

    private void onCreateTransaction(String transaction)
    {
        progress.postValue(false);
        newTransaction.postValue(transaction);
    }

    //TODO: get the current ETH price, update the fiat price, use CoinmarketcapTicker

}
