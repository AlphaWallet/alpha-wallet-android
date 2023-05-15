package com.alphawallet.app.ui;

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
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionReturn;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.analytics.ActionSheetMode;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.router.HomeRouter;
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
import com.alphawallet.hardware.SignatureFromKey;
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
    private TextView amount;
    private AWalletAlertDialog dialog;
    private FunctionButtonBar functionBar;
    private ActionSheetDialog confirmationDialog;
    private final ActivityResultLauncher<Intent> getGasSettings = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        result -> confirmationDialog.setCurrentGasIndex(result));
    private Transaction transaction;
    private Wallet wallet;
    private TextView txnTime;
    private TextView blockNumberTxt;
    private TextView network;
    private LinearLayout gasFeeLayout;
    private LinearLayout networkFeeLayout;
    private TextView gasUsed;
    private TextView networkFee;
    private TextView feeUnit;
    private LinearLayout gasPriceLayout;
    private ProgressBar progressBar;
    private CopyTextView toValue;
    private CopyTextView fromValue;
    private CopyTextView txHashView;
    private ImageView networkIcon;
    private ChainName chainNameTxt;
    private LinearLayout tokenDetailsLayout;
    private TokenIcon icon;
    private CopyTextView address;
    private TextView tokenName;
    private TextView gasPriceTxt;
    private LinearLayout extendedGas;
    private TextView textGasMax;
    private TextView textGasPriority;
    private TextView operationNameTxt;
    private TextView failed;
    private TextView failedF;
    private Token token;
    private String txHash;
    private long chainId;
    private String tokenAddress;
    private boolean isFromNotification;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        initViewModel();

        initViews();

        toolbar();

        setTitle(getString(R.string.title_transaction_details));

        txHash = getIntent().getStringExtra(C.EXTRA_TXHASH);
        chainId = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, MAINNET_ID);
        tokenAddress = getIntent().getStringExtra(C.EXTRA_ADDRESS);
        isFromNotification = getIntent().getBooleanExtra(C.FROM_NOTIFICATION, false);

        viewModel.prepare(chainId);
    }

    private void initViews()
    {
        functionBar = findViewById(R.id.layoutButtons);
        amount = findViewById(R.id.amount);
        progressBar = findViewById(R.id.pending_spinner);
        toValue = findViewById(R.id.to);
        fromValue = findViewById(R.id.from);
        txHashView = findViewById(R.id.txn_hash);
        txnTime = findViewById(R.id.txn_time);
        blockNumberTxt = findViewById(R.id.block_number);
        network = findViewById(R.id.network);
        gasFeeLayout = findViewById(R.id.layout_gas_fee);
        networkFeeLayout = findViewById(R.id.layout_network_fee);
        gasUsed = findViewById(R.id.gas_used);
        networkFee = findViewById(R.id.network_fee);
        feeUnit = findViewById(R.id.text_fee_unit);
        gasPriceLayout = findViewById(R.id.layout_gas_price);
        networkIcon = findViewById(R.id.network_icon);
        chainNameTxt = findViewById(R.id.chain_name);
        tokenDetailsLayout = findViewById(R.id.token_details);
        icon = findViewById(R.id.token_icon);
        address = findViewById(R.id.token_address);
        tokenName = findViewById(R.id.token_name);
        gasPriceTxt = findViewById(R.id.gas_price);
        extendedGas = findViewById(R.id.layout_1559);
        textGasMax = findViewById(R.id.text_gas_max);
        textGasPriority = findViewById(R.id.text_priority_fee);
        operationNameTxt = findViewById(R.id.text_operation_name);
        failed = findViewById(R.id.failed);
        failedF = findViewById(R.id.failedFace);
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
            .get(TransactionDetailViewModel.class);
        viewModel.wallet().observe(this, this::onWallet);
        viewModel.latestBlock().observe(this, this::onLatestBlock);
        viewModel.onTransaction().observe(this, this::onTransaction);
        viewModel.transactionFinalised().observe(this, this::txWritten);
        viewModel.transactionError().observe(this, this::txError);
    }

    private void onWallet(Wallet wallet)
    {
        this.wallet = wallet;

        viewModel.fetchTransaction(wallet, txHash, chainId);
    }

    private void onTransaction(Transaction tx)
    {
        transaction = tx;
        if (transaction == null)
        {
            finish();
            return;
        }

        String blockNumber = transaction.blockNumber;
        if (transaction.isPending())
        {
            //how long has this TX been pending
            progressBar.setVisibility(View.VISIBLE);
            blockNumber = "";

            viewModel.startPendingTimeDisplay(transaction.hash);
            viewModel.latestTx().observe(this, this::onTxUpdated);
        }
        List<Integer> functionList = new ArrayList<>(Collections.singletonList(R.string.action_open_etherscan));
        functionBar.setupFunctions(this, functionList);

        setupVisibilities();

        String from = transaction.from != null ? transaction.from : "";
        fromValue.setText(from);

        String to = transaction.to != null ? transaction.to : "";
        toValue.setText(to);

        String hash = transaction.hash != null ? transaction.hash : "";
        txHashView.setText(hash);

        txnTime.setText(Utils.localiseUnixDate(getApplicationContext(), transaction.timeStamp));

        blockNumberTxt.setText(blockNumber);

        network.setText(viewModel.getNetworkName(transaction.chainId));
        networkIcon.setImageResource(EthereumNetworkRepository.getChainLogo(transaction.chainId));

        token = viewModel.getToken(transaction.chainId, transaction.to);

        setupTokenDetails();

        chainNameTxt.setChainID(transaction.chainId);

        setOperationName();

        setupWalletDetails();
        checkFailed();
    }

    private void setupTokenDetails()
    {
        Token targetToken = viewModel.getToken(transaction.chainId, TextUtils.isEmpty(tokenAddress) ? transaction.to : tokenAddress);
        if (targetToken.isEthereum()) return;
        tokenDetailsLayout.setVisibility(View.VISIBLE);
        icon.bindData(targetToken, viewModel.getTokenService());
        address.setText(Keys.toChecksumAddress(targetToken.getAddress()));
        tokenName.setText(targetToken.getFullName());
    }

    private void onTxUpdated(Transaction latestTx)
    {
        if (latestTx.isPending())
        {
            long pendingTimeInSeconds = (System.currentTimeMillis() / 1000) - latestTx.timeStamp;
            blockNumberTxt.setText(getString(R.string.transaction_pending_for, Utils.convertTimePeriodInSeconds(pendingTimeInSeconds, this)));
        }
        else
        {
            transaction = latestTx;
            blockNumberTxt.setText(transaction.blockNumber);
            progressBar.setVisibility(View.GONE);
            txnTime.setText(Utils.localiseUnixDate(getApplicationContext(), transaction.timeStamp));
            //update function bar
            functionBar.setupSecondaryFunction(this, R.string.action_open_etherscan);
            checkFailed();
        }
    }

    private void onLatestBlock(BigInteger latestBlock)
    {
        try
        {
            if (!latestBlock.equals(BigInteger.ZERO) && !transaction.isPending())
            {
                //how many confirmations?
                BigInteger confirmations = latestBlock.subtract(new BigInteger(transaction.blockNumber));
                String confirmation = transaction.blockNumber + " (" + confirmations.toString(10) + " " + getString(R.string.confirmations) + ")";
                blockNumberTxt.setText(confirmation);
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
            gasFeeLayout.setVisibility(View.GONE);
            networkFeeLayout.setVisibility(View.GONE);
        }
        else
        {
            gasFeeLayout.setVisibility(View.VISIBLE);
            networkFeeLayout.setVisibility(View.VISIBLE);
            gasUsed.setText(BalanceUtils.getScaledValue(new BigDecimal(transaction.gasUsed), 0, 0));
            networkFee.setText(BalanceUtils.getScaledValue(BalanceUtils.weiToEth(gasFee), 0, 6));
            feeUnit.setText(viewModel.getNetworkSymbol(transaction.chainId));
        }

        if (gasPrice.equals(BigDecimal.ZERO))
        {
            gasPriceLayout.setVisibility(View.GONE);
        }
        else
        {
            gasPriceLayout.setVisibility(View.VISIBLE);
            gasPriceTxt.setText(BalanceUtils.weiToGwei(gasPrice, 2));
        }

        if (!TextUtils.isEmpty(transaction.maxFeePerGas))
        {
            setup1559Visibilities();
        }
    }

    private void setup1559Visibilities()
    {
        extendedGas.setVisibility(View.VISIBLE);
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
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_share, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            onBackPressed();
        }
        else if (item.getItemId() == R.id.action_share)
        {
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
            if (transaction.from.equalsIgnoreCase(wallet.address))
                operationName = getString(R.string.sent);
        }

        if (operationName != null)
        {
            operationNameTxt.setText(operationName);
        }
        else
        {
            operationNameTxt.setVisibility(View.GONE);
        }
    }

    private void checkFailed()
    {
        if (transaction.hasError())
        {
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
        viewModel.requestSignature(finalTx, wallet, token.tokenInfo.chainId);
    }

    @Override
    public void completeSendTransaction(Web3Transaction tx, SignatureFromKey signature)
    {
        viewModel.sendTransaction(wallet, token.tokenInfo.chainId, tx, signature);
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

    @Override
    public ActivityResultLauncher<Intent> gasSelectLauncher()
    {
        return getGasSettings;
    }

    @Override
    public WalletType getWalletType()
    {
        return wallet.type;
    }

    @Override
    public void notifyConfirm(String mode)
    {
        AnalyticsProperties props = new AnalyticsProperties();
        props.put(Analytics.PROPS_ACTION_SHEET_MODE, mode);
        viewModel.track(Analytics.Action.ACTION_SHEET_COMPLETED, props);
    }

    private void txWritten(TransactionReturn transactionReturn)
    {
        confirmationDialog.transactionWritten(transactionReturn.hash);
        //reset display to show new transaction (load transaction from database)
        viewModel.fetchTransaction(wallet, transactionReturn.hash, transaction.chainId);
    }

    //Transaction failed to be sent
    private void txError(TransactionReturn txError)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(ERROR);
        dialog.setTitle(R.string.error_transaction_failed);
        dialog.setMessage(txError.throwable.getMessage());
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v -> {
            dialog.dismiss();
        });
        dialog.show();
        confirmationDialog.dismiss();
    }

    @Override
    public void onBackPressed()
    {
        if (isFromNotification)
        {
            new HomeRouter().open(this, true);
        }
        else
        {
            super.onBackPressed();
        }
    }
}
