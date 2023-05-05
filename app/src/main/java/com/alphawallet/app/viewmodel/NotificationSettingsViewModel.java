package com.alphawallet.app.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.AlphaWalletNotificationService;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@HiltViewModel
public class NotificationSettingsViewModel extends BaseViewModel
{
    private final GenericWalletInteract genericWalletInteract;
    private final AlphaWalletNotificationService alphaWalletNotificationService;
    private final PreferenceRepositoryType preferenceRepository;

    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    @Nullable
    private Disposable findWalletDisposable;
    @Nullable
    private Disposable disposable;

    @Inject
    NotificationSettingsViewModel(
        GenericWalletInteract genericWalletInteract,
        AlphaWalletNotificationService alphaWalletNotificationService,
        PreferenceRepositoryType preferenceRepository)
    {
        this.genericWalletInteract = genericWalletInteract;
        this.alphaWalletNotificationService = alphaWalletNotificationService;
        this.preferenceRepository = preferenceRepository;

        prepare();
    }

    public LiveData<Wallet> wallet()
    {
        return wallet;
    }

    private void prepare()
    {
        findWalletDisposable = genericWalletInteract
            .find()
            .subscribe(wallet::setValue, this::onError);
    }

    public void subscribe(long chainId)
    {
        disposable = alphaWalletNotificationService.subscribe(chainId)
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .subscribe(result -> Timber.d("subscribe result => " + result), Timber::e);
    }

    public void unsubscribe(long chainId)
    {
        disposable = alphaWalletNotificationService.unsubscribe(chainId)
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .subscribe(result -> Timber.d("unsubscribe result => " + result), Timber::e);
    }

    // TODO: Delete when unsubscribe is implemented
    public void unsubscribeToTopic(long chainId)
    {
        alphaWalletNotificationService.unsubscribeToTopic(chainId);
    }

    public boolean getToggleState(String address)
    {
        return preferenceRepository.isTransactionNotificationsEnabled(address);
    }

    public void setToggleState(String address, boolean state)
    {
        preferenceRepository.setTransactionNotificationEnabled(address, state);
    }

    @Override
    protected void onCleared()
    {
        super.onCleared();
        if (disposable != null)
        {
            disposable.dispose();
        }
    }
}
