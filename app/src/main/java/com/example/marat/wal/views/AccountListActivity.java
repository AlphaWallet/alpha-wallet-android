package com.example.marat.wal.views;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.marat.wal.R;

import com.example.marat.wal.controller.Controller;
import com.example.marat.wal.model.VMAccount;

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

        mController = Controller.get();

        mRecyclerView = findViewById(R.id.account_list);
        assert mRecyclerView != null;
        setupRecyclerView((RecyclerView) mRecyclerView);

        if (findViewById(R.id.account_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        FloatingActionButton create_fab = (FloatingActionButton) findViewById(R.id.create_fab);
        create_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mController.navigateToCreateAccount(AccountListActivity.this);
            }
        });

        FloatingActionButton import_fab = (FloatingActionButton) findViewById(R.id.import_fab);
        import_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mController.navigateToImportAccount(AccountListActivity.this);
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
        Toast.makeText(AccountListActivity.this, "Delete dialog callback " + password, Toast.LENGTH_SHORT).show();
        try {
            mController.deleteAccount(address, password);
            setupRecyclerView((RecyclerView) mRecyclerView);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            Toast.makeText(AccountListActivity.this, "Error: " + e.toString(), Toast.LENGTH_SHORT).show();
        }
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
            holder.mIdView.setText(mValues.get(position).getAddress());
            holder.mContentView.setText(Controller.WeiToEth(mValues.get(position).getBalance().toString()));

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.setCurrentAddress(holder.mItem.getAddress());
                    AccountListActivity.this.finish();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mIdView;
            public final TextView mContentView;
            public final ImageButton mImageButton;
            public VMAccount mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mIdView = (TextView) view.findViewById(R.id.id);
                mContentView = (TextView) view.findViewById(R.id.content);
                mImageButton = (ImageButton) view.findViewById(R.id.delete_button);

                mImageButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Log.d(TAG, "Delete " + mItem.getAddress());
                        Toast.makeText(AccountListActivity.this, "Delete button pressed", Toast.LENGTH_SHORT).show();

                        DeleteAccountDialogFragment dialog = new DeleteAccountDialogFragment();
                        dialog.setAddress(mItem.getAddress()); // must carry address

                        dialog.show(getSupportFragmentManager(), "DeleteAccountDialogFragment");
                    }
                });

            }

            @Override
            public String toString() {
                return super.toString() + " '" + mContentView.getText() + "'";
            }
        }
    }
}
