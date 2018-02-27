package com.wallet.crypto.alphawallet.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import com.wallet.crypto.alphawallet.entity.GasSettings;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.interact.CreateTransactionInteract;
import com.wallet.crypto.alphawallet.interact.FetchGasSettingsInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.repository.TokenRepository;
import com.wallet.crypto.alphawallet.router.GasSettingsRouter;
import com.wallet.crypto.alphawallet.service.MarketQueueService;

import java.math.BigInteger;
import java.util.List;

public class ConfirmationViewModel extends BaseViewModel {
    private final MutableLiveData<String> newTransaction = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<GasSettings> gasSettings = new MutableLiveData<>();

    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final FetchGasSettingsInteract fetchGasSettingsInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final MarketQueueService marketQueueService;

    private final GasSettingsRouter gasSettingsRouter;

    private boolean confirmationForTokenTransfer = false;

    ConfirmationViewModel(FindDefaultWalletInteract findDefaultWalletInteract,
                                 FetchGasSettingsInteract fetchGasSettingsInteract,
                                 CreateTransactionInteract createTransactionInteract,
                                 GasSettingsRouter gasSettingsRouter,
                                 MarketQueueService marketQueueService) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.fetchGasSettingsInteract = fetchGasSettingsInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.gasSettingsRouter = gasSettingsRouter;
        this.marketQueueService = marketQueueService;
    }

    public void createTransaction(String from, String to, BigInteger amount, BigInteger gasPrice, BigInteger gasLimit) {
        progress.postValue(true);
        disposable = createTransactionInteract
                .create(new Wallet(from), to, amount, gasPrice, gasLimit, null)
                .subscribe(this::onCreateTransaction, this::onError);
    }

    public void createTokenTransfer(String from, String to, String contractAddress, BigInteger amount, BigInteger gasPrice, BigInteger gasLimit) {
        progress.postValue(true);
        final byte[] data = TokenRepository.createTokenTransferData(to, amount);
        disposable = createTransactionInteract
                .create(new Wallet(from), contractAddress, BigInteger.valueOf(0), gasPrice, gasLimit, data)
                .subscribe(this::onCreateTransaction, this::onError);
    }

    public void createTicketTransfer(String from, String to, String contractAddress, String ids, BigInteger gasPrice, BigInteger gasLimit) {
        progress.postValue(true);
        final byte[] data = TokenRepository.createTicketTransferData(to, ids);
        disposable = createTransactionInteract
                .create(new Wallet(from), contractAddress, BigInteger.valueOf(0), gasPrice, gasLimit, data)
                .subscribe(this::onCreateTransaction, this::onError);
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }

    public MutableLiveData<GasSettings> gasSettings() {
        return gasSettings;
    }

    public LiveData<String> sendTransaction() {
        return newTransaction;
    }

    public void prepare(boolean confirmationForTokenTransfer) {
        this.confirmationForTokenTransfer = confirmationForTokenTransfer;
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onCreateTransaction(String transaction) {
        progress.postValue(false);
        newTransaction.postValue(transaction);
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
        if (gasSettings.getValue() == null) {
            disposable = fetchGasSettingsInteract
                    .fetch(confirmationForTokenTransfer)
                    .subscribe(this::onGasSettings, this::onError);
        }
    }

    private void onGasSettings(GasSettings gasSettings) {
        this.gasSettings.postValue(gasSettings);
    }

    public void openGasSettings(Activity context) {
        gasSettingsRouter.open(context, gasSettings.getValue());
    }

    public void generateSalesOrders(String indexSendList, String contractAddr, BigInteger price, String idList) {
        //generate a list of integers
        Ticket t = new Ticket(null, "0", 0);
        List<Integer> sends = t.parseIDListInteger(indexSendList);
        List<Integer> iDs = t.parseIDListInteger(idList);

        if (sends != null && sends.size() > 0)
        {
            short[] ticketIDs = new short[sends.size()];
            int index = 0;

            for (Integer i : sends)
            {
                ticketIDs[index++] = i.shortValue();
            }

            int firstIndex = iDs.get(0);

            price = price.multiply(BigInteger.valueOf(iDs.size()));

            marketQueueService.createSalesOrders(defaultWallet.getValue(), price, ticketIDs, contractAddr, firstIndex);
        }
    }
}
