package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ConfirmationType;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionOperation;
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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.alphawallet.app.C.Key.TRANSACTION;
import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.repository.EthereumNetworkBase.MAINNET_ID;

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

        viewModel = ViewModelProviders.of(this, transactionDetailViewModelFactory)
                .get(TransactionDetailViewModel.class);
        viewModel.latestBlock().observe(this, this::onLatestBlock);
        viewModel.onTransaction().observe(this, this::onTransaction);

        String txHash = getIntent().getStringExtra(C.EXTRA_TXHASH);
        int chainId = getIntent().getIntExtra(C.EXTRA_CHAIN_ID, MAINNET_ID);
        wallet = getIntent().getParcelableExtra(WALLET);
        viewModel.fetchTransaction(wallet, txHash, chainId);
    }

    private void onTransaction(Transaction tx)
    {
        transaction = tx;
        if (transaction == null) {
            finish();
            return;
        }
        toolbar();
        setTitle();
        viewModel.prepare(transaction.chainId, wallet.address);
        functionBar = findViewById(R.id.layoutButtons);

        String blockNumber = transaction.blockNumber;
        TransactionOperation op = null;
        if (transaction.blockNumber != null && transaction.blockNumber.equals("0"))
        {
            //how long has this TX been pending
            findViewById(R.id.pending_spinner).setVisibility(View.VISIBLE);
            List<Integer> functionList = new ArrayList<>(Collections.singletonList(R.string.speedup_transaction));
            //functionList.add(R.string.cancel_transaction); No cancel TX at this stage
            functionList.add(R.string.action_open_etherscan);
            blockNumber = "";

            functionBar.setupFunctions(this, functionList);
            viewModel.startPendingTimeDisplay(transaction.hash);
            viewModel.lastestTx().observe(this, this::onTxUpdated);
        }
        else
        {
            functionBar.setupSecondaryFunction(this, R.string.action_open_etherscan);
        }

        setupVisibilities();

        amount = findViewById(R.id.amount);
        CopyTextView toValue = findViewById(R.id.to);
        CopyTextView fromValue = findViewById(R.id.from);
        CopyTextView txHashView = findViewById(R.id.txn_hash);

        fromValue.setText(transaction.from != null ? transaction.from : "");
        toValue.setText(transaction.to != null ? transaction.to : "");
        txHashView.setText(transaction.hash != null ? transaction.hash : "");
        ((TextView) findViewById(R.id.txn_time)).setText(localiseUnixTime(transaction.timeStamp));

        ((TextView) findViewById(R.id.block_number)).setText(blockNumber);

        if (transaction.operations != null && transaction.operations.length > 0)
        {
            op = transaction.operations[0];
            if (op != null && op.to != null) toValue.findViewById(R.id.to);
        }

        chainName = viewModel.getNetworkName(transaction.chainId);
        ((TextView) findViewById(R.id.network)).setText(chainName);

        token = viewModel.getToken(transaction.chainId, transaction.to);
        TextView chainLabel = findViewById(R.id.text_chain_name);

        Utils.setChainColour(chainLabel, transaction.chainId);
        chainLabel.setText(chainName);

        setOperationName();

        if (!viewModel.hasEtherscanDetail(transaction)) findViewById(R.id.more_detail).setVisibility(View.GONE);
        setupWalletDetails(op);
    }

    private void onTxUpdated(Transaction latestTx)
    {
        if (latestTx.blockNumber.equals("0"))
        {
            long pendingTimeInSeconds = (System.currentTimeMillis() / 1000) - latestTx.timeStamp;
            ((TextView) findViewById(R.id.block_number)).setText(getString(R.string.transaction_pending_for, Utils.convertTimePeriodInSeconds(pendingTimeInSeconds, this)));
        }
        else
        {
            transaction = latestTx;
            ((TextView) findViewById(R.id.block_number)).setText(transaction.blockNumber);
            findViewById(R.id.pending_spinner).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.txn_time)).setText(localiseUnixTime(transaction.timeStamp));
            //update function bar
            functionBar.setupSecondaryFunction(this, R.string.action_open_etherscan);
        }
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
                String confirmation = transaction.blockNumber + " (" + confirmations.toString(10) + " " + getString(R.string.confirmations)  + ")";
                ((TextView) findViewById(R.id.block_number)).setText(confirmation);
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
        BigDecimal gasPrice = new BigDecimal(transaction.gasPrice);
        //any gas fee?
        if (gasFee.equals(BigDecimal.ZERO))
        {
            findViewById(R.id.layout_gas_fee).setVisibility(View.GONE);
        }
        else
        {
            findViewById(R.id.layout_gas_fee).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.gas_fee)).setText(BalanceUtils.weiToEth(gasFee).toPlainString());// .toPlainString());
        }

        if (gasPrice.equals(BigDecimal.ZERO))
        {
            findViewById(R.id.layout_gas_price).setVisibility(View.GONE);
        }
        else
        {
            findViewById(R.id.layout_gas_price).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.gas_price)).setText(BalanceUtils.weiToGwei(gasPrice.toBigInteger()));
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

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        viewModel.onDispose();
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
    public void handleClick(String action, int id)
    {
        switch (id)
        {
            case R.string.speedup_transaction:
                //resend the transaction to speedup
                viewModel.reSendTransaction(transaction, this, token, ConfirmationType.RESEND);
                break;
            case R.string.cancel_transaction:
                //cancel the transaction
                viewModel.reSendTransaction(transaction, this, token, ConfirmationType.CANCEL_TX);
                break;
            case R.string.action_open_etherscan:
                viewModel.showMoreDetails(this, transaction);
                break;
            default:
                break;
        }
    }
}
