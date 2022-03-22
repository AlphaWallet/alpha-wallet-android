package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.AddressBookContact;
import com.alphawallet.app.ui.widget.entity.AddressReadyCallback;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.AddEditAddressViewModel;
import com.alphawallet.app.widget.InputAddress;
import com.alphawallet.app.widget.InputView;

import java.util.HashMap;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;
import java8.util.Optional;
import timber.log.Timber;

@AndroidEntryPoint
public class AddEditAddressActivity extends BaseActivity implements AddressReadyCallback {
    private static final String TAG = "AddAddressActivity";

    AddEditAddressViewModel viewModel;

    private ADDRESS_OPERATION_MODE mode = ADDRESS_OPERATION_MODE.ADD;   // default
    private InputAddress inputViewAddress;
    private InputView inputViewName;
    private Button buttonSave;
    private AddressBookContact contactToEdit = null;
    private Map<String, String> ensResolutionCache = new HashMap<>();  // address -> ensName cache to get ens name of entered wallet address
    private enum ADDRESS_OPERATION_MODE {
        ADD, EDIT
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_address);

        toolbar();

        contactToEdit = (AddressBookContact) getIntent().getParcelableExtra(C.EXTRA_CONTACT);
        if (contactToEdit == null) {
            setTitle(getString(R.string.title_add_address));
        } else {
            mode = ADDRESS_OPERATION_MODE.EDIT;
            setTitle(getString(R.string.title_edit_address));
        }
        initViews();

        viewModel.address.observe(this, this::onAddressCheck);
        viewModel.contactName.observe(this, this::onNameCheck);
        viewModel.saveContact.observe(this, this::onSave);
        viewModel.updateContact.observe(this, this::onUpdateContact);
        viewModel.error.observe(this, this::onError);
    }

    private void initViews() {
        viewModel = new ViewModelProvider(this)
                .get(AddEditAddressViewModel.class);

        inputViewAddress = findViewById(R.id.input_view_address);
        inputViewName = findViewById(R.id.input_view_name);

        inputViewAddress.setAddressCallback(this);
//        inputViewAddress.enableFocusListener();
        inputViewName.enableFocusListener();

        inputViewAddress.getEditText().setImeOptions(EditorInfo.IME_ACTION_NEXT);

        inputViewName.getEditText().setImeOptions(EditorInfo.IME_ACTION_DONE);

        buttonSave = findViewById(R.id.btn_save);
        buttonSave.setOnClickListener( (v) -> {
            String address = inputViewAddress.getInputText();
            String name = inputViewName.getText().toString().trim();
            boolean isDataValid = true;
            if (address.isEmpty()) {
                inputViewAddress.setError(getString(R.string.error_address_empty));
                isDataValid = false;
            } else if (!Utils.isAddressValid(address)) {
                inputViewAddress.setError(getString(R.string.error_invalid_address));
                isDataValid = false;
            }
            if (name.isEmpty()) {
                inputViewName.setError("Name should not be empty");
                isDataValid = false;
            }
            if (isDataValid) {
                if (mode == ADDRESS_OPERATION_MODE.ADD) {
                    String ensName = ensResolutionCache.get(inputViewAddress.getInputText());
                    ensName = ensName == null ? "" : ensName;
                    viewModel.onClickSave(address, name, ensName);
                } else {
                    viewModel.updateContact(contactToEdit, new AddressBookContact(address, name, contactToEdit.getEthName()));
                }
            }
        });

        if (mode == ADDRESS_OPERATION_MODE.EDIT) {
            inputViewAddress.setHandleENS(false);
            inputViewAddress.setAddressCallback(null);
            inputViewAddress.setAddress(contactToEdit.getWalletAddress());
            inputViewName.setText(contactToEdit.getName());
            inputViewAddress.setEditable(false);
            inputViewName.requestFocus();
        } else {
            String walletAddress = getIntent().getStringExtra(C.EXTRA_CONTACT_WALLET_ADDRESS);
            if (walletAddress != null) {
                inputViewAddress.setAddress(walletAddress);
            }
        }
    }

    private void onAddressCheck(Optional<AddressBookContact> userInfo) {
        if (userInfo.isPresent()) {
            // show error
            inputViewAddress.setError(getString(R.string.error_contact_address_exists, userInfo.get().getName()));
        } else {
            viewModel.checkName(inputViewName.getText().toString());
        }
    }

    private void onNameCheck(Optional<AddressBookContact> userInfo) {
        if (userInfo.isPresent()) {
            // show error
            inputViewName.setError(getString(R.string.error_contact_name_taken));
        } else {
            viewModel.addContact();
        }
    }

    private void onSave(Boolean isSaved) {
        if (isSaved) {
            setResult(RESULT_OK);
            finish();
        }

    }

    private void onUpdateContact(boolean success) {
        if (success) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mode == ADDRESS_OPERATION_MODE.EDIT) {
            getMenuInflater().inflate(R.menu.menu_close, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_cancel) {
            super.onBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case C.BARCODE_READER_REQUEST_CODE: {
                if (resultCode == RESULT_OK && data != null) {
                    String qrData = data.getStringExtra(C.EXTRA_QR_CODE);
                    Timber.d("onActivityResult: data: "+data+"\nqr: "+qrData);
                    if (Utils.isAddressValid(qrData)) {
                        inputViewAddress.setAddress(qrData);
                    }
                }
            }
        }
    }

    private void onError(Throwable error) {
        Timber.e(error, "onError: ");
        displayToast(error.getMessage());
    }

    @Override
    public void addressReady(String address, String ensName) {
        Timber.d("Address ready: address: %s, ensName: %s", address, ensName);

    }

    @Override
    public void resolvedAddress(String address, String ensName) {
        AddressReadyCallback.super.resolvedAddress(address, ensName);
        Timber.d("resolvedAddress: %s, %s", address, ensName);
        // if the input address content is unchanged and this resolution is for that content
        if (inputViewAddress.getInputText().equalsIgnoreCase(ensName) || inputViewAddress.getInputText().equalsIgnoreCase(address)) {
            inputViewAddress.setHandleENS(false);   // disable ens resolver
            inputViewAddress.setAddress(address);   // set the resolved address
            inputViewAddress.setHandleENS(true);    // enable ens resolver
        }
        inputViewName.setText(ensName);     // prefill name using ens name
        ensResolutionCache.put(address, ensName);
    }

    @Override
    public void addressValid(boolean valid) {
        AddressReadyCallback.super.addressValid(valid);
        Timber.d("address Valid: %s", valid);
    }

}
