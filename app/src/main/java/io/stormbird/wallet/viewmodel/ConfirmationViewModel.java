package io.stormbird.wallet.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import java.math.BigInteger;
import java.util.List;

import io.stormbird.token.tools.Numeric;
import io.stormbird.wallet.entity.GasSettings;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FetchGasSettingsInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.repository.TokenRepository;
import io.stormbird.wallet.router.GasSettingsRouter;
import io.stormbird.wallet.service.MarketQueueService;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.web3.entity.Web3Transaction;

public class ConfirmationViewModel extends BaseViewModel {
    private final MutableLiveData<String> newTransaction = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<GasSettings> gasSettings = new MutableLiveData<>();

    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final FetchGasSettingsInteract fetchGasSettingsInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final MarketQueueService marketQueueService;
    private final TokensService tokensService;

    private final GasSettingsRouter gasSettingsRouter;

    private byte[] transactionBytes = null;

    ConfirmationViewModel(FindDefaultWalletInteract findDefaultWalletInteract,
                                 FetchGasSettingsInteract fetchGasSettingsInteract,
                                 CreateTransactionInteract createTransactionInteract,
                                 GasSettingsRouter gasSettingsRouter,
                                 MarketQueueService marketQueueService,
                                 TokensService tokensService) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.fetchGasSettingsInteract = fetchGasSettingsInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.gasSettingsRouter = gasSettingsRouter;
        this.marketQueueService = marketQueueService;
        this.tokensService = tokensService;
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
        final byte[] data = getERC875TransferBytes(to, contractAddress, ids);
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

    public void prepare(byte[] transactionBytes) {
        this.transactionBytes = transactionBytes;
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
        if (gasSettings.getValue() == null)
        {
            disposable = fetchGasSettingsInteract
                        .fetch(transactionBytes)
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
        Ticket t = new Ticket(null, "0", "0", 0);
        List<Integer> sends = t.stringIntsToIntegerList(indexSendList);
        List<Integer> iDs = t.stringIntsToIntegerList(idList);

        if (sends != null && sends.size() > 0)
        {
            int[] ticketIDs = new int[sends.size()];
            int index = 0;

            for (Integer i : sends)
            {
                ticketIDs[index++] = i;
            }

            int firstIndex = iDs.get(0);

            price = price.multiply(BigInteger.valueOf(iDs.size()));

            //marketQueueService.createSalesOrders(defaultWallet.getValue(), price, ticketIDs, contractAddr, firstIndex);
        }
    }

    public void signWeb3DAppTransaction(Web3Transaction transaction, BigInteger gasPrice, BigInteger gasLimit)
    {
        progress.postValue(true);
        BigInteger addr = Numeric.toBigInt(transaction.recipient.toString());

        if (addr.equals(BigInteger.ZERO)) //constructor
        {
            disposable = createTransactionInteract
                    .create(defaultWallet.getValue(), gasPrice, gasLimit, transaction.payload)
                    .subscribe(this::onCreateTransaction,
                               this::onError);
        }
        else
        {
            byte[] data = Numeric.hexStringToByteArray(transaction.payload);
            disposable = createTransactionInteract
                    .create(defaultWallet.getValue(), transaction.recipient.toString(), transaction.value, gasPrice, gasLimit, data)
                    .subscribe(this::onCreateTransaction,
                               this::onError);
        }
    }

    public void createERC721Transfer(String to, String contractAddress, String tokenId, BigInteger gasPrice, BigInteger gasLimit)
    {
        progress.postValue(true);
        final byte[] data = getERC721TransferBytes(to, contractAddress, tokenId);
        disposable = createTransactionInteract
                .create(defaultWallet.getValue(), contractAddress, BigInteger.valueOf(0), gasPrice, gasLimit, data)
                .subscribe(this::onCreateTransaction, this::onError);
    }

    public byte[] getERC721TransferBytes(String to, String contractAddress, String tokenId)
    {
        Token token = tokensService.getToken(contractAddress);
        return TokenRepository.createERC721TransferFunction(to, token, tokenId);
    }

    public byte[] getERC875TransferBytes(String to, String contractAddress, String tokenIds)
    {
        Token token = tokensService.getToken(contractAddress);
        return TokenRepository.createTicketTransferData(to, tokenIds, token);
    }
}
