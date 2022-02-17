package com.alphawallet.app.repository;

import com.alphawallet.app.entity.AddressBookContact;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.entity.RealmContact;
import com.alphawallet.app.service.RealmManager;
import com.alphawallet.app.service.TokensService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.realm.Realm;
import io.realm.RealmResults;
import timber.log.Timber;

public class ContactRepository implements ContactRepositoryType {

    private RealmManager realmManager;
    private TokensService tokensService;
    private Wallet wallet;

    public ContactRepository(TokensService tokensService, RealmManager realmManager) {
        this.tokensService = tokensService;
        this.realmManager = realmManager;

        wallet = new Wallet(tokensService.getCurrentAddress());
    }

    @Override
    public Single<List<AddressBookContact>> getContacts() {

        return Single.fromCallable(this::getAllContacts);
    }

    @Override
    public Completable addContact(AddressBookContact contact) {
        try (Realm realm = realmManager.getAddressBookRealmInstance())
        {
            realm.beginTransaction();
            RealmContact realmContact = realm.createObject(RealmContact.class, contact.getWalletAddress());
            realmContact.setName(contact.getName());
            realmContact.setEthName(contact.getEthName());
            realm.commitTransaction();
            realm.close();
            return Completable.complete();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return Completable.error(new RuntimeException("Failed to add Contact"));
        }

    }

    @Override
    public Completable updateContact(AddressBookContact oldContact, AddressBookContact newContact) {
        Timber.d("New: %s, old: %s", newContact, oldContact);
        try (Realm realm = realmManager.getAddressBookRealmInstance())
        {
            realm.beginTransaction();
            RealmContact realmContact = realm.where(RealmContact.class)
                    .equalTo("walletAddress", oldContact.getWalletAddress())
                    .findFirst();
            if (realmContact != null) {
//                realmContact.setName(newContact.getName());
                realmContact.setName(newContact.getName());
            }
            realm.commitTransaction();
            realm.close();
            if (realmContact != null)
                return Completable.complete();
            else
                return Completable.error(new RuntimeException("No matching contact found"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return Completable.error(new RuntimeException("Failed to update Contact"));
        }
    }

    @Override
    public Completable removeContact(AddressBookContact contact) {
        try (Realm realm = realmManager.getAddressBookRealmInstance())
        {
            realm.beginTransaction();
            RealmContact realmContact = realm.where(RealmContact.class)
                    .equalTo("walletAddress", contact.getWalletAddress())
                    .findFirst();
            if (realmContact != null) {
                realmContact.deleteFromRealm();
            }
            realm.commitTransaction();
            realm.close();
            return Completable.complete();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return Completable.error(new RuntimeException("Failed to remove Contact"));
        }
    }

    @Override
    public Single<AddressBookContact> searchContact(String walletAddress) {
        return Single.fromCallable(new Callable<AddressBookContact>() {
            @Override
            public AddressBookContact call() throws Exception {
                List<AddressBookContact> contacts = getAllContacts();
                if (contacts != null) {
                    return searchContactFromList(walletAddress, contacts);
                } else  {
                    return null;
                }
            }
        });
    }

    private ArrayList<AddressBookContact> getAllContacts() {
        ArrayList<AddressBookContact> contacts = new ArrayList<>();
        try (Realm realm = realmManager.getAddressBookRealmInstance()) {
            RealmResults<RealmContact> realmContacts = realm.where(RealmContact.class)
                    .findAll();

            if (realmContacts != null) {
                for (RealmContact item : realmContacts) {
                    contacts.add(createUserInfo(item));
                }
            }
            Collections.sort(contacts, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            return contacts;
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    private AddressBookContact searchContactFromList(String walletAddress, List<AddressBookContact> contacts) {
        for (AddressBookContact contact : contacts) {
            if (contact.getWalletAddress().equalsIgnoreCase(walletAddress)) {
                return contact;
            }
        }
        return null;
    }

    private AddressBookContact createUserInfo(RealmContact realmItem)
    {
        return new AddressBookContact(realmItem.getWalletAddress(), realmItem.getName(), realmItem.getEthName());
    }
}
