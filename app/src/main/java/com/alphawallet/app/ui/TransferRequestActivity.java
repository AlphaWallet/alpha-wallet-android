package com.alphawallet.app.ui;

import static com.alphawallet.app.widget.AWalletAlertDialog.ERROR;
import static com.alphawallet.app.widget.AWalletAlertDialog.WARNING;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.EIP681Type;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.GasEstimate;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.TransactionReturn;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.ui.widget.entity.AddressReadyCallback;
import com.alphawallet.app.ui.widget.entity.AmountReadyCallback;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.SendViewModel;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ActionSheetDialog;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.InputAddress;
import com.alphawallet.app.widget.InputAmount;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.alphawallet.hardware.SignatureFromKey;
import com.alphawallet.token.tools.Convert;
import com.alphawallet.token.tools.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@AndroidEntryPoint
public class TransferRequestActivity extends BaseActivity implements
    AmountReadyCallback,
    StandardFunctionInterface,
    AddressReadyCallback,
    ActionSheetCallback
{
    private static final BigDecimal NEGATIVE = BigDecimal.ZERO.subtract(BigDecimal.ONE);
    private final Handler handler = new Handler();
    private SendViewModel viewModel;
    private Wallet wallet;
    private Token token;
    private AWalletAlertDialog dialog;
    private AWalletAlertDialog progressDialog;
    private QRResult result;
    private InputAmount amountInput;
    private InputAddress addressInput;
    private FunctionButtonBar functionBar;
    private String sendAddress = null;
    private String ensAddress;
    private BigDecimal sendAmount = NEGATIVE;
    private BigDecimal sendGasPrice = BigDecimal.ZERO;
    private ActionSheetDialog confirmationDialog;
    private final ActivityResultLauncher<Intent> getGasSettings = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        result -> confirmationDialog.setCurrentGasIndex(result));
    @Nullable
    private Disposable calcGasCost;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_transfer_request);

        toolbar();

        setTitle(R.string.empty);

        initViews();

        initViewModel();

        result = getIntent().getParcelableExtra(C.EXTRA_AMOUNT);

        evaluateQrResult(result);
    }

    private void initViews()
    {
        amountInput = findViewById(R.id.input_amount);
        addressInput = findViewById(R.id.input_address);
        functionBar = findViewById(R.id.layoutButtons);

        addressInput.setAddressCallback(this);
        amountInput.hideCaret();

        //TODO Send: set uneditable amount and address input.
        amountInput.setEnabled(false);
//        addressInput.setEnabled(false);
        addressInput.setEditable(false);
        addressInput.showControls(false);

        progressDialog = new AWalletAlertDialog(this);
        progressDialog.setTitle(R.string.searching_for_token);
        progressDialog.setIcon(AWalletAlertDialog.NONE);
        progressDialog.setProgressMode();
        progressDialog.setCancelable(false);
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this).get(SendViewModel.class);
        viewModel.wallet().observe(this, this::onWallet);
        viewModel.finalisedToken().observe(this, this::onFinalisedToken);
        viewModel.transactionFinalised().observe(this, this::txWritten);
        viewModel.transactionError().observe(this, this::txError);
        viewModel.error().observe(this, this::onError);
        viewModel.progress().observe(this, this::showProgress);
    }

    private void evaluateQrResult(QRResult r)
    {
        if (r != null)
        {
            this.result = r;
            viewModel.prepare();
        }
        else
        {
            displayScanError();
            finish();
        }
    }

    private void onWallet(Wallet wallet)
    {
        this.wallet = wallet;

        if (!isValidChain(result.chainId))
        {
            finish();
        }
        else if (result.type == EIP681Type.PAYMENT)
        {
            token = viewModel.getToken(result.chainId, wallet.address);
        }
        else if (result.type == EIP681Type.TRANSFER)
        {
            token = viewModel.getToken(result.chainId, result.getAddress());
        }

        if (token != null)
        {
            evaluateToken(token);
        }
        else
        {
            Timber.d("You don't have this token, attempt to fetch");
            showProgress(true);
            viewModel.fetchToken(result.chainId, result.getAddress(), wallet.address);
        }
    }

    private void evaluateToken(Token token)
    {
        if (token != null && token.balance.equals(BigDecimal.ZERO))
        {
            displayToast("Wallet does not have requested token");
            showProgress(false);
            finish();
        }
        else if (token != null && token.isERC20())
        {
            setupTokenContent(token);
            addressInput.setAddress(result.functionToAddress);
            amountInput.setAmount(result.tokenAmount.toString());
            amountInput.getInputAmount();
        }
        else if (token != null && token.isEthereum())
        {
            setupTokenContent(token);
            addressInput.setAddress(result.getAddress());
            amountInput.setAmount(Convert.getConvertedValue(new BigDecimal(result.weiValue), Convert.Unit.ETHER.getFactor()));
            amountInput.getInputAmount();
        }
        else // TODO: Handle NFT
        {
            displayToast("NFTs not supported yet.");
            finish();
        }
    }

    private boolean isValidChain(long chainId)
    {
        if (viewModel.getNetworkInfo(chainId) == null)
        {
            displayToast(getString(R.string.chain_not_support, String.valueOf(chainId)));
            return false;
        }
        if (!viewModel.isNetworkEnabled(chainId))
        {
            displayToast("Network not enabled");
            return false;
        }
        return true;
    }

    private void onFinalisedToken(Token token)
    {
        evaluateToken(token);
    }

//    private void onTokens(List<Token> tokens)
//    {
//        // Filter tokens here.
//        selectTokenDialog = new SelectTokenDialog(tokens, this, this);
//        selectTokenDialog.show();
//    }

    private void setupTokenContent(Token token)
    {
        this.token = token;
        amountInput.setupToken(token, viewModel.getAssetDefinitionService(), viewModel.getTokenService(), this);
        addressInput.setChainOverrideForWalletConnect(token.tokenInfo.chainId);

        setTitle(getString(R.string.action_send_tkn, token.getSymbol()));

        functionBar.revealButtons();
        functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.action_next)));
        viewModel.startGasCycle(token.tokenInfo.chainId);
    }

    private void onError(ErrorEnvelope errorEnvelope)
    {
        displayToast(errorEnvelope.message);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            onBackPressed();
        }
        return false;
    }

    @Override
    public void onBackPressed()
    {
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        {
            Operation taskCode = null;
            if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
            {
                taskCode = Operation.values()[requestCode - SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS];
                requestCode = SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS;
            }
            if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
            {
                if (confirmationDialog != null && confirmationDialog.isShowing())
                    confirmationDialog.completeSignRequest(resultCode == RESULT_OK);
            }
            else
            {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    private void calculateEstimateDialog()
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setTitle(getString(R.string.calc_gas_limit));
        dialog.setIcon(AWalletAlertDialog.NONE);
        dialog.setProgressMode();
        dialog.setCancelable(false);
        dialog.show();
    }

    private void displayScanError()
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.toast_qr_code_no_address);
        dialog.setButtonText(R.string.dialog_cancel_back);
        dialog.setButtonListener(v -> dialog.dismiss());
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void displayError(int titleId, String message)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(titleId);
        dialog.setMessage(message);
        dialog.setButtonText(R.string.dialog_ok);
        dialog.setButtonListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    protected void onDestroy()
    {
        if (dialog != null && dialog.isShowing())
        {
            dialog.dismiss();
        }
        super.onDestroy();
        if (viewModel != null) viewModel.onDestroy();
        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (amountInput != null) amountInput.onDestroy();
        if (confirmationDialog != null) confirmationDialog.onDestroy();
    }

    @Override
    public void amountReady(BigDecimal value, BigDecimal gasPrice)
    {
        if (isBalanceSufficient(value))
        {
            sendAmount = value;
            sendGasPrice = gasPrice;
            calculateTransactionCost();
        }
        else
        {
            sendAmount = NEGATIVE;
            amountInput.showError(true, 0);
            addressInput.stopNameCheck();
        }
    }

    private boolean isBalanceSufficient(BigDecimal value)
    {
        return (token.isEthereum() && token.balance.subtract(value).compareTo(BigDecimal.ZERO) > 0) // if sending base ethereum then check we have more than just the value
            || (token.getBalanceRaw().subtract(value).compareTo(BigDecimal.ZERO) >= 0);
    }

    @Override
    public void handleClick(String action, int actionId)
    {
        if (actionId == R.string.action_next)
        {
            amountInput.getInputAmount();
            addressInput.getAddress();
        }
    }

    @Override
    public void addressReady(String address, String ensName)
    {
        sendAddress = address;
        ensAddress = ensName;
        if (!Utils.isAddressValid(address))
        {
            //show address error
            addressInput.setError(getString(R.string.error_invalid_address));
        }
        else
        {
            calculateTransactionCost();
        }
    }

    private void calculateTransactionCost()
    {
        if ((calcGasCost != null && !calcGasCost.isDisposed()) ||
            (confirmationDialog != null && confirmationDialog.isShowing())) return;

        if (sendAmount.compareTo(NEGATIVE) > 0 && Utils.isAddressValid(sendAddress))
        {
            final String txSendAddress = sendAddress;
            sendAddress = null;
            //either sending base chain or ERC20 tokens.
            final byte[] transactionBytes = viewModel.getTransactionBytes(token, txSendAddress, sendAmount);

            final String txDestAddress = token.isEthereum() ? txSendAddress : token.getAddress(); //either another address, or ERC20 Token address

            calculateEstimateDialog();
            //form payload and calculate tx cost
            calcGasCost = viewModel.calculateGasEstimate(wallet, transactionBytes, token.tokenInfo.chainId, txDestAddress, BigDecimal.ZERO)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(estimate -> checkConfirm(estimate, transactionBytes, txDestAddress, txSendAddress),
                    error -> handleError(error, transactionBytes, token.getAddress(), txSendAddress));
        }
    }

    private void handleError(Throwable throwable, final byte[] transactionBytes, final String txSendAddress, final String resolvedAddress)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        displayErrorMessage(throwable.getMessage());
    }

    private void checkConfirm(GasEstimate estimate, final byte[] transactionBytes, final String txSendAddress, final String resolvedAddress)
    {
        BigInteger ethValue = token.isEthereum() ? sendAmount.toBigInteger() : BigInteger.ZERO;
        long leafCode = amountInput.isSendAll() ? -2 : -1;
        Web3Transaction w3tx = new Web3Transaction(
            new Address(txSendAddress),
            token.isEthereum() ? null : new Address(token.getAddress()),
            new Address(wallet.address),
            ethValue,
            sendGasPrice.toBigInteger(),
            estimate.getValue(),
            -1,
            Numeric.toHexString(transactionBytes),
            leafCode);

        if (estimate.hasError() || estimate.getValue().equals(BigInteger.ZERO))
        {
            estimateError(estimate, w3tx, transactionBytes, txSendAddress, resolvedAddress);
        }
        else
        {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();
            confirmationDialog = new ActionSheetDialog(this, w3tx, token, ensAddress,
                resolvedAddress, viewModel.getTokenService(), this);
            confirmationDialog.setCanceledOnTouchOutside(false);
            confirmationDialog.show();
            sendAmount = NEGATIVE;
        }
    }

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
    public void notifyConfirm(String mode)
    {
        AnalyticsProperties props = new AnalyticsProperties();
        props.put(Analytics.PROPS_ACTION_SHEET_MODE, mode);
        viewModel.track(Analytics.Action.ACTION_SHEET_COMPLETED, props);
    }

    @Override
    public WalletType getWalletType()
    {
        return wallet.type;
    }

    private void txWritten(TransactionReturn txData)
    {
        confirmationDialog.transactionWritten(txData.hash);
        viewModel.setLastSentToken(token);
    }

    private void txError(TransactionReturn txError)
    {
        Timber.e(txError.throwable);
        if (txError.throwable instanceof SocketTimeoutException)
        {
            showTxnTimeoutDialog();
        }
        else
        {
            showTxnErrorDialog(txError.throwable);
        }
        confirmationDialog.dismiss();
    }

    private void estimateError(GasEstimate estimate, final Web3Transaction w3tx, final byte[] transactionBytes, final String txSendAddress, final String resolvedAddress)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(WARNING);
        dialog.setTitle(estimate.hasError() ?
            R.string.dialog_title_gas_estimation_failed :
            R.string.confirm_transaction
        );
        String message = estimate.hasError() ?
            getString(R.string.dialog_message_gas_estimation_failed, estimate.getError()) :
            getString(R.string.error_transaction_may_fail);
        dialog.setMessage(message);
        dialog.setButtonText(R.string.action_proceed);
        dialog.setSecondaryButtonText(R.string.action_cancel);
        dialog.setButtonListener(v -> {
            BigInteger gasEstimate = GasService.getDefaultGasLimit(token, w3tx);
            checkConfirm(new GasEstimate(gasEstimate), transactionBytes, txSendAddress, resolvedAddress);
        });

        dialog.setSecondaryButtonListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    void showTxnErrorDialog(Throwable t)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(ERROR);
        dialog.setTitle(R.string.error_transaction_failed);
        dialog.setMessage(t.getMessage());
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v -> {
            dialog.dismiss();
        });
        dialog.show();
    }

    void showTxnTimeoutDialog()
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(WARNING);
        dialog.setTitle(R.string.error_transaction_timeout);
        dialog.setMessage(R.string.message_transaction_timeout);
        dialog.setButton(R.string.ok, v -> {
            dialog.dismiss();
        });
        dialog.show();
    }

//    private void showChainChangeDialog(long chainId)
//    {
//        if (dialog != null && dialog.isShowing()) dialog.dismiss();
//
//        token = viewModel.getToken(chainId, wallet.address);
//
//        dialog = new AWalletAlertDialog(this);
//        dialog.setIcon(AWalletAlertDialog.WARNING);
//        dialog.setTitle(R.string.change_chain_request);
//        dialog.setMessage(R.string.change_chain_message);
//        dialog.setButtonText(R.string.dialog_ok);
//        dialog.setButtonListener(v -> {
//            //we should change the chain.
//            token = viewModel.getToken(chainId, token.getAddress());
//            amountInput.setupToken(token, viewModel.getAssetDefinitionService(), viewModel.getTokenService(), this);
//            dialog.dismiss();
//            validateEIP681Request(currentResult, false);
//        });
//        dialog.setSecondaryButtonText(R.string.action_cancel);
//        dialog.setSecondaryButtonListener(v -> {
//            dialog.dismiss();
//            //proceed without changing the chain
//            currentResult.chainId = token.tokenInfo.chainId;
//            validateEIP681Request(currentResult, false);
//        });
//        dialog.show();
//    }

    private void showProgress(Boolean showProgress)
    {
        if (progressDialog != null)
        {
            if (showProgress)
            {
                progressDialog.show();
            }
            else
            {
                progressDialog.dismiss();
            }
        }
    }
}
