package com.wallet.crypto.trustapp.views;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.Toast;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.controller.Controller;
import com.wallet.crypto.trustapp.controller.OnTaskCompleted;
import com.wallet.crypto.trustapp.controller.ServiceErrorException;
import com.wallet.crypto.trustapp.controller.TaskResult;
import com.wallet.crypto.trustapp.model.TROperation;
import com.wallet.crypto.trustapp.model.TRTransaction;
import com.wallet.crypto.trustapp.model.VMAccount;
import com.wallet.crypto.trustapp.util.PMMigrateHelper;
import com.wallet.crypto.trustapp.util.PasswordStoreFactory;
import com.wallet.crypto.trustapp.util.RootUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class TransactionListActivity extends AppCompatActivity {

    private static final int SIGNIFICANT_FIGURES = 3;
    private static final String PREF_SHOULD_SHOW_SECURITY_WARNING = "should_show_security_warning";

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private static String TAG = "ITEM_LIST_ACTIVITY";
    private String mAddress = "";
    private Controller mController;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
	private BottomNavigationView navigation;

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
                case R.id.navigation_tokens:
                    mController.navigateToTokenList(TransactionListActivity.this);
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
        mController.setTransactionListActivity(this);

        navigation = findViewById(R.id.navigation);
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

                String usd = Controller.with(this).EthToUsd(balance);
                // Conversion data may not be available, in which case, hide it
                if (usd != null) {
                    getSupportActionBar().setTitle("$" + usd);
                    getSupportActionBar().setSubtitle(balance + " " + mController.getCurrentNetwork().getSymbol());
                } else {
                    getSupportActionBar().setTitle(balance + " " + mController.getCurrentNetwork().getSymbol());
                    getSupportActionBar().setSubtitle(mAddress);
                }

                toolbar.inflateMenu(R.menu.transaction_list_menu);
            } catch (Exception e) {
                Log.e(TAG, "Error updating balance: ", e);
            }
        }

        invalidateOptionsMenu(); // recreate menu to hide deposit option

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
        super.onResume();

        init();
	    checkGuard();
	    checkRoot();

	    if (Controller.with(this).getCurrentNetwork().isTest()) {
		    navigation.getMenu().removeItem(R.id.navigation_tokens);
	    } else if (navigation.getMenu().findItem(R.id.navigation_tokens) == null) {
		    MenuItem item = navigation.getMenu()
				    .add(0, R.id.navigation_tokens, Menu.NONE, R.string.title_tokens);
		    item.setIcon(R.drawable.token_icon);
	    }

	    if (mController.getAccounts().size() == 0) {
		    Intent intent = new Intent(getApplicationContext(), CreateAccountActivity.class);
		    this.startActivityForResult(intent, Controller.IMPORT_ACCOUNT_REQUEST);
		    finish();
	    } else {
		    mController.onResume();
		    try {
			    PMMigrateHelper.migrate(this);
		    } catch (ServiceErrorException e) {
			    if (e.code == ServiceErrorException.USER_NOT_AUTHENTICATED) {
				    PasswordStoreFactory.showAuthenticationScreen(this, Controller.UNLOCK_SCREEN_REQUEST);
			    } else {
				    Toast.makeText(this, "Could not process passwords.", Toast.LENGTH_LONG)
						    .show();
			    }
		    }
	    }
    }

	private void checkRoot() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		if (RootUtil.isDeviceRooted() && pref.getBoolean("should_show_root_warning", true)) {
			pref.edit().putBoolean("should_show_root_warning", false).apply();
			new AlertDialog.Builder(this)
					.setTitle(R.string.root_title)
					.setMessage(R.string.root_body)
					.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					})
					.show();
		}
	}

	private void checkGuard() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		if (!isDeviceSecure() && pref.getBoolean(PREF_SHOULD_SHOW_SECURITY_WARNING, true)) {
			pref.edit().putBoolean(PREF_SHOULD_SHOW_SECURITY_WARNING, false).apply();
			new AlertDialog.Builder(this)
					.setTitle(R.string.lock_title)
					.setMessage(R.string.lock_body)
					.setPositiveButton(R.string.lock_settings, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
							startActivity(intent);
						}
					})
					.setNegativeButton(R.string.skip, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					})
					.show();
		}
	}

	protected boolean isDeviceSecure() {
		KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
		if (keyguardManager == null) {
			return false;
		}
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
				? keyguardManager.isDeviceSecure()
				: keyguardManager.isKeyguardSecure();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Controller.IMPORT_ACCOUNT_REQUEST) {
            if (resultCode == RESULT_OK) {
                this.finish();
            }
        } else if (requestCode == Controller.UNLOCK_SCREEN_REQUEST) {
        	if (resultCode == RESULT_OK) {

	        }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.transaction_list_menu, menu);

        for (int i = 0; i < menu.size(); i++) {
            if (menu.getItem(i).getItemId() == R.id.action_deposit) {
                if (mController.getCurrentNetwork().getName().equals(Controller.ETHEREUM)) {
                    menu.getItem(i).setVisible(true);
                } else {
                    menu.getItem(i).setVisible(false);
                }
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_deposit:
                mController.depositMoney(this);
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
        Controller controller = Controller.with(this);
        List<TRTransaction> txns = controller.getTransactions(mAddress);
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(txns));
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final List<TRTransaction> mValues;

        public SimpleItemRecyclerViewAdapter(List<TRTransaction> items) {
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
            TRTransaction txn = holder.mItem;

            holder.mDateView.setText(Controller.GetDate(Long.decode(holder.mItem.getTimeStamp())));

            String from;
            String to;
            String symbol;
            String valueStr;
            long decimals;

            // If operations include token transfer, display token transfer instead
            boolean isTokenTransfer = false;
            List<TROperation> operations = holder.mItem.getOperations();

            try {
                TROperation op = operations.get(0);
                from = op.getFrom();
                to = op.getTo();
                symbol = op.getContract().getSymbol();
                decimals = Long.parseLong(op.getContract().getDecimals());
                valueStr = op.getValue();
                isTokenTransfer = true;
            } catch (Exception ex) {
                // default to ether transaction
                from = txn.getFrom();
                to = txn.getTo();
                symbol = mController.getCurrentNetwork().getSymbol();
                valueStr = txn.getValue();
                decimals = Controller.ETHER_DECIMALS;
            }

            boolean isSent = from.toLowerCase().equals(mAddress.toLowerCase());

            // TODO deduplicate with TransactionDetailFragment.java
            String sign = "+";

            if (isSent) {
                holder.mSentOrReceived.setText(getString(R.string.sent));
                holder.mValueView.setTextColor(getResources().getColor(R.color.red));
                holder.mAddressView.setText(to);
                sign = "-";
            } else {
                holder.mSentOrReceived.setText(getString(R.string.received));
                holder.mAddressView.setText(from);
                sign = "+";
                holder.mValueView.setTextColor(getResources().getColor(R.color.green));
            }

            if (isTokenTransfer) {
                holder.mSentOrReceived.setText(getString(R.string.transfer) + " " + symbol);
            }

            // Perform decimal conversion
            BigDecimal value = new BigDecimal(valueStr);
            BigDecimal decimalDivisor = new BigDecimal(Math.pow(10, decimals));
            value = value.divide(decimalDivisor);
            int scale = SIGNIFICANT_FIGURES - value.precision() + value.scale();
            BigDecimal scaledValue = value.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros();

            String valueText;

            if (valueStr.equals("0")) {
                valueText = "0 " + symbol;
            } else {
                valueText = sign + scaledValue.toPlainString() + " " + symbol;
            }

            holder.mValueView.setText(valueText);

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
            public final TextView mValueView;

            public TRTransaction mItem;

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
