package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.net.Uri;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ConfirmationType;
import com.alphawallet.app.entity.GasSettings;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.ui.ConfirmationActivity;
import com.alphawallet.app.web3.entity.Web3Transaction;

import com.alphawallet.token.tools.Numeric;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.router.GasSettingsRouter;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TokensService;

import java.math.BigInteger;

public class ConfirmationViewModel extends BaseViewModel {
    private final MutableLiveData<String> newTransaction = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<GasSettings> gasSettings = new MutableLiveData<>();
    private final MutableLiveData<TransactionData> newDappTransaction = new MutableLiveData<>();
    private final MutableLiveData<GasSettings> sendGasSettings = new MutableLiveData<>();

    private final GenericWalletInteract genericWalletInteract;
    private final GasService gasService;
    private final CreateTransactionInteract createTransactionInteract;
    private final TokensService tokensService;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final GasSettingsRouter gasSettingsRouter;
    private final KeyService keyService;

    ConfirmationViewModel(GenericWalletInteract genericWalletInteract,
                          GasService gasService,
                          CreateTransactionInteract createTransactionInteract,
                          GasSettingsRouter gasSettingsRouter,
                          TokensService tokensService,
                          FindDefaultNetworkInteract findDefaultNetworkInteract,
                          KeyService keyService) {
        this.genericWalletInteract = genericWalletInteract;
        this.gasService = gasService;
        this.createTransactionInteract = createTransactionInteract;
        this.gasSettingsRouter = gasSettingsRouter;
        this.tokensService = tokensService;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.keyService = keyService;
    }

    public void createTransaction(Wallet from, String to, BigInteger amount, BigInteger gasPrice, BigInteger gasLimit, int chainId) {
        progress.postValue(true);
        disposable = createTransactionInteract
                .create(from, to, amount, gasPrice, gasLimit, null, chainId)
                .subscribe(this::onCreateTransaction, this::onError);
    }

    public void createTokenTransfer(Wallet from, String to, String contractAddress, BigInteger amount, BigInteger gasPrice, BigInteger gasLimit, int chainId) {
        progress.postValue(true);
        final byte[] data = TokenRepository.createTokenTransferData(to, amount);
        disposable = createTransactionInteract
                .create(from, contractAddress, BigInteger.valueOf(0), gasPrice, gasLimit, data, chainId)
                .subscribe(this::onCreateTransaction, this::onError);
    }

    public void createTicketTransfer(Wallet from, String to, String contractAddress, String ids, BigInteger gasPrice, BigInteger gasLimit, int chainId) {
        progress.postValue(true);
        final byte[] data = getERC875TransferBytes(to, contractAddress, ids, chainId);
        disposable = createTransactionInteract
                .create(from, contractAddress, BigInteger.valueOf(0), gasPrice, gasLimit, data, chainId)
                .subscribe(this::onCreateTransaction, this::onError);
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }

    public MutableLiveData<GasSettings> gasSettings() {
        return gasSettings;
    }

    public MutableLiveData<GasSettings> sendGasSettings() {
        return sendGasSettings;
    }

    public LiveData<String> sendTransaction() {
        return newTransaction;
    }

    public LiveData<TransactionData> sendDappTransaction() {
        return newDappTransaction;
    }

    public void overrideGasSettings(GasSettings settings)
    {
        gasService.setOverrideGasLimit(settings.gasLimit);
        gasService.setOverrideGasPrice(settings.gasPrice);
        gasSettings.postValue(settings);
    }

    public void prepare(ConfirmationActivity ctx)
    {
        gasService.gasPriceUpdateListener().observe(ctx, this::onGasPrice);
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onCreateTransaction(String transaction) {
        progress.postValue(false);
        newTransaction.postValue(transaction);
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
    }

    public void calculateGasSettings(byte[] transaction, boolean isNonFungible, int chainId)
    {
        //start listening for gas if necessary
        gasService.startGasListener(chainId);
        if (gasSettings.getValue() == null)
        {
            GasSettings gasSettings = gasService.getGasSettings(transaction, isNonFungible, chainId);
            onGasSettings(gasSettings);
        }
    }

    public void getGasForSending(ConfirmationType confirmationType, Activity context, int chainId)
    {
        if (gasSettings.getValue() == null)
        {
            GasSettings settings = new GasSettings(gasService.getGasPrice(), gasService.getGasLimit(confirmationType != ConfirmationType.ETH));
            sendGasSettings.postValue(settings);
        }
        else
        {
            sendGasSettings.postValue(gasSettings.getValue());
        }
    }

    private void onGasSettings(GasSettings gasSettings) {
        this.gasSettings.postValue(gasSettings);
    }

    public void openGasSettings(Activity context, int chainId) {
        gasSettingsRouter.open(context, gasSettings.getValue(), chainId);
    }

    /**
     * Only update from the network price if:
     * - user hasn't overriden the default/network settings
     * - network is not xDai (which is priced at 1 GWei).
     * @param currentGasPrice
     */
    private void onGasPrice(BigInteger currentGasPrice)
    {
        GasSettings updateSettings = new GasSettings(gasService.getGasPrice(), gasService.getGasLimit());
        this.gasSettings.postValue(updateSettings);
    }

    public void signWeb3DAppTransaction(Web3Transaction transaction, BigInteger gasPrice, BigInteger gasLimit, int chainId)
    {
        progress.postValue(true);
        BigInteger addr = Numeric.toBigInt(transaction.recipient.toString());

        if (addr.equals(BigInteger.ZERO)) //constructor
        {
            disposable = createTransactionInteract
                    .createWithSig(defaultWallet.getValue(), gasPrice, gasLimit, transaction.payload, chainId)
                    .subscribe(this::onCreateDappTransaction,
                               this::onError);
        }
        else
        {
            byte[] data = Numeric.hexStringToByteArray(transaction.payload);
            disposable = createTransactionInteract
                    .createWithSig(defaultWallet.getValue(), transaction.recipient.toString(), transaction.value, gasPrice, gasLimit, data, chainId)
                    .subscribe(this::onCreateDappTransaction,
                               this::onError);
        }
    }

    public void signTokenScriptTransaction(String transactionData, String contractAddress, BigInteger gasPrice, BigInteger gasLimit, BigInteger value, int chainId)
    {
        progress.postValue(true);
        byte[] data = Numeric.hexStringToByteArray(transactionData);
            disposable = createTransactionInteract
                    .create(defaultWallet.getValue(), contractAddress, value, gasPrice, gasLimit, data, chainId)
                    .subscribe(this::onCreateTransaction,
                               this::onError);
    }

    private void onCreateDappTransaction(TransactionData txData) {
        progress.postValue(false);
        newDappTransaction.postValue(txData);
    }

    public void createERC721Transfer(String to, String contractAddress, String tokenId, BigInteger gasPrice, BigInteger gasLimit, int chainId)
    {
        progress.postValue(true);
        final byte[] data = getERC721TransferBytes(to, contractAddress, tokenId, chainId);
        disposable = createTransactionInteract
                .create(defaultWallet.getValue(), contractAddress, BigInteger.valueOf(0), gasPrice, gasLimit, data, chainId)
                .subscribe(this::onCreateTransaction, this::onError);
    }

    public byte[] getERC721TransferBytes(String to, String contractAddress, String tokenId, int chainId)
    {
        Token token = tokensService.getToken(chainId, contractAddress);
        return TokenRepository.createERC721TransferFunction(to, token, tokenId);
    }

    public byte[] getERC875TransferBytes(String to, String contractAddress, String tokenIds, int chainId)
    {
        Token token = tokensService.getToken(chainId, contractAddress);
        return TokenRepository.createTicketTransferData(to, tokenIds, token);
    }

    public String getNetworkName(int chainId)
    {
        return findDefaultNetworkInteract.getNetworkName(chainId);
    }

    public String getNetworkSymbol(int chainId)
    {
        NetworkInfo networkInfo = findDefaultNetworkInteract.getNetworkInfo(chainId);
        return networkInfo != null ? networkInfo.symbol : C.ETH_SYMBOL;
    }

    public void showMoreDetails(Activity ctx, String toAddress, int chainId)
    {
        Uri etherscanLink = Uri.parse(EthereumNetworkRepository.getEtherscanURLbyNetwork(chainId))
                .buildUpon()
                .appendEncodedPath("address")
                .appendEncodedPath(toAddress)
                .build();

        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, etherscanLink);
        ctx.startActivity(launchBrowser);
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
