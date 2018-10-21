package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SignatureException;
import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.tools.Numeric;
import io.stormbird.token.tools.ParseMagicLink;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.DAppFunction;
import io.stormbird.wallet.entity.GasSettings;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Ticker;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FetchGasSettingsInteract;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.router.ConfirmationRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.web3.entity.Message;
import io.stormbird.wallet.web3.entity.Web3Transaction;

import static io.stormbird.wallet.C.DAPP_DEFAULT_URL;
import static io.stormbird.wallet.entity.CryptoFunctions.sigFromByteArray;
import static io.stormbird.wallet.ui.ImportTokenActivity.getUsdString;

public class DappBrowserViewModel extends BaseViewModel {
    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<GasSettings> gasSettings = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchGasSettingsInteract fetchGasSettingsInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final ConfirmationRouter confirmationRouter;

    private double ethToUsd = 0;
    private ArrayList<String> bookmarks;
    private GasSettings internalGasSettings = null;

    DappBrowserViewModel(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            AssetDefinitionService assetDefinitionService,
            CreateTransactionInteract createTransactionInteract,
            FetchGasSettingsInteract fetchGasSettingsInteract,
            FetchTokensInteract fetchTokensInteract,
            ConfirmationRouter confirmationRouter) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchGasSettingsInteract = fetchGasSettingsInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.confirmationRouter = confirmationRouter;
    }

    public AssetDefinitionService getAssetDefinitionService() {
        return assetDefinitionService;
    }

    public LiveData<NetworkInfo> defaultNetwork() {
        return defaultNetwork;
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }
    public MutableLiveData<GasSettings> gasSettings() {
        return gasSettings;
    }

    public String getUSDValue(double eth)
    {
        if (defaultNetwork.getValue().chainId == 1)
        {
            return "$" + getUsdString(ethToUsd * eth);
        }
        else
        {
            return "$0.00";
        }
    }

    public String getNetworkName()
    {
        if (defaultNetwork.getValue().chainId == 1)
        {
            return "";
        }
        else
        {
            return defaultNetwork.getValue().name;
        }
    }

    public void prepare(Context context) {
        progress.postValue(true);
        loadBookmarks(context);

        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    private void loadBookmarks(Context context)
    {
        bookmarks = getBrowserBookmarksFromPrefs(context);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
        if (gasSettings.getValue() == null)
        {
            disposable = fetchGasSettingsInteract
                    .fetch(false)
                    .subscribe(this::onGasSettings, this::onError);
        }
    }

    private void onGasSettings(GasSettings gasSettings) {
        this.gasSettings.postValue(gasSettings);
        internalGasSettings = gasSettings;

        disposable = fetchTokensInteract.getEthereumTicker()
                .subscribeOn(Schedulers.io())
                .subscribe(this::onTicker, this::onError);
    }

    private void onTicker(Ticker ticker)
    {
        if (ticker != null && ticker.price_usd != null)
        {
            ethToUsd = Double.valueOf(ticker.price_usd);
        }
    }

    public Observable<Wallet> getWallet() {
        if (defaultWallet().getValue() != null) {
            return Observable.fromCallable(() -> defaultWallet().getValue());
        } else
            return findDefaultNetworkInteract.find()
                    .flatMap(networkInfo -> findDefaultWalletInteract
                            .find()).toObservable();
    }

    public void signMessage(String msg, DAppFunction dAppFunction, Message<String> message) {
        byte[] signRequest = msg.getBytes();
        //if we're passed a hex then sign it correctly
        if (msg.substring(0, 2).equals("0x")) {
            signRequest = Numeric.hexStringToByteArray(msg);
        }

        disposable = createTransactionInteract.sign(defaultWallet.getValue(), signRequest)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sig -> dAppFunction.DAppReturn(sig, message),
                           error -> dAppFunction.DAppError(error, message));
    }

    public void signTransaction(Web3Transaction transaction, DAppFunction dAppFunction, String url)
    {
        Message errorMsg = new Message<>("Error executing transaction", url, 0);

        BigInteger addr = Numeric.toBigInt(transaction.recipient.toString());

        if (addr.equals(BigInteger.ZERO)) //constructor
        {
            disposable = createTransactionInteract
                    .create(defaultWallet.getValue(), transaction.gasPrice, transaction.gasLimit, transaction.payload)
                    .subscribe(hash -> onCreateTransaction(hash, dAppFunction, url),
                               error -> dAppFunction.DAppError(error, errorMsg));

        }
        else
        {
            byte[] data = Numeric.hexStringToByteArray(transaction.payload);
            disposable = createTransactionInteract
                    .create(defaultWallet.getValue(), transaction.recipient.toString(), transaction.value, transaction.gasPrice, transaction.gasLimit, data)
                    .subscribe(hash -> onCreateTransaction(hash, dAppFunction, url),
                               error -> dAppFunction.DAppError(error, errorMsg));
        }
    }

    private void onCreateTransaction(String s, DAppFunction dAppFunction, String url)
    {
        //pushed transaction
        Message<String> msg = new Message<>(s, url, 0);
        dAppFunction.DAppReturn(s.getBytes(), msg);
    }

    public String checkSignature(Message<String> message, String signHex) {
        byte[] messageCheck = message.value.getBytes();
        //if we're passed a hex then sign it correctly
        if (message.value.substring(0, 2).equals("0x")) {
            messageCheck = Numeric.hexStringToByteArray(message.value);
        }

        //convert to signature
        Sign.SignatureData sigData = sigFromByteArray(Numeric.hexStringToByteArray(signHex));
        String recoveredAddress = "";

        try {
            BigInteger recoveredKey = Sign.signedMessageToKey(messageCheck, sigData);
            recoveredAddress = Keys.getAddress(recoveredKey);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }

        return recoveredAddress;
    }

    public String checkSignature(String message, String signHex) {
        byte[] messageCheck = message.getBytes();
        //if we're passed a hex then sign it correctly
        if (message.substring(0, 2).equals("0x")) {
            messageCheck = Numeric.hexStringToByteArray(message);
        }

        //convert to signature
        Sign.SignatureData sigData = sigFromByteArray(Numeric.hexStringToByteArray(signHex));
        String recoveredAddress = "";

        try {
            BigInteger recoveredKey = Sign.signedMessageToKey(messageCheck, sigData);
            recoveredAddress = Keys.getAddress(recoveredKey);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }

        return recoveredAddress;
    }

    public String getVerificationResult(Context context, Wallet wallet, String message, String signHex) {
//        Log.d(TAG, "message: " + message);
//        Log.d(TAG, "signHex: " + signHex);
//        Log.d(TAG, "verification address: " + viewModel.checkSignature(message, signHex));
//        Log.d(TAG, "address: " + wallet.address);
        message = Hash.sha3String(message);  // <--- When you send a string for signing, it already takes the SHA3 of it.
        StringBuilder recoveredAddress = new StringBuilder("0x");
        recoveredAddress.append(checkSignature(message, signHex));

//        Log.d(TAG, "recovered address: " + recoveredAddress.toString());
        String result = wallet.address.equals(recoveredAddress.toString()) ? context.getString(R.string.popup_verification_success) : context.getString(R.string.popup_verification_failed);
        return result;
    }

    public String getRecoveredAddress(String message, String signHex) {
        message = Hash.sha3String(message);
        StringBuilder recoveredAddress = new StringBuilder("0x");
        recoveredAddress.append(checkSignature(message, signHex));
        return recoveredAddress.toString();
    }

    public String getFormattedBalance(String balance) {
        if (balance == null) {
            return "0";
        } else {
            //TODO: Format balance text
            return balance;
        }
    }

    public void openConfirmation(Context context, Web3Transaction transaction, String requesterURL)
    {
        String networkName = defaultNetwork.getValue().name;
        boolean mainNet = defaultNetwork.getValue().isMainNetwork;
        confirmationRouter.open(context, transaction, networkName, mainNet, requesterURL);
    }

    public Web3Transaction doGasSettings(Web3Transaction transaction)
    {
        BigInteger gasLimit = transaction.gasLimit;
        BigInteger gasPrice = transaction.gasPrice;
        if (gasLimit.equals(BigInteger.ZERO))
        {
            gasLimit = internalGasSettings.gasLimit;
        }
        if (gasPrice.equals(BigInteger.ZERO))
        {
            gasPrice = internalGasSettings.gasPrice;
        }

        return new Web3Transaction(transaction.recipient,
                                                    transaction.contract,
                                                    transaction.value,
                                                    gasPrice,
                                                    gasLimit,
                                                    transaction.nonce,
                                                    transaction.payload,
                                                    transaction.leafPosition);
    }

    public ArrayList<String> getBrowserHistoryFromPrefs(Context context) {
        ArrayList<String> history;
        String historyJson = PreferenceManager.getDefaultSharedPreferences(context).getString(C.DAPP_BROWSER_HISTORY, "");
        if (!historyJson.isEmpty()) {
            history = new Gson().fromJson(historyJson, new TypeToken<ArrayList<String>>(){}.getType());
        } else {
            history = new ArrayList<>();
        }
        return history;
    }

    private ArrayList<String> getBrowserBookmarksFromPrefs(Context context) {
        ArrayList<String> storedBookmarks;
        String historyJson = PreferenceManager.getDefaultSharedPreferences(context).getString(C.DAPP_BROWSER_BOOKMARKS, "");
        if (!historyJson.isEmpty()) {
            storedBookmarks = new Gson().fromJson(historyJson, new TypeToken<ArrayList<String>>(){}.getType());
        } else {
            storedBookmarks = new ArrayList<>();
        }
        return storedBookmarks;
    }

    public void addToBrowserHistory(Context context, String url)
    {
        if (url.contains(DAPP_DEFAULT_URL) || context == null) return; // don't record the homepage

        String checkVal = url.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)","");
        ArrayList<String> history = getBrowserHistoryFromPrefs(context);
        for (String item : history)
        {
            if (item.contains(checkVal))
            {
                //replace with this new one
                history.remove(item);
                if (!history.contains(item))
                {
                    history.add(0, url);
                }
                writeURLHistory(context, history);
                return;
            }
        }

        history.add(0, url);
        writeURLHistory(context, history);
    }

    private void writeURLHistory(Context context, ArrayList<String> history)
    {
        String historyJson = new Gson().toJson(history);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(C.DAPP_BROWSER_HISTORY, historyJson).apply();
    }

    private void writeBookmarks(Context context, ArrayList<String> bookmarks)
    {
        String historyJson = new Gson().toJson(bookmarks);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(C.DAPP_BROWSER_BOOKMARKS, historyJson).apply();
    }

    public String getLastUrl(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(C.DAPP_LASTURL_KEY, DAPP_DEFAULT_URL);
    }

    public void setLastUrl(Context context, String url) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(C.DAPP_LASTURL_KEY, url).apply();
    }

    public ArrayList<String> getBookmarks()
    {
        return bookmarks;
    }

    public void addBookmark(Context context, String url)
    {
        //add to list
        bookmarks.add(url);
        //store
        writeBookmarks(context, bookmarks);
    }

    public void removeBookmark(Context context, String url)
    {
        if (bookmarks.contains(url)) bookmarks.remove(url);
        writeBookmarks(context, bookmarks);
    }
}
