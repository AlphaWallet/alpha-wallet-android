package com.alphawallet.app.viewmodel;

import androidx.annotation.Nullable;

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
    private final AlphaWalletNotificationService alphaWalletNotificationService;
    private final PreferenceRepositoryType preferenceRepository;
    @Nullable
    private Disposable disposable;

    @Inject
    NotificationSettingsViewModel(
        AlphaWalletNotificationService alphaWalletNotificationService,
        PreferenceRepositoryType preferenceRepository)
    {
        this.alphaWalletNotificationService = alphaWalletNotificationService;
        this.preferenceRepository = preferenceRepository;
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

    public boolean getToggleState()
    {
        return preferenceRepository.getNotificationsState();
    }

    public void setToggleState(boolean state)
    {
        preferenceRepository.setNotificationState(state);
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
