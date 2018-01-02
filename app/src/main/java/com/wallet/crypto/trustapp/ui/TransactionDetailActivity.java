package com.wallet.crypto.trustapp.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.Transaction;
import com.wallet.crypto.trustapp.viewmodel.TransactionDetailViewModel;
import com.wallet.crypto.trustapp.viewmodel.TransactionDetailViewModelFactory;

import java.util.Calendar;
import java.util.Locale;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.wallet.crypto.trustapp.C.Key.TRANSACTION;

public class TransactionDetailActivity extends BaseActivity implements View.OnClickListener {

    @Inject
    TransactionDetailViewModelFactory transactionDetailViewModelFactory;
    private TransactionDetailViewModel viewModel;

    private Transaction transaction;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidInjection.inject(this);

        setContentView(R.layout.activity_transaction_detail);

        transaction = getIntent().getParcelableExtra(TRANSACTION);
        if (transaction == null) {
            finish();
            return;
        }
        toolbar();

        ((TextView) findViewById(R.id.from)).setText(transaction.from);
        ((TextView) findViewById(R.id.to)).setText(transaction.to);
        ((TextView) findViewById(R.id.gas_fee)).setText(transaction.gasUsed);
        ((TextView) findViewById(R.id.txn_hash)).setText(transaction.hash);
        ((TextView) findViewById(R.id.txn_time)).setText(getDate(transaction.timeStamp));
        ((TextView) findViewById(R.id.block_number)).setText(transaction.blockNumber);
        findViewById(R.id.more_detail).setOnClickListener(this);

        viewModel = ViewModelProviders.of(this, transactionDetailViewModelFactory)
                .get(TransactionDetailViewModel.class);
        viewModel.defaultNetwork().observe(this, this::onDefaultNetwork);
    }

    private String getDate(long timeStampInSec) {
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(timeStampInSec * 1000);
        return DateFormat.getLongDateFormat(this).format(cal.getTime());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_share, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            viewModel.shareTransactionDetail(this, transaction);
        }
        return super.onOptionsItemSelected(item);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        findViewById(R.id.more_detail).setVisibility(
                TextUtils.isEmpty(networkInfo.etherscanUrl) ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        viewModel.showMoreDetails(v.getContext(), transaction);
    }
}
