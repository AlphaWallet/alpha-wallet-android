package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionOperation;
import com.alphawallet.app.entity.VisibilityFilter;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.holder.TransactionHolder;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.LocaleUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.TransactionDetailViewModel;
import com.alphawallet.app.viewmodel.TransactionDetailViewModelFactory;
import com.alphawallet.app.widget.CopyTextView;
import com.alphawallet.app.widget.FunctionButtonBar;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.alphawallet.app.C.Key.TRANSACTION;
import static com.alphawallet.app.C.Key.WALLET;

public class TransactionDetailActivity extends BaseActivity implements StandardFunctionInterface
{
    @Inject
    TransactionDetailViewModelFactory transactionDetailViewModelFactory;
    private TransactionDetailViewModel viewModel;

    private Transaction transaction;
    private TextView amount;
    private Token token;
    private String chainName;
    private Wallet wallet;
    private FunctionButtonBar functionBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        transaction = getIntent().getParcelableExtra(TRANSACTION);
        wallet = getIntent().getParcelableExtra(WALLET);
        if (transaction == null) {
            finish();
            return;
        }
        toolbar();
        setTitle();

        String blockNumber = transaction.blockNumber;
        TransactionOperation op = null;
        if (transaction.blockNumber != null && transaction.blockNumber.equals("0"))
        {
            blockNumber = getString(R.string.status_pending);
            findViewById(R.id.pending_spinner).setVisibility(View.VISIBLE);
        }

        setupVisibilities();

        amount = findViewById(R.id.amount);
        CopyTextView toValue = findViewById(R.id.to);
        CopyTextView fromValue = findViewById(R.id.from);
        CopyTextView txHashView = findViewById(R.id.txn_hash);
        functionBar = findViewById(R.id.layoutButtons);

        fromValue.setText(transaction.from);
        toValue.setText(transaction.to);
        txHashView.setText(transaction.hash);
        ((TextView) findViewById(R.id.txn_time)).setText(localiseUnixTime(transaction.timeStamp));

        ((TextView) findViewById(R.id.block_number)).setText(blockNumber);

        if (transaction.operations != null && transaction.operations.length > 0)
        {
            op = transaction.operations[0];
            if (op != null && op.to != null) toValue.findViewById(R.id.to);
        }

        viewModel = ViewModelProviders.of(this, transactionDetailViewModelFactory)
                .get(TransactionDetailViewModel.class);
        viewModel.latestBlock().observe(this, this::onLatestBlock);
        viewModel.prepare(transaction.chainId);

        chainName = viewModel.getNetworkName(transaction.chainId);
        ((TextView) findViewById(R.id.network)).setText(chainName);

        token = viewModel.getToken(transaction.chainId, transaction.to);
        TextView chainLabel = findViewById(R.id.text_chain_name);

        Utils.setChainColour(chainLabel, transaction.chainId);
        chainLabel.setText(chainName);

        setOperationName();

        if (!viewModel.hasEtherscanDetail(transaction)) findViewById(R.id.more_detail).setVisibility(View.GONE);
        setupWalletDetails(op);

        functionBar.setupSecondaryFunction(this, R.string.action_open_etherscan);
    }

    private void setTitle() {
        findViewById(R.id.toolbar_title).setVisibility(View.VISIBLE);
        ((TextView)findViewById(R.id.toolbar_title)).setText(R.string.title_transaction_details);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("");
    }

    private void onLatestBlock(BigInteger latestBlock)
    {
        try
        {
            BigInteger txBlock = new BigInteger(transaction.blockNumber);
            if (!latestBlock.equals(BigInteger.ZERO) && !txBlock.equals(BigInteger.ZERO))
            {
                //how many confirmations?
                BigInteger confirmations = latestBlock.subtract(txBlock);
                String confirmation = " (" + confirmations.toString(10) + " " + getString(R.string.confirmations)  + ")";
                ((TextView) findViewById(R.id.block_number)).append(confirmation);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void setupVisibilities()
    {
        BigDecimal gasFee = new BigDecimal(transaction.gasUsed).multiply(new BigDecimal(transaction.gasPrice));
        //any gas fee?
        BigDecimal gasFeeEth = BalanceUtils.weiToEth(gasFee);
        if (gasFeeEth.equals(BigDecimal.ZERO))
        {
            findViewById(R.id.gas_fee).setVisibility(View.GONE);
            findViewById(R.id.title_gas_fee).setVisibility(View.GONE);
        }
        else
        {
            ((TextView) findViewById(R.id.gas_fee)).setText(BalanceUtils.weiToEth(gasFee).toPlainString());
        }
    }

    private void setupWalletDetails(TransactionOperation op) {
        boolean isSent = transaction.from.equalsIgnoreCase(wallet.address);
        String rawValue;
        String prefix = "";

        if (token == null && op == null)
        {
            token = viewModel.getToken(transaction.chainId, wallet.address);
        }

        if (token != null)
        {
            if (token.isNonFungible() || op != null)
            {
                rawValue = token.getTransactionResultValue(transaction, TransactionHolder.TRANSACTION_BALANCE_PRECISION);
            }
            else
            {
                rawValue = BalanceUtils.getScaledValueWithLimit(token.getTxValue(transaction), token.tokenInfo.decimals) + " " + token.getSymbol();
                prefix = (token.getIsSent(transaction) ? "-" : "+");
            }
        }
        else
        {
            BigDecimal txValue = new BigDecimal(transaction.value);
            rawValue = BalanceUtils.getScaledValueWithLimit(txValue, 18) + " " + viewModel.getNetworkSymbol(transaction.chainId);
            prefix = (isSent ? "-" : "+");
        }

        rawValue =  prefix + rawValue;
        amount.setText(rawValue);
    }

    private String localiseUnixTime(long timeStampInSec)
    {
        Date date = new java.util.Date(timeStampInSec*DateUtils.SECOND_IN_MILLIS);
        DateFormat timeFormat = java.text.DateFormat.getTimeInstance(DateFormat.SHORT, LocaleUtils.getDeviceLocale(this));
        DateFormat dateFormat = java.text.DateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtils.getDeviceLocale(this));
        return timeFormat.format(date) + " | " + dateFormat.format(date);
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

    private void setOperationName()
    {
        String operationName = null;
        if (token != null)
        {
            operationName = token.getOperationName(transaction, getApplicationContext());
        }
        else
        {
            //no token, did we send? If from == our wallet then we sent this
            if (transaction.from.equalsIgnoreCase(wallet.address)) operationName = getString(R.string.sent);
        }

        if (operationName != null)
        {
            ((TextView)findViewById(R.id.text_operation_name)).setText(operationName);
        }
        else
        {
            findViewById(R.id.text_operation_name).setVisibility(View.GONE);
        }
    }

    @Override
    public void handleClick(String action)
    {
        viewModel.showMoreDetails(this, transaction);
    }
}
