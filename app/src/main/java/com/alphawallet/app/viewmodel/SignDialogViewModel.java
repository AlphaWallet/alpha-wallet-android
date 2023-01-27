package com.alphawallet.app.viewmodel;

import android.app.Activity;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.TransactionRepositoryType;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.hardware.SignatureFromKey;
import com.alphawallet.token.entity.Signable;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by JB on 22/11/2022.
 */
@HiltViewModel
public class SignDialogViewModel extends BaseViewModel
{
    private final PreferenceRepositoryType preferenceRepository;
    private final TransactionRepositoryType transactionRepositoryType;
    private final KeyService keyService;
    private final GenericWalletInteract walletInteract;
    private final MutableLiveData<Boolean> completed = new MutableLiveData<>(false);
    private final MutableLiveData<Pair<Integer, Integer>> message = new MutableLiveData<>();
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();

    @Inject
    public SignDialogViewModel(
            PreferenceRepositoryType preferenceRepository,
            GenericWalletInteract walletInteract,
            TransactionRepositoryType transactionRepositoryType,
            KeyService keyService)
    {
        this.preferenceRepository = preferenceRepository;
        this.transactionRepositoryType = transactionRepositoryType;
        this.keyService = keyService;
        this.walletInteract = walletInteract;

        disposable = walletInteract.find()
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(wallet::postValue, this::onError);
    }

    public LiveData<Boolean> completed()
    {
        return completed;
    }

    public LiveData<Pair<Integer, Integer>> message()
    {
        return message;
    }

    public LiveData<Wallet> onWallet()
    {
        return wallet;
    }

    private void compareToActiveWallet(String signingAddress)
    {
        String activeWallet = preferenceRepository.getCurrentWalletAddress();
        if (!activeWallet.equalsIgnoreCase(signingAddress))
        {
            message.postValue(new Pair<>(R.string.message_wc_wallet_different_from_active_wallet, R.drawable.ic_red_warning));
        }
    }

    public void getAuthentication(Activity activity, SignAuthenticationCallback sCallback)
    {
        keyService.getAuthenticationForSignature(wallet.getValue(), activity, sCallback);
    }

    public void signMessage(Signable message, ActionSheetCallback aCallback)
    {
        disposable = transactionRepositoryType.getSignature(wallet.getValue(), message)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sig -> signComplete(sig, message, aCallback),
                        error -> signFailed(error, message, aCallback));
    }

    private Signable signMessage;

    private void signComplete(SignatureFromKey signature, Signable message, ActionSheetCallback aCallback)
    {
        switch (signature.sigType)
        {
            case SIGNATURE_GENERATED:
                aCallback.signingComplete(signature, message);
                completed.postValue(true);
                break;
            case SIGNING_POSTPONED:
                signMessage = message; //cache message
                break;
            case KEY_FILE_ERROR:
            case KEY_AUTHENTICATION_ERROR:
            case KEY_CIPHER_ERROR:
                signFailed(new Throwable(signature.failMessage), message, aCallback);
                break;
        }
    }

    public void completeSignMessage(SignatureFromKey signature, ActionSheetCallback aCallback)
    {
        //return from hardware card with hardware card's signature
        aCallback.signingComplete(signature, signMessage);
        completed.postValue(true);
    }

    private void signFailed(Throwable error, Signable message, ActionSheetCallback aCallback)
    {
        aCallback.signingFailed(error, message);
        completed.postValue(false);
    }

    public void setSigningWallet(String account)
    {
        disposable = walletInteract.findWallet(account)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(wallet::postValue, this::onError); // TODO: If wallet not found then report error to user rather than trying to sign on default wallet

        compareToActiveWallet(account);
    }
}
