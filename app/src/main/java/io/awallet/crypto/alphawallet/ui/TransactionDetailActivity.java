package io.awallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.ERC875ContractTransaction;
import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Transaction;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.util.BalanceUtils;
import io.awallet.crypto.alphawallet.viewmodel.TransactionDetailViewModel;
import io.awallet.crypto.alphawallet.viewmodel.TransactionDetailViewModelFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Locale;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static io.awallet.crypto.alphawallet.C.Key.TRANSACTION;

public class TransactionDetailActivity extends BaseActivity implements View.OnClickListener {

    @Inject
    TransactionDetailViewModelFactory transactionDetailViewModelFactory;
    private TransactionDetailViewModel viewModel;

    private Transaction transaction;
    private TextView amount;

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
        setTitle(R.string.empty);

        BigDecimal gasFee = new BigDecimal(transaction.gasUsed).multiply(new BigDecimal(transaction.gasPrice));
        amount = findViewById(R.id.amount);
        ((TextView) findViewById(R.id.from)).setText(transaction.from);
        ((TextView) findViewById(R.id.to)).setText(transaction.to);
        ((TextView) findViewById(R.id.gas_fee)).setText(BalanceUtils.weiToEth(gasFee).toPlainString());
        ((TextView) findViewById(R.id.txn_hash)).setText(transaction.hash);
        ((TextView) findViewById(R.id.txn_time)).setText(getDate(transaction.timeStamp));
        ((TextView) findViewById(R.id.block_number)).setText(transaction.blockNumber);
        findViewById(R.id.more_detail).setOnClickListener(this);

        viewModel = ViewModelProviders.of(this, transactionDetailViewModelFactory)
                .get(TransactionDetailViewModel.class);
        viewModel.defaultNetwork().observe(this, this::onDefaultNetwork);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);


    }

    private void onDefaultWallet(Wallet wallet) {
        boolean isSent = transaction.from.toLowerCase().equals(wallet.address);
        String rawValue;
        String symbol;
        long decimals = 18;
        NetworkInfo networkInfo = viewModel.defaultNetwork().getValue();
        if (transaction.operations == null || transaction.operations.length == 0 || transaction.operations[0].contract instanceof ERC875ContractTransaction) {
            rawValue = transaction.value;
            symbol = networkInfo == null ? "" : networkInfo.symbol;
        } else {
            rawValue = transaction.operations[0].value;
            decimals = transaction.operations[0].contract.decimals;
            symbol = transaction.operations[0].contract.symbol;
        }

        amount.setTextColor(ContextCompat.getColor(this, isSent ? R.color.red : R.color.green));
        if (rawValue.equals("0")) {
            rawValue = "0 " + symbol;
        } else {
            rawValue = (isSent ? "-" : "+") + getScaledValue(rawValue, decimals) + " " + symbol;
        }
        amount.setText(rawValue);
    }

    private String getScaledValue(String valueStr, long decimals) {
        // Perform decimal conversion
        BigDecimal value = new BigDecimal(valueStr);
        value = value.divide(new BigDecimal(Math.pow(10, decimals)));
        int scale = 3 - value.precision() + value.scale();
        return value.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
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
