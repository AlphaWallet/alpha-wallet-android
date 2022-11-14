package com.alphawallet.app.ui;

import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.ui.widget.holder.TransactionHolder.TRANSACTION_BALANCE_PRECISION;
import static com.alphawallet.app.widget.AWalletAlertDialog.ERROR;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.analytics.ActionSheetMode;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.TransactionDetailViewModel;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ActionSheetDialog;
import com.alphawallet.app.widget.ChainName;
import com.alphawallet.app.widget.CopyTextView;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.alphawallet.app.widget.TokenIcon;
import com.alphawallet.token.tools.Numeric;

import org.web3j.crypto.Keys;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class TransactionDetailActivity extends BaseActivity implements StandardFunctionInterface, ActionSheetCallback
{
    private TransactionDetailViewModel viewModel;

    private Transaction transaction;
    private TextView amount;
    private Token token;
    private String chainName;
    private Wallet wallet;
    private AWalletAlertDialog dialog;
    private FunctionButtonBar functionBar;
    private ActionSheetDialog confirmationDialog;
    private String tokenAddress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        viewModel = new ViewModelProvider(this)
                .get(TransactionDetailViewModel.class);
        viewModel.latestBlock().observe(this, this::onLatestBlock);
        viewModel.onTransaction().observe(this, this::onTransaction);
        viewModel.transactionFinalised().observe(this, this::txWritten);
        viewModel.transactionError().observe(this, this::txError);

        String txHash = getIntent().getStringExtra(C.EXTRA_TXHASH);
        long chainId = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, MAINNET_ID);
        wallet = getIntent().getParcelableExtra(WALLET);
        tokenAddress = getIntent().getStringExtra(C.EXTRA_ADDRESS);
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
            blockNumber = "";

            viewModel.startPendingTimeDisplay(transaction.hash);
            viewModel.latestTx().observe(this, this::onTxUpdated);
        }
        List<Integer> functionList = new ArrayList<>(Collections.singletonList(R.string.action_open_etherscan));
        functionBar.setupFunctions(this, functionList);

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

        setupTokenDetails();

        ChainName chainName = findViewById(R.id.chain_name);
        chainName.setChainID(transaction.chainId);

        setOperationName();

        setupWalletDetails();
        checkFailed();
    }

    private void setupTokenDetails()
    {
        Token targetToken = viewModel.getToken(transaction.chainId, TextUtils.isEmpty(tokenAddress) ? transaction.to : tokenAddress );
        if (targetToken.isEthereum()) return;
        LinearLayout tokenDetailsLayout = findViewById(R.id.token_details);
        tokenDetailsLayout.setVisibility(View.VISIBLE);
        TokenIcon icon = findViewById(R.id.token_icon);
        icon.bindData(targetToken, viewModel.getTokenService());
        CopyTextView address = findViewById(R.id.token_address);
        address.setText(Keys.toChecksumAddress(targetToken.getAddress()));
        TextView tokenName = findViewById(R.id.token_name);
        tokenName.setText(targetToken.getFullName());
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
            Timber.e(e);
        }
    }

    private void setupVisibilities()
    {
        BigDecimal gasPrice = getValue(transaction.gasPrice);
        BigDecimal gasFee = getValue(transaction.gasUsed).multiply(gasPrice);

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

        if (!TextUtils.isEmpty(transaction.maxFeePerGas))
        {
            setup1559Visibilities();
        }
    }

    private void setup1559Visibilities()
    {
        LinearLayout extendedGas = findViewById(R.id.layout_1559);
        extendedGas.setVisibility(View.VISIBLE);

        TextView textGasMax = findViewById(R.id.text_gas_max);
        TextView textGasPriority = findViewById(R.id.text_priority_fee);

        BigDecimal gasMax = getValue(transaction.maxFeePerGas);
        BigDecimal gasPriorityFee = getValue(transaction.maxPriorityFee);

        textGasMax.setText(BalanceUtils.weiToGwei(gasMax, 4));
        textGasPriority.setText(BalanceUtils.weiToGwei(gasPriorityFee, 4));
    }

    private BigDecimal getValue(String input)
    {
        if (TextUtils.isEmpty(input)) return BigDecimal.ZERO;
        BigDecimal value = BigDecimal.ZERO;

        try
        {
            if (input.startsWith("0x"))
            {
                value = new BigDecimal(Numeric.toBigInt(input));
            }
            else
            {
                value = new BigDecimal(input);
            }
        }
        catch (NumberFormatException e)
        {
            value = BigDecimal.ZERO;
        }

        return value;
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
            checkConfirm(ActionSheetMode.SPEEDUP_TRANSACTION);
        }
        else if (id == R.string.cancel_transaction)
        {
            checkConfirm(ActionSheetMode.CANCEL_TRANSACTION);
        }
        else if (id == R.string.action_open_etherscan)
        {
            viewModel.showMoreDetails(this, transaction);
        }
    }

    /**
     * Called to check if we're ready to send user to confirm screen / activity sheet popup
     *
     */
    private void checkConfirm(ActionSheetMode mode)
    {
        BigInteger minGasPrice = viewModel.calculateMinGasPrice(new BigInteger(transaction.gasPrice));

        Web3Transaction w3tx = new Web3Transaction(transaction, mode, minGasPrice);

        if (dialog != null && dialog.isShowing())
        {
            dialog.dismiss();
        }

        confirmationDialog = new ActionSheetDialog(this, w3tx, token, null,
                transaction.to, viewModel.getTokenService(), this);
        confirmationDialog.setupResendTransaction(mode);
        confirmationDialog.setCanceledOnTouchOutside(false);
        confirmationDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
        {
            if (confirmationDialog != null && confirmationDialog.isShowing())
            {
                confirmationDialog.completeSignRequest(resultCode == RESULT_OK);
            }
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * ActionSheetCallback, comms hooks for the ActionSheetDialog to trigger authentication & send transactions
     *
     * @param callback
     */
    @Override
    public void getAuthorisation(SignAuthenticationCallback callback)
    {
        viewModel.getAuthentication(this, wallet, callback);
    }

    @Override
    public void sendTransaction(Web3Transaction finalTx)
    {
        viewModel.sendTransaction(finalTx, wallet, token.tokenInfo.chainId, transaction.hash); //return point is txWritten
    }

    @Override
    public void dismissed(String txHash, long callbackId, boolean actionCompleted)
    {
        //ActionSheet was dismissed
        if (!TextUtils.isEmpty(txHash))
        {
            Intent intent = new Intent();
            intent.putExtra(C.EXTRA_TXHASH, txHash);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    ActivityResultLauncher<Intent> getGasSettings = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> confirmationDialog.setCurrentGasIndex(result));

    @Override
    public ActivityResultLauncher<Intent> gasSelectLauncher()
    {
        return getGasSettings;
    }

    @Override
    public void notifyConfirm(String mode)
    {
        AnalyticsProperties props = new AnalyticsProperties();
        props.put(Analytics.PROPS_ACTION_SHEET_MODE, mode);
        viewModel.track(Analytics.Action.ACTION_SHEET_COMPLETED, props);
    }

    private void txWritten(TransactionData transactionData)
    {
        confirmationDialog.transactionWritten(transactionData.txHash);
        //reset display to show new transaction (load transaction from database)
        viewModel.fetchTransaction(wallet, transactionData.txHash, transaction.chainId);
    }

    //Transaction failed to be sent
    private void txError(Throwable throwable)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(ERROR);
        dialog.setTitle(R.string.error_transaction_failed);
        dialog.setMessage(throwable.getMessage());
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v -> {
            dialog.dismiss();
        });
        dialog.show();
        confirmationDialog.dismiss();
    }
}
