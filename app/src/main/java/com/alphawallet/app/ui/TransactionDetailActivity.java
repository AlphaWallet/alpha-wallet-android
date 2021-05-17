package com.alphawallet.app.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ConfirmationType;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.TransactionDetailViewModel;
import com.alphawallet.app.viewmodel.TransactionDetailViewModelFactory;
import com.alphawallet.app.widget.ChainName;
import com.alphawallet.app.widget.CopyTextView;
import com.alphawallet.app.widget.FunctionButtonBar;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.ui.widget.holder.TransactionHolder.TRANSACTION_BALANCE_PRECISION;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

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

        viewModel = new ViewModelProvider(this, transactionDetailViewModelFactory)
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
        if (transaction.isPending())
        {
            //how long has this TX been pending
            findViewById(R.id.pending_spinner).setVisibility(View.VISIBLE);
            List<Integer> functionList = new ArrayList<>(Collections.singletonList(R.string.speedup_transaction));
            functionList.add(R.string.action_open_etherscan);
            functionList.add(R.string.cancel_transaction);
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
        ((TextView) findViewById(R.id.txn_time)).setText(Utils.localiseUnixDate(getApplicationContext(), transaction.timeStamp));

        ((TextView) findViewById(R.id.block_number)).setText(blockNumber);

        chainName = viewModel.getNetworkName(transaction.chainId);
        ((TextView) findViewById(R.id.network)).setText(chainName);
        ((ImageView) findViewById(R.id.network_icon)).setImageResource(EthereumNetworkRepository.getChainLogo(transaction.chainId));

        token = viewModel.getToken(transaction.chainId, transaction.to);

        ChainName chainName = findViewById(R.id.chain_name);
        chainName.setChainID(transaction.chainId);

        setOperationName();

        if (!viewModel.hasEtherscanDetail(transaction)) findViewById(R.id.more_detail).setVisibility(View.GONE);
        setupWalletDetails();
        checkFailed();
    }

    private void onTxUpdated(Transaction latestTx)
    {
        if (latestTx.isPending())
        {
            long pendingTimeInSeconds = (System.currentTimeMillis() / 1000) - latestTx.timeStamp;
            ((TextView) findViewById(R.id.block_number)).setText(getString(R.string.transaction_pending_for, Utils.convertTimePeriodInSeconds(pendingTimeInSeconds, this)));
        }
        else
        {
            transaction = latestTx;
            ((TextView) findViewById(R.id.block_number)).setText(transaction.blockNumber);
            findViewById(R.id.pending_spinner).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.txn_time)).setText(Utils.localiseUnixDate(getApplicationContext(), transaction.timeStamp));
            //update function bar
            functionBar.setupSecondaryFunction(this, R.string.action_open_etherscan);
            checkFailed();
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
            if (!latestBlock.equals(BigInteger.ZERO) && !transaction.isPending())
            {
                //how many confirmations?
                BigInteger confirmations = latestBlock.subtract(new BigInteger(transaction.blockNumber));
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
            findViewById(R.id.layout_network_fee).setVisibility(View.GONE);
        }
        else
        {
            findViewById(R.id.layout_gas_fee).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_network_fee).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.gas_used)).setText(BalanceUtils.getScaledValue(new BigDecimal(transaction.gasUsed), 0, 0));
            ((TextView) findViewById(R.id.network_fee)).setText(BalanceUtils.getScaledValue(BalanceUtils.weiToEth(gasFee), 0, 6));
            ((TextView) findViewById(R.id.text_fee_unit)).setText(viewModel.getNetworkSymbol(transaction.chainId));
        }

        if (gasPrice.equals(BigDecimal.ZERO))
        {
            findViewById(R.id.layout_gas_price).setVisibility(View.GONE);
        }
        else
        {
            findViewById(R.id.layout_gas_price).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.gas_price)).setText(BalanceUtils.weiToGwei(gasPrice, 2));
        }
    }

    private void setupWalletDetails()
    {
        String operationName = token.getOperationName(transaction, this);
        String transactionOperation = token.getTransactionResultValue(transaction, TRANSACTION_BALANCE_PRECISION);
        amount.setText(Utils.isContractCall(this, operationName) ? "" : transactionOperation);
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
    public void onResume()
    {
        super.onResume();
        viewModel.restartServices();
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

    private void checkFailed()
    {
        if (transaction.hasError())
        {
            TextView failed = findViewById(R.id.failed);
            TextView failedF = findViewById(R.id.failedFace);
            if (failed != null) failed.setVisibility(View.VISIBLE);
            if (failedF != null) failedF.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void handleClick(String action, int id)
    {
        if (id == R.string.speedup_transaction)
        {
            //resend the transaction to speedup
            viewModel.reSendTransaction(transaction, this, token, ConfirmationType.RESEND);
        }
        else if (id == R.string.cancel_transaction)
        {
            //cancel the transaction
            viewModel.reSendTransaction(transaction, this, token, ConfirmationType.CANCEL_TX);
        }
        else if (id == R.string.action_open_etherscan)
        {
            viewModel.showMoreDetails(this, transaction);
        }
    }
}
