package com.alphawallet.app.ui;

import static com.alphawallet.app.widget.AWalletAlertDialog.ERROR;
import static com.alphawallet.app.widget.AWalletAlertDialog.WARNING;
import static org.web3j.crypto.WalletUtils.isValidAddress;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.ui.QRScanning.QRScanner;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.ui.widget.adapter.NonFungibleTokenAdapter;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.ui.widget.entity.AddressReadyCallback;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.util.QRParser;
import com.alphawallet.app.viewmodel.TransferTicketDetailViewModel;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.AWalletConfirmationDialog;
import com.alphawallet.app.widget.ActionSheetDialog;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.InputAddress;
import com.alphawallet.app.widget.ProgressView;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.alphawallet.app.widget.SystemView;
import com.alphawallet.token.tools.Numeric;

import org.jetbrains.annotations.NotNull;
import org.web3j.protocol.core.methods.response.EthEstimateGas;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by JB on 11/08/2021
 */
@AndroidEntryPoint
public class TransferNFTActivity extends BaseActivity implements TokensAdapterCallback, StandardFunctionInterface, AddressReadyCallback, ActionSheetCallback
{
    protected TransferTicketDetailViewModel viewModel;
    private AWalletAlertDialog dialog;

    private Token token;
    private ArrayList<Pair<BigInteger, NFTAsset>> assetSelection;

    private InputAddress addressInput;
    private String sendAddress;
    private String ensAddress;

    private ActionSheetDialog actionDialog;
    private AWalletConfirmationDialog confirmationDialog;

    @Nullable
    private Disposable calcGasCost;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_nft);
        viewModel = new ViewModelProvider(this)
                .get(TransferTicketDetailViewModel.class);

        long chainId = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID);
        token = viewModel.getTokenService().getToken(chainId, getIntent().getStringExtra(C.EXTRA_ADDRESS));

        String tokenIds = getIntent().getStringExtra(C.EXTRA_TOKENID_LIST);
        List<BigInteger> tokenIdList = token.stringHexToBigIntegerList(tokenIds);
        List<NFTAsset> assets = getIntent().getParcelableArrayListExtra(C.EXTRA_NFTASSET_LIST);
        assetSelection = formAssetSelection(tokenIdList, assets);

        toolbar();
        SystemView systemView = findViewById(R.id.system_view);
        systemView.hide();
        ProgressView progressView = findViewById(R.id.progress_view);
        progressView.hide();

        addressInput = findViewById(R.id.input_address);
        addressInput.setAddressCallback(this);

        sendAddress = null;
        ensAddress = null;

        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);
        viewModel.newTransaction().observe(this, this::onTransaction);
        viewModel.error().observe(this, this::onError);
        viewModel.transactionFinalised().observe(this, this::txWritten);
        viewModel.transactionError().observe(this, this::txError);
        //we should import a token and a list of chosen ids
        RecyclerView list = findViewById(R.id.listTickets);

        NonFungibleTokenAdapter adapter = new NonFungibleTokenAdapter(null, token, assetSelection, viewModel.getAssetDefinitionService());
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        final FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);

        functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.action_transfer)));
        functionBar.revealButtons();

        setupScreen();
    }

    private void setupScreen()
    {
        addressInput.setVisibility(View.GONE);
        addressInput.setVisibility(View.VISIBLE);
        setTitle(getString(R.string.send_tokens));
    }

    private void onTransaction(String success)
    {
        hideDialog();
        dialog = new AWalletAlertDialog(this);
        dialog.setTitle(R.string.transaction_succeeded);
        dialog.setMessage(success);
        dialog.setIcon(AWalletAlertDialog.SUCCESS);
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v -> finish());

        dialog.show();
    }

    private void hideDialog()
    {
        if (dialog != null && dialog.isShowing())
        {
            dialog.dismiss();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        viewModel.resetSignDialog();
    }

    private void onError(@NotNull ErrorEnvelope error)
    {
        hideDialog();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.error_transaction_failed);
        dialog.setMessage(error.message);
        dialog.setCancelable(true);
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        viewModel.prepare(token);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        viewModel.stopGasSettingsFetch();
        if (confirmationDialog != null && confirmationDialog.isShowing())
        {
            confirmationDialog.dismiss();
            confirmationDialog = null;
        }
    }

    @Override
    public void onTokenClick(View view, Token token, List<BigInteger> ids, boolean selection) {
        Context context = view.getContext();
        //TODO: what action should be performed when clicking on a range?
    }

    @Override
    public void onLongTokenClick(View view, Token token, List<BigInteger> tokenId)
    {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
        {
            requestCode = SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS;
        }

        switch (requestCode)
        {
            case C.BARCODE_READER_REQUEST_CODE:
                switch (resultCode)
                {
                    case Activity.RESULT_OK:
                        if (data != null)
                        {
                            String barcode = data.getStringExtra(C.EXTRA_QR_CODE);

                            //if barcode is still null, ensure we don't GPF
                            if (barcode == null)
                            {
                                Toast.makeText(this, R.string.toast_qr_code_no_address, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            QRParser parser = QRParser.getInstance(EthereumNetworkBase.extraChains());
                            String extracted_address = parser.extractAddressFromQrString(barcode);
                            if (extracted_address == null)
                            {
                                Toast.makeText(this, R.string.toast_qr_code_no_address, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            addressInput.setAddress(extracted_address);
                        }
                        break;
                    case QRScanner.DENY_PERMISSION:
                        showCameraDenied();
                        break;
                    default:
                        Timber.tag("SEND").e(String.format(getString(R.string.barcode_error_format),
                                "Code: " + resultCode
                        ));
                        break;
                }
                break;

            case C.SET_GAS_SETTINGS:
                if (data != null && actionDialog != null)
                {
                    int gasSelectionIndex = data.getIntExtra(C.EXTRA_SINGLE_ITEM, -1);
                    long customNonce = data.getLongExtra(C.EXTRA_NONCE, -1);
                    BigDecimal customGasPrice = data.hasExtra(C.EXTRA_GAS_PRICE) ?
                            new BigDecimal(data.getStringExtra(C.EXTRA_GAS_PRICE)) : BigDecimal.ZERO; //may not have set a custom gas price
                    BigDecimal customGasLimit = new BigDecimal(data.getStringExtra(C.EXTRA_GAS_LIMIT));
                    long expectedTxTime = data.getLongExtra(C.EXTRA_AMOUNT, 0);
                    actionDialog.setCurrentGasIndex(gasSelectionIndex, customGasPrice, customGasLimit, expectedTxTime, customNonce);
                }
                break;
            case C.COMPLETED_TRANSACTION:
                Intent i = new Intent();
                i.putExtra(C.EXTRA_TXHASH, data.getStringExtra(C.EXTRA_TXHASH));
                setResult(RESULT_OK, new Intent());
                finish();
                break;
            case SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS:
                if (actionDialog != null && actionDialog.isShowing()) actionDialog.completeSignRequest(resultCode == RESULT_OK);
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void showCameraDenied()
    {
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

    @Override
    public void addressReady(String address, String ensName)
    {
        sendAddress = address;
        ensAddress = ensName;
        //complete the transfer
        if (TextUtils.isEmpty(address) || !isValidAddress(address))
        {
            //show address error
            addressInput.setError(getString(R.string.error_invalid_address));
        }
        else
        {
            calculateTransactionCost();
        }
    }

    @Override
    public void showTransferToken(List<BigInteger> selection)
    {
        KeyboardUtils.hideKeyboard(getCurrentFocus());
        addressInput.getAddress();
    }

    private void calculateTransactionCost()
    {
        if ((calcGasCost != null && !calcGasCost.isDisposed()) ||
                (actionDialog != null && actionDialog.isShowing())) return;

        final String txSendAddress = sendAddress;
        sendAddress = null;

        final byte[] transactionBytes = token.getTransferBytes(txSendAddress, assetSelection);

        calculateEstimateDialog();
        //form payload and calculate tx cost
        calcGasCost = viewModel.calculateGasEstimate(viewModel.getWallet(), transactionBytes, token.tokenInfo.chainId, token.getAddress(), BigDecimal.ZERO)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(estimate -> checkConfirm(estimate, transactionBytes, token.getAddress(), txSendAddress),
                        error -> handleError(error, transactionBytes, token.getAddress(), txSendAddress));
    }

    private void handleError(Throwable throwable, final byte[] transactionBytes, final String txSendAddress, final String resolvedAddress)
    {
        Timber.w(throwable.getMessage());
        checkConfirm(BigInteger.ZERO, transactionBytes, txSendAddress, resolvedAddress);
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

    /**
     * Called to check if we're ready to send user to confirm screen / activity sheet popup
     */
    private void checkConfirm(final BigInteger sendGasLimit, final byte[] transactionBytes, final String txSendAddress, final String resolvedAddress) {

        Web3Transaction w3tx = new Web3Transaction(
                new Address(txSendAddress),
                new Address(token.getAddress()),
                BigInteger.ZERO,
                BigInteger.ZERO,
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
            if (dialog != null && dialog.isShowing())
            {
                dialog.dismiss();
            }

            actionDialog = new ActionSheetDialog(this, w3tx, token, ensAddress,
                    resolvedAddress, viewModel.getTokenService(), this);
            actionDialog.setCanceledOnTouchOutside(false);
            actionDialog.show();
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
        viewModel.getAuthentication(this, viewModel.getWallet(), callback);
    }

    @Override
    public void sendTransaction(Web3Transaction finalTx)
    {
        viewModel.sendTransaction(finalTx, viewModel.getWallet(), token.tokenInfo.chainId);
    }

    @Override
    public void dismissed(String txHash, long callbackId, boolean actionCompleted)
    {
        //ActionSheet was dismissed
        if (!TextUtils.isEmpty(txHash)) {
            Intent intent = new Intent();
            intent.putExtra(C.EXTRA_TXHASH, txHash);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public void notifyConfirm(String mode) { viewModel.actionSheetConfirm(mode); }

    private void txWritten(TransactionData transactionData)
    {
        actionDialog.transactionWritten(transactionData.txHash);
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
        actionDialog.dismiss();
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

    private ArrayList<Pair<BigInteger, NFTAsset>> formAssetSelection(List<BigInteger> tokenIdList, List<NFTAsset> assets)
    {
        ArrayList<Pair<BigInteger, NFTAsset>> assetList = new ArrayList<>();
        if (tokenIdList.size() != assets.size())
        {
            if (BuildConfig.DEBUG) //warn developer of problem
            {
                throw new RuntimeException("Token ID list size != Asset size");
            }
            else if (assets.size() < tokenIdList.size())  //ensure below code doesn't crash
            {
                tokenIdList = tokenIdList.subList(0, assets.size());
            }
        }

        for (int i = 0; i < tokenIdList.size(); i++)
        {
            assetList.add(new Pair<>(tokenIdList.get(i), assets.get(i)));
        }

        return assetList;
    }
}
