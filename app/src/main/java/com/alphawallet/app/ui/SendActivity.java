package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
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
import com.alphawallet.app.service.GasService2;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.ui.widget.entity.AddressReadyCallback;
import com.alphawallet.app.ui.widget.entity.AmountReadyCallback;
import com.alphawallet.app.ui.zxing.FullScannerFragment;
import com.alphawallet.app.ui.zxing.QRScanningActivity;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.util.QRParser;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.SendViewModel;
import com.alphawallet.app.viewmodel.SendViewModelFactory;
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

import org.web3j.protocol.core.methods.response.EthEstimateGas;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.alphawallet.app.C.GAS_LIMIT_MIN;
import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.repository.EthereumNetworkBase.hasGasOverride;
import static com.alphawallet.app.widget.AWalletAlertDialog.ERROR;
import static com.alphawallet.app.widget.AWalletAlertDialog.WARNING;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

public class SendActivity extends BaseActivity implements AmountReadyCallback, StandardFunctionInterface, AddressReadyCallback, ActionSheetCallback
{
    private static final BigDecimal NEGATIVE = BigDecimal.ZERO.subtract(BigDecimal.ONE);

    @Inject
    SendViewModelFactory sendViewModelFactory;
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

    @Nullable
    private Disposable calcGasCost;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        toolbar();

        viewModel = new ViewModelProvider(this, sendViewModelFactory)
                .get(SendViewModel.class);

        String contractAddress = getIntent().getStringExtra(C.EXTRA_CONTRACT_ADDRESS);
        wallet = getIntent().getParcelableExtra(WALLET);
        token = getIntent().getParcelableExtra(C.EXTRA_TOKEN_ID);
        QRResult result = getIntent().getParcelableExtra(C.EXTRA_AMOUNT);
        int currentChain = getIntent().getIntExtra(C.EXTRA_NETWORKID, MAINNET_ID);
        viewModel.transactionFinalised().observe(this, this::txWritten);
        viewModel.transactionError().observe(this, this::txError);

        sendAddress = null;
        sendGasPrice = BigDecimal.ZERO;
        sendAmount = NEGATIVE;

        if (!checkTokenValidity(currentChain, contractAddress)) { return; }

        setTitle(getString(R.string.action_send_tkn, token.getShortName()));
        setupTokenContent();

        if (result != null)
        {
            //restore payment request
            validateEIP681Request(result, true);
        }
    }

    private boolean checkTokenValidity(int currentChain, String contractAddress)
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

    private void onBack() {
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            onBack();
        }
        else if (item.getItemId() == R.id.action_qr)
        {
            viewModel.showContractInfo(this, wallet, token);
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        onBack();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Operation taskCode = null;
        if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10) {
            taskCode = Operation.values()[requestCode - SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS];
            requestCode = SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS;
        }

        if (requestCode == C.SET_GAS_SETTINGS)
        {
            //will either be an index, or if using custom then it will contain a price and limit
            if (data != null && confirmationDialog != null)
            {
                int gasSelectionIndex = data.getIntExtra(C.EXTRA_SINGLE_ITEM, -1);
                long customNonce = data.getLongExtra(C.EXTRA_NONCE, -1);
                BigDecimal customGasPrice = data.hasExtra(C.EXTRA_GAS_PRICE) ?
                        new BigDecimal(data.getStringExtra(C.EXTRA_GAS_PRICE)) : BigDecimal.ZERO; //may not have set a custom gas price
                BigDecimal customGasLimit = new BigDecimal(data.getStringExtra(C.EXTRA_GAS_LIMIT));
                long expectedTxTime = data.getLongExtra(C.EXTRA_AMOUNT, 0);
                confirmationDialog.setCurrentGasIndex(gasSelectionIndex, customGasPrice, customGasLimit, expectedTxTime, customNonce);
            }
        }
        else if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
        {
            if (confirmationDialog != null && confirmationDialog.isShowing()) confirmationDialog.completeSignRequest(resultCode == RESULT_OK);
        }
        else if (requestCode == C.BARCODE_READER_REQUEST_CODE) {
            switch (resultCode)
            {
                case FullScannerFragment.SUCCESS:
                    if (data != null) {
                        String qrCode = data.getStringExtra(FullScannerFragment.BarcodeObject);

                        //if barcode is still null, ensure we don't GPF
                        if (qrCode == null)
                        {
                            //Toast.makeText(this, R.string.toast_qr_code_no_address, Toast.LENGTH_SHORT).show();
                            displayScanError();
                            return;
                        }
                        else if (qrCode.startsWith("wc:"))
                        {
                            startWalletConnect(qrCode);
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
                case QRScanningActivity.DENY_PERMISSION:
                    showCameraDenied();
                    break;
                default:
                    Log.e("SEND", String.format(getString(R.string.barcode_error_format),
                                                "Code: " + String.valueOf(resultCode)
                    ));
                    break;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
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
                if (result.functionToAddress != null) addressInput.setAddress(result.functionToAddress);
                break;

            default:
                displayScanError();
        }
    }

    private void showChainChangeDialog(int chainId)
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
    protected void onDestroy() {
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

    private void setupTokenContent() {
        amountInput = findViewById(R.id.input_amount);
        amountInput.setupToken(token, viewModel.getAssetDefinitionService(), viewModel.getTokenService(), this);
        addressInput = findViewById(R.id.input_address);
        addressInput.setAddressCallback(this);
        addressInput.setChainOverrideForWalletConnect(token.tokenInfo.chainId);
        FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);
        functionBar.revealButtons();
        List<Integer> functions = new ArrayList<>(Collections.singletonList(R.string.action_next));
        functionBar.setupFunctions(this, functions);
        viewModel.startGasCycle(token.tokenInfo.chainId);
    }

    @Override
    public void amountReady(BigDecimal value, BigDecimal gasPrice)
    {
        Token base = viewModel.getToken(token.tokenInfo.chainId, wallet.address);
        //validate that we have sufficient balance
        if ((token.isEthereum() && token.balance.subtract(value).compareTo(BigDecimal.ZERO) > 0) // if sending base ethereum then check we have more than just the value
             || (hasGasOverride(token.tokenInfo.chainId) && token.getBalanceRaw().subtract(value).compareTo(BigDecimal.ZERO) >= 0) //allow for chains with no gas requirement
             || (base.balance.compareTo(BigDecimal.ZERO) > 0 && token.getBalanceRaw().subtract(value).compareTo(BigDecimal.ZERO) >= 0)) // contract token, check gas and sufficient token balance
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

            if (token.isEthereum())
            {
                checkConfirm(BigInteger.valueOf(GAS_LIMIT_MIN), transactionBytes, txSendAddress, txSendAddress);
            }
            else
            {
                calculateEstimateDialog();
                //form payload and calculate tx cost
                calcGasCost = viewModel.calculateGasEstimate(wallet, transactionBytes, token.tokenInfo.chainId, token.getAddress(), BigDecimal.ZERO)
                        .map(this::convertToGasLimit)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(estimate -> checkConfirm(estimate, transactionBytes, token.getAddress(), txSendAddress),
                                error -> handleError(error, transactionBytes, token.getAddress(), txSendAddress));
            }
        }
    }

    private BigInteger convertToGasLimit(EthEstimateGas estimate)
    {
        if (estimate.hasError())
        {
            return BigInteger.ZERO;
        }
        else
        {
            return estimate.getAmountUsed();
        }
    }

    private void handleError(Throwable throwable, final byte[] transactionBytes, final String txSendAddress, final String resolvedAddress)
    {
        Log.w(this.getLocalClassName(), throwable.getMessage());
        checkConfirm(BigInteger.ZERO, transactionBytes, txSendAddress, resolvedAddress);
    }

    /**
     * Called to check if we're ready to send user to confirm screen / activity sheet popup
     *
     */
    private void checkConfirm(final BigInteger sendGasLimit, final byte[] transactionBytes, final String txSendAddress, final String resolvedAddress)
    {
        BigInteger ethValue = token.isEthereum() ? sendAmount.toBigInteger() : BigInteger.ZERO;
        Web3Transaction w3tx = new Web3Transaction(
                new Address(txSendAddress),
                new Address(token.getAddress()),
                ethValue,
                sendGasPrice.toBigInteger(),
                sendGasLimit,
                -1,
                Numeric.toHexString(transactionBytes),
                -1);

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
            intent.putExtra("tx_hash", txHash);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public void notifyConfirm(String mode)
    {
        viewModel.actionSheetConfirm(mode);
    }

    private void txWritten(TransactionData transactionData)
    {
        confirmationDialog.transactionWritten(transactionData.txHash);
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
            BigInteger gasEstimate = GasService2.getDefaultGasLimit(token, w3tx);
            checkConfirm(gasEstimate, transactionBytes, txSendAddress, resolvedAddress);
        });

        dialog.setSecondaryButtonListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }
}
