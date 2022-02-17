package com.alphawallet.app.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.AddressBookContact;
import com.alphawallet.app.interact.AddressBookInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.service.RealmManager;
import com.alphawallet.app.service.TokensService;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@HiltViewModel
public class AddressBookViewModel extends BaseViewModel {
    private static final String TAG = "AddressBookViewModel";

    private final RealmManager realmManager;
    private final TokensService tokensService;
    private final AddressBookInteract addressBookInteract;
    private final GenericWalletInteract genericWalletInteract;

    public MutableLiveData<List<AddressBookContact>> contactsLD = new MutableLiveData<>();
    public MutableLiveData<List<AddressBookContact>> filteredContacts = new MutableLiveData<>();
    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();

    private List<AddressBookContact> contactsList;

    @Inject
    AddressBookViewModel(TokensService tokensService,
                         RealmManager realmManager,
                         AddressBookInteract addressBookInteract,
                         GenericWalletInteract genericWalletInteract
                         ) {
        this.tokensService = tokensService;
        this.realmManager = realmManager;
        this.addressBookInteract = addressBookInteract;
        this.genericWalletInteract = genericWalletInteract;
        loadContacts();
    }

    public void loadContacts() {
        addressBookInteract.getAllContacts()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::postContacts, this::onError)
                .isDisposed();
    }

    public void addContact(AddressBookContact addressBookContact) {
        addressBookInteract.addContact(addressBookContact)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onComplete, this::onError)
                .isDisposed();
    }

    public void removeContact(AddressBookContact addressBookContact) {
        addressBookInteract.removeContact(addressBookContact)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onComplete, this::onError)
                .isDisposed();
    }

    private void onComplete() {
        loadContacts();
    }

    private void postContacts(List<AddressBookContact> contacts) {
        this.contactsList = contacts;
        contactsLD.postValue(contacts);
    }

    @Override
    protected void onError(Throwable throwable) {
        super.onError(throwable);
        Timber.d("onError: %s", throwable.getMessage());
    }

    public void filterByName(String nameText) {
        ArrayList<AddressBookContact> filtered = new ArrayList<>();
        for (AddressBookContact u : contactsList) {
            if (u.getName().toLowerCase().startsWith(nameText.toLowerCase())) {
                filtered.add(u);
            }
        }
        // post
        filteredContacts.postValue(filtered);
    }
}
