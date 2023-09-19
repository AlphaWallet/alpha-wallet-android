package com.alphawallet.app.viewmodel;


import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.content.Context;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.WalletItem;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.util.ens.AWEnsResolver;
import com.alphawallet.app.util.ens.EnsResolver;

import javax.annotation.Nullable;
import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

@HiltViewModel
public class NameThisWalletViewModel extends BaseViewModel
{
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<String> ensName = new MutableLiveData<>();
    private final GenericWalletInteract genericWalletInteract;

    private final AWEnsResolver ensResolver;

    @Nullable
    Disposable ensResolveDisposable;

    @Inject
    NameThisWalletViewModel(
            GenericWalletInteract genericWalletInteract,
            @ApplicationContext Context context,
            AnalyticsServiceType analyticsService)
    {
        this.genericWalletInteract = genericWalletInteract;
        this.ensResolver = new AWEnsResolver(TokenRepository.getWeb3jService(MAINNET_ID), context);
        setAnalyticsService(analyticsService);
    }

    @Override
    protected void onCleared()
    {
        super.onCleared();
        if (ensResolveDisposable != null && !ensResolveDisposable.isDisposed())
            ensResolveDisposable.dispose();
    }

    public LiveData<Wallet> defaultWallet()
    {
        return defaultWallet;
    }

    public LiveData<String> ensName()
    {
        return ensName;
    }

    public void prepare()
    {
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet)
    {
        defaultWallet.setValue(wallet);

        // skip resolve if wallet already has an ENSName
        if (!TextUtils.isEmpty(wallet.ENSname))
        {
            onENSSuccess(wallet.ENSname);
            return;
        }

        ensResolveDisposable = ensResolver.reverseResolveEns(wallet.address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onENSSuccess);

    }

    private void onENSSuccess(String address)
    {
        ensName.setValue(address);
    }

    public void setWalletName(String name, Realm.Transaction.OnSuccess onSuccess)
    {
        Wallet wallet = defaultWallet().getValue();
        wallet.name = name;
        genericWalletInteract.updateWalletItem(wallet, WalletItem.NAME, onSuccess);
    }

    public boolean checkEnsName(String newName, Realm.Transaction.OnSuccess onSuccess)
    {
        if (!TextUtils.isEmpty(newName) && EnsResolver.isValidEnsName(newName))
        {
            //does this new name correspond to ENS?
            ensResolver.resolveENSAddress(newName)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(addr -> checkAddress(addr, newName, onSuccess))
                    .isDisposed();

            return true;
        }
        else
        {
            return false;
        }
    }

    //check if this is a valid ENS name, if so then replace the ENS name
    private void checkAddress(String address, String ensName, Realm.Transaction.OnSuccess onSuccess)
    {
        if (defaultWallet.getValue() != null && !TextUtils.isEmpty(address) && address.equalsIgnoreCase(defaultWallet.getValue().address))
        {
            Wallet wallet = defaultWallet().getValue();
            wallet.ENSname = ensName;
            genericWalletInteract.updateWalletItem(wallet, WalletItem.ENS_NAME, onSuccess);
        }
        else
        {
            onSuccess.onSuccess();
        }
    }
}

