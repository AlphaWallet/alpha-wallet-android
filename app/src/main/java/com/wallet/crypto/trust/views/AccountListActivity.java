package com.wallet.crypto.trust.views;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.wallet.crypto.trust.R;

import com.wallet.crypto.trust.controller.Controller;
import com.wallet.crypto.trust.model.VMAccount;

import java.util.List;

/**
 * Activity representing a list of accounts
 */
public class AccountListActivity extends AppCompatActivity implements DeleteAccountDialogFragment.DeleteAccountDialogListener {

    private Controller mController;
    private View mRecyclerView;
    private static String TAG = "AccountListActivity";

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mController = Controller.get();

        mRecyclerView = findViewById(R.id.account_list);
        assert mRecyclerView != null;
        setupRecyclerView((RecyclerView) mRecyclerView);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mController.navigateToCreateAccount(AccountListActivity.this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupRecyclerView((RecyclerView) mRecyclerView);
    }

    @Override
    public void onDialogPositiveClick(String address, String password) {
        try {
            mController.deleteAccount(address, password);
            if (mController.getAccounts().size() == 0) {
                finish(); // Don't show account list if there are no accounts,
                          // go to main view which will ask to create a new account
                return;
            }
            setupRecyclerView((RecyclerView) mRecyclerView);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            Toast.makeText(AccountListActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(mController.getAccounts()));
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private List<VMAccount> mValues;

        public SimpleItemRecyclerViewAdapter(List<VMAccount> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.account_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);
            holder.mAddressView.setText(mValues.get(position).getAddress());

            holder.checkItem();
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mAddressView;
            public final RadioButton mRadioButton;
            public final ImageButton mDeleteButton;
            public VMAccount mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mAddressView = (TextView) view.findViewById(R.id.address);

                mRadioButton = view.findViewById(R.id.radio);
                mDeleteButton = (ImageButton) view.findViewById(R.id.delete_button);

                mRadioButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "Selected account " + mItem.getAddress());
                        mController.setCurrentAddress(mItem.getAddress());
                        notifyDataSetChanged();
                        AccountListActivity.this.finish();
                    }
                });

                mDeleteButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Log.d(TAG, "Delete " + mItem.getAddress());

                        DeleteAccountDialogFragment dialog = new DeleteAccountDialogFragment();
                        dialog.setAddress(mItem.getAddress()); // must carry address
                        dialog.show(getSupportFragmentManager(), "DeleteAccountDialogFragment");
                    }
                });
            }

            public void checkItem() {
                boolean selected = false;
                VMAccount currentAccount = mController.getCurrentAccount();
                if (currentAccount != null) {
                    selected = currentAccount.getAddress().equals(mItem.getAddress());
                }
                mRadioButton.setChecked(selected);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mAddressView.getText() + "'";
            }
        }
    }
}
