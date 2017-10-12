package com.wallet.crypto.trust.views;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.wallet.crypto.trust.R;
import com.wallet.crypto.trust.controller.Controller;
import com.wallet.crypto.trust.controller.OnTaskCompleted;
import com.wallet.crypto.trust.model.ESTransaction;
import com.wallet.crypto.trust.model.VMAccount;

import java.util.List;

/**
 * An activity representing a list of Items. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link TransactionDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class TransactionListActivity extends AppCompatActivity implements OnTaskCompleted {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private static String TAG = "ITEM_LIST_ACTIVITY";
    private String mAddress = "";
    private Controller mController;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_list);

        showIntro();

        mController = Controller.get();
        mController.init(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.item_list);
        assert mRecyclerView != null;
        setupRecyclerView(mRecyclerView);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
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

    private void fetchModelsAndReinit() {
        mController.loadViewModels(new OnTaskCompleted() {
            @Override
            public void onTaskCompleted() {
                asyncInit();
            }
        });
    }

    protected void init() {
        VMAccount account = mController.getCurrentAccount();
        if (account != null) {
            mAddress = account.getAddress();
            Log.d(TAG, "Address: %s, Balance: %s".format(mAddress, account.getBalance().toString()));
        } else {
            //Intent intent = new Intent(this, CreateAccountActivity.class);
            //this.startActivity(intent);

            mAddress ="0xDEADBEEF";
            account = new VMAccount(mAddress, "0");
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(mAddress.substring(0, 5) + ": " + Controller.WeiToEth(account.getBalance().toString(), 5) + " ETH");
        toolbar.inflateMenu(R.menu.transaction_list_menu);

        refreshTransactions();
    }

    private void showIntro() {
        //  Declare a new thread to do a preference check
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                //  Initialize SharedPreferences
                SharedPreferences getPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getBaseContext());

                //  Create a new boolean and preference and set it to true
                boolean isFirstStart = getPrefs.getBoolean("firstStart", true);

                //  If the activity has never started before...
                if (isFirstStart) {

                    //  Launch app intro
                    final Intent i = new Intent(TransactionListActivity.this, IntroActivity.class);

                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            startActivity(i);
                        }
                    });

                    //  Make a new preferences editor
                    SharedPreferences.Editor e = getPrefs.edit();

                    //  Edit preference to make it false because we don't want this to run again
                    e.putBoolean("firstStart", true);

                    //  Apply changes
                    e.apply();
                }
            }
        });

        // Start the thread
        t.start();
    }

    private void asyncInit() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                init();
            }
        });
    }

    private void refreshTransactions() {
        setupRecyclerView(mRecyclerView);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.transaction_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_receive:
                mController.navigateToReceive(this);
                break;
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
        Controller controller = Controller.get();
        List<ESTransaction> txns = controller.getTransactions(mAddress);
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(txns));
    }

    @Override
    public void onTaskCompleted() {
        // Populate transactions
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
                holder.mValueView.setTextColor(Color.RED);
                holder.mAddressView.setText(holder.mItem.getTo());
                sign = "-";
            } else {
                holder.mSentOrReceived.setText(getString(R.string.received));
                holder.mAddressView.setText(holder.mItem.getFrom());
                sign = "+";
                holder.mValueView.setTextColor(Color.GREEN);
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
