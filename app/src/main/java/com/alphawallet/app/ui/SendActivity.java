package com.alphawallet.app.ui;

import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.widget.AWalletAlertDialog.ERROR;
import static com.alphawallet.app.widget.AWalletAlertDialog.WARNING;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.EIP681Type;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.ui.QRScanning.QRScannerActivity;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.ui.widget.entity.AddressReadyCallback;
import com.alphawallet.app.ui.widget.entity.AmountReadyCallback;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.util.QRParser;
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
import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.tools.Convert;
import com.alphawallet.token.tools.Numeric;
import com.alphawallet.token.tools.ParseMagicLink;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@AndroidEntryPoint
public class SendActivity extends BaseActivity implements AmountReadyCallback, StandardFunctionInterface, AddressReadyCallback, ActionSheetCallback
{
    private static final BigDecimal NEGATIVE = BigDecimal.ZERO.subtract(BigDecimal.ONE);

    SendViewModel viewModel;

    private Wallet wallet;
    private Token token;
    private final Handler handler = new Handler();
    private AWalletAlertDialog dialog;

    private QRResult currentResult;

    private InputAmount amountInput;
    private InputAddress addressInput;
    private String sendAddress;
    private String ensAddress;
    private BigDecimal sendAmount;
    private BigDecimal sendGasPrice;
    private ActionSheetDialog confirmationDialog;
    private AWalletAlertDialog alertDialog;

    @Nullable
    private Disposable calcGasCost;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        toolbar();

        viewModel = new ViewModelProvider(this)
                .get(SendViewModel.class);

        String contractAddress = getIntent().getStringExtra(C.EXTRA_CONTRACT_ADDRESS);
        long currentChain = getIntent().getLongExtra(C.EXTRA_NETWORKID, MAINNET_ID);
        wallet = getIntent().getParcelableExtra(WALLET);
        token = viewModel.getToken(currentChain, getIntent().getStringExtra(C.EXTRA_ADDRESS));
        QRResult result = getIntent().getParcelableExtra(C.EXTRA_AMOUNT);

        viewModel.transactionFinalised().observe(this, this::txWritten);
        viewModel.transactionError().observe(this, this::txError);

        sendAddress = null;
        sendGasPrice = BigDecimal.ZERO;
        sendAmount = NEGATIVE;

        if (!checkTokenValidity(currentChain, contractAddress))
        {
            return;
        }

        setTitle(getString(R.string.action_send_tkn, token.getShortName()));
        setupTokenContent();

        if (result != null)
        {
            //restore payment request
            validateEIP681Request(result, true);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        QRResult result = getIntent().getParcelableExtra(C.EXTRA_AMOUNT);

        if (result != null && (result.type == EIP681Type.PAYMENT || result.type == EIP681Type.TRANSFER))
        {
            handleClick("", R.string.action_next);
        }
    }

    private boolean checkTokenValidity(long currentChain, String contractAddress)
    {
        if (token == null || token.tokenInfo == null)
        {
            //bad token - try to load from service
            token = viewModel.getToken(currentChain, contractAddress);

            if (token == null)
            {
                //TODO: possibly invoke token finder in tokensService
                finish();
            }
        }

        return (token != null);
    }

    private void onBack()
    {
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            onBack();
        }
        else if (item.getItemId() == R.id.action_show_contract)
        {
            viewModel.showContractInfo(this, wallet, token);
        }

        return false;
    }

    @Override
    public void onBackPressed()
    {
        onBack();
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
            else if (requestCode == C.BARCODE_READER_REQUEST_CODE)
            {
                switch (resultCode)
                {
                    case Activity.RESULT_OK:
                        if (data != null)
                        {
                            String qrCode = data.getStringExtra(C.EXTRA_QR_CODE);

                            //if barcode is still null, ensure we don't GPF
                            if (qrCode == null)
                            {
                                //Toast.makeText(this, R.string.toast_qr_code_no_address, Toast.LENGTH_SHORT).show();
                                displayScanError();
                                return;
                            }
                            else
                            {
                                QRParser parser = QRParser.getInstance(EthereumNetworkBase.extraChains());
                                QRResult result = parser.parse(qrCode);
                                String extracted_address = null;
                                if (result != null)
                                {
                                    extracted_address = result.getAddress();
                                    switch (result.getProtocol())
                                    {
                                        case "address":
                                            addressInput.setAddress(extracted_address);
                                            break;
                                        case "ethereum":
                                            //EIP681 protocol
                                            validateEIP681Request(result, false);
                                            break;
                                        default:
                                            break;
                                    }
                                }
                                else //try magiclink
                                {
                                    ParseMagicLink magicParser = new ParseMagicLink(new CryptoFunctions(), EthereumNetworkRepository.extraChains());
                                    try
                                    {
                                        if (magicParser.parseUniversalLink(qrCode).chainId > 0) //see if it's a valid link
                                        {
                                            //let's try to import the link
                                            viewModel.showImportLink(this, qrCode);
                                            finish();
                                            return;
                                        }
                                    }
                                    catch (SalesOrderMalformed e)
                                    {
                                        e.printStackTrace();
                                    }
                                }

                                if (extracted_address == null)
                                {
                                    displayScanError();
                                }
                            }
                        }
                        break;
                    case QRScannerActivity.DENY_PERMISSION:
                        showCameraDenied();
                        break;
                    default:
                        Timber.tag("SEND").e(String.format(getString(R.string.barcode_error_format),
                                "Code: " + resultCode
                        ));
                        break;
                }
            }
            else
            {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    private void startWalletConnect(String qrCode)
    {
        Intent intent = new Intent(this, WalletConnectActivity.class);
        intent.putExtra("qrCode", qrCode);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        startActivity(intent);
        setResult(RESULT_OK);
        finish();
    }

    private void showCameraDenied()
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setTitle(R.string.title_dialog_error);
        dialog.setMessage(R.string.error_camera_permission_denied);
        dialog.setIcon(ERROR);
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v -> {
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showTokenFetch()
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setTitle(R.string.searching_for_token);
        dialog.setIcon(AWalletAlertDialog.NONE);
        dialog.setProgressMode();
        dialog.setCancelable(false);
        dialog.show();
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

    private void validateEIP681Request(QRResult result, boolean overrideNetwork)
    {
        if (dialog != null) dialog.dismiss();
        //check chain
        if (result == null)
        {
            displayScanError();
            return;
        }

        NetworkInfo info = viewModel.getNetworkInfo(result.chainId);
        if (info == null)
        {
            displayScanError();
            return;
        }
        else if (result.type != EIP681Type.ADDRESS && result.chainId != token.tokenInfo.chainId && token.isEthereum())
        {
            //Display chain change warning
            currentResult = result;
            showChainChangeDialog(result.chainId);
            return;
        }

        TextView sendText = findViewById(R.id.text_payment_request);

        switch (result.type)
        {
            case ADDRESS:
                addressInput.setAddress(result.getAddress());
                break;

            case PAYMENT:
                //correct chain and asset type
                String ethAmount = Convert.getConvertedValue(new BigDecimal(result.weiValue), Convert.Unit.ETHER.getFactor());
                sendText.setVisibility(View.VISIBLE);
                sendText.setText(R.string.transfer_request);
                token = viewModel.getToken(result.chainId, wallet.address);
                addressInput.setAddress(result.getAddress());
                amountInput.setupToken(token, viewModel.getAssetDefinitionService(), viewModel.getTokenService(), this);
                amountInput.setAmount(ethAmount);
                setupTokenContent();
                break;

            case TRANSFER:
                Token resultToken = viewModel.getToken(result.chainId, result.getAddress());
                if (resultToken == null)
                {
                    currentResult = result;
                    showTokenFetch();
                    viewModel.fetchToken(result.chainId, result.getAddress(), wallet.address);
                }
                else if (resultToken.isERC20())
                {
                    //ERC20 send request
                    token = resultToken;
                    setupTokenContent();
                    amountInput.setupToken(token, viewModel.getAssetDefinitionService(), viewModel.getTokenService(), this);
                    //convert token amount into scaled value
                    String convertedAmount = Convert.getConvertedValue(result.tokenAmount, token.tokenInfo.decimals);
                    amountInput.setAmount(convertedAmount);
                    addressInput.setAddress(result.functionToAddress);
                    sendText.setVisibility(View.VISIBLE);
                    sendText.setText(getString(R.string.token_transfer_request, resultToken.getFullName()));
                }
                //TODO: Handle NFT eg ERC721
                break;

            case FUNCTION_CALL:
                //Generic function call, not handled yet
                displayScanError(R.string.toast_qr_code_no_address, getString(R.string.no_tokens));
                if (result.functionToAddress != null)
                    addressInput.setAddress(result.functionToAddress);
                break;

            default:
                displayScanError();
        }
    }

    private void showChainChangeDialog(long chainId)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.WARNING);
        dialog.setTitle(R.string.change_chain_request);
        dialog.setMessage(R.string.change_chain_message);
        dialog.setButtonText(R.string.dialog_ok);
        dialog.setButtonListener(v -> {
            //we should change the chain.
            token = viewModel.getToken(chainId, token.getAddress());
            amountInput.setupToken(token, viewModel.getAssetDefinitionService(), viewModel.getTokenService(), this);
            dialog.dismiss();
            validateEIP681Request(currentResult, false);
        });
        dialog.setSecondaryButtonText(R.string.action_cancel);
        dialog.setSecondaryButtonListener(v -> {
            dialog.dismiss();
            //proceed without changing the chain
            currentResult.chainId = token.tokenInfo.chainId;
            validateEIP681Request(currentResult, false);
        });
        dialog.show();
    }

    private void displayScanError()
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.toast_qr_code_no_address);
        dialog.setButtonText(R.string.dialog_ok);
        dialog.setButtonListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void displayScanError(int titleId, String message)
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
        //if (addressInput != null)
        //    addressInput.setEnsNodeNotSyncCallback(null); // prevent leak by removing reference to activity method
    }

    private void setupTokenContent()
    {
        amountInput = findViewById(R.id.input_amount);
        amountInput.setupToken(token, viewModel.getAssetDefinitionService(), viewModel.getTokenService(), this);
        addressInput = findViewById(R.id.input_address);
        addressInput.setAddressCallback(this);
        addressInput.setChainOverrideForWalletConnect(token.tokenInfo.chainId);
        //addressInput.setEnsHandlerNodeSyncFlag(true);   // allow node sync
        //addressInput.setEnsNodeNotSyncCallback(this::showNodeNotSyncSheet);  // callback to invoke if node not synced
        FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);
        functionBar.revealButtons();
        List<Integer> functions = new ArrayList<>(Collections.singletonList(R.string.action_next));
        functionBar.setupFunctions(this, functions);
        viewModel.startGasCycle(token.tokenInfo.chainId);
    }

    @Override
    public void amountReady(BigDecimal value, BigDecimal gasPrice)
    {
        //validate that we have sufficient balance
        if ((token.isEthereum() && token.balance.subtract(value).compareTo(BigDecimal.ZERO) > 0) // if sending base ethereum then check we have more than just the value
                || (token.getBalanceRaw().subtract(value).compareTo(BigDecimal.ZERO) >= 0)) // contract token, check sufficient token balance (gas widget will check sufficient gas)
        {
            sendAmount = value;
            sendGasPrice = gasPrice;
            calculateTransactionCost();
        }
        else
        {
            sendAmount = NEGATIVE;
            //insufficient balance
            amountInput.showError(true, 0);
            //if currently resolving ENS, stop
            addressInput.stopNameCheck();
        }
    }

    @Override
    public void handleClick(String action, int actionId)
    {
        //clicked the next button
        if (actionId == R.string.action_next)
        {
            KeyboardUtils.hideKeyboard(getCurrentFocus());
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
        Timber.w(throwable);
        checkConfirm(BigInteger.ZERO, transactionBytes, txSendAddress, resolvedAddress);
    }

    /**
     * Called to check if we're ready to send user to confirm screen / activity sheet popup
     */
    private void checkConfirm(final BigInteger sendGasLimit, final byte[] transactionBytes, final String txSendAddress, final String resolvedAddress)
    {
        BigInteger ethValue = token.isEthereum() ? sendAmount.toBigInteger() : BigInteger.ZERO;
        long leafCode = amountInput.isSendAll() ? -2 : -1;
        Web3Transaction w3tx = new Web3Transaction(
                new Address(txSendAddress),
                new Address(token.getAddress()),
                ethValue,
                sendGasPrice.toBigInteger(),
                sendGasLimit,
                -1,
                Numeric.toHexString(transactionBytes),
                leafCode);

        if (sendGasLimit.equals(BigInteger.ZERO))
        {
            estimateError(w3tx, transactionBytes, txSendAddress, resolvedAddress);
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
        viewModel.sendTransaction(finalTx, wallet, token.tokenInfo.chainId);
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
    }

    //Transaction failed to be sent
    private void txError(Throwable throwable)
    {
        Timber.d("txError: %s", throwable.getMessage());
        if (throwable instanceof SocketTimeoutException)
        {
            showTxnTimeoutDialog();
        }
        else
        {
            showTxnErrorDialog(throwable);
        }
        confirmationDialog.dismiss();
    }

    private void estimateError(final Web3Transaction w3tx, final byte[] transactionBytes, final String txSendAddress, final String resolvedAddress)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(WARNING);
        dialog.setTitle(R.string.confirm_transaction);
        dialog.setMessage(R.string.error_transaction_may_fail);
        dialog.setButtonText(R.string.button_ok);
        dialog.setSecondaryButtonText(R.string.action_cancel);
        dialog.setButtonListener(v -> {
            BigInteger gasEstimate = GasService.getDefaultGasLimit(token, w3tx);
            checkConfirm(gasEstimate, transactionBytes, txSendAddress, resolvedAddress);
        });

        dialog.setSecondaryButtonListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    void showNodeNotSyncSheet()
    {
        Timber.d("showNodeNotSync: ");
        try
        {
            if (alertDialog != null && alertDialog.isShowing()) alertDialog.dismiss();
            alertDialog = new AWalletAlertDialog(this, R.drawable.ic_warning);
            alertDialog.setTitle(R.string.title_ens_lookup_warning);
            alertDialog.setMessage(R.string.message_ens_node_not_sync);
            alertDialog.setButtonText(R.string.action_cancel);
            alertDialog.setButtonListener(v -> alertDialog.dismiss());
            alertDialog.setSecondaryButtonText(R.string.ignore);
            alertDialog.setSecondaryButtonListener(v -> {
                //addressInput.setEnsHandlerNodeSyncFlag(false);  // skip node sync check
                // re enter current input to resolve again
                String currentInput = addressInput.getEditText().getText().toString();
                addressInput.getEditText().setText("");
                addressInput.getEditText().setText(currentInput);
                addressInput.getEditText().setSelection(currentInput.length());
                alertDialog.dismiss();
            });
            alertDialog.show();
        }
        catch (Exception e)
        {
            Timber.e(e);
        }
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
}
