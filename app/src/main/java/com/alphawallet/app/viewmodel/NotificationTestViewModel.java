package com.alphawallet.app.viewmodel;

import static com.alphawallet.app.repository.TokenRepository.callSmartContractFunction;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.NotificationTestService;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;

@HiltViewModel
public class NotificationTestViewModel extends BaseViewModel
{
    private final GenericWalletInteract genericWalletInteract;
    private final PreferenceRepositoryType preferenceRepository;

    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<String> rawJsonString = new MutableLiveData<>();

    private OkHttpClient httpClient;

    @Nullable
    private Disposable disposable;

    @Inject
    NotificationTestViewModel(
        GenericWalletInteract genericWalletInteract,
        PreferenceRepositoryType preferenceRepository)
    {
        this.genericWalletInteract = genericWalletInteract;
        this.preferenceRepository = preferenceRepository;

        prepare();
    }

    public LiveData<Wallet> defaultWallet()
    {
        return defaultWallet;
    }

    public LiveData<String> rawJsonString()
    {
        return rawJsonString;
    }

    public void prepare()
    {
        httpClient = new OkHttpClient.Builder()
            .connectTimeout(C.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .connectTimeout(C.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(C.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

        disposable = genericWalletInteract
            .find()
            .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet)
    {
        defaultWallet.postValue(wallet);
    }

    public void fetchNotifications(String address)
    {
        disposable = Single.fromCallable(() -> NotificationTestService.get(httpClient).fetchNotifications(address))
            .observeOn(Schedulers.io())
            .subscribe(this::onNotifications, this::onError);
    }

    private void onNotifications(String result)
    {
        rawJsonString.postValue(result);
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
