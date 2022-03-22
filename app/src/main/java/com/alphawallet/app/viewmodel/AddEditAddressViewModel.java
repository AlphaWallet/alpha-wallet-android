package com.alphawallet.app.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.AddressBookContact;
import com.alphawallet.app.interact.AddressBookInteract;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java8.util.Optional;
import timber.log.Timber;

@HiltViewModel
public class AddEditAddressViewModel extends BaseViewModel {
    private static final String TAG = "AddAddressViewModel";

    private final AddressBookInteract addressBookInteract;

    public MutableLiveData<Optional<AddressBookContact>> address = new MutableLiveData<>();
    public MutableLiveData<String> addressError = new MutableLiveData<>();
    public MutableLiveData<Optional<AddressBookContact>> contactName = new MutableLiveData<>();
    public MutableLiveData<String> contactNameError = new MutableLiveData<>();
    public MutableLiveData<Boolean> saveContact = new MutableLiveData<>();
    public MutableLiveData<Boolean> updateContact = new MutableLiveData<>();
    public MutableLiveData<Throwable> error = new MutableLiveData<>();

    private final List<AddressBookContact> contactsList = new ArrayList<>();
    private AddressBookContact addressBookContactToSave = new AddressBookContact("", "", "");

    @Inject
    public AddEditAddressViewModel(AddressBookInteract addressBookInteract) {
        this.addressBookInteract = addressBookInteract;
        loadContacts();
    }

    // load contacts in list
    public void loadContacts() {
        addressBookInteract.getAllContacts()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( (list) -> {
                    Timber.d("loadContacts: list: %s", list);
                    contactsList.clear();
                    contactsList.addAll(list);
                }, (throwable -> Timber.e(throwable, "loadContacts: error: ")))
                .isDisposed();
    }

    public void onClickSave(String walletAddress, String name, String ensName) {
        addressBookContactToSave.setWalletAddress(walletAddress);
        addressBookContactToSave.setEthName(ensName);
        addressBookContactToSave.setName(name);
        checkAddress(walletAddress);
    }

    public void updateContact(AddressBookContact oldContact, AddressBookContact newContact) {
        if (oldContact.getName().equalsIgnoreCase(newContact.getName()))
            return;
        // search by address
        // if found, update the name
        // else something went wrong.
        Timber.d("New name: %s", newContact.getName());
        Optional<AddressBookContact> matchedContactOpt = findByAddress(oldContact.getWalletAddress());
        if (matchedContactOpt.isPresent()) {
            Optional<AddressBookContact> matchedByName = findByName(newContact.getName());
            if (matchedByName.isPresent()) {
                contactName.postValue(matchedByName);
                return;
            }
            addressBookInteract.updateContact(oldContact, newContact)
            .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe( () -> updateContact.postValue(true), (throwable -> error.postValue(new RuntimeException("Failed to update contact"))))
                    .isDisposed();
        } else {
            // error
            error.postValue(new RuntimeException("No match found"));
            updateContact.postValue(false);
        }
    }

    /**
     * @param walletAddress wallet address to match with existing contacts.
     * Finds address and posts result to addressError Live Data
     */
    public void checkAddress(String walletAddress) {
        addressBookContactToSave.setWalletAddress(walletAddress); // required when called from editorAction done
        Single.fromCallable( () -> findByAddress(walletAddress)).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(address::postValue, (throwable -> Timber.e(throwable, "checkAddress: ")))
                .isDisposed();
    }

    public void checkName(String name) {
        addressBookContactToSave.setName(name); // required when called from editorAction done
        Single.fromCallable( () -> findByName(name)).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( (contactName::postValue) , (throwable -> Timber.e(throwable, "checkName: ")))
                .isDisposed();
    }

    private Optional<AddressBookContact> findByAddress(String walletAddress) {
        AddressBookContact matchedContact = null;
        for (AddressBookContact addressBookContact : contactsList) {
            if (addressBookContact.getWalletAddress().equalsIgnoreCase(walletAddress)) {
                matchedContact = addressBookContact;
                break;
            }
        }
        if (matchedContact == null) {
            return Optional.empty();
        } else {
            return Optional.of(matchedContact);
        }
    }

    private Optional<AddressBookContact> findByName(String name) {
        AddressBookContact matchedContact = null;
        for (AddressBookContact addressBookContact : contactsList) {
            if (addressBookContact.getName().equalsIgnoreCase(name)) {
                matchedContact = addressBookContact;
                break;
            }
        }
        if (matchedContact == null) {
            return Optional.empty();
        } else {
            return Optional.of(matchedContact);
        }
    }

    public void addContact() {

        addressBookInteract.addContact(addressBookContactToSave)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( () -> saveContact.postValue(true), (throwable -> saveContact.postValue(false)) )
                .isDisposed();
    }
}
