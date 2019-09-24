package com.alphawallet.app.ui;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;

import com.alphawallet.app.entity.ENSCallback;
import com.alphawallet.app.entity.ERC721Token;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.FinishReceiver;
import com.alphawallet.app.entity.PinAuthenticationCallbackInterface;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Ticket;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.ui.widget.OnTokenClickListener;
import com.alphawallet.app.ui.widget.adapter.AutoCompleteUrlAdapter;
import com.alphawallet.app.ui.widget.adapter.NonFungibleTokenAdapter;
import com.alphawallet.app.ui.widget.entity.ENSHandler;
import com.alphawallet.app.ui.widget.entity.ItemClickListener;
import com.alphawallet.app.ui.zxing.FullScannerFragment;
import com.alphawallet.app.ui.zxing.QRScanningActivity;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.util.QRURLParser;

import dagger.android.AndroidInjection;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.router.HomeRouter;
import com.alphawallet.app.viewmodel.TransferTicketDetailViewModel;
import com.alphawallet.app.viewmodel.TransferTicketDetailViewModelFactory;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.AWalletConfirmationDialog;
import com.alphawallet.app.widget.ProgressView;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.alphawallet.app.widget.SystemView;

import org.web3j.abi.datatypes.Address;

import javax.inject.Inject;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.alphawallet.app.C.*;
import static com.alphawallet.app.C.Key.TICKET;
import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.entity.Operation.SIGN_DATA;
import static com.alphawallet.app.widget.AWalletAlertDialog.ERROR;

/**
 * Created by James on 21/02/2018.
 */

public class TransferTicketDetailActivity extends BaseActivity implements Runnable, ItemClickListener, OnTokenClickListener
{
    private static final int BARCODE_READER_REQUEST_CODE = 1;
    private static final int SEND_INTENT_REQUEST_CODE = 2;
    private static final int CHOOSE_QUANTITY = 0;
    private static final int PICK_TRANSFER_METHOD = 1;
    private static final int TRANSFER_USING_LINK = 2;
    public  static final int TRANSFER_TO_ADDRESS = 3;

    @Inject
    protected TransferTicketDetailViewModelFactory viewModelFactory;
    protected TransferTicketDetailViewModel viewModel;
    private SystemView systemView;
    private ProgressView progressView;
    private AWalletAlertDialog dialog;

    private FinishReceiver finishReceiver;

    private Token token;
    private NonFungibleTokenAdapter adapter;

    private TextView titleText;
    private TextView toAddressError;
    private TextView validUntil;
    private AutoCompleteTextView toAddressEditText;
    private ImageButton qrImageView;
    private TextView textQuantity;

    private String ticketIds;
    private String prunedIds;
    private int transferStatus;

    private ENSHandler ensHandler;
    private Handler handler;

    private AWalletConfirmationDialog confirmationDialog;

    private AppCompatRadioButton pickLink;
    private AppCompatRadioButton pickTransfer;

    private LinearLayout pickTransferAddress;
    private LinearLayout pickTicketQuantity;
    private LinearLayout pickTransferMethod;
    private LinearLayout pickExpiryDate;
    private LinearLayout buttonLinkPick;
    private LinearLayout buttonTransferPick;

    private EditText expiryDateEditText;
    private EditText expiryTimeEditText;
    private DatePickerDialog datePickerDialog;
    private TimePickerDialog timePickerDialog;

    private PinAuthenticationCallbackInterface authInterface;
    private SignAuthenticationCallback signCallback;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_detail);

        token = getIntent().getParcelableExtra(TICKET);
        handler = new Handler();

        Wallet wallet = getIntent().getParcelableExtra(WALLET);
        ticketIds = getIntent().getStringExtra(EXTRA_TOKENID_LIST);
        transferStatus = getIntent().getIntExtra(EXTRA_STATE, 0);
        prunedIds = ticketIds;

        toolbar();
        setTitle(getString(R.string.empty));
        systemView = findViewById(R.id.system_view);
        systemView.hide();
        progressView = findViewById(R.id.progress_view);
        progressView.hide();

        toAddressEditText = findViewById(R.id.edit_to_address);

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(TransferTicketDetailViewModel.class);
        viewModel.setWallet(wallet);
        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);
        viewModel.newTransaction().observe(this, this::onTransaction);
        viewModel.error().observe(this, this::onError);
        viewModel.universalLinkReady().observe(this, this::linkReady);
        viewModel.userTransaction().observe(this, this::onUserTransaction);

        //we should import a token and a list of chosen ids
        RecyclerView list = findViewById(R.id.listTickets);
        adapter = new NonFungibleTokenAdapter(this, token, ticketIds, viewModel.getAssetDefinitionService(), null);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        textQuantity = findViewById(R.id.text_quantity);
        titleText = findViewById(R.id.title_transfer);
        validUntil = findViewById(R.id.text_valid_until);
        toAddressError = findViewById(R.id.to_address_error);

        pickTransferAddress = findViewById(R.id.layout_addressbar);
        pickTicketQuantity = findViewById(R.id.layout_ticket_quantity);
        pickTransferMethod = findViewById(R.id.layout_choose_method);
        pickExpiryDate = findViewById(R.id.layout_date_picker);

        setupAddressEditField();

        expiryDateEditText = findViewById(R.id.edit_expiry_date);
        expiryDateEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            @SuppressLint("StringFormatInvalid")
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validUntil.setText(getString(R.string.link_valid_until, s.toString(), expiryTimeEditText.getText().toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        expiryTimeEditText = findViewById(R.id.edit_expiry_time);
        expiryTimeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @SuppressLint("StringFormatInvalid")
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validUntil.setText(getString(R.string.link_valid_until, expiryDateEditText.getText().toString(), s.toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        pickLink = findViewById(R.id.radio_pickup_link);
        pickTransfer = findViewById(R.id.radio_transfer_now);

        buttonLinkPick = findViewById(R.id.layout_link_pick);
        buttonTransferPick = findViewById(R.id.layout_transfer_now);

        Button nextAction = findViewById(R.id.button_next);
        nextAction.setOnClickListener((View v) -> {
            int nextState = getNextState();
            if (nextState >= 0)
            {
                viewModel.openTransferState(this, token, prunedIds, nextState);
            }
        });

        qrImageView = findViewById(R.id.img_scan_qr);
        qrImageView.setOnClickListener(view -> {
            Intent intent = new Intent(this, QRScanningActivity.class);
            startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
        });

        setupScreen();

        finishReceiver = new FinishReceiver(this);
    }

    private void setupAddressEditField()
    {
        AutoCompleteUrlAdapter adapterUrl = new AutoCompleteUrlAdapter(getApplicationContext(), C.ENS_HISTORY);
        adapterUrl.setListener(this);
        ENSCallback ensCallback = new ENSCallback()
        {
            @Override
            public void ENSComplete()
            {
                confirmTransfer();
            }

            @Override
            public void ENSCheck(String name)
            {
                viewModel.checkENSAddress(token.tokenInfo.chainId, name);
            }
        };
        ensHandler = new ENSHandler(this, handler, adapterUrl, this, ensCallback);
        viewModel.ensResolve().observe(this, ensHandler::onENSSuccess);
        viewModel.ensFail().observe(this, ensHandler::hideENS);
    }

    //TODO: This is repeated code also in SellDetailActivity. Probably should be abstracted out into generic view code routine
    private void initQuantitySelector() {
        pickTicketQuantity.setVisibility(View.VISIBLE);
        RelativeLayout plusButton = findViewById(R.id.layout_quantity_add);
        plusButton.setOnClickListener(v -> {
            int quantity = Integer.parseInt(textQuantity.getText().toString());
            if ((quantity + 1) <= adapter.getTicketRangeCount()) {
                quantity++;
                textQuantity.setText(String.valueOf(quantity));
                prunedIds = ((Ticket)token).pruneIDList(ticketIds, quantity);
            }
        });

        RelativeLayout minusButton = findViewById(R.id.layout_quantity_minus);
        minusButton.setOnClickListener(v -> {
            int quantity = Integer.parseInt(textQuantity.getText().toString());
            if ((quantity - 1) >= 0) {
                quantity--;
                textQuantity.setText(String.valueOf(quantity));
                prunedIds = ((Ticket)token).pruneIDList(ticketIds, quantity);
            }
        });

        textQuantity.setText("1");
        prunedIds = ((Ticket)token).pruneIDList(ticketIds, 1);
    }

    private void setupRadioButtons()
    {
        buttonLinkPick.setSelected(true);
        buttonLinkPick.setOnClickListener((View v) -> {
            pickLink.setChecked(true);
            pickTransfer.setChecked(false);
            buttonLinkPick.setSelected(true);
            buttonTransferPick.setSelected(false);
        });

        buttonTransferPick.setOnClickListener((View v) -> {
            pickLink.setChecked(false);
            pickTransfer.setChecked(true);
            buttonLinkPick.setSelected(false);
            buttonTransferPick.setSelected(true);
        });

        pickLink.setOnClickListener((View v) -> {
            pickLink.setChecked(true);
            pickTransfer.setChecked(false);
            buttonLinkPick.setSelected(true);
            buttonTransferPick.setSelected(false);
        });

        pickTransfer.setOnClickListener((View v) -> {
            pickLink.setChecked(false);
            pickTransfer.setChecked(true);
            buttonLinkPick.setSelected(false);
            buttonTransferPick.setSelected(true);
        });
    }

    private int getNextState()
    {
        int newState = -1;

        switch (transferStatus)
        {
            case CHOOSE_QUANTITY:
                newState = PICK_TRANSFER_METHOD;
                break;
            case PICK_TRANSFER_METHOD:
                if (pickTransfer.isChecked())
                {
                    newState = TRANSFER_TO_ADDRESS;
                }
                else
                {
                    newState = TRANSFER_USING_LINK;
                }
                break;
            case TRANSFER_USING_LINK:
                //generate link
                getAuthenticationForLinkGeneration();
                break;
            case TRANSFER_TO_ADDRESS:
                //transfer using eth
                confirmTransfer();
                break;
        }

        return newState;
    }

    private void getAuthenticationForLinkGeneration()
    {
        signCallback = new SignAuthenticationCallback()
        {
            @Override
            public void GotAuthorisation(boolean gotAuth)
            {
                if (gotAuth && authInterface != null) authInterface.CompleteAuthentication(SIGN_DATA);
                else if (!gotAuth && authInterface != null) authInterface.FailedAuthentication(SIGN_DATA);

                if (gotAuth) viewModel.generateUniversalLink(token.getTicketIndices(ticketIds), token.getAddress(), calculateExpiryTime());
            }

            @Override
            public void setupAuthenticationCallback(PinAuthenticationCallbackInterface authCallback)
            {
                authInterface = authCallback;
            }
        };

        viewModel.getAuthorisation(this, signCallback);
    }

    private long calculateExpiryTime()
    {
        String expiryDate = expiryDateEditText.getText().toString();
        String expiryTime = expiryTimeEditText.getText().toString();
        String tempDateString = expiryDate + " " + expiryTime;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Date date;
        String dateString = "";
        long UTCTimeStamp = 0;
        try
        {
            date = simpleDateFormat.parse(tempDateString);
            dateString = simpleDateFormat.format(date);
            Log.d(SellDetailActivity.class.getSimpleName(), "date : " + dateString);
            UTCTimeStamp = (date.getTime()) / 1000;
        }
        catch (ParseException e)
        {
            Log.e(SellDetailActivity.class.getSimpleName(), e.getMessage(), e);
        }

        return UTCTimeStamp;
    }

    private void setupScreen()
    {
        pickTransferAddress.setVisibility(View.GONE);
        pickTicketQuantity.setVisibility(View.GONE);
        pickTransferMethod.setVisibility(View.GONE);
        pickExpiryDate.setVisibility(View.GONE);

        switch (transferStatus)
        {
            case CHOOSE_QUANTITY:
                initQuantitySelector();
                pickTicketQuantity.setVisibility(View.VISIBLE);
                String typeName = viewModel.getAssetDefinitionService().getTokenName(token.tokenInfo.chainId, token.tokenInfo.address, 1);
                titleText.setText(getString(R.string.title_select_ticket_quantity, typeName != null ? typeName : getString(R.string.ticket)));
                break;
            case PICK_TRANSFER_METHOD:
                setupRadioButtons();
                pickTransferMethod.setVisibility(View.VISIBLE);
                titleText.setText(R.string.title_select_transfer_method);
                break;
            case TRANSFER_USING_LINK:
                initDatePicker();
                initTimePicker();
                expiryDateEditText.setOnClickListener(v -> datePickerDialog.show());
                expiryTimeEditText.setOnClickListener(v -> timePickerDialog.show());
                pickExpiryDate.setVisibility(View.VISIBLE);
                titleText.setText(R.string.title_set_universal_link_expiry);
                break;
            case TRANSFER_TO_ADDRESS:
                pickTransferAddress.setVisibility(View.VISIBLE);
                titleText.setText(R.string.title_input_wallet_address);
                break;
        }
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

    private void onUserTransaction(String hash)
    {
        hideDialog();
        dialog = new AWalletAlertDialog(this);
        dialog.setTitle(R.string.transaction_succeeded);
        dialog.setMessage(hash);
        dialog.setButtonText(R.string.copy);
        dialog.setButtonListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("transaction hash", hash);
            clipboard.setPrimaryClip(clip);
            dialog.dismiss();
            sendBroadcast(new Intent(PRUNE_ACTIVITY));
        });
        dialog.setOnDismissListener(v -> {
            dialog.dismiss();
            sendBroadcast(new Intent(PRUNE_ACTIVITY));
            new HomeRouter().open(this, true);
            finish();
        });
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

    private void onProgress(boolean shouldShowProgress)
    {
        hideDialog();
        if (shouldShowProgress)
        {
            dialog = new AWalletAlertDialog(this);
            dialog.setIcon(AWalletAlertDialog.NONE);
            dialog.setTitle(R.string.title_dialog_sending);
            dialog.setMessage(R.string.transfer);
            dialog.setProgressMode();
            dialog.setCancelable(false);
            dialog.show();
        }
    }

    private void onENSProgress(boolean shouldShowProgress)
    {
        hideDialog();
        if (shouldShowProgress)
        {
            dialog = new AWalletAlertDialog(this);
            dialog.setIcon(AWalletAlertDialog.NONE);
            dialog.setTitle(R.string.title_dialog_check_ens);
            dialog.setProgressMode();
            dialog.setCancelable(false);
            dialog.show();
        }
    }

    private void onError(ErrorEnvelope error)
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

    private void transferTicketFinal()
    {
        //complete the transfer
        String to = ensHandler.getAddressFromEditView();
        if (to == null) return;

        onProgress(true);

        viewModel.createTicketTransfer(
                to,
                token,
                token.integerListToString(token.ticketIdStringToIndexList(prunedIds), true));
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        viewModel.prepare(token);
        KeyboardUtils.hideKeyboard(toAddressEditText);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(finishReceiver);
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
            case BARCODE_READER_REQUEST_CODE:
                switch (resultCode)
                {
                    case FullScannerFragment.SUCCESS:
                        if (data != null)
                        {
                            String barcode = data.getStringExtra(FullScannerFragment.BarcodeObject);

                            //if barcode is still null, ensure we don't GPF
                            if (barcode == null)
                            {
                                Toast.makeText(this, R.string.toast_qr_code_no_address, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            QRURLParser parser = QRURLParser.getInstance();
                            String extracted_address = parser.extractAddressFromQrString(barcode);
                            if (extracted_address == null)
                            {
                                Toast.makeText(this, R.string.toast_qr_code_no_address, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            toAddressEditText.setText(extracted_address);
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
                break;

            case SEND_INTENT_REQUEST_CODE:
                sendBroadcast(new Intent(PRUNE_ACTIVITY));
                break;

            case SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS:
                signCallback.GotAuthorisation(resultCode == RESULT_OK);
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

    private void linkReady(String universalLink)
    {
        int quantity = token.ticketIdStringToIndexList(prunedIds).size();
        int ticketName = (quantity > 1) ? R.string.tickets : R.string.ticket;
        String qty = String.valueOf(quantity) + " " +
                getResources().getString(ticketName) + "\n" +
                getString(R.string.universal_link_expiry_on) + expiryDateEditText.getText().toString() + " " + expiryTimeEditText.getText().toString();

        confirmationDialog = new AWalletConfirmationDialog(this);
        confirmationDialog.setTitle(R.string.generate_pick_up_link);
        confirmationDialog.setSmallText(R.string.generate_free_transfer_link);
        confirmationDialog.setMediumText(qty);
        confirmationDialog.setPrimaryButtonText(R.string.send_universal_link);
        confirmationDialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        confirmationDialog.setPrimaryButtonListener(v1 -> transferLinkFinal(universalLink));
        confirmationDialog.setSecondaryButtonListener(v1 -> confirmationDialog.dismiss());
        confirmationDialog.showShareLink();
        confirmationDialog.show();
    }

    private void confirmTransfer()
    {
        //complete the transfer
        toAddressEditText.dismissDropDown();
        String to = ensHandler.getAddressFromEditView();
        if (to == null) return;

        if (token instanceof ERC721Token)
        {
            viewModel.openConfirm(this, to, token, ticketIds, ensHandler.getEnsName());
        }
        else
        {
            handleERC875Transfer(to);
        }
    }

    private void handleERC875Transfer(final String to)
    {
        //how many indices are we selling?
        int quantity = token.stringHexToBigIntegerList(prunedIds).size();
        int ticketName = (quantity > 1) ? R.string.tickets : R.string.ticket;

        String toAddress = (ensHandler.getEnsName() == null) ? to : ensHandler.getEnsName();

        String qty = String.valueOf(quantity) + " " +
                getResources().getString(ticketName) + "\n" +
                getResources().getString(R.string.to) + " " +
                toAddress;

        signCallback = new SignAuthenticationCallback()
        {
            @Override
            public void GotAuthorisation(boolean gotAuth)
            {
                if (gotAuth && authInterface != null) authInterface.CompleteAuthentication(SIGN_DATA);
                else if (!gotAuth && authInterface != null) authInterface.FailedAuthentication(SIGN_DATA);

                if (gotAuth) transferTicketFinal();
            }

            @Override
            public void setupAuthenticationCallback(PinAuthenticationCallbackInterface authCallback)
            {
                authInterface = authCallback;
            }
        };

        confirmationDialog = new AWalletConfirmationDialog(this);
        confirmationDialog.setTitle(R.string.title_transaction_details);
        confirmationDialog.setSmallText(R.string.confirm_transfer_details);
        confirmationDialog.setMediumText(qty);
        confirmationDialog.setPrimaryButtonText(R.string.transfer_tickets);
        confirmationDialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        confirmationDialog.setPrimaryButtonListener(v1 -> viewModel.getAuthorisation(this, signCallback));
        confirmationDialog.setSecondaryButtonListener(v1 -> confirmationDialog.dismiss());
        confirmationDialog.show();
    }

    private void transferLinkFinal(String universalLink)
    {
        //create share intent
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, universalLink);
        sendIntent.setType("text/plain");
        startActivityForResult(sendIntent, SEND_INTENT_REQUEST_CODE);
    }

    private boolean isAddressValid(String address)
    {
        try
        {
            new Address(address);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private void initDatePicker()
    {
        String dateFormat = "dd/MM/yyyy";
        Calendar newCalendar = Calendar.getInstance();
        SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat, Locale.ENGLISH);

        datePickerDialog = new DatePickerDialog(this, (view, year, monthOfYear, dayOfMonth) -> {
            Calendar newDate = Calendar.getInstance();
            newDate.set(year, monthOfYear, dayOfMonth);
            expiryDateEditText.setText(dateFormatter.format(newDate.getTime()));
        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

        //set default for tomorrow
        long tomorrowStamp = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
        Date tomorrow = new Date(tomorrowStamp);
        expiryDateEditText.setText(dateFormatter.format(tomorrow.getTime()));
    }

    private void initTimePicker()
    {
        Calendar newCalendar = Calendar.getInstance();
        timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            expiryTimeEditText.setText(time);
        }, newCalendar.get(Calendar.HOUR_OF_DAY), newCalendar.get(Calendar.MINUTE), true);

        //set for now
        String time = String.format(Locale.getDefault(), "%02d:%02d", Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                                    Calendar.getInstance().get(Calendar.MINUTE));
        expiryTimeEditText.setText(time);
    }

    @Override
    public void run()
    {
        ensHandler.checkENS();
    }

    @Override
    public void onItemClick(String url)
    {
        ensHandler.handleHistoryItemClick(url);
    }
}

