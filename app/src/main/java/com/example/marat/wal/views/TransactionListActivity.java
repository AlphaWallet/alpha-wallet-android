package com.example.marat.wal.views;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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


import com.example.marat.wal.R;
import com.example.marat.wal.controller.Controller;
import com.example.marat.wal.controller.OnTaskCompleted;
import com.example.marat.wal.model.ESTransaction;
import com.example.marat.wal.model.VMAccount;

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
    private boolean mTwoPane;
    private static String TAG = "ITEM_LIST_ACTIVITY";
    private String mAddress = "";
    private Controller mController;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_list);

        mController = Controller.get();
        mController.init(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.item_list);
        assert mRecyclerView != null;
        setupRecyclerView(mRecyclerView);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.send_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mController.navigateToSend(TransactionListActivity.this);
            }
        });

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
        getSupportActionBar().setTitle(mAddress.substring(0, 5) + " ETH " + Controller.WeiToEth(account.getBalance().toString()));
        toolbar.inflateMenu(R.menu.toolbar_menu);

        refreshTransactions();
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
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mController.navigateToAccountList(this);
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
            holder.mIdView.setText(mValues.get(position).getBlockNumber());
            //holder.mContentView.setText(mValues.get(position).getBlockNumber());
            String wei = holder.mItem.getValue();
            holder.mItem.getTimeStamp();
            String sign = holder.mItem.getTo().equals(mAddress) ? "+" : "-";
            String eth = Controller.WeiToEth(wei);

            if (holder.mItem.getValue().equals("0")) {
                holder.mValueView.setText(eth);
            } else {
                holder.mValueView.setText(sign + eth);
            }

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTwoPane) {
                        Bundle arguments = new Bundle();
                        arguments.putString(TransactionDetailFragment.ARG_ITEM_ID, holder.mItem.getBlockNumber());
                        TransactionDetailFragment fragment = new TransactionDetailFragment();
                        fragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.item_detail_container, fragment)
                                .commit();
                    } else {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, TransactionDetailActivity.class);
                        intent.putExtra(TransactionDetailFragment.ARG_ITEM_ID, holder.mItem.getBlockNumber());

                        context.startActivity(intent);
                    }
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
            //public final TextView mContentView;
            public final TextView mValueView;

            public ESTransaction mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mIdView = (TextView) view.findViewById(R.id.id);
                // mContentView = (TextView) view.findViewById(R.id.content);
                mValueView = (TextView) view.findViewById(R.id.value);
            }

            @Override
            public String toString() {
                return super.toString(); //+ " '" + mContentView.getText() + "'";
            }
        }
    }
}
