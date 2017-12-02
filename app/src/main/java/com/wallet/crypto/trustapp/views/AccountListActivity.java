package com.wallet.crypto.trustapp.views;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.controller.Controller;
import com.wallet.crypto.trustapp.model.VMAccount;

import java.util.List;

/**
 * Activity representing a list of accounts
 */
public class AccountListActivity extends AppCompatActivity {

    private Controller mController;
    private View mRecyclerView;
    private static String TAG = "AccountsManageActivity";

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

        mController = Controller.with(this);

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

                        new AlertDialog.Builder(AccountListActivity.this)
                                .setTitle(getString(R.string.title_delete_account))
                                .setMessage(getString(R.string.confirm_delete_account))
                                .setIcon(R.drawable.ic_warning_black_24dp)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        try {
                                            mController.deleteAccount(mAddressView.getText().toString());
                                            if (mController.getAccounts().size() == 0) {
                                                finish(); // Don't show account list if there are no accounts,
                                                          // go to main view which will ask to create a new account
                                                return;
                                            }
                                            setupRecyclerView((RecyclerView) mRecyclerView);
                                        } catch (Exception e) {
                                            Toast.makeText(AccountListActivity.this,
                                                getString(R.string.error_deleting_account) + " " + e.getLocalizedMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }})
                                .setNegativeButton(android.R.string.no, null).show();
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
