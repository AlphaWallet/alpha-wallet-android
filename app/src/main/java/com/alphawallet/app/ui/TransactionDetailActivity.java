package com.alphawallet.app.ui;

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

import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Utils;

import com.alphawallet.app.R;

import com.alphawallet.app.viewmodel.TransactionDetailViewModel;
import com.alphawallet.app.viewmodel.TransactionDetailViewModelFactory;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Locale;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.alphawallet.app.C.Key.TRANSACTION;

public class TransactionDetailActivity extends BaseActivity implements View.OnClickListener {

    @Inject
    TransactionDetailViewModelFactory transactionDetailViewModelFactory;
    private TransactionDetailViewModel viewModel;

    private Transaction transaction;
    private TextView amount;
    private Token token;
    private String chainName;

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

        String blockNumber = transaction.blockNumber;
        if (transaction.blockNumber.equals("0"))
        {
            blockNumber = getString(R.string.status_pending);
            findViewById(R.id.pending_spinner).setVisibility(View.VISIBLE);
        }

        BigDecimal gasFee = new BigDecimal(transaction.gasUsed).multiply(new BigDecimal(transaction.gasPrice));
        amount = findViewById(R.id.amount);
        ((TextView) findViewById(R.id.from)).setText(transaction.from);
        ((TextView) findViewById(R.id.to)).setText(transaction.to);
        ((TextView) findViewById(R.id.gas_fee)).setText(BalanceUtils.weiToEth(gasFee).toPlainString());
        ((TextView) findViewById(R.id.txn_hash)).setText(transaction.hash);
        ((TextView) findViewById(R.id.txn_time)).setText(getDate(transaction.timeStamp));
        ((TextView) findViewById(R.id.block_number)).setText(blockNumber);
        findViewById(R.id.more_detail).setOnClickListener(this);

        viewModel = ViewModelProviders.of(this, transactionDetailViewModelFactory)
                .get(TransactionDetailViewModel.class);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);

        chainName = viewModel.getNetworkName(transaction.chainId);

        token = viewModel.getToken(transaction.chainId, transaction.to);
        TextView chainLabel = findViewById(R.id.text_chain_name);

        Utils.setChainColour(chainLabel, transaction.chainId);
        chainLabel.setText(chainName);

        String operationName = null;
        if (token != null)
        {
            ((TextView)findViewById(R.id.contract_name)).setText(token.getFullName());
            operationName = token.getOperationName(transaction, getApplicationContext());
        }
        else
        {
            findViewById(R.id.contract_name_title).setVisibility(View.GONE);
            findViewById(R.id.contract_name).setVisibility(View.GONE);
        }

        if (operationName != null)
        {
            ((TextView)findViewById(R.id.transaction_name)).setText(operationName);
        }
        else
        {
            findViewById(R.id.transaction_name).setVisibility(View.GONE);
        }
    }

    private void onDefaultWallet(Wallet wallet) {
        boolean isSent = transaction.from.toLowerCase().equals(wallet.address);
        String rawValue;
        String symbol;
        String prefix = "";

        if (token == null && (transaction.input == null || transaction.input.equals("0x")))
        {
            token = viewModel.getToken(transaction.chainId, wallet.address);
        }

        if (token != null)
        {
            rawValue = token.getTransactionValue(transaction, getApplicationContext());
            isSent = token.getIsSent(transaction);
        }
        else
        {
            rawValue = Token.getScaledValue(transaction.value, 18);
            prefix = (isSent ? "-" : "+");
        }

        amount.setTextColor(ContextCompat.getColor(this, isSent ? R.color.red : R.color.green));
        rawValue =  prefix + rawValue;

        amount.setText(rawValue);
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
        viewModel.showMoreDetails(this, transaction);
    }
}
