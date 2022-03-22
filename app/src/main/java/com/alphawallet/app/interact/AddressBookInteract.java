package com.alphawallet.app.interact;

import com.alphawallet.app.entity.AddressBookContact;
import com.alphawallet.app.repository.ContactRepositoryType;
import com.alphawallet.app.util.ItemCallback;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class AddressBookInteract {

    private ContactRepositoryType contactRepository;

    public AddressBookInteract(ContactRepositoryType contactRepository) {
        this.contactRepository = contactRepository;
    }

    public Single<List<AddressBookContact>> getAllContacts() {
        return contactRepository.getContacts();
    }

    public Completable addContact(AddressBookContact addressBookContact) {
        return contactRepository.addContact(addressBookContact);
    }

    public Completable updateContact(AddressBookContact oldContact, AddressBookContact newContact) {
        return contactRepository.updateContact(oldContact, newContact);
    }

    public Completable removeContact(AddressBookContact addressBookContact) {
        return contactRepository.removeContact(addressBookContact);
    }

    public Single<AddressBookContact> searchContact(String walletAddress) {
        return contactRepository.searchContact(walletAddress);
    }

    /** Search a contact and invoke the callback*/
    public void searchContactAsync(String walletAddress, ItemCallback<AddressBookContact> callback) {
        contactRepository.searchContact(walletAddress)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe( (callback::call), (throwable -> callback.call(null)) )
                .isDisposed();
    }
}
