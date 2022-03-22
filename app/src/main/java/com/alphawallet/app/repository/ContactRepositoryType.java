package com.alphawallet.app.repository;

import com.alphawallet.app.entity.AddressBookContact;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * Created by Asif Ghanchi on 24/11/21
 * Repository for managing Contacts for Address Book.
 */
public interface ContactRepositoryType {

    Single<List<AddressBookContact>> getContacts();
    Completable addContact(AddressBookContact contact);
    Completable updateContact(AddressBookContact oldContact, AddressBookContact newContact);
    Completable removeContact(AddressBookContact contact);
    Single<AddressBookContact> searchContact(String walletAddress);
}
