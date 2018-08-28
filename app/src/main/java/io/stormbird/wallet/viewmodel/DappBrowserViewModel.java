package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.security.SignatureException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.tools.Numeric;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.DAppFunction;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.web3.entity.Message;

import static io.stormbird.wallet.entity.CryptoFunctions.sigFromByteArray;

public class DappBrowserViewModel extends BaseViewModel {
    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final CreateTransactionInteract createTransactionInteract;

    DappBrowserViewModel(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            AssetDefinitionService assetDefinitionService,
            CreateTransactionInteract createTransactionInteract) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.createTransactionInteract = createTransactionInteract;
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

    public void prepare() {
        progress.postValue(true);
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

    public Observable<Wallet> getWallet() {
        if (defaultWallet().getValue() != null) {
            return Observable.fromCallable(() -> defaultWallet().getValue());
        } else
            return findDefaultNetworkInteract.find()
                    .flatMap(networkInfo -> findDefaultWalletInteract
                            .find()).toObservable();
    }

    public void signMessage(String hexSig, DAppFunction dAppFunction, Message<String> message) {
        byte[] signRequest = message.value.getBytes();
        //if we're passed a hex then sign it correctly
        if (message.value.substring(0, 2).equals("0x")) {
            signRequest = Numeric.hexStringToByteArray(message.value);
        }

        disposable = createTransactionInteract.sign(defaultWallet.getValue(), signRequest)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sig -> dAppFunction.DAppReturn(sig, message),
                        error -> dAppFunction.DAppError(error, message));
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
}
