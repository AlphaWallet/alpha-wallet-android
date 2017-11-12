package com.wallet.crypto.trustapp.views;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.controller.Controller;
import com.wallet.crypto.trustapp.controller.OnTaskCompleted;
import com.wallet.crypto.trustapp.controller.TaskResult;
import com.wallet.crypto.trustapp.model.ESTransaction;
import com.wallet.crypto.trustapp.model.VMAccount;

import java.util.List;

public class TransactionListActivity extends AppCompatActivity {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private static String TAG = "ITEM_LIST_ACTIVITY";
    private String mAddress = "";
    private Controller mController;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_receive:
                    mController.navigateToReceive(TransactionListActivity.this);
                    break;
                case R.id.navigation_send:
                    mController.navigateToSend(TransactionListActivity.this);
                    break;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_list);

        mController = Controller.with(getApplicationContext());
        mController.init(getApplicationContext(), this);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mRecyclerView = findViewById(R.id.item_list);
        assert mRecyclerView != null;
        setupRecyclerView(mRecyclerView);

        mSwipeRefreshLayout = findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener(){
                    @Override
                    public void onRefresh() {
                        Log.d(TAG, "onRefresh has been called.");
                        fetchModelsAndReinit();
                    }
                }
        );
        mSwipeRefreshLayout.setRefreshing(true);

        fetchModelsAndReinit();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        mController.onStop();
    }

    public void fetchModelsAndReinit() {
        mController.loadViewModels(new OnTaskCompleted() {
            @Override
            public void onTaskCompleted(TaskResult result) {
                asyncInit();
            }
        });
    }

    protected void init() {
        Log.d(TAG, "init");
        VMAccount account = mController.getCurrentAccount();
        if (account != null) {
            mAddress = account.getAddress();
            Log.d(TAG, "Address: %s, Balance: %s".format(mAddress, account.getBalance().toString()));

            try {
                String balance = Controller.WeiToEth(account.getBalance().toString(), 5);
                Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
                setSupportActionBar(toolbar);
                getSupportActionBar().setTitle(balance + " ETH");
                getSupportActionBar().setSubtitle(mAddress);
                toolbar.inflateMenu(R.menu.transaction_list_menu);
            } catch (Exception e) {
                Log.e(TAG, "Error updating balance: ", e);
            }
        }

        refreshTransactions(mAddress);
    }

    private void asyncInit() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                init();
            }
        });
    }

    private void refreshTransactions(String address) {
        if (mController.getTransactions(address).size() == 0) {
            findViewById(R.id.no_transactions_text).setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            findViewById(R.id.no_transactions_text).setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
            setupRecyclerView(mRecyclerView);
        }
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");

        super.onResume();

        init();
        Log.d(TAG, "Number of accounts: " + mController.getNumberOfAccounts());

        if (mController.getAccounts().size() == 0) {
            Intent intent = new Intent(getApplicationContext(), CreateAccountActivity.class);
            this.startActivityForResult(intent, Controller.IMPORT_ACCOUNT_REQUEST);
            finish();
        } else {
            mController.onResume();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Controller.IMPORT_ACCOUNT_REQUEST) {
            if (resultCode == RESULT_OK) {
                this.finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.transaction_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_select_account:
                mController.navigateToAccountList(this);
                break;
            case R.id.action_settings:
                mController.navigateToSettings(this);
                break;
            case R.id.navigation_send:
                mController.navigateToSend(TransactionListActivity.this);
                break;
            case R.id.navigation_receive:
                mController.navigateToSend(TransactionListActivity.this);
                break;
        }
        return true;
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        Controller controller = Controller.with(getApplicationContext());
        List<ESTransaction> txns = controller.getTransactions(mAddress);
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(txns));
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final List<ESTransaction> mValues;

        public SimpleItemRecyclerViewAdapter(List<ESTransaction> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.transaction_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);

            holder.mDateView.setText(Controller.GetDate(Long.decode(holder.mItem.getTimeStamp())));

            boolean isSent = holder.mItem.getFrom().toLowerCase().equals(mAddress.toLowerCase());
            String wei = holder.mItem.getValue();

            // TODO deduplicate with TransactionDetailFragment.java
            String sign = "+";

            if (isSent) {
                holder.mSentOrReceived.setText(getString(R.string.sent));
                holder.mValueView.setTextColor(getResources().getColor(R.color.red));
                holder.mAddressView.setText(holder.mItem.getTo());
                sign = "-";
            } else {
                holder.mSentOrReceived.setText(getString(R.string.received));
                holder.mAddressView.setText(holder.mItem.getFrom());
                sign = "+";
                holder.mValueView.setTextColor(getResources().getColor(R.color.green));
            }

            String eth = Controller.WeiToEth(wei);

            if (holder.mItem.getValue().equals("0")) {
                holder.mValueView.setText(eth);
            } else {
                holder.mValueView.setText(sign + eth);
            }

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context context = v.getContext();
                    Intent intent = new Intent(context, TransactionDetailActivity.class);
                    intent.putExtra(TransactionDetailFragment.ARG_TXN_HASH, holder.mItem.getHash());
                    intent.putExtra(TransactionDetailFragment.ARG_ADDRESS, mAddress);

                    context.startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mSentOrReceived;
            public final TextView mAddressView;
            public final TextView mDateView;
            //public final TextView mBalanceView;
            public final TextView mValueView;

            public ESTransaction mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mSentOrReceived = (TextView) view.findViewById(R.id.sent_or_received);
                mAddressView = (TextView) view.findViewById(R.id.transaction_address);
                mDateView = (TextView) view.findViewById(R.id.date);
                mValueView = (TextView) view.findViewById(R.id.value);
            }

            @Override
            public String toString() {
                return super.toString(); //+ " '" + mBalanceView.getText() + "'";
            }
        }
    }
}
