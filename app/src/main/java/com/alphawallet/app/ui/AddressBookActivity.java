package com.alphawallet.app.ui;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.AddressBookContact;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.router.AddEditAddressRouter;
import com.alphawallet.app.router.HomeRouter;
import com.alphawallet.app.ui.widget.adapter.AddressBookAdapter;
import com.alphawallet.app.ui.widget.adapter.TokenListAdapter;
import com.alphawallet.app.ui.widget.decorator.RecyclerViewSwipeDecorator;
import com.alphawallet.app.viewmodel.AddressBookViewModel;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

@AndroidEntryPoint
public class AddressBookActivity extends BaseActivity
{
    private static final String TAG = "AddressBookActivity";

    AddressBookViewModel viewModel;

    private RecyclerView contactList;
    private Button saveButton;
    private TokenListAdapter adapter;
    private EditText search;
    private LinearLayout searchTokens;
    private LinearLayout emptyAddressBook;
    private Button addContactButton;

    private Wallet wallet;
    private String realmId;

    private Handler delayHandler = new Handler(Looper.getMainLooper());

    private final ArrayList<AddressBookContact> contacts = new ArrayList<>();

    /**
     * Flag to check whether activity is started for result or not.
     */
    private Boolean setResult = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_book);
        toolbar();
        setResult = getCallingActivity() != null;
        setTitle(setResult ? getString(R.string.title_select_recipient) : getString(R.string.title_address_book));
        initViews();


    }

    private void initViews()
    {
        viewModel = new ViewModelProvider(this)
                .get(AddressBookViewModel.class);

        viewModel.contactsLD.observe(this, this::updateContactList);
        viewModel.filteredContacts.observe(this, this::onContactsFiltered);

//        wallet = new Wallet(viewModel.getTokensService().getCurrentAddress());
        contactList = findViewById(R.id.contact_list);
        saveButton = findViewById(R.id.btn_apply);
        search = findViewById(R.id.edit_search);
        searchTokens = findViewById(R.id.layout_search_tokens);
        emptyAddressBook = findViewById(R.id.layout_empty_address_book);
        addContactButton = findViewById(R.id.btn_add_contact);

        contactList.setLayoutManager(new LinearLayoutManager(this));

        saveButton.setOnClickListener(v -> {
            new HomeRouter().open(this, true);
        });

        addContactButton.setOnClickListener( (v) -> {
            startAddAddressActivity();
        });

        contactList.requestFocus();
//        search.addTextChangedListener(textWatcher);
        setupSearch();

        AddressBookAdapter adapter = new AddressBookAdapter(contacts, this::OnAddressSelected);
        contactList.setAdapter(adapter);

        // Create and add a callback
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                try
                {
                    if (direction == ItemTouchHelper.RIGHT)
                    {
                        return;
                    }
                    final int position = viewHolder.getAdapterPosition();
                    final AddressBookContact item = adapter.removeItem(position);
                    viewModel.removeContact(item);
                    Snackbar snackbar = Snackbar.make(viewHolder.itemView, "Item " + (direction == ItemTouchHelper.LEFT ? "deleted" : "archived") + ".", Snackbar.LENGTH_LONG);
                    snackbar.setAction(android.R.string.cancel, new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            try
                            {
                                adapter.addItem(item, position);
                                viewModel.addContact(item);
                            }
                            catch(Exception e)
                            {
                                Timber.e(e);
                            }
                        }
                    });
                    snackbar.show();
                }
                catch(Exception e)
                {
                    Timber.e(e);
                }
            }

            // You must use @RecyclerViewSwipeDecorator inside the onChildDraw method
            @Override
            public void onChildDraw (Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive){
                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addSwipeLeftBackgroundColor(ContextCompat.getColor(AddressBookActivity.this, R.color.danger))
                        .addSwipeLeftActionIcon(R.drawable.ic_delete_contact)
                        .addSwipeLeftLabel(getString(R.string.delete))
                        .setSwipeLeftLabelColor(Color.WHITE)
                        .create()
                        .decorate();
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        if (!setResult) itemTouchHelper.attachToRecyclerView(contactList);

    }

    private void updateContactList(List<AddressBookContact> contacts)
    {
        if (contacts.size() == 0) {
            searchTokens.setVisibility(View.INVISIBLE);
            contactList.setVisibility(View.INVISIBLE);
            emptyAddressBook.setVisibility(View.VISIBLE);
        } else {
            searchTokens.setVisibility(View.VISIBLE);
            contactList.setVisibility(View.VISIBLE);
            emptyAddressBook.setVisibility(View.INVISIBLE);
        }
        this.contacts.clear();
        this.contacts.addAll(contacts);
        for (AddressBookContact contact : contacts)
        {
            Timber.d("Contact : " + contact.getWalletAddress() + ", " + contact.getName());
        }
        contactList.getAdapter().notifyDataSetChanged();
    }

    private void OnAddressSelected(int position, AddressBookContact addressBookContact) {
        Timber.d("OnAddressSelected: pos: %s", position);

        if (setResult) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(C.EXTRA_CONTACT, addressBookContact);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            // edit address
            new AddEditAddressRouter().open(this, C.UPDATE_ADDRESS_REQUEST_CODE, addressBookContact);
        }
    }

    private void setupSearch() {
        search.setImeOptions(EditorInfo.IME_ACTION_DONE);
        PublishSubject<String> subject = PublishSubject.create();
        subject.debounce(300, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(viewModel::filterByName, (throwable -> Timber.e(throwable, "searchException")))
                .isDisposed();

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                subject.onNext(s.toString());
            }
        });
    }

    private void onContactsFiltered(List<AddressBookContact> contacts) {
        this.contacts.clear();
        this.contacts.addAll(contacts);
        for (AddressBookContact contact : contacts)
        {
            Timber.d("onContactsFiltered: Contact : " + contact.getWalletAddress() + ", " + contact.getName());
        }
        contactList.getAdapter().notifyDataSetChanged();
    }

//    private void showSnackBar(String text)
//    {
//        Snackbar.make(findViewById(R.id.button_sign_in), text, Snackbar.LENGTH_LONG).show();
//    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadContacts();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!setResult) getMenuInflater().inflate(R.menu.menu_add, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add: {
                startAddAddressActivity();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case C.ADD_ADDRESS_REQUEST_CODE: {
                if (resultCode == RESULT_OK) {
                    // show tick dialog
                    AWalletAlertDialog dialog = new AWalletAlertDialog(this);
                    dialog.setIcon(AWalletAlertDialog.SUCCESS);
                    dialog.setMessage(getString(R.string.msg_address_added_succes));
                    dialog.setTextStyle(AWalletAlertDialog.TEXT_STYLE.CENTERED);
                    dialog.show();
                }
            }
            break;

            case C.UPDATE_ADDRESS_REQUEST_CODE: {
                if (resultCode == RESULT_OK) {
                    // show tick dialog
                    AWalletAlertDialog dialog = new AWalletAlertDialog(this);
                    dialog.setIcon(AWalletAlertDialog.SUCCESS);
                    dialog.setMessage(getString(R.string.msg_address_update_succes));
                    dialog.setTextStyle(AWalletAlertDialog.TEXT_STYLE.CENTERED);
                    dialog.show();
                    viewModel.loadContacts();
                }
            }
            break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startAddAddressActivity() {
        new AddEditAddressRouter().open(this, C.ADD_ADDRESS_REQUEST_CODE);
    }
}
