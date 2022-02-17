package com.alphawallet.app.ui.widget;

import com.alphawallet.app.entity.AddressBookContact;

public interface OnAddressBookItemCLickListener {
    void OnAddressSelected(int position, AddressBookContact addressBookContact);
}
